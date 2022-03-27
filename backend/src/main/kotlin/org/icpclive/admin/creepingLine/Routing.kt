package org.icpclive.admin.creepingLine

import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.runBlocking
import kotlinx.html.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.icpclive.admin.*
import org.icpclive.admin.setupSimpleWidgetRouting
import org.icpclive.admin.simpleWidgetViewFun
import org.icpclive.api.*
import org.icpclive.data.CreepingLineManager

fun Routing.configureCreepingLine(presetPath: String) : SimpleWidgetUrls {
    val extra = fun BODY.(presets: Presets<CreepingLineMessage>?) {
        val shown = runBlocking { CreepingLineManager.getMessagesSubscribeEvents().second.map { it.id }.toSet() }
        form {
            method = FormMethod.post
            action = "/admin/creepingLine/add"
            for (i in presets!!.data.get()) {
                if (i.id !in shown) {
                    p {
                        radioInput {
                            name = "value"
                            value = Json.encodeToString(i)
                            +Json.encodeToString(i)
                        }
                    }
                }
            }
            submitInput { value = "Add" }
        }
        form {
            method = FormMethod.post
            action = "/admin/creepingLine/remove"
            for (i in presets!!.data.get()) {
                if (i.id in shown) {
                    p {
                        radioInput {
                            name = "value"
                            value = i.id.toString()
                            +Json.encodeToString(i)
                        }
                    }
                }
            }
            submitInput { value = "Remove" }
        }
    }
    val urls = setupSimpleWidgetRouting(
        prefix = "creepingLine",
        widgetId = CreepingLineWidget.WIDGET_ID,
        presetPath = presetPath,
        createWidget = {
            CreepingLineWidget(CreepingLineSettings())
        },
        view = simpleWidgetViewFun("Creeping Line", extra) {},
    )
    route("/admin/creepingLine") {
        post("/add") {
            call.catchAdminAction(urls.mainPage) {
                CreepingLineManager.addMessage(Json.decodeFromString(receiveParameters()["value"] ?: throw AdminActionException("No value specified")))
                respondRedirect(urls.mainPage)
            }
        }
        post("/remove") {
            call.catchAdminAction(urls.mainPage) {
                CreepingLineManager.removeMessage(Json.decodeFromString(receiveParameters()["value"] ?: throw AdminActionException("No value specified")))
                respondRedirect(urls.mainPage)
            }

        }
    }
    return urls
}