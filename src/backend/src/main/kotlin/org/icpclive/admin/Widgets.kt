package org.icpclive.admin

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.icpclive.api.ObjectSettings
import org.icpclive.api.TypeWithId
import org.icpclive.controllers.PresetsController
import org.icpclive.controllers.SingleWidgetController
import kotlin.time.Duration.Companion.milliseconds

inline fun <reified SettingsType : ObjectSettings, reified OverlayWidgetType : TypeWithId> Route.setupController(
    controller: SingleWidgetController<SettingsType, OverlayWidgetType>
) {
    get {
        // run is workaround for https://youtrack.jetbrains.com/issue/KT-34051
        run {
            call.respond(controller.getStatus())
        }
    }
    post {
        call.adminApiAction {
            controller.setSettings(call.safeReceive())
        }
    }
    post("/show") {
        call.adminApiAction {
            controller.show()
        }
    }
    post("/show_with_settings") {
        call.adminApiAction {
            controller.show(call.safeReceive())
        }
    }
    post("/hide") {
        call.adminApiAction {
            controller.hide()
        }
    }
    get("/preview") {
        // run is workaround for https://youtrack.jetbrains.com/issue/KT-34051
        run {
            call.respond(controller.previewWidget())
        }
    }
    post("/preview") {
        run {
            call.respond(controller.previewWidget(call.safeReceive<SettingsType>()))
        }
    }
}

inline fun <reified SettingsType : ObjectSettings, reified OverlayWidgetType : TypeWithId> Route.setupControllerGroup(
    controllers: Map<String, SingleWidgetController<SettingsType, OverlayWidgetType>>
) {
    get {
        // run is workaround for https://youtrack.jetbrains.com/issue/KT-34051
        run {
            call.respond(controllers.mapValues { it.value.getStatus() })
        }
    }
    post("/show") {
        call.adminApiAction {
            controllers.forEach { it.value.show() }
        }
    }
    post("/hide") {
        call.adminApiAction {
            controllers.forEach { it.value.hide() }
        }
    }
}

fun ApplicationCall.id() =
    parameters["id"]?.toIntOrNull() ?: throw ApiActionException("Error load preset by id")

inline fun <reified SettingsType : ObjectSettings, reified OverlayWidgetType : TypeWithId> Route.setupController(
    controller: PresetsController<SettingsType, OverlayWidgetType>
) {
    get {
        // run is workaround for https://youtrack.jetbrains.com/issue/KT-34051
        run {
            call.respond(controller.getStatus())
        }
    }
    post {
        call.adminApiAction {
            controller.createWidget(call.safeReceive(), null)
        }
    }
    post("/reload") {
        call.adminApiAction { controller.reload() }
    }
    post("/create_and_show_with_ttl") {
        call.adminApiAction {
            val ttl =
                call.request.queryParameters["ttl"]?.toLongOrNull() ?: throw ApiActionException("ttl should be set")
            controller.createWidget(
                call.safeReceive(),
                ttl.milliseconds
            ).apply {
                controller.show(this)
            }
        }
    }
    post("/{id}") {
        call.adminApiAction {
            controller.edit(call.id(), call.safeReceive())
        }
    }
    delete("/{id}") {
        call.adminApiAction {
            controller.delete(call.id())
        }
    }
    post("/{id}/show") {
        call.adminApiAction {
            controller.show(call.id())
        }
    }
    post("/{id}/hide") {
        call.adminApiAction {
            controller.hide(call.id())
        }
    }
    get("/{id}/preview") {
        call.adminApiAction {
            call.respond(controller.previewWidget(call.id()))
        }
    }
}
