package net.justmachinery.shellin.exec

import com.zaxxer.nuprocess.NuProcess
import com.zaxxer.nuprocess.NuProcessBuilder
import net.justmachinery.shellin.Shellin
import net.justmachinery.shellin.ShellinProcessConfiguration
import java.util.concurrent.atomic.AtomicBoolean


internal fun ShellinProcessConfiguration.start() : ShellinProcess {
    if(context.logCommands.value()){
        Shellin.logger.debug { this.toString() }
    }

    val override = overrideEnvironmentVariables.value()
    val process = if(override != null) NuProcessBuilder(arguments, override) else NuProcessBuilder(arguments)

    val handler = ShellinNuHandler(context, stdin.value(), stdout.value(), stderr.value())
    val handle = ShellinProcess(
        handler = handler,
        acceptableExitCodes = exitValues.value()
    )
    val launched = process
        .apply { setCwd(workingDirectory.value()) }
        .apply { setProcessListener(handler)}
        .start()
    Shellin.logger.debug { "Launched process ${launched.pid}" }
    if(exitWithJava.value()){
        processStopper.value.addProcess(launched)
        handle.exitCode.whenComplete { _, _ ->
            processStopper.value.removeProcess(launched)
        }
    }
    return handle
}

internal val processStopper = lazy { ProcessStopper() }
internal class ProcessStopper {
    init {
        Runtime.getRuntime().addShutdownHook(Thread { stop() })
    }
    private val stopping = AtomicBoolean(false)
    private val runningProcesses = mutableSetOf<NuProcess>()
    fun addProcess(process: NuProcess){
        synchronized(this){
            if(stopping.get()){
                Shellin.logger.debug { "Destroy process ${process.pid}" }
                process.destroy(false)
            } else {
                runningProcesses.add(process)
            }
        }
    }
    fun removeProcess(process : NuProcess){
        synchronized(this){
            runningProcesses.remove(process)
        }
    }
    private fun stop(){
        synchronized(this){
            Shellin.logger.debug { "Destroy processes due to VM shutdown" }
            stopping.set(true)
            runningProcesses.forEach {
                Shellin.logger.debug { "Destroy process ${it.pid}" }
                it.destroy(false)
            }
        }
    }
}