package org.icpclive.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

public fun getLogger(clazz: KClass<*>): Logger = LoggerFactory.getLogger(clazz.java)!!
