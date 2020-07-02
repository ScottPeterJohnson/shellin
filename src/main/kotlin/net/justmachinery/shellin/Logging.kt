package net.justmachinery.shellin

import mu.KLogging
import okio.Buffer
import okio.Sink
import okio.Timeout
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction


fun Shellin.logStdout(
    doLog : (CharSequence)->Unit
){
    defaultStdout {
        NewlineDelimiterSink {
            doLog(it)
        }
    }
}

fun Shellin.logStderr(
    doLog : (CharSequence)->Unit
){
    defaultStderr {
        NewlineDelimiterSink {
            doLog(it)
        }
    }
}

class NewlineDelimiterSink(
    private val onLine : (CharSequence)->Unit
) : Sink {
    companion object : KLogging()

    override fun close() {
        logger.trace { "Closing" }
        decodeBuffer(true)
    }

    override fun flush() {}

    override fun timeout() = Timeout.NONE

    private val decoder = Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.IGNORE)
        .onUnmappableCharacter(CodingErrorAction.IGNORE)

    private val inputBuffer = ByteBuffer.allocate(8192)
    private val outputBuffer = ByteBuffer.allocate(8192).asCharBuffer()

    override fun write(source: Buffer, byteCount: Long) {
        logger.trace { "Write $byteCount bytes" }
        val buf = Buffer()
        source.read(buf, byteCount)
        while(buf.size > 0){
            buf.read(inputBuffer)
            decodeBuffer(false)
        }
    }

    private fun decodeBuffer(end : Boolean){
        while(true){
            val oldLast = outputBuffer.position()
            inputBuffer.flip()
            val decoded = decoder.decode(inputBuffer, outputBuffer, end)
            inputBuffer.compact()

            logger.trace { "Decoded ${outputBuffer.position() - oldLast} chars" }
            checkForNewline(oldLast, end, decoded.isOverflow)

            if(!decoded.isOverflow){ break }
        }
    }

    private fun checkForNewline(start : Int, end : Boolean, wasOverflow : Boolean){
        logger.trace { "Checking for newline with ${outputBuffer.position()} chars at start $start (final pass: $end)" }
        var foundAnything = false
        var currentStart = start
        while(true){
            var foundNewline = false
            outputBuffer.flip()
            for(i in currentStart until outputBuffer.limit()){
                val char = outputBuffer[i]
                if(char == '\n'){
                    logger.trace { "Found a line of length $i" }
                    onLine(outputBuffer.duplicate().slice(0 until i))
                    outputBuffer.position(i + 1)
                    foundNewline = true
                    foundAnything = true
                    break
                }
            }
            outputBuffer.compact()
            if(!foundNewline){ break }
            currentStart = 0
        }
        if((wasOverflow && !foundAnything) || (end && outputBuffer.position() != 0)){
            logger.trace { "Dumping buffer" }
            outputBuffer.flip()
            onLine(outputBuffer)
            outputBuffer.clear()
        }
    }
}


