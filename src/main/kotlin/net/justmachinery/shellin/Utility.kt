package net.justmachinery.shellin

import net.justmachinery.futility.mechanisms.SingleConcurrentExecution
import java.util.concurrent.ExecutorService


internal class AsyncSingleConcurrentExecution(executorService: ExecutorService, cb: () -> Unit) {
    val execution = SingleConcurrentExecution(cb) { executorService.submit(it) }
    fun run() {
        execution.run()
    }
}