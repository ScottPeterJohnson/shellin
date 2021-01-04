package net.justmachinery.shellin.exec

import com.zaxxer.nuprocess.NuProcessBuilder
import net.justmachinery.shellin.Shellin
import net.justmachinery.shellin.ShellinProcessConfiguration


internal fun ShellinProcessConfiguration.start() : ShellinProcess {
    if(context.logCommands){
        Shellin.logger.debug { this.toString() }
    }

    val override = overrideEnvironmentVariables
    val process = if(override != null) NuProcessBuilder(arguments, override) else NuProcessBuilder(arguments)

    val handler = ShellinNuHandler(context, stdin, stdout, stderr)
    val handle = ShellinProcess(
        handler = handler,
        acceptableExitCodes = exitValues
    )
    val launched = process
        .apply { setCwd(workingDirectory) }
        .apply { setProcessListener(handler)}
        .start()
    Shellin.logger.debug { "Launched process ${launched.pid}" }
    context.shutdownHandler.add(handle)
    handle.exitCode.whenComplete { _, _ ->
        context.shutdownHandler.remove(handle)
    }
    return handle
}