package org.icpclive.cds.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.serializer
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import kotlin.io.path.absolute

public val jsonForConfigs: Json = Json {
    allowTrailingComma = true
    allowComments = true
}

public inline fun <reified T> loadConfigFile(file: Path): T = file.toFile().inputStream().use { jsonForConfigs.decodeFromStream<T>(it) }

private val prettyPrintJson = Json { prettyPrint = true }

public fun Path.writeAtomically(block: OutputStream.() -> Unit) {
    val tempFile = Files.createTempFile(absolute().parent, null, null)
    try {
        tempFile.toFile().outputStream().use { stream ->
            stream.block()
        }
        Files.move(tempFile, this, REPLACE_EXISTING, ATOMIC_MOVE)
    } finally {
        Files.deleteIfExists(tempFile)
    }

}

public fun <T> Path.writeJsonAtomically(serializer: KSerializer<T>, value: T) {
    writeAtomically {
        prettyPrintJson.encodeToStream(serializer, value, this)
    }
}

public fun Path.writeContentAtomically(value: String) {
    writeAtomically {
        write(value.toByteArray())
    }
}


public inline fun <reified T> Path.writeJsonAtomically(value: T) {
    writeJsonAtomically(serializer(), value)
}