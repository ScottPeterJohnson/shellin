package net.justmachinery.shellin.exec

import com.zaxxer.nuprocess.NuAbstractProcessHandler
import com.zaxxer.nuprocess.NuProcess
import mu.KLogging
import net.justmachinery.shellin.ShellinReadonly
import okio.Buffer
import okio.Sink
import okio.Source
import java.lang.IllegalStateException
import java.nio.ByteBuffer
import java.util.concurrent.CompletableFuture

/**
 * Process handler for shuttling data to/from a nuprocess
 */
internal class ShellinNuHandler(
    private val context : ShellinReadonly,
    stdin: Source?,
    stdout: Sink?,
    stderr: Sink?
) : NuAbstractProcessHandler() {
    companion object : KLogging()

    private val inPumper = stdin?.let {
        InputPumper(
            pumperPool = context.executorService,
            input = it,
            onInputReady = {
                nuProcess.wantWriteIfPossible()
            },
            onInputDone = {
                nuProcess.wantWriteIfPossible()
            }
        )
    }
    private val outPumper = stdout?.let {
        OutputPumper(
            context.executorService,
            it
        )
    }
    private val errPumper = stderr?.let {
        OutputPumper(
            context.executorService,
            it
        )
    }

    lateinit var nuProcess: NuProcess
    override fun onStart(nuProcess: NuProcess) {
        logger.trace { "Start" }
        this.nuProcess = nuProcess
        if(inPumper != null){
            nuProcess.wantWriteIfPossible()
            inPumper.startPump()
        }
    }

    @Volatile private var waitingInput : Buffer? = null
    override fun onStdinReady(buffer: ByteBuffer): Boolean {
        val waiting = waitingInput ?: inPumper?.read()
        val ret = if(waiting == null){
            logger.trace { "Closing stdin" }
            nuProcess.closeStdin(false)
            false
        } else {
            waitingInput = waiting
            if(waiting.size == 0L){
                logger.trace { "Input not ready" }
                waitingInput = null
                false
            } else {
                val readCount = waiting.read(buffer)
                if(waiting.size == 0L){
                    waitingInput = null
                }
                logger.trace { "Write $readCount to stdin" }
                true
            }
        }
        buffer.flip()
        return ret
    }

    override fun onStderr(buffer: ByteBuffer, closed: Boolean) {
        logger.trace { "Stderr received ${buffer.remaining()} bytes ($closed)" }
        if(errPumper != null){
            errPumper.write(buffer)
            if(closed){
                errPumper.close()
            }
        } else {
            buffer.position(buffer.limit())
        }
    }
    override fun onStdout(buffer: ByteBuffer, closed: Boolean) {
        logger.trace { "Stdout received ${buffer.remaining()} bytes ($closed)" }
        if(outPumper != null){
            outPumper.write(buffer)
            if(closed){
                outPumper.close()
            }
        } else {
            buffer.position(buffer.limit())
        }
    }

    val exitCode = CompletableFuture<Int>()
    override fun onExit(exitCode: Int) {
        logger.trace { "Exiting with code $exitCode" }
        inPumper?.close()
        outPumper?.close()
        errPumper?.close()
        this.exitCode.complete(exitCode)
    }
}

private fun NuProcess.wantWriteIfPossible(){
    try {
        wantWrite()
    } catch(t : IllegalStateException){
        /* ignored */
    }
}