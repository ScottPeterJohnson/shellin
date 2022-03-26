package net.justmachinery.shellin

import okio.Sink
import okio.Source
import okio.sink
import okio.source
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import java.nio.file.Paths

@ShellinDsl
public data class ShellinProcessConfiguration internal constructor(
    internal val context : ShellinReadonly
) {
    var arguments: MutableList<String> = mutableListOf()
    public operator fun String.unaryPlus() {
        argument(this)
    }

    public fun arg(argument: String): Unit = argument(argument)
    public fun argument(argument: String) {
        arguments.add(argument)
    }

    public fun args(vararg arguments: String): Unit = arguments(*arguments)
    public fun arguments(vararg arguments: String) {
        this.arguments.addAll(arguments)
    }


    public var workingDirectory: Path by ShellinConfig { context.workingDirectory }
    public fun workingDirectory(path: String) {
        workingDirectory = Paths.get(path)
    }

    /**
     * A list of exit values that will not throw an exception
     */
    public var exitValues: List<Int> = listOf(0)

    /**
     * If supplied, a list of environment variables that will replace Java's environment variables
     */
    var overrideEnvironmentVariables: Map<String, String>? = null

    /**
     * stdin for launched process.
     * Note that the input stream given will be closed.
     */
    var stdin: Source? = null
    public fun stdin(input: InputStream) {
        stdin = input.source()
    }

    /**
     * stdout for launched process. Note that the sink given will be closed when the process exits.
     */
    public var stdout: Sink? by ShellinConfig { context.defaultStdout(this) }
    public fun stdout(output: OutputStream) {
        stdout = output.sink()
    }

    /**
     * See [stdout] but for stderr
     */
    public var stderr: Sink? by ShellinConfig { context.defaultStderr(this) }
    public fun stderr(output: OutputStream) {
        stderr = output.sink()
    }

    override fun toString(): String {
        val sb = StringBuilder()

        sb.appendLine("$ ${arguments.joinToString(" ")}")
        if (workingDirectory != context.workingDirectory) {
            sb.appendLine("$ With working directory: $workingDirectory")
        }
        val override = overrideEnvironmentVariables
        if (override != null) {
            sb.appendLine("$ With custom environment: " + override.entries.joinToString(" ") {
                it.key + "=" + it.value.let { value ->
                    if (value.length > 50) value.take(50) + "..." else value
                }
            })
        }

        return sb.toString()
    }
}