package org.icpclive.admin

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerializationException
import org.icpclive.api.*
import org.icpclive.data.DataBus
import org.icpclive.data.TickerManager
import org.icpclive.data.WidgetManager
import java.nio.file.Path

private suspend inline fun <reified T> ApplicationCall.safeReceive(): T = try {
    receive()
} catch (e: SerializationException) {
    throw AdminActionApiException("Failed to deserialize data")
}

internal inline fun <reified SettingsType : ObjectSettings, reified WidgetType : Widget> Route.setupSimpleWidgetRouting(
    widgetWrapper: Wrapper<SettingsType, WidgetType>
) {
    get {
        // run is workaround for https://youtrack.jetbrains.com/issue/KT-34051
        run {
            call.respond(widgetWrapper.getStatus())
        }
    }
    post {
        call.adminApiAction {
            widgetWrapper.set(call.safeReceive())
        }
        DataBus.adminActionsFlow.emit(call.request.uri)
    }
    post("/show") {
        call.adminApiAction {
            widgetWrapper.show()
        }
        DataBus.adminActionsFlow.emit(call.request.uri)
    }
    post("/show_with_settings") {
        call.adminApiAction {
            widgetWrapper.show(call.safeReceive())
        }
        DataBus.adminActionsFlow.emit(call.request.uri)
    }
    post("/hide") {
        call.adminApiAction {
            widgetWrapper.hide()
        }
        DataBus.adminActionsFlow.emit(call.request.uri)
    }
    get("/preview") {
        // run is workaround for https://youtrack.jetbrains.com/issue/KT-34051
        run {
            call.respond(widgetWrapper.getWidget())
        }
    }
}

private fun ApplicationCall.id() =
    parameters["id"]?.toIntOrNull() ?: throw AdminActionApiException("Error load preset by id")

internal inline fun <reified SettingsType : ObjectSettings, reified WidgetType : TypeWithId> Route.setupPresetRouting(
    presets: PresetsManager<SettingsType, WidgetType>
) {
    get {
        // run is workaround for https://youtrack.jetbrains.com/issue/KT-34051
        run {
            call.respond(presets.getStatus())
        }
    }
    post {
        call.adminApiAction {
            presets.append(call.safeReceive())
        }
        DataBus.adminActionsFlow.emit(call.request.uri)
    }
    post("/reload") {
        call.adminApiAction {
            presets.reload()
        }
        DataBus.adminActionsFlow.emit(call.request.uri)
    }
    post("/create_and_show_with_ttl") {
        call.adminApiAction {
            presets.createAndShowWithTtl(
                call.safeReceive<SettingsType>(),
                call.request.queryParameters["ttl"]?.toLong()
            )
        }
        DataBus.adminActionsFlow.emit(call.request.uri)
    }
    post("/{id}") {
        call.adminApiAction {
            presets.edit(call.id(), call.safeReceive())
        }
        DataBus.adminActionsFlow.emit(call.request.uri)
    }
    delete("/{id}") {
        call.adminApiAction {
            presets.delete(call.id())
        }
        DataBus.adminActionsFlow.emit(call.request.uri)
    }
    post("/{id}/show") {
        call.adminApiAction {
            presets.show(call.id())
        }
        DataBus.adminActionsFlow.emit(call.request.uri)
    }
    post("/{id}/hide") {
        call.adminApiAction {
            presets.hide(call.id())
        }
        DataBus.adminActionsFlow.emit(call.request.uri)
    }
    get("/{id}/preview") {
        call.adminApiAction {
            call.respond(presets.getWidget(call.id()))
        }
    }
}

internal inline fun <reified SettingsType : ObjectSettings, reified WidgetType : Widget> Route.setupSimpleWidgetRouting(
    initialSettings: SettingsType,
    noinline createWidget: (SettingsType) -> WidgetType
) = setupSimpleWidgetRouting(Wrapper(createWidget, initialSettings, WidgetManager))

internal inline fun <reified SettingsType : ObjectSettings, reified WidgetType : Widget> Route.setupPresetWidgetRouting(
    presetPath: Path,
    noinline createWidget: (SettingsType) -> WidgetType
) = setupPresetRouting(Presets(presetPath, WidgetManager, createWidget))

internal fun Route.setupPresetTitleRouting(
    presetPath: Path,
    createMessage: (TitleSettings) -> SvgWidget,
) {
    val presetsManager = Presets(presetPath, WidgetManager, createMessage)
    setupPresetRouting(presetsManager)
    get("/{id}/preview") {
        call.adminApiAction {
            val id = call.id()
            createMessage(presetsManager.get(id).settings).content
        }
    }
}

internal fun Route.setupPresetTickerRouting(
    presetPath: Path,
    createMessage: (TickerMessageSettings) -> TickerMessage,
) = setupPresetRouting(Presets(presetPath, TickerManager, createMessage))
