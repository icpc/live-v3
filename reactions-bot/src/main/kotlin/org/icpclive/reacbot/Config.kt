package org.icpclive.reacbot

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required

class Config(
    val eventPropertiesFile: String,
    val enableCdsLoader: Boolean,
    val telegramToken: String,
    val loaderThreads: Int,
    val videoPathPrefix: String,
    val botSystemChar: Int,
)

fun parseConfig(args: Array<String>): Config {
    val configParser = ArgParser("reactions-bot")
    val event by configParser.option(ArgType.String, shortName = "event", description = "Event.properties file path")
        .default("./events.properties")
    val cdsEnable by configParser.option(
        ArgType.Boolean,
        shortName = "cds",
        description = "Enable loading events from cds"
    ).default(true)
    val telegramToken by configParser.option(ArgType.String, shortName = "token", description = "Telegram bot token")
        .required()
    val loaderThreads by configParser.option(
        ArgType.Int,
        shortName = "threads",
        description = "Count of video converter and loader threads"
    ).default(8)
    val videoPathPrefix by configParser.option(
        ArgType.String,
        shortName = "video",
        description = "Prefix for video url"
    ).default("")
    val botSystemChar by configParser.option(
        ArgType.Int,
        shortName = "chat",
        description = "System chat id for bot management"
    ).default(316671439)
    configParser.parse(args)
    return Config(event, cdsEnable, telegramToken, loaderThreads, videoPathPrefix, botSystemChar)
}
