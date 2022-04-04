package org.icpclive.adminapi

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import org.icpclive.api.*
import org.icpclive.data.TickerManager
import org.icpclive.data.WidgetManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

// TODO: Unify with PresetManager

class TickerWrappper(
        private val createMessage: (TickerMessageSettings) -> TickerMessage,
        private var settings: TickerMessageSettings,
        val id: Int? = null) {
    private val mutex = Mutex()

    private var tickerId: String? = null

    suspend fun getStatus(): ObjectStatus<TickerMessageSettings> = mutex.withLock {
        return ObjectStatus(tickerId != null, settings, id)
    }

    //TODO: Use under mutex
    fun getSettings(): TickerMessageSettings {
        return settings
    }

    suspend fun set(newSettings: TickerMessageSettings) {
        mutex.withLock {
            settings = newSettings
        }
    }

    suspend fun show() {
        mutex.withLock {
            if (tickerId != null)
                return
            val message = createMessage(settings)
            TickerManager.addMessage(message)
            tickerId = message.id
        }
    }

    suspend fun show(newSettings: TickerMessageSettings) {
        set(newSettings)
        show()
    }

    suspend fun hide() {
        mutex.withLock {
            tickerId?.let {
                TickerManager.removeMessage(it)
            }
            tickerId = null
        }
    }
}

class TickerPresetsManager(
        private val path: String,
        private val decode: (String) -> List<TickerWrappper>,
        private val encode: (List<TickerWrappper>, String) -> Unit,
        private val createMessage: (TickerMessageSettings) -> TickerMessage,
        private var innerData: List<TickerWrappper> = decode(path),
        private var currentID: Int = innerData.size
) {
    private val mutex = Mutex()

    suspend fun getStatus(): List<ObjectStatus<TickerMessageSettings>> = mutex.withLock {
        return innerData.map { it.getStatus() }
    }

    suspend fun append(settings: TickerMessageSettings) {
        mutex.withLock {
            innerData = innerData.plus(TickerWrappper(createMessage, settings, ++currentID))
        }
        save()
    }

    suspend fun edit(id: Int, content: TickerMessageSettings) {
        mutex.withLock {
            for (preset in innerData) {
                if (preset.id == id)
                    preset.set(content)
            }
        }
        save()
    }

    suspend fun delete(id: Int) {
        mutex.withLock {
            for (preset in innerData) {
                if (preset.id != id)
                    continue
                preset.hide()
            }
            innerData = innerData.filterNot { it.id == id }
        }
        save()
    }

    suspend fun show(id: Int) {
        mutex.withLock {
            for (preset in innerData) {
                if (preset.id != id)
                    continue
                preset.show()
                break
            }
        }
    }

    suspend fun hide(id: Int) {
        mutex.withLock {
            for (preset in innerData) {
                if (preset.id != id)
                    continue
                preset.hide()
            }
        }
    }

    suspend private fun load() {
        mutex.withLock {
            innerData = decode(path)
        }
    }

    suspend private fun save() {
        mutex.withLock {
            encode(innerData, path)
        }
    }
}

inline fun TickerPresets(path: String,
                                noinline createMessage: (TickerMessageSettings) -> TickerMessage) =
        TickerPresetsManager(path,
                {
                    Json.decodeFromStream<List<TickerMessageSettings>>(FileInputStream(File(it))).mapIndexed { index, content ->
                        TickerWrappper(createMessage, content, index + 1)
                    }
                },
                { data, fileName ->
                    Json { prettyPrint = true }.encodeToStream(data.map { it.getSettings() }, FileOutputStream(File(fileName)))
                },
                createMessage)