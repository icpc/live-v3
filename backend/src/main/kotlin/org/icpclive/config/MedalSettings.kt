package org.icpclive.config

//TODO: tie-break strategy?
data class MedalType(val name: String, val count: Int)

class MedalSettings(private val medals: List<MedalType>) {
    fun medalColorByRank(rank_: Int): String? {
        var rank = rank_
        for ((color, count) in medals) {
            if (rank <= count) return color
            rank -= count
        }
        return null
    }
}

fun loadMedalSettings() = MedalSettings(Config.loadPropertiesIfExists("medals")?.let {
    it.getProperty("order")
        .split(",")
        .map { name -> name.trim() }
        .map { name ->
            MedalType(
                name,
                it.getProperty("$name.count")?.toInt()
                    ?: throw IllegalStateException("Medals setting must have $name.count"),
            )
        }
} ?: emptyList())