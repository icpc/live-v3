@file:Suppress("UNUSED")
package org.icpclive.service.social.twitch

// This code is rewritten js code from https://dev.twitch.tv/docs/irc/example-parser

data class MessageSource(val nickname: String?, val host: String)
data class BotCommand(val command: String, val params: List<String>)
data class ParsedMessage(
    val tags: Map<String, TagValue?>?,
    val source: MessageSource?,
    val command: Command,
    val parameters: String?,
)


fun parseMessage(message: String): ParsedMessage? {
    var idx = 0

    val rawTagsComponent = if (message.getOrNull(idx) == '@') {
        val endIdx = message.indexOf(' ')
        message.substring(1, endIdx).also {
            idx = endIdx + 1
        }
    } else {
        null
    }

    val rawSourceComponent = if (message.getOrNull(idx) == ':') {
        idx += 1
        val endIdx = message.indexOf(' ', idx)
        message.substring(idx, endIdx).also {
            idx = endIdx + 1
        }
    } else {
        null
    }

    val endIdx = message.indexOf(':', idx).takeIf { it != -1 } ?: message.length
    val rawCommandComponent = message.substring(idx, endIdx).trim()

    val rawParametersComponent = if (endIdx != message.length) {  // Check if the IRC message contains a parameters component.
        idx = endIdx + 1            // Should point to the parameters part of the message.
        message.substring(idx)
    } else {
        null
    }

    return ParsedMessage(
        rawTagsComponent?.let(::parseTags),
        rawSourceComponent?.let(::parseSource),
        parseCommand(rawCommandComponent) ?: return null,
        rawParametersComponent,
    )
}

sealed class TagValue {
    data class Badges(val value: Map<String, String>) : TagValue()
    data class Emotes(val value: Map<String, List<IntRange>>) : TagValue()
    data class EmoteSets(val value: List<Int>) : TagValue()
    data class RawValue(val value: String) : TagValue()
}

fun parseTags(tags: String): Map<String, TagValue?> {
    // badge-info=badges=broadcaster/1color=#0000FF...

    val tagsToIgnore = setOf(
        "client-nonce",
        "flags"
    )

    val dictParsedTags = mutableMapOf<String, TagValue?>()
    val parsedTags = tags.split(" ")

    for (tag in parsedTags) {
        val parsedTag = tag.split('=')  // Tags are key/value pairs.
        val tagName = parsedTag[0].takeIf { it !in tagsToIgnore } ?: continue
        dictParsedTags[tagName] = parsedTag[1].takeIf { it.isNotEmpty() }?.let { tagValue ->
            when (tagName) {
                "badges", "badge-info" -> TagValue.Badges(
                    tagValue.split(',').associate { pair ->
                        val badgeParts = pair.split('/')
                        badgeParts[0] to badgeParts[1]
                    }
                )

                "emotes" -> TagValue.Emotes(
                    tagValue.split('/').associate { emote ->
                        val emoteParts = emote.split(':')
                        emoteParts[0] to emoteParts[1].split(',').map { position ->
                            val positionParts = position.split('-')
                            positionParts[0].toInt() until positionParts[1].toInt()
                        }
                    }
                )

                "emote-sets" -> TagValue.EmoteSets(
                    tagValue.split(',').map { it.toInt() }  // Array of emote set IDs.
                )

                else -> TagValue.RawValue(tagValue)
            }
        }
    }

    return dictParsedTags
}

sealed class Command {
    class Join(val channel: String) : Command()
    class Part(val channel: String) : Command()
    class Notice(val channel: String) : Command()
    class Message(val channel: String) : Command()
    object Ping : Command()
    class Cap(val isCapRequestEnabled: Boolean) : Command()
    object Reconnect : Command()
    class LoggedIn(val channel: String) : Command()
}

fun parseCommand(rawCommandComponent: String): Command? {
    val commandParts = rawCommandComponent.split(' ')

    return when (commandParts[0]) {
        "JOIN" -> Command.Join(commandParts[1])
        "PART" -> Command.Part(commandParts[1])
        "NOTICE" -> Command.Notice(commandParts[1])
        "PRIVMSG" -> Command.Message(commandParts[1])
        "PING" -> Command.Ping
        "CAP" -> Command.Cap(commandParts[2] == "ACK")
        "RECONNECT" -> Command.Reconnect
        "001" -> Command.LoggedIn(commandParts[1])
        else -> null
    }
}

fun parseSource(rawSourceComponent: String): MessageSource {
    val sourceParts = rawSourceComponent.split('!')
    return MessageSource(
        if (sourceParts.size == 2) sourceParts[0] else null,
        if (sourceParts.size == 2) sourceParts[1] else sourceParts[0]
    )
}