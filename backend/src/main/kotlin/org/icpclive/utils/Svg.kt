package org.icpclive.utils

import java.nio.file.Path
import java.util.*

object Svg {
    fun loadAndSubstitute(paths: Path, substitute: Map<String, String>) : String {
        val text = substitute.entries.fold(paths.toFile().readText()) { text, it ->
            text.replace("{${it.key}}", it.value)
        }
        return "data: image/svg+xml; utf8; base64," + Base64.getEncoder().encodeToString(text.toByteArray())
    }
}
