package org.icpclive.controllers

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import org.icpclive.api.ObjectSettings
import org.icpclive.api.TypeWithId
import org.icpclive.cds.util.getLogger
import java.nio.file.Path

private val logger by getLogger()

class WidgetsStateController(private val statePath: Path) {
    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Default)
    private val saveRequests = Channel<Unit>(Channel.CONFLATED)

    private val restorers = mutableMapOf<String, suspend (JsonElement) -> Unit>()
    private val snapshots = mutableMapOf<String, suspend () -> JsonElement>()
    private val states = mutableMapOf<String, JsonElement>()

    fun <SettingsType : ObjectSettings, WidgetType : TypeWithId> register(
        name: String,
        controller: SingleWidgetController<SettingsType, WidgetType>,
        settingsSerializer: KSerializer<SettingsType>,
    ): SingleWidgetController<SettingsType, WidgetType> {
        val stateSerializer = WidgetState.serializer(settingsSerializer)
        restorers[name] = { element ->
            val state = widgetStateJson.decodeFromJsonElement(stateSerializer, element)
            controller.setSettings(state.settings)
            if (state.visible) {
                controller.show()
            }
        }
        snapshots[name] = {
            val status = controller.getStatus()
            widgetStateJson.encodeToJsonElement(stateSerializer, WidgetState(status.settings, status.shown))
        }
        controller.setStateListener { state ->
            mutex.withLock { states[name] = widgetStateJson.encodeToJsonElement(stateSerializer, state) }
            val _ = saveRequests.trySend(Unit)
        }
        return controller
    }

    /**
     * Restores the state of all registered widgets, and then keeps updating the file
     * on any change.
     *
     * You must call [register] for all widgets before calling this method.
     */
    fun launchStateSync() {
        scope.launch {
            mutex.withLock {
                for ((name, snapshot) in snapshots) {
                    states[name] = snapshot()
                }
            }
            restore()

            while (saveRequests.tryReceive().isSuccess) {
                // no-op, just dropping the requests
            }
            while (true) {
                // the request itself carries no data, it only signals that [states] was changed
                saveRequests.receive()
                save()
            }
        }
    }

    private suspend fun restore() {
        for ((name, state) in load()) {
            val restorer = restorers[name]
            if (restorer == null) {
                logger.warning { "Unknown widget $name in $statePath, ignoring it" }
                continue
            }
            try {
                restorer(state)
            } catch (e: Exception) {
                logger.error(e) { "Failed to restore state of widget $name, it would use the default one" }
                // the state we failed to restore is kept in the file, so it's not lost by the next save
                mutex.withLock { states[name] = state }
            }
        }
    }

    private fun load(): Map<String, JsonElement> {
        val file = statePath.toFile().takeIf { it.exists() } ?: return emptyMap()
        return try {
            file.inputStream().use { widgetStateJson.decodeFromStream<JsonObject>(it) }
        } catch (e: Exception) {
            logger.error(e) { "Failed to load widgets state from $statePath, widgets would use the default one" }
            emptyMap()
        }
    }

    private suspend fun save() {
        val content = mutex.withLock { states.toMap() }
        try {
            statePath.writeJsonAtomically(fileSerializer, content)
        } catch (e: Exception) {
            logger.error(e) { "Failed to save widgets state to $statePath" }
        }
    }

    companion object {
        private val fileSerializer = MapSerializer(String.serializer(), JsonElement.serializer())
    }
}

inline fun <reified SettingsType : ObjectSettings, WidgetType : TypeWithId> WidgetsStateController.register(
    name: String,
    controller: SingleWidgetController<SettingsType, WidgetType>,
): SingleWidgetController<SettingsType, WidgetType> = register(name, controller, serializer())
