import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.icpclive.cds.settings.CDSSettings
import org.icpclive.cds.settings.fromFile
import org.icpclive.cds.tunning.AdvancedProperties
import org.junit.jupiter.api.*
import kotlin.io.path.*


class AllConfigsAreParsable {
    @OptIn(ExperimentalPathApi::class)
    @TestFactory
    fun testSettings() : List<DynamicTest> {
        val configDir = Path("").absolute().parent.parent.parent.resolve("config")
        return configDir.walk().filter {
            it.name == "settings.json" || it.name == "settings.json5" || it.name == "events.properties" && !it.pathString.contains("v2-configs")
        }.map {
            DynamicTest.dynamicTest(it.relativeTo(configDir).toString()) {
                try {
                    CDSSettings.fromFile(it) { "" }
                } catch (e: SerializationException) {
                    throw AssertionError("Failed to parse file ${it.relativeTo(configDir.parent)}", e)
                }
            }
        }.toList().also {
            require(it.isNotEmpty())
        }
    }

    @OptIn(ExperimentalPathApi::class, ExperimentalSerializationApi::class)
    @TestFactory
    fun testAdvancedJson() : List<DynamicTest> {
        val configDir = Path("").absolute().parent.parent.parent.resolve("config")
        val json = Json {
            allowComments = true
            allowTrailingComma = true
        }
        return configDir.walk().filter {
            it.name == "advanced.json"
        }.map { path ->
            DynamicTest.dynamicTest(path.relativeTo(configDir).toString()) {
                path.toFile().inputStream().use {
                    AdvancedProperties.fromInputStream(it)
                }
            }
        }.toList().also {
            require(it.isNotEmpty())
        }
    }

}