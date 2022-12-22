package org.icpclive.cds.clics.model

data class ClicsJudgementTypeInfo(
    val id: String,
    val isAccepted: Boolean,
    val isAddingPenalty: Boolean,
) {
    val verdict = judgementTypeMapping[id] ?: id

    companion object {
        val judgementTypeMapping = mapOf(
            "OLE" to "WA",
            "PE" to "WA",
            "EO" to "WA",
            "IO" to "WA",
            "NO" to "WA",
            "TLE" to "TL",
            "WTL" to "TL",
            "TCO" to "TL",
            "TWA" to "TL",
            "TPE" to "TL",
            "TEO" to "TL",
            "TIO" to "TL",
            "TNO" to "TL",
            "RTE" to "RE",
            "MLE" to "RE",
            "SV" to "RE",
            "IF" to "RE",
            "RCO" to "RE",
            "RWA" to "RE",
            "RPE" to "RE",
            "REO" to "RE",
            "RIO" to "RE",
            "RNO" to "RE",
            "CTL" to "CE",
        ) // https://ccs-specs.icpc.io/2021-11/contest_api#known-judgement-types
    }
}
