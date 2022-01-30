package org.icpclive.admin.overlayEvents

import kotlinx.html.*
import org.icpclive.admin.adminHead

fun HTML.overlayEventsView(events: List<String>) {
    body {
        adminHead()
        h1 { +"Debug events" }
        ul {
            for (event in events) {
                li {
                    +event
                }
            }
        }
    }
}