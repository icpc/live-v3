package org.icpclive.cds.util

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.serializer
import java.io.InputStream
import java.nio.file.*
import java.util.concurrent.*
import kotlin.io.path.*
import kotlin.time.Duration.Companion.seconds


public fun directoryChangesFlow(path: Path): Flow<Path> = flow {
    while (currentCoroutineContext().isActive) {
        path.fileSystem.newWatchService().use { watcher ->
            path.register(
                watcher,
                arrayOf(StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY)
            )
            path.listDirectoryEntries().forEach { emit(it.fileName) }
            while (currentCoroutineContext().isActive) {
                val key = watcher.poll(100, TimeUnit.MILLISECONDS) ?: continue
                for (event in key.pollEvents()) {
                    val kind = event.kind()
                    if (kind === StandardWatchEventKinds.OVERFLOW) {
                        path.listDirectoryEntries().forEach { emit(it) }
                        continue
                    }
                    @Suppress("UNCHECKED_CAST")
                    emit((event as WatchEvent<Path>).context())
                }

                if (!key.reset()) {
                    break
                }
            }
        }
    }
}.map { path.resolve(it).toAbsolutePath() }
    .flowOn(Dispatchers.IO)

public fun fileChangesFlow(path: Path): Flow<Path> = directoryChangesFlow(path.toAbsolutePath().parent)
    .filter { it.endsWith(path.fileName) }

public fun <T : Any> fileContentFlow(
    path: Path,
    noData: T? = null,
    reader: (InputStream) -> T,
): Flow<T> =
    fileChangesFlow(path)
        .map {
            logger.info { "Reloaded ${path.fileName}" }
            path.inputStream().use { reader(it) }
        }
        .flowOn(Dispatchers.IO)
        .logAndRetryWithDelay(5.seconds) { logger.error(it) { "Failed to reload ${path.fileName}" } }
        .onStart { if (!path.exists() && noData != null) emit(noData) }



public inline fun <reified T : Any> fileJsonContentFlow(path: Path, json: Json = Json, noData: T? = null): Flow<T> =
    fileContentFlow(path, noData) { json.decodeFromStream<T>(it) }

private val logger by getLogger()