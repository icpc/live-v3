package org.icpclive.adminapi

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.icpclive.admin.AdminActionException
import org.icpclive.admin.Urls
import org.icpclive.api.ObjectSettings
import org.icpclive.api.Widget

open class SimpleWidgetApiUrls(prefix: String) : Urls {
    override val mainPage = "/adminapi/$prefix"
    val showPage = "/adminapi/$prefix/show"
    val hidePage = "/adminapi/$prefix/hide"
}

open class PresetWidgetApiUrls(prefix: String) : Urls {
    override val mainPage = "/adminapi/$prefix"
    val showPage = "/adminapi/$prefix/{id}/show"
    val hidePage = "/adminapi/$prefix/{id}/hide"
    val addPage = "/adminapi/$prefix"
    val editPage = "/adminapi/$prefix/{id}"
    val deletePage = "/adminapi/$prefix/{id}/delete"
}

internal inline fun <reified SettingsType : ObjectSettings, reified WidgetType : Widget> Routing.setupSimpleWidgetRouting(
        prefix: String,
        noinline createWidget: (SettingsType) -> WidgetType,
): SimpleWidgetApiUrls {
    val urls = SimpleWidgetApiUrls(prefix)
    val widgetWrapper = WidgetWrapper(createWidget)
    get(urls.mainPage) {
        call.catchAdminApiAction {
            val response = widgetWrapper.getStatus()
            call.respond(response)
        }
    }
    post(urls.showPage) {
        call.catchAdminApiAction {
            val settings = call.receive<SettingsType>()
            widgetWrapper.show(settings)
            call.respond(mapOf("status" to "ok"))
        }
    }
    post(urls.hidePage) {
        call.catchAdminApiAction {
            widgetWrapper.hide()
            call.respond(mapOf("status" to "ok"))
        }
    }
    return urls
}

internal inline fun <reified SettingsType : ObjectSettings, reified WidgetType : Widget> Routing.setupPresetWidgetRouting(
        prefix: String,
        presetPath: String,
        noinline createWidget: (SettingsType) -> WidgetType,
): PresetWidgetApiUrls {
    val urls = PresetWidgetApiUrls(prefix)
    val presets = Presets<SettingsType, WidgetType>(presetPath, createWidget)
    get(urls.mainPage) {
        presets.let {
            val response = it.getStatus()
            call.respond(response)
        }
    }
    post(urls.addPage) {
        call.catchAdminApiAction {
            val settings = call.receive<SettingsType>()

            presets.append(settings)
            call.respond(mapOf("status" to "ok"))
        }
    }
    post(urls.editPage) {
        call.catchAdminApiAction {
            val id = call.parameters["id"]?.toInt() ?: throw AdminActionException("Error load preset by id")
            val settings = call.receive<SettingsType>()

            presets.edit(id, settings)
            call.respond(mapOf("status" to "ok"))
        }
    }
    post(urls.deletePage) {
        call.catchAdminApiAction {
            val id = call.parameters["id"]?.toInt() ?: throw AdminActionException("Error load preset by id")

            presets.delete(id)
            call.respond(mapOf("status" to "ok"))
        }
    }
    post(urls.showPage) {
        call.catchAdminApiAction {
            val id = call.parameters["id"]?.toInt() ?: throw AdminActionException("Error load preset by id")

            presets.show(id)
            call.respond(mapOf("status" to "ok"))
        }
    }
    post(urls.hidePage) {
        call.catchAdminApiAction {
            val id = call.parameters["id"]?.toInt() ?: throw AdminActionException("Error load preset by id")

            presets.hide(id)
            call.respond(mapOf("status" to "ok"))
        }
    }
    return urls
}