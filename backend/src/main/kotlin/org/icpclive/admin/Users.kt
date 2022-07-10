package org.icpclive.admin

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import org.icpclive.Config
import org.icpclive.api.AdminUser
import java.security.MessageDigest

private val usersMutex = Mutex()

@Serializable
data class User(val name: String, val pass: String, val confirmed: Boolean) : Principal

private val users = mutableMapOf<String, User>()
private fun String.digest() = MessageDigest.getInstance("SHA-256").run {
    update("icpclive".toByteArray())
    update(this@digest.toByteArray())
    digest()
}

private val prettyPrinter = Json { prettyPrint = true }

private fun saveUsers() {
    Config.configDirectory.resolve("users.json").toFile().outputStream().use {
        prettyPrinter.encodeToStream(users.values.toList(), it)
    }
}

private fun loadUsers() {
    users.clear()
    Config.configDirectory.resolve("users.json").toFile().takeIf { it.exists() }?.inputStream()?.use {
        Json.decodeFromStream<List<User>>(it).forEach { user -> users[user.name] = user }
    }
}

val fakeUser by lazy { User("developer", "", true) }
fun createFakeUser() = fakeUser

suspend fun validateAdminApiCredits(name: String, password: String): User? {
    val digest = password.digest()
    val user = usersMutex.withLock {
        users[name] ?: run {
            val newUser = User(name, digest.encodeBase64(), users.isEmpty())
            users[name] = newUser
            saveUsers()
            newUser
        }
    }
    return user.takeIf { MessageDigest.isEqual(digest, user.pass.decodeBase64Bytes()) }
}

fun Route.setupUserRouting() {
    loadUsers()
    get {
        val result = usersMutex.withLock {
            users.values.map { AdminUser(it.name, it.confirmed) }
        }
        call.respond(result)
    }
    post("/{username}/confirm") {
        call.adminApiAction {
            usersMutex.withLock {
                val user = users[call.parameters["username"]] ?: throw AdminActionApiException("No such user")
                users[user.name] = user.copy(confirmed = true)
                saveUsers()
            }
        }
    }
    post("/reload") {
        call.adminApiAction {
            usersMutex.withLock {
                loadUsers()
            }
        }
    }
}
