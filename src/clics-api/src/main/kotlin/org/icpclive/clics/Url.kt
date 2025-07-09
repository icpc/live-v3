package org.icpclive.clics

import kotlinx.serialization.ContextualSerializer

@JvmInline
public value class Url(public val value: String)

internal val UrlSerializer = ContextualSerializer(Url::class)