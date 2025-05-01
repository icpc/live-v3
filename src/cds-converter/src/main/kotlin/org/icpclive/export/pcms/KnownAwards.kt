package org.icpclive.export.pcms

import kotlinx.html.TD

enum class KnownAwards(
    val awardId: String,
    val xmlAttribute: String,
    val awardGroup: Int,
    val code: String,
    val style: String,
) {
    QUALIFIED("qualified", "qual",0, "Q", "winner" ),

    GOLD("gold-medal", "gold",1, "G", "winner" ),
    SILVER("silver-medal", "silver",1, "S", "award" ),
    BRONZE("bronze-medal", "bronze",1, "B", "award3" ),

    FIRST_DIPL("first-diploma", "first",2,"I", "award4" ),
    SECOND_DIPL("second-diploma", "second",2,"II", "award4" ),
    THIRD_DIPL("third-diploma", "third",2,"III", "award4" );
}