package org.icpclive.service

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import org.icpclive.cds.NetworkUtils
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.stream.Collectors
import kotlin.time.Duration

abstract class RegularLoaderService<T> {

    abstract val url: String
    abstract val login: String
    abstract val password: String
    abstract fun processLoaded(data: String) : T

    fun loadOnce() : T {
        val inputStream = NetworkUtils.openAuthorizedStream(url, login, password)
        val xml = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining())
        return processLoaded(xml)
    }

    suspend fun run(flow: MutableStateFlow<T>, period: Duration) {
        while (true) {
            try {
                flow.value = loadOnce()
                delay(period)
            } catch (e: IOException) {
                logger.error("Failed to load xml", e)
            }
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(RegularLoaderService::class.java)
    }
}