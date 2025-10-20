import io.github.optimumcode.json.schema.JsonSchemaLoader
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromStream
import org.icpclive.cds.settings.CDSSettings
import org.icpclive.cds.settings.fromFile
import org.icpclive.cds.tunning.TuningRule
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.nio.file.Path
import kotlin.io.path.*


class AllConfigsAreParsable {
    @OptIn(ExperimentalPathApi::class)
    private fun test(
        configDir: Path,
        nameFileter: (String) -> Boolean,
        parser: (Path) -> Unit
    ) : List<DynamicTest> {
        return configDir.walk()
            .filter { nameFileter(it.name) }
            .map {
                DynamicTest.dynamicTest(it.relativeTo(configDir).toString()) { parser(it) }
            }.toList()
            .also { require(it.isNotEmpty()) }
    }

    @OptIn(ExperimentalPathApi::class)
    @TestFactory
    fun testSettings() : List<DynamicTest> {
        return test(
            Path("").absolute().parent.parent.parent.resolve("config"),
            nameFileter = { it == "settings.json" },
            parser = { CDSSettings.fromFile(it) { "" } }
        )
    }

    @OptIn(ExperimentalPathApi::class, ExperimentalSerializationApi::class)
    @TestFactory
    fun testAdvancedJson() : List<DynamicTest> {
        return test(
            Path("").absolute().parent.parent.parent.resolve("config"),
            nameFileter = { it == "advanced.json" },
            parser = { path ->
                path.toFile().inputStream().use {
                    TuningRule.listFromInputStream(it)
                }
            }
        )
    }

    @OptIn(ExperimentalPathApi::class, ExperimentalSerializationApi::class)
    @TestFactory
    fun testVisualConfigs() : List<DynamicTest> {
        val projectDir = Path("").absolute().parent.parent.parent
        val jsonSchema = JsonSchemaLoader
            .create()
            .fromDefinition(projectDir.resolve("schemas/visual-config.schema.json").toFile().readText())
        val json = Json {
            allowTrailingComma = true
        }
        return test(
            projectDir.resolve("config"),
            nameFileter = { it.startsWith("visual-config") || it.startsWith("visualConfig") },
            parser = { path ->
                path.toFile().inputStream().use {
                    jsonSchema.validate(json.decodeFromStream<JsonElement>(it)) {error ->
                        throw IllegalArgumentException("Invalid ${path}: $error")
                    }
                }
            }
        )
    }

    @OptIn(ExperimentalPathApi::class, ExperimentalSerializationApi::class)
    @TestFactory
    fun testAdvancedJsonExamples() : List<DynamicTest> {
        return test(
            Path("").absolute().parent.parent.parent.resolve("config/_examples/_advanced"),
            nameFileter = { true },
            parser = { path ->
                path.toFile().inputStream().use {
                    TuningRule.listFromInputStream(it)
                }
            }
        )
    }


}