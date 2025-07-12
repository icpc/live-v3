package org.icpclive.util

import java.nio.file.Path
import kotlin.io.encoding.Base64

object Svg {
    fun loadAndSubstitute(paths: Path, substitute: Map<String, String>): String =
        substitute.entries.fold(paths.toFile().readText()) { text, it ->
            text.replace("{${it.key}}", it.value)
        }
}

fun String.toBase64SVG() =
    "data: image/svg+xml; utf8; base64," + Base64.encode(this.toByteArray())
