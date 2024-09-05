package org.icpclive.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.icpclive.api.*
import org.icpclive.cds.api.toRunId

fun String.toAnalyticsMessageId() = when {
    startsWith("run_") -> AnalyticsMessageIdRun(substring(4).toRunId())
    startsWith("text_") -> AnalyticsMessageIdCommentary(substring(5))
    else -> throw SerializationException("Incorrect AnalyticsMessageId")
}

object AnalyticsMessageIdSerializer : KSerializer<AnalyticsMessageId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("AnalyticsMessageId", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: AnalyticsMessageId) {
        encoder.encodeString(
            when (value) {
                is AnalyticsMessageIdRun -> "run_${value.runId}"
                is AnalyticsMessageIdCommentary -> "text_${value.commentaryId}"
            }
        )
    }

    override fun deserialize(decoder: Decoder) = decoder.decodeString().toAnalyticsMessageId()
}
