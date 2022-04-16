package org.icpclive.cds.yandex.api

import kotlinx.serialization.Serializable

@Serializable
data class Participant(
    val id:Int,
    val name:String,
    val login:String,
)