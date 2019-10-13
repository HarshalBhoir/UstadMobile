package com.ustadmobile.lib.contentscrapers

import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.config.Configurator

object UMLogUtil {

    private val LOG = LogManager.getRootLogger()

    fun logTrace(message: String) {
        LOG.log(Level.TRACE, message)
    }

    fun logDebug(message: String) {
        LOG.log(Level.DEBUG, message)
    }

    fun logInfo(message: String) {
        LOG.log(Level.INFO, message)
    }

    fun logError(message: String) {
        LOG.log(Level.ERROR, message)
    }

    fun logWarn(message: String) {
        LOG.log(Level.WARN, message)
    }

    fun logFatal(message: String) {
        LOG.log(Level.FATAL, message)
    }

    fun setLevel(level: String) {
        var logLevel = Level.ERROR
        when (level.toUpperCase()) {
            "TRACE" -> logLevel = Level.TRACE
            "DEBUG" -> logLevel = Level.DEBUG
            "INFO" -> logLevel = Level.INFO
            "ERROR" -> logLevel = Level.ERROR
            "WARN" -> logLevel = Level.WARN
            "FATAL" -> logLevel = Level.FATAL
        }
        Configurator.setRootLevel(logLevel)
    }
}