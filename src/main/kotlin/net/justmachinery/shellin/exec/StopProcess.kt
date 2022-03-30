package net.justmachinery.shellin.exec

import mu.KLogging
import net.justmachinery.futility.execution.ShutdownHookPriority
import net.justmachinery.futility.execution.addJvmShutdownHook
import net.justmachinery.shellin.Shellin
import net.justmachinery.shellin.ShellinShutdownHandler
import java.util.concurrent.atomic.AtomicBoolean

public class ShellinProcessStopper(priority : ShutdownHookPriority = ShutdownHookPriority(0.0)) : ShellinShutdownHandler {
    public companion object : KLogging()
    init {
        addJvmShutdownHook(priority) { stop() }
    }
    private val stopping = AtomicBoolean(false)
    private val runningProcesses = mutableSetOf<ShellinProcess>()
    override fun add(process: ShellinProcess) {
        synchronized(this){
            if(stopping.get()){
                Shellin.logger.debug { "Destroy process ${process.pid}" }
                process.destroy(false)
            } else {
                runningProcesses.add(process)
            }
        }
    }

    override fun remove(process: ShellinProcess) {
        synchronized(this){
            runningProcesses.remove(process)
        }
    }

    override fun isShuttingDown(): Boolean = stopping.get()

    private fun stop(){
        synchronized(this){
            logger.debug { "Destroy processes due to VM shutdown" }
            stopping.set(true)
            runningProcesses.forEach {
                logger.debug { "Destroy process ${it.pid}" }
                it.destroy(false)
            }

        }
    }
}