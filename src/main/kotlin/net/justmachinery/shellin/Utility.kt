package net.justmachinery.shellin

import mu.KLogging
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class NoCloseOutputStream(wrapped : OutputStream) : OverrideOutputStream(wrapped) {
    override fun close() {}
}

open class OverrideOutputStream(val output : OutputStream) : OutputStream() {
    override fun write(b: Int) = output.write(b)
    override fun close() = output.close()
    override fun flush() = output.flush()
    override fun write(b: ByteArray?) = output.write(b)
    override fun write(b: ByteArray?, off: Int, len: Int) = output.write(b, off, len)
}

class AsyncSingleConcurrentExecution(
    private val executorService: ExecutorService,
    private val cb : ()->Unit
) {
    companion object : KLogging()

    private var running = AtomicBoolean(false)
    private var required = AtomicBoolean(false)

    fun run(){
        val toRun = synchronized(this){
            required.set(true)
            if(!running.get()){
                running.set(true)
                true
            } else {
                false
            }
        }
        if(toRun){
            executorService.execute(::runInternal)
        }
    }

    private fun runInternal(){
        while(true){
            logger.trace { "Starting job" }
            synchronized(this){
                required.set(false)
            }
            try {
                cb()
            } catch(t : Throwable){
                logger.error(t){ "While running single concurrent job" }
            }
            val cont = synchronized(this){
                if(required.get()){
                    true
                } else {
                    running.set(false)
                    false
                }
            }
            if(cont){
                continue
            } else {
                break
            }
        }
    }
}