package net.justmachinery.shellin.exec

import net.justmachinery.futility.execution.pools

internal val defaultThreadPool by lazy {
    pools.defaultExecutor
}