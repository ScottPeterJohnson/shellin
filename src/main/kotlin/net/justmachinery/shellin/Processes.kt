package net.justmachinery.shellin

import org.apache.commons.exec.*
import java.io.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import javax.annotation.CheckReturnValue


/**
 * Create a launch template.
 * This implementation mostly uses Apache Commons Exec for correctness: https://commons.apache.org/proper/commons-exec/index.html
 * @param command Either the full commandline command, or just the program to invoke.
 * @param extraArguments Arguments to be appended to the end of the command, individually escaped.
 * Don't try to pass "--flag value" for instance; pass "--flag", "value".
 * @param cb Builder DSL for specifying extra options.
 */
@CheckReturnValue
fun command(
		command : String,
		vararg extraArguments : String,
		cb : (ProcessLaunchTemplate.Builder.()->Unit)? = null
) : ProcessLaunchTemplate {
	val builder = ProcessLaunchTemplate.Builder(ProcessLaunchTemplate(command))
	for(argument in extraArguments){
		builder.argument(argument)
	}
	if(cb != null) {
		cb(builder)
	}
	return builder.build()
}

/**
 * Run a bash script.
 * @param script The script to run.
 * @param saneErrorHandling See https://vaneyckt.io/posts/safer_bash_scripts_with_set_euxo_pipefail/
 * @param cb The same builder as "command". Any arguments will be passed as arguments to the bash script.
 */
fun bash(
		script : String,
		saneErrorHandling : Boolean = true,
		printCommands : Boolean = false,
		cb : (ProcessLaunchTemplate.Builder.()->Unit)? = null
) : ProcessLaunchTemplate {
	val printCommandOption = if(printCommands) "x" else ""
	val finalScript = if(saneErrorHandling) "set -eu${printCommandOption}o pipefail\n$script" else script
	return command("bash", "-c", finalScript, cb = cb)
}
/**
 * An immutable description of a process to launch.
 */
data class ProcessLaunchTemplate internal constructor(
	private var command : String,
	private var arguments : List<String> = listOf(),
	private var workingDirectory : File? = null,
	private var exitValues : List<Int>? = null,
	private var overrideEnvironmentVariables : Map<String,String>? = null,
	private var stdin : InputStream? = null
){
	class Builder internal constructor(private val modified : ProcessLaunchTemplate) {
		internal fun build() : ProcessLaunchTemplate { return modified }
		fun argument(argument : String){
			modified.arguments += argument
		}
		fun arguments(vararg arguments : String){
			modified.arguments += arguments
		}
		fun workingDirectory(file : File){
			modified.workingDirectory = file
		}
		fun workingDirectory(path : String){
			modified.workingDirectory = File(path)
		}

		/**
		 * If supplied, a list of nonzero exit values that will not throw an exception
		 */
		fun acceptExitValues(vararg values : Int){
			modified.exitValues = values.toList()
		}

		/**
		 * If supplied, a list of environment variables that will replace Java's environment variables
		 */
		fun environmentVariables(variables : Map<String,String>){
			modified.overrideEnvironmentVariables = variables
		}
		fun input(stdin : InputStream){
			modified.stdin = stdin
		}
	}

	/**
	 * Print a useful description of this command to stdout.
	 */
	fun show() : ProcessLaunchTemplate {
		println("$ " + renderCommand())
		if(workingDirectory != null){
			println("$ With working directory: $workingDirectory")
		}
		if(overrideEnvironmentVariables != null){
			println("$ With custom environment: " + overrideEnvironmentVariables!!.entries.joinToString(" "){
				it.key + "=" + it.value.let { value ->
					if(value.length > 50) value.take(50) + "..." else value
				} })
		}
		return this
	}

	/**
	 * Start and wait synchronously for program to finish. Will throw an ExecutionException on unacceptable exit codes.
	 */
	fun run() : Int {
		return start().exitCode.get()
	}

	/**
	 * As "run", but swallows execution exceptions.
	 */
	fun runIgnoringErrors(){
		try {
			start().exitCode.get()
		} catch(e : ExecutionException){
			/* do nothing */
		}
	}

	/**
	 * Start asynchronously. Uses the supplied output and error streams, otherwise uses System.out and System.err.
	 * The returned process handle gives a reference to the spawned process and a future that can be used to get the exit code.
	 * Unacceptable exit codes will result in an ExecutionException being thrown.
	 */
	@CheckReturnValue
	fun start(stdout : OutputStream? = System.out, stderr : OutputStream? = System.err) : ProcessHandle {
		val commandLine = buildCommandLine()

		val launchedProcess = CompletableFuture<Process>()
		val executor = CaptureExecutor(launchedProcess)
		executor.processDestroyer = processDestroyer
		executor.workingDirectory = workingDirectory
		exitValues?.let { executor.setExitValues(it.toIntArray()) }

		executor.streamHandler = PumpStreamHandler(stdout, stderr, stdin)

		val future = CompletableFuture<Int>()

		executor.execute(commandLine, overrideEnvironmentVariables, object : ExecuteResultHandler {
			override fun onProcessComplete(exitValue: Int) {
				future.complete(exitValue)
			}

			override fun onProcessFailed(e: ExecuteException) {
				future.completeExceptionally(e)
			}

		})
		return ProcessHandleImpl(
				processFuture = launchedProcess,
				exitCode = future
		)
	}

	/**
	 * Starts the program and collects the output as stdout. This uses an internal buffer and spawns threads to copy from
	 * the program's output streams, so as to not deadlock the spawned program.
	 */
	@CheckReturnValue
	fun output() : OutputProcessHandle {
		val stdoutIn = PipedInputStream()
		val stdoutOut = PipedOutputStream(stdoutIn)
		val handle = start(stdout = stdoutOut)
		return OutputProcessHandle(
				processHandle = handle,
				stdout = stdoutIn
		)
	}

	fun renderCommand() : String {
		val commandLine = buildCommandLine()
		return "${commandLine.executable} ${commandLine.arguments.joinToString(" ")}"
	}

	internal fun buildCommandLine() : CommandLine {
		val commandLine = CommandLine.parse(command)
		for(arg in arguments){ commandLine.addArgument(arg) }
		return commandLine
	}
}

interface ProcessHandle {
	val process : Process
	val exitCode : CompletableFuture<Int>
}

internal data class ProcessHandleImpl(
		private val processFuture : CompletableFuture<Process>,
		override val exitCode : CompletableFuture<Int>
) : ProcessHandle {
	override val process
		get() = processFuture.get()!!
}

class OutputProcessHandle internal constructor(
		processHandle : ProcessHandle,
		val stdout : PipedInputStream
) : ProcessHandle by processHandle {
	val string : CompletableFuture<String> by lazy { processHandle.exitCode.thenApply { stdout.reader().readText() } }
}

private class CaptureExecutor(val launchedProcess : CompletableFuture<Process>) : DefaultExecutor() {
	override fun launch(command: CommandLine?, env: MutableMap<String, String>?, dir: File?): java.lang.Process {
		try {
			val process = super.launch(command, env, dir)
			launchedProcess.complete(process)
			return process
		} catch(e : Exception){
			launchedProcess.completeExceptionally(e)
			throw e
		}
	}
}

private val processDestroyer = ShutdownHookProcessDestroyer()