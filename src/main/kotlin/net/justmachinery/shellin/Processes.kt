package net.justmachinery.shellin

import org.apache.commons.exec.*
import java.io.*
import java.util.concurrent.CompletableFuture
import javax.annotation.CheckReturnValue


/**
 * Create a launch template based off command, which can either be the full command line or just the program name to execute.
 * This implementation mostly uses Apache Commons Exec for correctness: https://commons.apache.org/proper/commons-exec/index.html
 */
@CheckReturnValue
fun command(command : String) : ProcessLaunchTemplate {
	return ProcessLaunchTemplate(command)
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
	operator fun invoke(cb : Builder.()->Int) : ProcessLaunchTemplate {
		val modified = copy()
		cb(Builder(modified))
		return this
	}

	class Builder(private val modified : ProcessLaunchTemplate) {
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
	 * Start and wait synchronously for program to finish.
	 */
	fun run() : Int {
		return start().exitCode.get()
	}

	/**
	 * Start asynchronously. Uses the supplied output and error streams, otherwise uses System.out and System.err.
	 * The returned process handle gives a reference to the spawned process and a future that can be used to get the exit code.
	 */
	@CheckReturnValue
	fun start(stdout : OutputStream? = System.out, stderr : OutputStream? = System.err) : ProcessHandle {
		val commandLine = CommandLine.parse(command)
		for(arg in arguments){ commandLine.addArgument(arg) }

		val executor = CaptureExecutor()
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
				process = executor.launchedProcess!!,
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
}

interface ProcessHandle {
	val process : Process
	val exitCode : CompletableFuture<Int>
}

internal data class ProcessHandleImpl(
		override val process : Process,
		override val exitCode : CompletableFuture<Int>
) : ProcessHandle

class OutputProcessHandle internal constructor(
		processHandle : ProcessHandle,
		val stdout : PipedInputStream
) : ProcessHandle by processHandle {
	val outputString : CompletableFuture<String> by lazy { processHandle.exitCode.thenApply { stdout.reader().readText() } }
}

private class CaptureExecutor : DefaultExecutor() {
	var launchedProcess : Process? = null
	override fun launch(command: CommandLine?, env: MutableMap<String, String>?, dir: File?): java.lang.Process {
		launchedProcess = super.launch(command, env, dir)
		return launchedProcess!!
	}
}