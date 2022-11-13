package org.icpclive.reacbot


class Config(
    val eventPropertiesFile: String,
    val disableCdsLoader: Boolean,
    val telegramToken: String,
    val loaderThreads: Int,
    val videoPathPrefix: String,
    val botSystemChat: Int,
)