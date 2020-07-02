package net.justmachinery.shellin.exec

import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory

internal val defaultThreadPool by lazy {
    val result = Executors.newCachedThreadPool(object :
        ThreadFactory {
        private var threadCount = 0
        override fun newThread(r: Runnable) = Thread(r).apply {
            isDaemon = true
            name = "Shellin Thread-${++threadCount}"
        }
    })
    Runtime.getRuntime().addShutdownHook(Thread {
        result.shutdown()
    })
    result
}