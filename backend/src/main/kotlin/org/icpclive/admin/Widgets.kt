package org.icpclive.admin

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.icpclive.api.ObjectSettings
import org.icpclive.api.TypeWithId
import org.icpclive.widget.PresetsController
import org.icpclive.widget.SimpleController

inline fun <reified SettingsType : ObjectSettings, reified OverlayWidgetType : TypeWithId> Route.setupController(
    controller: SimpleController<SettingsType, OverlayWidgetType>
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
            call.respond(controller.getWidget())
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
            controller.append(call.safeReceive())
        }
    }
    post("/reload") {
        call.adminApiAction { controller.reload() }
    }
    post("/create_and_show_with_ttl") {
        call.adminApiAction {
            controller.createAndShowWithTtl(
                call.safeReceive(),
                call.request.queryParameters["ttl"]?.toLong()
            )
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
            call.respond(controller.getWidget(call.id()))
        }
    }
}
