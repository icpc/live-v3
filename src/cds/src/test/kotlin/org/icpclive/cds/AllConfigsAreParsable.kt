package org.icpclive.cds

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.icpclive.api.tunning.AdvancedProperties
import org.icpclive.cds.settings.parseFileToCdsSettings
import org.junit.jupiter.api.*
import kotlin.io.path.*

class AllConfigsAreParsable {
    @OptIn(ExperimentalPathApi::class)
    @TestFactory
    fun testSettings() : List<DynamicTest> {
        val configDir = Path("").absolute().parent.parent.resolve("config")
        return configDir.walk().filter {
            it.name == "settings.json" || it.name == "settings.json5" || it.name == "events.properties" && !it.pathString.contains("v2-configs")
        }.map {
            DynamicTest.dynamicTest(it.relativeTo(configDir).toString()) {
                parseFileToCdsSettings(it) { "" }
            }
        }.toList()
    }

    @OptIn(ExperimentalPathApi::class, ExperimentalSerializationApi::class)
    @TestFactory
    fun testAdvancedJson() : List<DynamicTest> {
        val configDir = Path("").absolute().parent.parent.resolve("config")
        return configDir.walk().filter {
            it.name == "advanced.json"
        }.map { path ->
            DynamicTest.dynamicTest(path.relativeTo(configDir).toString()) {
                path.toFile().inputStream().use {
                    Json.decodeFromStream<AdvancedProperties>(it)
                }
            }
        }.toList()
    }

}