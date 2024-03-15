package org.icpclive.reacbot

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.transactions.transaction

class Storage {
    private val connection = Database.connect("jdbc:sqlite:data.db", "org.sqlite.JDBC")

    init {
        transaction(connection) {
            SchemaUtils.create(Reactions)
            SchemaUtils.create(Votes)
        }
    }

    fun addReactions(teamId: Int, problemId: String, runId: Int, isOk: Boolean, fileName: String): Reaction =
        transaction(connection) {
            return@transaction Reaction.wrapRow(
                Reactions.selectAll().where { Reactions.fileName eq fileName }.singleOrNull<ResultRow>()
                    ?: Reactions.insert {
                        it[Reactions.teamId] = teamId
                        it[Reactions.problemId] = problemId
                        it[Reactions.runId] = runId
                        it[Reactions.isOk] = isOk
                        it[Reactions.fileName] = fileName
                    }.resultedValues!!.first()
            )
        }

    fun setReactionTelegramFileId(reactionId: Int, uploadedFileId: String) {
        transaction(connection) {
            Reactions.update(where = { Reactions.id eq reactionId }) {
                it[telegramFileId] = uploadedFileId
            }
        }
    }

    fun getReactionForVote(chatId: Long): Reaction? {
        return transaction(connection) {
            val reactions = Reactions.selectAll()
                .where { Reactions.telegramFileId neq null }
                .orderBy(
                    Reactions.voteCount to SortOrder.ASC,
                    Reactions.isOk to SortOrder.DESC,
                    Random() to SortOrder.ASC
                )
            val voted = Votes.selectAll()
                .where { Votes.chatId eq chatId }
                .map { Vote.wrapRow(it).id.value }
                .toSet()

            return@transaction reactions.map { Reaction.wrapRow(it) }.firstOrNull { it.runId !in voted }
        }
    }

    fun storeReactionVote(reactionId: Int, chatId: Long, delta: Int) {
        transaction(connection) {
            Reactions.update(where = { Reactions.id eq reactionId }) {
                it[voteCount] = voteCount + 1
                it[rating] = rating + delta
            }
            Votes.insert {
                it[Votes.reactionId] = reactionId
                it[Votes.chatId] = chatId
                it[Votes.vote] = delta
            }
        }
    }
}
