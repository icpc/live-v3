package org.icpclive.reacbot

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.callbackQuery
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.Chat
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.TelegramFile
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import com.github.kotlintelegrambot.logging.LogLevel
import org.icpclive.api.*
import org.icpclive.cds.InfoUpdate
import org.icpclive.cds.RunUpdate
import org.icpclive.cds.adapters.*
import org.icpclive.cds.settings.parseFileToCdsSettings
import java.nio.file.Path
import kotlin.io.path.createDirectories

class Bot(private val config: Config) {
    @OptIn(DelicateCoroutinesApi::class)
    private val reactionsProcessingPool = newFixedThreadPoolContext(config.loaderThreads, "ReactionsProcessing")
    private val cds = parseFileToCdsSettings(
        Path.of(config.settingsFile),
        emptyMap()
    ).toFlow()
        .contestState()
        .filterUseless()
        .removeFrozenSubmissions()
        .processHiddenTeamsAndGroups()
    private val storage = Storage()
    private val bot = bot {
        logLevel = LogLevel.Error
        token = config.telegramToken
        setupDispatch()
    }
    private val alreadyProcessedReactionIds = TreeSet<Int>()
    private val contestInfo = CompletableDeferred<StateFlow<ContestInfo>>()
    private val sendAdditionalInfo = true

    private fun reactionRatingButtons(reaction: Reaction): InlineKeyboardMarkup {
        return InlineKeyboardMarkup.createSingleRowKeyboard(
            InlineKeyboardButton.CallbackData("Like \uD83D\uDC4D", "vote:${reaction.id.value}:like"),
            InlineKeyboardButton.CallbackData("Dislike \uD83D\uDC4E", "vote:${reaction.id.value}:dislike"),
        )
    }

    private fun Bot.Builder.setupDispatch() {
        dispatch {
            val nextReaction: ((Chat) -> Unit) = { chat: Chat ->
                val reaction = storage.getReactionForVote(chat.id)
                if (reaction == null) {
                    bot.sendMessage(ChatId.fromId(chat.id), "No such reaction videos")
                } else {
                    var caption: String? = null
                    if (sendAdditionalInfo) {
                        val ci = runBlocking { contestInfo.await().value }
                        ci.teams[reaction.teamId]?.let { team ->
                            ci.problems[reaction.problemId]?.let { problem ->
                                caption = "${team.fullName}, problem ${problem.displayName}"
                            }
                        }
                    }

                    bot.sendVideo(
                        ChatId.fromId(chat.id),
                        TelegramFile.ByFileId(reaction.telegramFileId!!),
                        caption = caption,
                        replyMarkup = reactionRatingButtons(reaction)
                    )
                }
            }

            text {
                nextReaction(message.chat)
            }

            callbackQuery("vote:") {
                val query = this.callbackQuery.data.split(":")
                if (query.size == 3) {
                    val reactionId = query[1].toIntOrNull() ?: return@callbackQuery
                    storage.storeReactionVote(
                        reactionId, this.callbackQuery.message?.chat?.id ?: 0L, when (query[2]) {
                            "like" -> +1
                            "dislike" -> -1
                            else -> 0
                        }
                    )
                    nextReaction(callbackQuery.message!!.chat)
                }
            }
        }
    }

    private fun processReaction(scope: CoroutineScope, run: RunInfo, reactionUrl: String) {
        val reaction = storage.addReactions(run.teamId, run.problemId, run.id, (run.result as? ICPCRunResult)?.verdict?.isAccepted == true, reactionUrl)
        if (reaction.telegramFileId == null && reaction.id.value !in alreadyProcessedReactionIds) {
            alreadyProcessedReactionIds.add(reaction.id.value)
            scope.launch(reactionsProcessingPool) {
                Path.of("converted").createDirectories()
                val outputFileName = "converted/${reaction.id.value}.mp4"
                try {
                    convertVideo(config.videoPathPrefix + reactionUrl, outputFileName)
                    val message = bot.sendVideo(
                        ChatId.fromId(config.botSystemChat.toLong()),
                        TelegramFile.ByFile(Path.of(outputFileName).toFile()),
                        caption = "New reaction run=${run.id} team=${run.teamId} problem=${run.problemId} ${run.time}",
                        disableNotification = true,
                    ).first?.body()?.result ?: return@launch
                    println("send reaction ${reaction.id} into telegram")
                    val uploadedFileId = message.video?.fileId ?: return@launch
                    storage.setReactionTelegramFileId(reaction.id.value, uploadedFileId)
                } catch (ignore: FfmpegException) {
                    println(ignore)
                }
            }
        }
    }

    fun run(scope: CoroutineScope) {
        val loaded = if (config.disableCdsLoader) emptyFlow() else cds.shareIn(scope, SharingStarted.Eagerly, Int.MAX_VALUE)
        val runs = loaded.filterIsInstance<RunUpdate>().map { it.newInfo }
        scope.launch {
            contestInfo.complete(loaded.filterIsInstance<InfoUpdate>().map { it.newInfo }.stateIn(scope))
        }
        scope.launch { bot.startPolling() }
        scope.launch {
            println("starting runs processing ...")
            println("runs processing stated for ${contestInfo.await().value.currentContestTime}")
            runs.collect { run ->
                run.reactionVideos.forEach {
                    if (it is MediaType.Video) {
                        processReaction(scope, run, it.url)
                    }
                }
            }
        }
    }
}

class BotCommand : CliktCommand() {
    private val settings by option(help = "settings file path").default("./settings.json")
    private val disableCds by option(help = "Enable loading events from cds").flag()
    private val token by option(help = "Telegram bot token").required()
    private val threads by option("--threads", "-t", help = "Count of video converter and loader threads").int().default(8)
    private val video by option( help = "Prefix for video url").default("")
    private val chat by option("--chat", "-c", help = "System chat id for bot management").int().default(316671439)

    override fun run() {
        runBlocking {
            Bot(
                Config(
                    settingsFile = settings,
                    disableCdsLoader = disableCds,
                    telegramToken = token,
                    loaderThreads = threads,
                    videoPathPrefix = video,
                    botSystemChat = chat,
                )
            ).run(this)
        }
    }
}

fun main(args: Array<String>) {
    BotCommand().main(args)
}

