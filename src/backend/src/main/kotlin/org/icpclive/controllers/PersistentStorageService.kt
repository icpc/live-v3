package org.icpclive.controllers

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.serializer
import org.icpclive.cds.util.getLogger
import org.icpclive.util.writeJsonAtomically
import java.nio.file.Files
import java.nio.file.Path
import kotlin.time.Clock


interface PersistentData<T: Any> {
    val persistentState: Flow<T>
    suspend fun onLoad(data: T?)
}

class PersistentStorageService<T: Any>(
    private val statePath: Path,
    private val persistentData: PersistentData<T>,
    private val serializer: KSerializer<T>,
) {

    suspend fun restore() {
        withContext(Dispatchers.IO) {
            persistentData.onLoad(load())
        }
    }

    suspend fun runSaving() {
        withContext(Dispatchers.IO + CoroutineName("Persisting $statePath")) {
            statePath.toFile().parentFile.mkdirs()
            persistentData.persistentState.collect { content ->
                try {
                    statePath.writeJsonAtomically(serializer, content)
                } catch (e: Exception) {
                    logger.error(e) { "Failed to save widgets state to $statePath" }
                }
            }
        }
    }

    private fun load(): T? {
        val file = statePath.toFile().takeIf { it.exists() } ?: return null
        return try {
            file.inputStream().use { Json.decodeFromStream(serializer, it) }
        } catch (e: Exception) {
            val backup = backupUnreadableState()
            logger.error(e) {
                "Failed to load widgets state from $statePath, widgets would use the default one. " +
                    if (backup != null) "The original file is kept as $backup." else "The original file was lost."
            }
            null
        }
    }

    private fun backupUnreadableState(): Path? = try {
        statePath.resolveSibling("${statePath.fileName}.${Clock.System.now().toEpochMilliseconds()}")
            .also { Files.move(statePath, it) }
    } catch (e: Exception) {
        logger.error(e) { "Failed to back up unreadable widgets state from $statePath" }
        null
    }

    private companion object {
        private val logger by getLogger()
    }
}

class PersistenceRegistry {
    private val services = mutableListOf<PersistentStorageService<*>>()

    fun register(service: PersistentStorageService<*>) {
        services.add(service)
    }

    inline fun <reified T: Any> register(persistentData: PersistentData<T>, storagePath: Path) {
        register(PersistentStorageService(storagePath, persistentData, serializer()))
    }

    suspend fun restoreAll() {
        services.forEach { it.restore() }
    }

    fun startSaving(scope: CoroutineScope) {
        services.forEach { service -> scope.launch { service.runSaving() } }
    }
}
