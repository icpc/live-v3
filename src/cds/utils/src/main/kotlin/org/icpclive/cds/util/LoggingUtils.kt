package org.icpclive.cds.util

import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter
import org.slf4j.Logger as Slf4jLogger

public class Logger internal constructor(private val delegate: Slf4jLogger) {
    @Deprecated(level = DeprecationLevel.ERROR, message = "Don't use slow version", replaceWith = ReplaceWith("debug { msg }"))
    public fun debug(msg: String) { delegate.debug(msg) }
    @Deprecated(level = DeprecationLevel.ERROR, message = "Don't use slow version", replaceWith = ReplaceWith("info { msg }"))
    public fun info(msg: String) { delegate.info(msg) }
    @Deprecated(level = DeprecationLevel.ERROR, message = "Don't use slow version", replaceWith = ReplaceWith("warning { msg }"))
    public fun warn(msg: String) { delegate.warn(msg) }
    @Deprecated(level = DeprecationLevel.ERROR, message = "Don't use slow version", replaceWith = ReplaceWith("error { msg }"))
    public fun error(msg: String) { delegate.error(msg) }
    @Deprecated(level = DeprecationLevel.ERROR, message = "Don't use slow version", replaceWith = ReplaceWith("error(e) { msg }"))
    public fun error(msg: String, e: Throwable) { delegate.error(msg, e) }
    public val isDebugEnabled: Boolean get() = delegate.isDebugEnabled
    public val isInfoEnabled: Boolean get() = delegate.isInfoEnabled
    public val isWarningEnabled: Boolean get() = delegate.isWarnEnabled
    public val isErrorEnabled: Boolean get() = delegate.isErrorEnabled
    @Suppress("DEPRECATION_ERROR")
    public inline fun debug(msg: () -> String) { if (isDebugEnabled) debug(msg()) }
    @Suppress("DEPRECATION_ERROR")
    public inline fun info(msg: () -> String) { if (isInfoEnabled) info(msg()) }
    @Suppress("DEPRECATION_ERROR")
    public inline fun warning(msg: () -> String) { if (isWarningEnabled) warn(msg()) }
    @Suppress("DEPRECATION_ERROR")
    public inline fun error(msg: () -> String) { if (isErrorEnabled) error(msg()) }
    @Suppress("DEPRECATION_ERROR")
    public inline fun error(e: Throwable, msg: () -> String) { if (isErrorEnabled) error(msg(), e) }
}

public object LoggerProvider {
    public operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): Lazy<Logger> = lazy {
        var klass = property.javaGetter?.declaringClass ?: property.javaField?.declaringClass ?: thisRef?.javaClass ?: error("LoggerProvider can't be used for local properties")
        while (klass.enclosingClass != null) {
            klass = klass.enclosingClass
        }
        Logger(LoggerFactory.getLogger(klass))
    }
}

public fun logger(klass: Class<*>): Logger = Logger(LoggerFactory.getLogger(klass))
public fun logger(klass: KClass<*>): Logger = logger(klass.java)
public fun getLogger(): LoggerProvider = LoggerProvider