package net.justmachinery.shellin

import mu.KLogging
import net.justmachinery.shellin.exec.defaultThreadPool
import okio.Sink
import okio.sink
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ExecutorService

class Shellin {
    companion object : KLogging()
    operator fun invoke(cb : Shellin.()->Unit){
        run(cb)
    }

    val workingDirectory = ShellinConfig { Paths.get(".").toAbsolutePath() }
    fun workingDirectory(dir : String) = workingDirectory(Paths.get(dir))

    //Whether to log each command before its execution
    val logCommands = ShellinConfig { true }

    //Default location for stdout
    val defaultStdout : ShellinConfig<()->Sink?> = ShellinConfig { { NoCloseOutputStream(System.out).sink() } }
    val defaultStderr : ShellinConfig<()->Sink?> = ShellinConfig { { NoCloseOutputStream(System.err).sink() } }

    //Used to start threads to copy data from pipes
    val executorService = ShellinConfig { defaultThreadPool }

    fun env(name : String) : String? = System.getenv(name)
    fun userHome() : String = System.getProperty("user.home")
}


data class ShellinConfig<T>(val default : ()->T){
    var lazyValue = lazy { default() }
    operator fun invoke(value : T){
        this.lazyValue = lazyOf(value)
    }
    fun value() = lazyValue.value
}

