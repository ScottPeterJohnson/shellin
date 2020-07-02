package net.justmachinery.shellin.exec


/**
 * A wrapper for interacting with launched processes.
 */
class ShellinProcess internal constructor(
    private val handler: ShellinNuHandler,
    private val acceptableExitCodes : List<Int>
) {
    /**
     * Waits for this process to finish. Throws [InvalidExitCodeException] if exit code is not acceptable.
     */
    fun waitFor() : Int {
        val exit = exitCode.get()
        if(!successful().get()){
            throw InvalidExitCodeException(exit)
        }
        return exit
    }

    private val process get() = handler.nuProcess
    val exitCode get() = handler.exitCode
    fun successful() = exitCode.thenApply { acceptableExitCodes.contains(it) }!!

    fun destroy(force : Boolean){
        process.destroy(force)
    }
    val pid get() = process.pid
}


open class InvalidExitCodeException(val exitCode : Int) : RuntimeException("Unacceptable exit code $exitCode")