package org.icpclive.util

import java.nio.file.Path
import java.util.*

object Svg {
    fun loadAndSubstitute(paths: Path, substitute: Map<String, String>): String =
        substitute.entries.fold(paths.toFile().readText()) { text, it ->
            text.replace("{${it.key}}", it.value)
        }
}

fun String.toBase64SVG() =
    "data: image/svg+xml; utf8; base64," + Base64.getEncoder().encodeToString(this.toByteArray())
