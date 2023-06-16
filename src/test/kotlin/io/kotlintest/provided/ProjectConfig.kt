package io.kotlintest.provided

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import org.slf4j.LoggerFactory
import io.kotest.core.config.AbstractProjectConfig

/**
 * Kotlintest finds this object by convention.
 */
object ProjectConfig : AbstractProjectConfig() {
    override suspend fun beforeProject() {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        loggerContext.getLogger("net.justmachinery").level = Level.TRACE
    }
}