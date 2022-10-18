package org.icpclive.cds.yandex.api

import kotlinx.serialization.Serializable
import org.icpclive.util.DurationInMillisecondsSerializer
import kotlin.time.Duration

@Serializable
data class Submissions(
    val count: Long,
    val submissions: List<Submission>
)

@Serializable
data class Submission(
    val id: Long,
    val authorId: Long,
    val author: String,
    val compiler: String,
    val problemId: String,
    val problemAlias: String,
    val verdict: String,
    val test: Long,
    val time: Long,
    val memory: Long,
    @Serializable(with = DurationInMillisecondsSerializer::class)
    val timeFromStart: Duration
)
