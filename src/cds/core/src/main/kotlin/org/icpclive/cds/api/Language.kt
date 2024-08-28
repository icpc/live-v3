package org.icpclive.cds.api

import kotlinx.serialization.Serializable

@JvmInline
@Serializable
public value class LanguageId internal constructor(public val value: String) {
    override fun toString(): String = value
}

public fun String.toLanguageId(): LanguageId = LanguageId(this)
public fun Int.toLanguageId(): LanguageId = toString().toLanguageId()
public fun Long.toLanguageId(): LanguageId = toString().toLanguageId()


@Serializable
public data class LanguageInfo(
    val id: LanguageId,
    val name: String,
    val extensions: List<String>,
)