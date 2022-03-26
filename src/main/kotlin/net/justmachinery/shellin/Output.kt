package net.justmachinery.shellin

import okio.Pipe
import okio.Source
import okio.buffer
import java.io.InputStream
import kotlin.reflect.KMutableProperty1

public inline fun <T : ShellinReadonly> T.collectStdout(crossinline cb: ShellinWriteable.()->Unit): ProgramOutput = collectPipe(ShellinWriteable::defaultStdout, cb)
public inline fun <T : ShellinReadonly> T.collectStderr(crossinline cb: ShellinWriteable.()->Unit): ProgramOutput = collectPipe(ShellinWriteable::defaultStderr, cb)

public inline fun <T : ShellinReadonly> T.collectPipe(prop : KMutableProperty1<ShellinWriteable, ShellinSinkProducer>, crossinline cb : ShellinWriteable.()->Unit) : ProgramOutput {
    return this.new {
        val output = Pipe(Long.MAX_VALUE)
        prop.set(this) { output.sink }
        this.cb()
        ProgramOutput(output.source)
    }
}



public class ProgramOutput(public val source : Source) {
    public val stream: InputStream by lazy { source.buffer().inputStream() }
    public val text: String by lazy {
        stream.use {
            it.reader().use {
                it.readText()
            }
        }
    }
}