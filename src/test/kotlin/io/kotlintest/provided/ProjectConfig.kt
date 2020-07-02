package io.kotlintest.provided;

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.read.ListAppender
import ch.qos.logback.core.spi.FilterReply
import io.kotlintest.AbstractProjectConfig
import org.slf4j.LoggerFactory

/**
 * Kotlintest finds this object by convention.
 */
object ProjectConfig : AbstractProjectConfig() {
    override fun beforeAll() {
        val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
        loggerContext.getLogger("net.justmachinery").level = Level.TRACE
    }
}