package net.justmachinery.shellin

import okio.Pipe
import okio.Source
import okio.buffer
import kotlin.reflect.KMutableProperty1

inline fun <T : ShellinReadonly> T.collectStdout(crossinline cb: ShellinWriteable.()->Unit) = collectPipe(ShellinWriteable::defaultStdout, cb)
inline fun <T : ShellinReadonly> T.collectStderr(crossinline cb: ShellinWriteable.()->Unit) = collectPipe(ShellinWriteable::defaultStderr, cb)

inline fun <T : ShellinReadonly> T.collectPipe(prop : KMutableProperty1<ShellinWriteable, ShellinSinkProducer>, crossinline cb : ShellinWriteable.()->Unit) : ProgramOutput {
    return this.new {
        val output = Pipe(Long.MAX_VALUE)
        prop.set(this) { output.sink }
        this.cb()
        ProgramOutput(output.source)
    }
}



class ProgramOutput(val source : Source) {
    val stream by lazy { source.buffer().inputStream() }
    val text by lazy {
        stream.use {
            it.reader().use {
                it.readText()
            }
        }
    }
}