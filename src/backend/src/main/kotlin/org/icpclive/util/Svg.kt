package org.icpclive.util

import io.ktor.util.*
import kotlinx.serialization.json.JsonPrimitive
import org.icpclive.cds.api.*
import org.icpclive.cds.tunning.buildTemplateSubstitutor
import java.nio.file.Path
import kotlin.io.encoding.Base64

fun String.toBase64SVG() =
    "data: image/svg+xml; utf8; base64," + Base64.encode(this.toByteArray())

fun loadSVG(path: Path, replacements: Map<String, String>, info: ContestInfo?): String =
    buildTemplateSubstitutor {
        for ((k, v) in replacements) {
            addJson(k, JsonPrimitive(v))
            if (k == "teamId") {
                info?.teams[v.toTeamId()]?.let {
                    addTeam(it, withFlatCustomFields = false)
                    info.organizations[it.organizationId]?.let {
                        addOrganization(it)
                    }
                }
            }
            if (k == "organizationId") {
                info?.organizations[v.toOrganizationId()]?.let {
                    addOrganization(it)
                }
            }
        }
    }.substitute(path.toFile().readText()) {
        it.escapeHTML()
    }