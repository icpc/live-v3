package org.icpclive.admin

import java.nio.file.Path
import java.util.*

class SvgTransformer(paths: Path, name: String, substitute: Map<String, String>) {
    private val content: String

    init {
        var text: String = paths.resolve(name).toFile().readText()
        substitute.forEach { text = text.replace("{${it.key}}", it.value) }
        content = text
    }

    fun toBase64() = "data: image/svg+xml; utf8; base64," + Base64.getEncoder().encodeToString(content.toByteArray())
}
