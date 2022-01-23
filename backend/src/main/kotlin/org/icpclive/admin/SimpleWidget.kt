package org.icpclive.admin

import io.ktor.application.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.routing.*
import kotlinx.html.HTML
import org.icpclive.WidgetManager
import org.icpclive.api.Widget

open class SimpleWidgetUrls(prefix: String) : Urls {
    override val mainPage = "/admin/$prefix"
    val showQuery = "/admin/$prefix/show"
    val hideQuery = "/admin/$prefix/hide"
    val reloadQuery = "/admin/$prefix/reload"
}

inline fun <reified ContentType, reified WidgetType: Widget> Routing.setupSimpleWidgetRouting(
    prefix: String,
    widgetId: String,
    presetPath: String,
    crossinline view: HTML.(presets:Presets<ContentType>, urls:SimpleWidgetUrls) -> Unit,
    crossinline createWidget: (Parameters) -> WidgetType,
) : SimpleWidgetUrls {
    val urls = SimpleWidgetUrls(prefix)
    val presets = Presets<ContentType>(presetPath)
    get(urls.mainPage) { call.respondHtml { view(presets, urls) }}
    post(urls.showQuery) {
        call.catchAdminAction(urls.mainPage) {
            WidgetManager.showWidget(createWidget(receiveParameters()))
            respondHtml { view(presets, urls) }
        }
    }
    post(urls.hideQuery) {
        call.catchAdminAction(urls.mainPage) {
            WidgetManager.hideWidget(widgetId)
            respondHtml { view(presets, urls) }
        }
    }
    post(urls.reloadQuery) {
        call.catchAdminAction(urls.mainPage) {
            presets.reload()
            respondHtml { view(presets, urls) }
        }
    }
    return urls
}