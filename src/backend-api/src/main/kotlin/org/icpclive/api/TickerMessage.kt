@file:Suppress("UNUSED")

package org.icpclive.api


import kotlinx.serialization.Serializable

/**
 * A message shown in one of the ticker lines.
 *
 * Everything except the identity of the message lives in [settings]. This is deliberately not a
 * sealed hierarchy: the kinds of a ticker message are exactly the kinds of [TickerMessageSettings],
 * and having both sealed would mean two sets of subclasses with the same serial names.
 */
@Serializable
class TickerMessage(
    override val id: String,
    val settings: TickerMessageSettings,
) : TypeWithId {
    constructor(settings: TickerMessageSettings) : this(generateId(prefix(settings)), settings)
    companion object {
        private fun prefix(settings: TickerMessageSettings) = when (settings) {
            is ClockTickerSettings -> "ticker_clock"
            is EmptyTickerSettings -> "ticker_empty"
            is ImageTickerSettings -> "ticker_image"
            is ScoreboardTickerSettings -> "ticker_scoreboard"
            is TextTickerSettings -> "ticker_text"
        }

    }
}
