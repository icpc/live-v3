package org.icpclive.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.util.concurrent.TimeUnit
import org.slf4j.Logger
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

@OptIn(ExperimentalSerializationApi::class)
public inline fun <reified T> fileJsonContentFlow(path: Path, logger: Logger): Flow<T> = fileChangesFlow(path).map {
    logger.info("Reloaded ${path.fileName}")
    path.inputStream().use {
        Json.decodeFromStream<T>(it)
    }
}.flowOn(Dispatchers.IO)
    .logAndRetryWithDelay(5.seconds) {
        logger.error("Failed to reload ${path.fileName}", it)
    }

public inline fun <reified T> fileJsonContentFlow(path: Path, logger: Logger, noData: T): Flow<T> =
    fileJsonContentFlow<T>(path, logger)
        .onStart { if (!path.exists()) emit(noData) }
