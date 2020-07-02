package net.justmachinery.shellin

import okio.Pipe
import okio.Sink
import okio.Source
import okio.buffer

inline fun Shellin.collectStdout(cb: ()->Unit) = collectPipe(defaultStdout, cb)
inline fun Shellin.collectStderr(cb: ()->Unit) = collectPipe(defaultStderr, cb)

inline fun Shellin.collectPipe(configOut : ShellinConfig<()->Sink?>, cb : ()->Unit) : ProgramOutput {
    val output = Pipe(Long.MAX_VALUE)
    val oldValue = configOut.lazyValue
    configOut.lazyValue = lazyOf { output.sink }
    cb()
    configOut.lazyValue = oldValue
    return ProgramOutput(output.source)
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