package org.icpclive.admin.ticker

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import kotlinx.html.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.icpclive.admin.*
import org.icpclive.api.*
import org.icpclive.data.TickerManager

fun Routing.configureTicker(presetPath: String) : SimpleWidgetUrls {
    val extra = fun BODY.(presets: Presets<TickerMessage>?) {
        val shown = runBlocking { TickerManager.getMessagesSubscribeEvents().second.map { it.id }.toSet() }
        form {
            method = FormMethod.post
            action = "/admin/ticker/add"
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
            action = "/admin/ticker/remove"
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
        prefix = "ticker",
        widgetId = TickerWidget.WIDGET_ID,
        presetPath = presetPath,
        createWidget = {
            TickerWidget(TickerSettings())
        },
        view = simpleWidgetViewFun("Ticker", extra) {},
    )
    route("/admin/ticker") {
        post("/add") {
            call.catchAdminAction(urls.mainPage) {
                TickerManager.addMessage(Json.decodeFromString(receiveParameters()["value"] ?: throw AdminActionException("No value specified")))
                respondRedirect(urls.mainPage)
            }
        }
        post("/remove") {
            call.catchAdminAction(urls.mainPage) {
                TickerManager.removeMessage(Json.decodeFromString(receiveParameters()["value"] ?: throw AdminActionException("No value specified")))
                respondRedirect(urls.mainPage)
            }

        }
    }
    return urls
}
