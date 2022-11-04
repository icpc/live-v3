package org.icpclive.reacbot

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.transactions.transaction

class Storage {
    private val connection = Database.connect("jdbc:sqlite:data.db", "org.sqlite.JDBC")

    init {
        transaction(connection) {
            SchemaUtils.create(Reactions)
        }
    }

    fun addReactions(teamId: Int, problemId: Int, runId: Int, isOk: Boolean, fileName: String): Reaction =
        transaction(connection) {
            val reaction = Reactions.select { Reactions.fileName eq fileName }.singleOrNull() ?: Reactions.insert {
                it[Reactions.teamId] = teamId
                it[Reactions.problemId] = problemId
                it[Reactions.runId] = runId
                it[Reactions.isOk] = isOk
                it[Reactions.fileName] = fileName
            }.let { Reactions.select { Reactions.fileName eq fileName }.single() }
            return@transaction Reaction.wrapRow(reaction)
        }

    fun setReactionTelegramFileId(reactionId: Int, uploadedFileId: String) {
        transaction(connection) {
            Reactions.update(where = { Reactions.id eq reactionId }) {
                it[telegramFileId] = uploadedFileId
            }
        }
    }

    fun getReactionForVote(): Reaction? {
        return transaction(connection) {
            val reactions = Reactions.select { Reactions.telegramFileId neq null }.orderBy(
                Reactions.voteCount to SortOrder.ASC,
                Reactions.isOk to SortOrder.DESC,
                Random() to SortOrder.ASC
            )
            return@transaction reactions.firstOrNull()?.let { Reaction.wrapRow(it) }
        }
    }

    fun storeReactionVote(reactionId: Int, delta: Int) {
        transaction(connection) {
            Reactions.update(where = { Reactions.id eq reactionId }) {
                it[voteCount] = voteCount + 1
                it[rating] = rating + delta
            }
        }
    }
}
