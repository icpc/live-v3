package org.icpclive.adminapi

import io.ktor.application.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.html.*
import org.icpclive.admin.AdminActionException
import org.icpclive.data.WidgetManager
import org.icpclive.admin.Urls
import org.icpclive.admin.catchAdminAction
import org.icpclive.api.ContentPreset
import org.icpclive.api.Widget

open class SimpleWidgetApiUrls(prefix: String, val isReloadable: Boolean) : Urls {
    override val mainPage = "/adminapi/$prefix"
    val showQuery = "/adminapi/$prefix/show"
    val hideQuery = "/adminapi/$prefix/hide"
    val reloadQuery = "/adminapi/$prefix/reload"
    val addPresetQuery = "/adminapi/$prefix/add"
    val deletePresetQuery = "/adminapi/$prefix/delete"
}

internal inline fun <reified ContentType : ContentPreset, reified WidgetType : Widget> Routing.setupSimpleWidgetRouting(
    prefix: String,
    widgetId: String,
    presetPath: String?,
    crossinline createContent: (Parameters) -> ContentType,
    crossinline createWidget: (ContentType) -> WidgetType,
): SimpleWidgetApiUrls {
    val urls = SimpleWidgetApiUrls(prefix, presetPath != null)
    val presets = presetPath?.let { Presets<ContentType>(it) }
    get(urls.mainPage) {
        presets?.let {
            val response = it.data
            call.response.header("Access-Control-Expose-Headers", "X-Total-Count")
            call.response.header("Content-Range", response.size.toString())
            call.respond(response)
        }
    }
//    get("${urls.mainPage}/{id}") {
//        call.catchAdminApiAction {
//            val id = call.parameters["id"]?.toInt() ?: throw AdminActionException("Error load preset by id")
//
//            presets?.let {
//                for (preset in it.data) {
//                    if (preset.id == id) {
//                        call.respond(preset)
//                        return@let
//                    }
//                }
//            }
//        }
//    }
//    post(urls.mainPage) {
//        call.catchAdminApiAction {
//            val prm = call.parameters
//            val getting = call.receive<ContentType>()
//            println(getting)
//
//            call.respond(mapOf("status" to "ok"))
////
//            presets?.let {}
//        }
//    }
////    post("${urls.mainPage}/{id}") {
////        call.catchAdminApiAction {
////            val id = call.parameters["id"]?.toInt() ?: throw AdminActionException("Error load preset by id")
////
////            presets?.let {
////                for (preset in it.data) {
////                    if (preset.id == id) {
////                        call.respond(preset)
////                        return@let
////                    }
////                }
////            }
////        }
////    }
//    post(urls.showQuery) {
//        call.catchAdminApiAction {
//            WidgetManager.showWidget(createWidget(createContent(receiveParameters())))
//            call.respond(mapOf("status" to "ok"))
//        }
//    }
//    post(urls.hideQuery) {
//        call.catchAdminApiAction {
//            WidgetManager.hideWidget(widgetId)
//            call.respond(mapOf("status" to "ok"))
//        }
//    }
//
//    if (urls.isReloadable) {
//        post(urls.addPresetQuery) {
//            call.catchAdminApiAction {
//                presets?.let { it.data += listOf(createContent(receiveParameters())) }
//                call.respond(mapOf("status" to "ok"))
//            }
//        }
//        post(urls.deletePresetQuery) {
//            call.catchAdminApiAction {
//                presets?.let { it.data -= listOf(createContent(receiveParameters())) }
//                call.respond(mapOf("status" to "ok"))
//            }
//        }
//        post(urls.reloadQuery) {
//            call.catchAdminAction(urls.mainPage) {
//                respondText("abobus")
//            }
//        }
//    }
    return urls
}

internal fun HTML.simpleWidgetApiView(name: String, urls: SimpleWidgetApiUrls, parameters: FORM.() -> Unit) {
    body {
        adminApiHead()
        h1 { +name }
        form {
            method = FormMethod.post
            action = urls.showQuery

            parameters()
            submitInput { value = "Show" }
        }
        form {
            method = FormMethod.post
            action = urls.hideQuery
            submitInput { value = "Hide" }
        }
        if (urls.reloadQuery != null) {
            form {
                method = FormMethod.post
                action = urls.reloadQuery
                submitInput { value = "Reload" }
            }
        }
    }
}

internal inline fun <reified T> simpleWidgetApiViewFun(
    name: String,
    crossinline parameters: FORM.(Presets<T>?) -> Unit
): HTML.(Presets<T>?, SimpleWidgetApiUrls) -> Unit = { presets, urls ->
    simpleWidgetApiView(name, urls) { parameters(presets) }
}