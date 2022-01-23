@file:Suppress("unused")

package org.icpclive.api

import kotlinx.serialization.Serializable

@Serializable
data class Advertisement(val text: String)

@Serializable
data class Picture(val url: String, val name: String)