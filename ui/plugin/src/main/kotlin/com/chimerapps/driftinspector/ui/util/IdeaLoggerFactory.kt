package com.chimerapps.driftinspector.ui.util


import com.chimerapps.discovery.utils.Logger
import com.chimerapps.discovery.utils.LoggerFactory

internal class IdeaLoggerFactory : LoggerFactory {

    override fun getInstanceForClass(clazz: Class<*>): Logger {
        return LoggerWrapper(com.intellij.openapi.diagnostic.Logger.getInstance(clazz))
    }

}

private class LoggerWrapper(private val instance: com.intellij.openapi.diagnostic.Logger) : Logger {
    override fun doLogDebug(message: String?, t: Throwable?) {
        instance.debug(message, t)
    }

    override fun doLogInfo(message: String?, t: Throwable?) {
        instance.info(message, t)
    }

    override fun doLogWarn(message: String?, t: Throwable?) {
        instance.warn(message, t)
    }

    override fun doLogError(message: String?, t: Throwable?) {
        instance.error(message, t)
    }

}