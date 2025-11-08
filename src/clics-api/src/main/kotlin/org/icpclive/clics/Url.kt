package org.icpclive.clics

import kotlinx.serialization.ContextualSerializer
import java.net.URI
import java.net.URLEncoder

@JvmInline
public value class Url(public val value: String)

public fun Url.withQueryParams( vararg params: Pair<String, String>): Url {
    val uri = URI(value)
    val existingQuery = uri.rawQuery ?: ""

    val newParams = params.joinToString("&") { (key, value) ->
        "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
    }

    val newQuery = when {
        existingQuery.isEmpty() -> newParams
        else -> "$existingQuery&$newParams"
    }

    return Url(URI(
        uri.scheme,
        uri.authority,
        uri.rawPath,
        newQuery,
        uri.fragment
    ).toString())
}


internal val UrlSerializer = ContextualSerializer(Url::class)
