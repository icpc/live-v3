package org.icpclive.adminapi

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.icpclive.api.*
import org.icpclive.data.TickerManager
import org.icpclive.data.WidgetManager


internal inline fun <reified SettingsType : ObjectSettings, reified WidgetType : Widget> Route.setupSimpleWidgetRouting(
        initialSettings: SettingsType,
        noinline createWidget: (SettingsType) -> WidgetType
) {
    val widgetWrapper = Wrapper(createWidget, initialSettings, WidgetManager)
    get {
        call.adminApiAction {
            call.respond(widgetWrapper.getStatus())
        }
    }
    post("/show") {
        call.adminApiAction {
            widgetWrapper.show(call.receive())
        }
    }
    post("/hide") {
        call.adminApiAction {
            widgetWrapper.hide()
        }
    }
}


fun ApplicationCall.id() = parameters["id"]?.toIntOrNull() ?: throw AdminActionApiException("Error load preset by id")

internal inline fun <reified SettingsType : ObjectSettings, reified WidgetType : TypeWithId> Route.setupPresetRouting(
    presets: PresetsManager<SettingsType, WidgetType>
) {
    get {
        //TODO: Somehow it drops an error when you erase let
        presets?.let {
            call.respond(it.getStatus())
        }
    }
    post {
        call.adminApiAction {
            presets.append(call.receive())
        }
    }
    post("/{id}") {
        call.adminApiAction {
            presets.edit(call.id(), call.receive())
        }
    }
    delete("/{id}") {
        call.adminApiAction {
            presets.delete(call.id())
        }
    }
    post("/{id}/show") {
        call.adminApiAction {
            presets.show(call.id())
        }
    }
    post("/{id}/hide") {
        call.adminApiAction {
            presets.hide(call.id())
        }
    }
}

internal inline fun <reified SettingsType : ObjectSettings, reified WidgetType : Widget> Route.setupPresetWidgetRouting(
    presetPath: String,
    noinline createWidget: (SettingsType) -> WidgetType
) = setupPresetRouting(Presets(presetPath, WidgetManager, createWidget))

internal fun Route.setupPresetTickerRouting(
        presetPath: String,
        createMessage: (TickerMessageSettings) -> TickerMessage,
) = setupPresetRouting(Presets(presetPath, TickerManager, createMessage))