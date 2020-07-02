package net.justmachinery.shellin

import mu.KLogging
import net.justmachinery.shellin.exec.defaultThreadPool
import okio.Sink
import okio.sink
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ExecutorService
import kotlin.reflect.KProperty

@ShellinDsl
interface ShellinReadonly {
    val workingDirectory : Path
    //Whether to log each command before its execution
    val logCommands : Boolean
    //Default location for stdout
    val defaultStdout : ShellinSinkProducer
    val defaultStderr : ShellinSinkProducer
    //Used to start threads to copy data from pipes
    val executorService : ExecutorService

    fun <T> new(cb : ShellinWriteable.()->T) : T {
        val copy = ShellinImpl(this)
        return cb(copy)
    }
}

@ShellinDsl
interface ShellinWriteable : ShellinReadonly {
    override var workingDirectory : Path
    override var logCommands : Boolean
    override var defaultStdout : ShellinSinkProducer
    override var defaultStderr : ShellinSinkProducer
    override var executorService : ExecutorService
    fun workingDirectory(dir : String) { workingDirectory = Paths.get(dir) }
}

fun shellin(configure : ShellinWriteable.()->Unit) : ShellinReadonly {
    val impl = ShellinImpl(null)
    configure(impl)
    return impl
}

private class ShellinImpl(private val parent : ShellinReadonly?) : ShellinWriteable {
    companion object : KLogging()

    override var workingDirectory by ShellinConfig<Path> { parent?.workingDirectory ?: Paths.get(".").toAbsolutePath() }
    override var logCommands by ShellinConfig { true }
    override var defaultStdout by ShellinConfig<ShellinSinkProducer> { {  NoCloseOutputStream(System.out).sink() } }
    override var defaultStderr by ShellinConfig<ShellinSinkProducer> { { NoCloseOutputStream(System.err).sink() } }
    override var executorService by ShellinConfig { defaultThreadPool }

    fun env(name : String) : String? = System.getenv(name)
    fun userHome() : String = System.getProperty("user.home")
}

typealias ShellinSinkProducer = (ShellinProcessConfiguration)->Sink?


internal data class ShellinConfig<T>(val default : ()->T){
    var lazyValue = lazy { default() }
    operator fun invoke(value : T){
        this.lazyValue = lazyOf(value)
    }
    fun value() = lazyValue.value

    operator fun getValue(parent : Any, property: KProperty<*>): T {
        return value()
    }
    operator fun setValue(parent : Any, property: KProperty<*>, value: T) {
        lazyValue = lazyOf(value)
    }
}

@DslMarker
annotation class ShellinDsl
