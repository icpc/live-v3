package org.icpclive.api

import kotlinx.serialization.Serializable

@Serializable
data class AdminUser(val login: String, val confirmed: Boolean)