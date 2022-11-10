package org.icpclive.reacbot

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.icpclive.api.AnalyticsMessage
import org.icpclive.api.ContestInfo
import org.icpclive.api.MediaType
import org.icpclive.api.RunInfo
import org.icpclive.cds.getContestDataSource
import java.io.FileInputStream
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
import org.icpclive.cds.common.setAllowUnsecureConnections
import java.nio.file.Path
import kotlin.io.path.createDirectories

class Bot(private val config: Config) {
    @OptIn(DelicateCoroutinesApi::class)
    private val reactionsProcessingPool = newFixedThreadPoolContext(config.loaderThreads, "ReactionsProcessing")
    private val cds = getContestDataSource(getProperties(config.eventPropertiesFile))
    private val storage = Storage()
    private val bot = bot {
        logLevel = LogLevel.Error
        token = config.telegramToken
        setupDispatch()
    }
    private val alreadyProcessedReactionIds = TreeSet<Int>()

    private fun reactionRatingButtons(reaction: Reaction): InlineKeyboardMarkup {
        return InlineKeyboardMarkup.createSingleRowKeyboard(
            InlineKeyboardButton.CallbackData("Like \uD83D\uDC4D", "vote:${reaction.id.value}:like"),
            InlineKeyboardButton.CallbackData("Dislike \uD83D\uDC4E", "vote:${reaction.id.value}:dislike"),
        )
    }

    private fun (Bot.Builder).setupDispatch() {
        dispatch {
            val nextReaction: ((Chat) -> Unit) = { chat: Chat ->
                val reaction = storage.getReactionForVote(chat.id)
                if (reaction == null) {
                    bot.sendMessage(ChatId.fromId(chat.id), "No such reaction videos")
                } else {
                    bot.sendVideo(
                        ChatId.fromId(chat.id),
                        TelegramFile.ByFileId(reaction.telegramFileId!!),
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
        val reaction = storage.addReactions(run.teamId, run.problemId, run.id, run.isAccepted, reactionUrl)
        if (reaction.telegramFileId == null && reaction.id.value !in alreadyProcessedReactionIds) {
            alreadyProcessedReactionIds.add(reaction.id.value)
            scope.launch(reactionsProcessingPool) {
                Path.of("converted").createDirectories()
                val outputFileName = "converted/${reaction.id.value}.mp4"
                try {
                    convertVideo(config.videoPathPrefix + reactionUrl, outputFileName)
                    val message = bot.sendVideo(
                        ChatId.fromId(config.botSystemChar.toLong()),
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
        val contestInfo = CompletableDeferred<StateFlow<ContestInfo>>()
        val runs = CompletableDeferred<Flow<RunInfo>>()
        val analyticsFlow = CompletableDeferred<Flow<AnalyticsMessage>>()

        scope.launch { bot.startPolling() }
        scope.launch {
            if (config.disableCdsLoader) {
                return@launch
            }
            setAllowUnsecureConnections(true)
            println("starting cds processing ...")
            cds.run(contestInfo, runs, analyticsFlow)
        }
        scope.launch {
            println("starting runs processing ...")
            println("runs processing stated for ${contestInfo.await().value.currentContestTime}")
            runs.await().collect { run ->
                run.reactionVideos.forEach {
                    if (it is MediaType.Video) {
                        processReaction(scope, run, it.url)
                    }
                }
            }
        }
    }
}

private fun getProperties(fileName: String): Properties {
    val properties = Properties()
    FileInputStream(fileName).use { properties.load(it) }
    return properties
}


fun main(args: Array<String>) {
    val config = parseConfig(args)
    runBlocking {
        Bot(config).run(this)
    }
}

