package net.justmachinery.shellin.exec

import mu.KLogging
import net.justmachinery.shellin.AsyncSingleConcurrentExecution
import okio.Buffer
import okio.Pipe
import okio.Sink
import okio.Source
import java.io.Closeable
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

/**
 * A thread-efficient output pumper which will run on the executor pool only when it has
 * output to write.
 */
internal class OutputPumper(
    pumperPool : ExecutorService,
    private val output : Sink,
    private val bufferSize : Long = 131_072
) : Closeable {
    companion object : KLogging()
    //The OKIO pipe's limit blocks when reached, so we can't use it.
    private val pipe = Pipe(Long.MAX_VALUE)
    private val usedBytes = AtomicLong()

    /**
     * Attempts to write from input into buffer. May not use all of input if pipe is full.
     */
    fun write(input : ByteBuffer, closing : Boolean){
        if(done.get()) {
            throw IllegalStateException("Pumper was closed")
        }
        val writeAmount = if(closing) input.remaining() else min(input.remaining(), (bufferSize - usedBytes.get()).toInt())
        if(writeAmount > 0){
            val buf = Buffer()
            val end = input.position() + writeAmount
            val written = buf.write(input.slice().limit(end))
            input.position(end)
            pipe.sink.write(buf, buf.size)
            synchronized(this){
                usedBytes.addAndGet(written.toLong())
            }
            logger.trace { "Received ${written} bytes" }
            pump.run()
        }
    }

    private val pump = AsyncSingleConcurrentExecution(pumperPool) {
        while(true){
            val hadBytes = synchronized(this){
                val bytes = usedBytes.get()
                when {
                    bytes > 0 -> bytes
                    done.get() -> {
                        -1L
                    }
                    else -> 0L
                }
            }
            if(hadBytes > 0){
                val buf = Buffer()
                val bytesRead = pipe.source.read(buf, hadBytes)
                output.write(buf, bytesRead)
                synchronized(this){
                    usedBytes.addAndGet(-1 * bytesRead)
                }
                logger.trace { "Wrote $bytesRead bytes" }
                continue
            } else if (hadBytes == -1L){
                logger.trace { "Closing output" }
                output.close()
            }
            break
        }
    }

    private val done = AtomicBoolean(false)

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
        const val DEFAULT_BUFFER_SIZE = 131_072L
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
                if(!closedInput.get()){
                    closedInput.set(true)
                    logger.trace { "Closing input" }
                    input.close()
                    pipe.sink.close()
                    onInputDone()
                }
                if(doneWithOutput.get() && !closedOutput.get()){
                    logger.trace { "Closing output" }
                    closedOutput.set(true)
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


