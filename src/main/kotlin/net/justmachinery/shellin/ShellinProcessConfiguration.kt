package net.justmachinery.shellin

import okio.Source
import okio.sink
import okio.source
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Paths

@ShellinDsl
data class ShellinProcessConfiguration internal constructor(
    internal val context : ShellinReadonly
){
    var arguments : MutableList<String> = mutableListOf()
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


    var workingDirectory by ShellinConfig { context.workingDirectory }
    fun workingDirectory(path : String) {
        workingDirectory = Paths.get(path)
    }

    /**
     * A list of exit values that will not throw an exception
     */
    var exitValues = listOf(0)

    /**
     * If supplied, a list of environment variables that will replace Java's environment variables
     */
    var overrideEnvironmentVariables :  Map<String,String>? = null

    /**
     * stdin for launched process.
     * Note that the input stream given will be closed.
     */
    var stdin : Source? = null
    fun stdin(input : InputStream) { stdin = input.source() }

    /**
     * stdout for launched process. Note that the sink given will be closed when the process exits.
     */
    var stdout by ShellinConfig { context.defaultStdout(this) }
    fun stdout(output : OutputStream) { stdout = output.sink() }

    /**
     * See [stdout] but for stderr
     */
    var stderr by ShellinConfig { context.defaultStderr(this) }
    fun stderr(output : OutputStream) { stderr = output.sink() }

    override fun toString(): String {
        val sb = StringBuilder()

        sb.appendln("$ ${arguments.joinToString(" ")}")
        if(workingDirectory != context.workingDirectory){
            sb.appendln("$ With working directory: $workingDirectory")
        }
        val override = overrideEnvironmentVariables
        if(override != null){
            sb.appendln("$ With custom environment: " + override.entries.joinToString(" "){
                it.key + "=" + it.value.let { value ->
                    if(value.length > 50) value.take(50) + "..." else value
                } })
        }

        return sb.toString()
    }
}