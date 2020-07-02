package net.justmachinery.shellin

import okio.Source
import okio.sink
import okio.source
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Paths

data class ShellinProcessConfiguration internal constructor(
    internal val context : Shellin
){
    internal var arguments : MutableList<String> = mutableListOf()
    operator fun String.unaryPlus() {
        argument(this)
    }
    fun arg(argument : String) = argument(argument)
    fun argument(argument : String) {
        arguments.add(argument)
    }

    fun args(vararg arguments : String) = arguments(*arguments)
    fun arguments(vararg arguments : String) {
        this.arguments.addAll(arguments)
    }


    val workingDirectory = ShellinConfig { context.workingDirectory.value() }
    fun workingDirectory(path : String) {
        workingDirectory(Paths.get(path))
    }

    /**
     * Whether the child process will be killed as part of a shutdown hook when the VM exits
     */
    val exitWithJava = ShellinConfig { true }

    /**
     * A list of exit values that will not throw an exception
     */
    val exitValues = ShellinConfig { listOf(0) }

    /**
     * If supplied, a list of environment variables that will replace Java's environment variables
     */
    val overrideEnvironmentVariables = ShellinConfig<Map<String,String>?> { null }

    /**
     * stdin for launched process.
     * Note that the input stream given will be closed.
     */
    val stdin = ShellinConfig<Source?> { null }
    fun stdin(input : InputStream) { stdin(input.source()) }

    /**
     * stdout for launched process. Note that the sink given will be closed when the process exits.
     */
    val stdout = ShellinConfig { context.defaultStdout.value()() }
    fun stdout(output : OutputStream) { stdout(output.sink()) }

    /**
     * See [stdout] but for stderr
     */
    val stderr = ShellinConfig { context.defaultStderr.value()() }
    fun stderr(output : OutputStream) { stderr(output.sink()) }

    override fun toString(): String {
        val sb = StringBuilder()

        sb.appendln("$ ${arguments.joinToString(" ")}")
        if(workingDirectory != context.workingDirectory){
            sb.appendln("$ With working directory: $workingDirectory")
        }
        val override = overrideEnvironmentVariables.value()
        if(override != null){
            sb.appendln("$ With custom environment: " + override.entries.joinToString(" "){
                it.key + "=" + it.value.let { value ->
                    if(value.length > 50) value.take(50) + "..." else value
                } })
        }

        return sb.toString()
    }
}