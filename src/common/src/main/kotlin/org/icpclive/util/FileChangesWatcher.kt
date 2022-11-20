package org.icpclive.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.util.concurrent.TimeUnit
import kotlin.io.path.listDirectoryEntries


fun directoryChangesFlow(path: Path) =
    flow {
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

fun fileChangesFlow(path: Path) = directoryChangesFlow(path.parent.toAbsolutePath())
    .filter { it.endsWith(path.fileName) }