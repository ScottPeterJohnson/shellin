package net.justmachinery.shellin

import net.justmachinery.shellin.exec.ShellinProcess
import net.justmachinery.shellin.exec.start
import org.codehaus.plexus.util.cli.CommandLineUtils


/**
 * Run a process.
 * @param command Either the full commandline command, or just the program to invoke.
 *  (If the program name contains spaces, use the [program] function instead.)
 * @param extraArguments Arguments to be appended to the end of the command, individually escaped.
 *  Don't try to pass "--flag value" for instance; pass "--flag", "value".
 * @param cb Builder DSL for specifying extra options.
 */
fun ShellinReadonly.command(
    command : String,
    vararg extraArguments : String,
    cb : (ShellinProcessConfiguration.()->Unit)? = null
) : ShellinProcess {
    val commandArgs = CommandLineUtils.translateCommandline(command)
    return program(commandArgs.first()){
        commandArgs.drop(1).forEach {
            arguments(it)
        }
        arguments(*extraArguments)
        if(cb != null) {
            cb(this)
        }
    }
}

/**
 * Run a program by name.
 */
fun ShellinReadonly.program(name : String, cb : (ShellinProcessConfiguration.()->Unit)? = null) : ShellinProcess {
    val builder = ShellinProcessConfiguration(this)
    builder.argument(name)
    if(cb != null) {
        cb(builder)
    }
    return builder.start()
}

/**
 * Run a bash script.
 * @param script The script to run.
 * @param saneErrorHandling See https://vaneyckt.io/posts/safer_bash_scripts_with_set_euxo_pipefail/
 * @param cb The same builder as "command". Any arguments will be passed as arguments to the bash script.
 */
fun ShellinReadonly.bash(
    script : String,
    saneErrorHandling : Boolean = true,
    printCommands : Boolean = false,
    cb : (ShellinProcessConfiguration.()->Unit)? = null
) : ShellinProcess {
    val printCommandOption = if(printCommands) "x" else ""
    val errorHandlingOption = if(saneErrorHandling) "euo pipefail" else ""
    val options = "$printCommandOption$errorHandlingOption"
    val finalScript = if(options.isNotEmpty()) "set -$options\n$script" else script
    return command("bash", "-c", finalScript, cb = cb)
}