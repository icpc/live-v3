package org.icpclive.oracle

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.util.*
import kotlin.io.path.exists

@Serializable
private data class AdminCreds(val username: String, val password: String)

object BasicAuthKey {
    val key: String

    init {
        if (!Config.authDisabled) {
            val creds = if (Config.credsJsonPath.exists()) {
                Json.decodeFromStream<AdminCreds>(Config.credsJsonPath.toFile().inputStream())
            } else {
                AdminCreds("admin", "admin")
            }
            key = "Basic " +
                    Base64.getEncoder().encodeToString("${creds.username}:${creds.password}".toByteArray())

        } else {
            key = ""
        }
    }
}
