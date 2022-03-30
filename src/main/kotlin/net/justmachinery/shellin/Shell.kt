package net.justmachinery.shellin

import NoCloseOutputStream
import mu.KLogging
import net.justmachinery.shellin.exec.ShellinProcessStopper
import net.justmachinery.shellin.exec.ShellinProcess
import net.justmachinery.shellin.exec.defaultThreadPool
import okio.Sink
import okio.sink
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ExecutorService
import kotlin.reflect.KProperty

@ShellinDsl
public interface ShellinReadonly {
    public val workingDirectory : Path
    /**
     * Whether to log each command before its execution
     */
    public val logCommands : Boolean

    public val defaultStdout : ShellinSinkProducer
    public val defaultStderr : ShellinSinkProducer

    /**
     * Used to start threads to copy data from pipes
     */
    public val executorService : ExecutorService

    /**
     * Action to perform (if any) to clean up launched processes. By default, this is a JVM shutdown hook.
     */
    public val shutdownHandler : ShellinShutdownHandler

    public fun <T> new(cb : ShellinWriteable.()->T) : T {
        val copy = ShellinImpl(this)
        return cb(copy)
    }
}

@ShellinDsl
public interface ShellinWriteable : ShellinReadonly {
    override var workingDirectory : Path
    override var logCommands : Boolean
    override var defaultStdout : ShellinSinkProducer
    override var defaultStderr : ShellinSinkProducer
    override var executorService : ExecutorService
    override var shutdownHandler: ShellinShutdownHandler
    public fun workingDirectory(dir : String) { workingDirectory = Paths.get(dir) }
}

public fun shellin(configure : ShellinWriteable.()->Unit) : ShellinReadonly {
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
public interface ShellinShutdownHandler {
    public fun add(process : ShellinProcess)
    public fun remove(process : ShellinProcess)
    public fun isShuttingDown() : Boolean

    public companion object {
        public object None : ShellinShutdownHandler {
            override fun add(process: ShellinProcess) {}
            override fun remove(process: ShellinProcess) {}
            override fun isShuttingDown(): Boolean = false
        }
        public val JvmShutdownHook : ShellinShutdownHandler by lazy { ShellinProcessStopper() }
    }
}
public typealias ShellinSinkProducer = (ShellinProcessConfiguration)->Sink?


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
public annotation class ShellinDsl
