package org.icpclive.admin

import io.ktor.server.application.*
import io.ktor.server.request.*
import kotlinx.serialization.*
import org.icpclive.server.ApiActionException


suspend inline fun <reified T> ApplicationCall.safeReceive(): T = try {
    receive()
} catch (e: SerializationException) {
    throw ApiActionException("Failed to deserialize data: ${e.message}", e)
}
