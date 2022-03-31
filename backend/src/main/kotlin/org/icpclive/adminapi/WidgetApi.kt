package org.icpclive.adminapi

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.icpclive.admin.AdminActionException
import org.icpclive.admin.Urls
import org.icpclive.api.ContentPreset
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

internal inline fun <reified WidgetType : Widget> Routing.setupSimpleWidgetRouting(
        prefix: String,
        widgetId: String,
        noinline createWidget: (Parameters) -> WidgetType,
): SimpleWidgetApiUrls {
    val urls = SimpleWidgetApiUrls(prefix)
    val widgetWrapper = WidgetWrapper(createWidget)
    get(urls.mainPage) {
        call.catchAdminApiAction {
            val response = widgetWrapper.status
            call.respond(response)
        }
    }
    post(urls.showPage) {
        call.catchAdminApiAction {
            widgetWrapper.show(receiveParameters())
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

internal inline fun <reified ContentType : ContentPreset, reified WidgetType : Widget> Routing.setupPresetWidgetRouting(
        prefix: String,
        widgetId: String,
        presetPath: String,
        noinline createWidget: (ContentType) -> WidgetType,
): PresetWidgetApiUrls {
    val urls = PresetWidgetApiUrls(prefix)
    val presets = Presets<ContentType, WidgetType>(presetPath, createWidget)
    get(urls.mainPage) {
        presets.let {
            val response = it.data
            call.respond(response)
        }
    }
    post(urls.addPage) {
        call.catchAdminApiAction {
            val getting = call.receive<ContentType>()

            presets.append(getting)
            call.respond(mapOf("status" to "ok"))
        }
    }
    post(urls.editPage) {
        call.catchAdminApiAction {
            val id = call.parameters["id"]?.toInt() ?: throw AdminActionException("Error load preset by id")
            val getting = call.receive<ContentType>()

            presets.edit(id, getting)
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