package org.icpclive.service.social.twitch

import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import org.icpclive.utils.catchToNull
import org.icpclive.utils.defaultHttpClient
import org.icpclive.utils.getLogger
import java.io.IOException

// This code is rewritten js code from https://dev.twitch.tv/docs/irc/example-bot

class Client(
    val channels: List<String>,
    val account: String,
    val password: String
) {
    private val httpClient = defaultHttpClient(null) {
        install(WebSockets)
    }

    suspend fun run(onMessage: suspend WebSocketSession.(String?, String) -> Unit) {
        httpClient.webSocket("ws://irc-ws.chat.twitch.tv") {
            send("PASS ${password}")
            send("NICK ${account}")

            for (frame in incoming) {
                frame as? Frame.Text ?: continue
                val receivedText = frame.readText()
                receivedText.split("\r\n")
                    .mapNotNull { catchToNull { parseMessage(it) } }
                    .forEach {
                        when (it.command) {
                            is Command.Ping -> send("PONG ${it.parameters}")
                            is Command.LoggedIn -> channels.forEach { send("JOIN $it") }
                            is Command.Message -> {
                                onMessage(it.source?.nickname, it.parameters!!)
                            }

                            is Command.Notice -> {
                                // The server will close the connection.
                                when (it.parameters) {
                                    "Login authentication failed" -> {
                                        channels.forEach { send("PART $it") }
                                        throw IllegalStateException(it.parameters)
                                    }
                                    "You don’t have permission to perform that action" -> {
                                        channels.forEach { send("PART $it") }
                                        throw IllegalStateException(it.parameters)
                                    }
                                    else -> logger.info("Got notice from irc: ${it.parameters}")
                                }
                            }
                            is Command.Reconnect -> {
                                throw IOException("Reconnect Requested")
                            }
                            is Command.Join -> {}
                            is Command.Part -> {}
                            is Command.Cap -> {}
                        }
                    }
            }
        }
    }

    companion object {
        val logger = getLogger(Client::class)
    }
}