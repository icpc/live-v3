package org.icpclive.admin

import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.icpclive.Config
import org.icpclive.api.AdminUser
import java.io.File
import java.security.MessageDigest


@Serializable
data class User(val name: String, val pass: String, val confirmed: Boolean)

interface UsersController {
    suspend fun validateAdminApiCredits(name: String, password: String): User?
    suspend fun getAllUsers() : List<User>
    suspend fun confirm(name: String)
    suspend fun reload()
}

class FileBasedUsersController(val file: File) : UsersController {
    private val mutex = Mutex()
    private val users = mutableMapOf<String, User>()
    private val prettyPrinter = Json { prettyPrint = true }

    init {
        loadUsers()
    }

    private fun saveUsers() {
        file.outputStream().use {
            prettyPrinter.encodeToStream(users.values.toList(), it)
        }
    }

    private fun loadUsers() {
        users.clear()
        file.takeIf { it.exists() }?.inputStream()?.use {
            Json.decodeFromStream<List<User>>(it).associateByTo(users, User::name)
        }
    }

    override suspend fun validateAdminApiCredits(name: String, password: String): User? {
        val digest = MessageDigest.getInstance("SHA-256").run {
            update(":icpc:".toByteArray())
            update(name.toByteArray())
            update(":live:".toByteArray())
            update(password.toByteArray())
            digest()
        }
        val user = mutex.withLock {
            users[name] ?: run {
                val newUser = User(name, digest.encodeBase64(), users.isEmpty())
                users[name] = newUser
                saveUsers()
                newUser
            }
        }
        return user.takeIf { MessageDigest.isEqual(digest, user.pass.decodeBase64Bytes()) }
    }

    override suspend fun getAllUsers() = mutex.withLock {
        users.values.toList()
    }

    override suspend fun reload() = mutex.withLock {
        loadUsers()
    }

    override suspend fun confirm(name: String) = mutex.withLock {
        val user = users[name] ?: throw ApiActionException("No such user")
        users[user.name] = user.copy(confirmed = true)
        saveUsers()
    }
}

class FakeUsersController : UsersController {
    private val fakeUser = User("developer", "", true)
    override suspend fun validateAdminApiCredits(name: String, password: String) = fakeUser

    override suspend fun getAllUsers() = listOf(fakeUser)

    override suspend fun confirm(name: String) {}

    override suspend fun reload() {}
}

fun Route.setupUserRouting(manager: UsersController) {
    get {
        run {
            call.respond(manager.getAllUsers().map { AdminUser(it.name, it.confirmed) })
        }
    }
    post("/{username}/confirm") {
        call.adminApiAction {
            manager.confirm(call.parameters["username"]!!)
        }
    }
    post("/reload") {
        call.adminApiAction { manager.reload() }
    }
}

fun Config.createUsersController() = when {
    authDisabled -> FakeUsersController()
    else -> FileBasedUsersController(usersFile.toFile())
}