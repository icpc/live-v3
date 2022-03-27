package org.icpclive.admin

import io.ktor.application.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.routing.*
import kotlinx.html.*
import org.icpclive.data.WidgetManager
import org.icpclive.api.Widget

open class SimpleWidgetUrls(prefix: String, reloadable: Boolean) : Urls {
    override val mainPage = "/admin/$prefix"
    val showQuery = "/admin/$prefix/show"
    val hideQuery = "/admin/$prefix/hide"
    val reloadQuery = "/admin/$prefix/reload".takeIf { reloadable }
}

internal inline fun <reified ContentType, reified WidgetType : Widget> Routing.setupSimpleWidgetRouting(
    prefix: String,
    widgetId: String,
    presetPath: String?,
    crossinline view: HTML.(presets: Presets<ContentType>?, urls: SimpleWidgetUrls) -> Unit,
    crossinline createWidget: (Parameters) -> WidgetType,
): SimpleWidgetUrls {
    val urls = SimpleWidgetUrls(prefix, presetPath != null)
    val presets = presetPath?.let { Presets<ContentType>(it) }
    get(urls.mainPage) { call.respondHtml { view(presets, urls) } }
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
    if (urls.reloadQuery != null) {
        post(urls.reloadQuery) {
            call.catchAdminAction(urls.mainPage) {
                presets!!.reload()
                respondHtml { view(presets, urls) }
            }
        }
    }
    return urls
}

internal fun HTML.simpleWidgetView(name: String, urls: SimpleWidgetUrls, extra: BODY.() -> Unit, parameters: FORM.() -> Unit) {
    body {
        adminHead()
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
        extra()
    }
}

internal inline fun <reified T> simpleWidgetViewFun(
    name: String,
    crossinline extra: BODY.(Presets<T>?) -> Unit = {},
    crossinline parameters: FORM.(Presets<T>?) -> Unit,
): HTML.(Presets<T>?, SimpleWidgetUrls) -> Unit = { presets, urls ->
    simpleWidgetView(name, urls, { extra(presets) }, { parameters(presets) })
}