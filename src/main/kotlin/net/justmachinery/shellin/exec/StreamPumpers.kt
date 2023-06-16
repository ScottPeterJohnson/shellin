package net.justmachinery.shellin.exec

import mu.KLogging
import net.justmachinery.futility.bytes.KiB
import net.justmachinery.futility.bytes.MiB
import net.justmachinery.shellin.AsyncSingleConcurrentExecution
import okio.*
import java.io.Closeable
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * A thread-efficient output pumper which will run on the executor pool only when it has
 * output to write.
 */
internal class OutputPumper(
    pumperPool : ExecutorService,
    private val output : Sink,
) : Closeable {
    companion object : KLogging()
    //Since nuprocess has no backpressure, we need to be very generous about taking input from it-
    //but if the output can't be pumped fast enough, we do have to stop at some point.
    private val pipe = Pipe(20L.MiB)
    private val availableBytes = AtomicLong()

    /**
     * Attempts to write from input into buffer. May block if pipe is full.
     */
    fun write(input : ByteBuffer){
        if(done.get()) {
            throw IllegalStateException("Pumper was closed")
        }
        val buf = Buffer()
        val written = buf.write(input)
        synchronized(this){
            availableBytes.addAndGet(written.toLong())
        }
        pump.run()
        pipe.sink.write(buf, buf.size)
        logger.trace { "Received ${written} bytes" }
    }

    private val pump = AsyncSingleConcurrentExecution(pumperPool) {
        while(true){
            val haveBytes = synchronized(this){
                val bytes = availableBytes.get()
                when {
                    bytes > 0 -> bytes
                    done.get() -> {
                        -1L
                    }
                    else -> 0L
                }
            }
            if(haveBytes > 0){
                val buf = Buffer()
                val bytesRead = pipe.source.read(buf, haveBytes)
                output.write(buf, bytesRead)
                synchronized(this){
                    availableBytes.addAndGet(-1 * bytesRead)
                }
                logger.trace { "Wrote $bytesRead bytes" }
                continue
            } else if (haveBytes == -1L){
                if(!closed.getAndSet(true)){
                    logger.trace { "Closing output" }
                    output.close()
                }
            }
            break
        }
    }

    private val done = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)

    override fun close() {
        logger.trace { "Was closed" }
        synchronized(this){
            done.set(true)
        }
        pump.run()
    }
}

/**
 * A thread-efficient input pumper that only runs a thread to pull from its input when its pipe isn't full.
 */
internal class InputPumper(
    pumperPool: ExecutorService,
    private val input: Source,
    private val onInputReady : ()->Unit,
    private val onInputDone: ()->Unit,
    private val bufferSize : Long = DEFAULT_BUFFER_SIZE
) : Closeable {
    companion object : KLogging() {
        val DEFAULT_BUFFER_SIZE = 256L.KiB
    }
    private val pipe = Pipe(Long.MAX_VALUE)
    private val usedBytes = AtomicLong()
    private var doneWithInput = AtomicBoolean(false)
    private var closedInput = AtomicBoolean(false)

    private var doneWithOutput = AtomicBoolean(false)
    private var closedOutput = AtomicBoolean(false)

    private val pump = AsyncSingleConcurrentExecution(pumperPool) {
        while(true){
            if(doneWithInput.get()){
                if(!closedInput.getAndSet(true)){
                    logger.trace { "Closing input" }
                    input.close()
                    pipe.sink.close()
                    onInputDone()
                }
                if(doneWithOutput.get() && !closedOutput.getAndSet(true)){
                    logger.trace { "Closing output" }
                    pipe.source.close()
                }
                break
            } else {
                val remaining = bufferSize - usedBytes.get()
                if(remaining != 0L){
                    val buf = Buffer()
                    val bytesRead = input.read(buf, remaining)
                    if(bytesRead != -1L){
                        logger.trace { "Writing ${buf.size} bytes" }
                        usedBytes.addAndGet(buf.size)
                        pipe.sink.write(buf, buf.size)
                        onInputReady()
                    } else {
                        doneWithInput.set(true)
                    }
                    continue
                } else {
                    break
                }
            }
        }
    }

    fun startPump() = pump.run()

    /**
     * Returns a buffer containing bytes that are ready, or null if the stream is exhausted
     */
    fun read() : Buffer? {
        if(doneWithOutput.get()){ throw IllegalStateException("Cannot read after close") }
        val buf = Buffer()
        return if(pipe.source.read(buf, Long.MAX_VALUE) != -1L){
            logger.trace { "Read ${buf.size} bytes" }
            usedBytes.addAndGet(-1 * buf.size)
            pump.run()
            buf
        } else {
            null
        }

    }

    override fun close() {
        logger.trace { "Closing" }
        doneWithInput.set(true)
        doneWithOutput.set(true)
        pump.run()
    }
}


