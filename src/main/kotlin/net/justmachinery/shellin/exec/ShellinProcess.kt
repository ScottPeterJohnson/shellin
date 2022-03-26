package net.justmachinery.shellin.exec

import com.zaxxer.nuprocess.NuProcess
import java.util.concurrent.CompletableFuture


/**
 * A wrapper for interacting with launched processes.
 */
public class ShellinProcess internal constructor(
    private val handler: ShellinNuHandler,
    private val acceptableExitCodes : List<Int>
) {
    /**
     * Waits for this process to finish. Throws [InvalidExitCodeException] if exit code is not acceptable.
     */
    public fun waitFor() : Int {
        val exit = exitCode.get()
        if(!successful().get()){
            throw InvalidExitCodeException(exit)
        }
        return exit
    }

    public val nuProcess: NuProcess get() = handler.nuProcess
    public val exitCode: CompletableFuture<Int> get() = handler.exitCode
    public fun successful(): CompletableFuture<Boolean> = exitCode.thenApply { acceptableExitCodes.contains(it) }!!

    public fun destroy(force : Boolean){
        nuProcess.destroy(force)
    }
    public val pid: Int get() = nuProcess.pid
}


public open class InvalidExitCodeException(public val exitCode : Int) : RuntimeException("Unacceptable exit code $exitCode")