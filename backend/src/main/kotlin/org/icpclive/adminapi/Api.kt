package org.icpclive.adminapi

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.icpclive.api.*


internal inline fun <reified SettingsType : ObjectSettings, reified WidgetType : Widget> Route.setupSimpleWidgetRouting(
        initialSettings: SettingsType,
        noinline createWidget: (SettingsType) -> WidgetType
) {
    val widgetWrapper = WidgetWrapper(createWidget = createWidget, initialSettings)
    get {
        call.adminApiAction {
            val response = widgetWrapper.getStatus()

            call.respond(response)
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

internal inline fun <reified SettingsType : ObjectSettings, reified WidgetType : Widget> Route.setupPresetWidgetRouting(
        presetPath: String,
        noinline createWidget: (SettingsType) -> WidgetType,
) {
    val presets = Presets(presetPath, createWidget)
    get {
        call.respond(presets.getStatus())
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
    //TODO: why not delete("/{id}")?
    post("/{id}/delete") {
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

internal inline fun Route.setupPresetTickerRouting(
        presetPath: String,
        noinline createMessage: (TickerMessageSettings) -> TickerMessage,
) {
    val presets = TickerPresets(presetPath, createMessage)
    get {
        call.respond(presets.getStatus())
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
    //TODO: why not delete("/{id}")?
    post("/{id}/delete") {
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