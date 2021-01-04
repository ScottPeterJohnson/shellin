package net.justmachinery.shellin

import mu.KLogging
import net.justmachinery.shellin.exec.ProcessStopper
import net.justmachinery.shellin.exec.ShellinProcess
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
    /**
     * Whether to log each command before its execution
     */
    val logCommands : Boolean

    val defaultStdout : ShellinSinkProducer
    val defaultStderr : ShellinSinkProducer

    /**
     * Used to start threads to copy data from pipes
     */
    val executorService : ExecutorService

    /**
     * Action to perform (if any) to clean up launched processes. By default, this is a JVM shutdown hook.
     */
    val shutdownHandler : ShellinShutdownHandler

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
    override val shutdownHandler: ShellinShutdownHandler
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
    override var logCommands by ShellinConfig { parent?.logCommands ?: true }
    override var defaultStdout by ShellinConfig { parent?.defaultStdout ?: {  NoCloseOutputStream(System.out).sink() } }
    override var defaultStderr by ShellinConfig { parent?.defaultStderr ?: { NoCloseOutputStream(System.err).sink() } }
    override var executorService by ShellinConfig { parent?.executorService ?: defaultThreadPool }
    override var shutdownHandler by ShellinConfig { parent?.shutdownHandler ?: ShellinShutdownHandler.JvmShutdownHook }
}

/**
 * Handler for stopping processes- e.g. on JVM shutdown.
 */
interface ShellinShutdownHandler {
    fun add(process : ShellinProcess)
    fun remove(process : ShellinProcess)

    companion object {
        object None : ShellinShutdownHandler {
            override fun add(process: ShellinProcess) {}
            override fun remove(process: ShellinProcess) {}
        }
        val JvmShutdownHook : ShellinShutdownHandler by lazy { ProcessStopper() }
    }
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
