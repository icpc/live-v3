package org.icpclive.admin

import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

class SvgTransformer(private val content: String) {
    constructor(paths: Path, name: String) : this(Paths.get(paths.toString(), name).toFile().readText())

    fun format(vararg args: String) = SvgTransformer(content.format(*args))

    fun toBase64() = "data: image/svg+xml; utf8; base64," + Base64.getEncoder().encodeToString(content.toByteArray())
}