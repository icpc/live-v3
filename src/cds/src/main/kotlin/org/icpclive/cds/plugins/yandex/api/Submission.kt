package org.icpclive.cds.plugins.yandex.api

import kotlinx.serialization.Serializable
import org.icpclive.util.DurationInMillisecondsSerializer
import kotlin.time.Duration

@Serializable
internal data class Submissions(
    val count: Long,
    val submissions: List<Submission>
)

@Serializable
internal data class Submission(
    val id: Long,
    val authorId: Long,
    val author: String,
    val compiler: String,
    val problemId: String,
    val problemAlias: String,
    val verdict: String,
    val score: Double?,
    val test: Long,
    val time: Long,
    val memory: Long,
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val timeFromStart: Duration
)
