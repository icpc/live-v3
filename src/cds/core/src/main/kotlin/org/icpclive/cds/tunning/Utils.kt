package org.icpclive.cds.tunning

import io.ktor.http.*
import org.icpclive.cds.api.*

private val templateRegex = kotlin.text.Regex("\\{(!?[a-z0-9A-Z_.-]*)}")

internal fun ContestInfo.getTemplateValue(s: String, teamId: TeamId, isUrl: Boolean): String {
    val team = teams[teamId] ?: return s
    val org = organizations[team.organizationId]
    val provider = fun(it: String): String? {
        return when {
            it.startsWith("team.") -> {
                when (it.removePrefix("team.")) {
                    "id" -> team.id.value
                    "displayName" -> team.displayName
                    "fullName" -> team.fullName
                    else -> null
                }
            }

            it.startsWith("org.") -> {
                when (it.removePrefix("org.")) {
                    "id" -> org?.id?.value
                    "displayName" -> org?.displayName
                    "fullName" -> org?.fullName
                    else -> null
                }
            }

            else -> {
                team.customFields[it]
            }
        }
    }
    return s.replace(templateRegex) {
        val str = it.groups[1]!!.value
        if (isUrl) {
            if (str.startsWith("!")) {
                provider(str.removePrefix("!"))
            } else {
                provider(str)?.encodeURLParameter()
            }
        } else {
            provider(str)
        } ?: it.value
    }
}

internal fun <T, O, ID> mergeOverrides(
    infos: List<T>,
    overrides: Map<ID, O>?,
    id: T.() -> ID,
    logUnused: (Set<ID>) -> Unit = { },
    merge: (T, O) -> T,
): List<T> {
    return if (overrides == null) {
        infos
    } else {
        val idsSet = infos.map { it.id() }.toSet()
        fun mergeIfNotNull(a: T, b: O?) = if (b == null) a else merge(a, b)
        (overrides.keys - idsSet)
            .takeIf { it.isNotEmpty() }
            ?.let(logUnused)
        infos.map {
            mergeIfNotNull(it, overrides[it.id()])
        }
    }
}

internal fun <K, V> mergeMaps(original: Map<K, V>, override: Map<K, V?>?) = buildMap {
    putAll(original)
    if (override != null) {
        for ((k, v) in override.entries) {
            if (v == null) {
                remove(k)
            } else {
                put(k, v)
            }
        }
    }
}