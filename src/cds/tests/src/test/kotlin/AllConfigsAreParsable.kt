import io.github.optimumcode.json.schema.JsonSchemaLoader
import kotlinx.serialization.json.JsonObject
import org.icpclive.cds.settings.CDSSettings
import org.icpclive.cds.settings.fromFile
import org.icpclive.cds.tunning.TuningRule
import org.icpclive.cds.util.loadConfigFile
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.nio.file.Path
import kotlin.io.path.*


class AllConfigsAreParsable {
    private fun test(
        configDir: Path,
        nameFilter: (String) -> Boolean,
        parser: (Path) -> Any?
    ) : List<DynamicTest> {
        return configDir.walk()
            .filter { nameFilter(it.name) }
            .map {
                DynamicTest.dynamicTest(it.relativeTo(configDir).toString()) { val _ = parser(it) }
            }.toList()
            .also { require(it.isNotEmpty()) }
    }

    @TestFactory
    fun testSettings() : List<DynamicTest> {
        return test(
            Path("").absolute().parent.parent.parent.resolve("config"),
            nameFilter = { it == "settings.json" },
            parser = { CDSSettings.fromFile(it) { "" } }
        )
    }

    @TestFactory
    fun testAdvancedJson() : List<DynamicTest> {
        return test(
            Path("").absolute().parent.parent.parent.resolve("config"),
            nameFilter = { it == "advanced.json" },
            parser = { path ->
                path.toFile().inputStream().use {
                    TuningRule.listFromInputStream(it)
                }
            }
        )
    }

    @TestFactory
    fun testVisualConfigs() : List<DynamicTest> {
        val projectDir = Path("").absolute().parent.parent.parent
        val jsonSchema = JsonSchemaLoader
            .create()
            .fromDefinition(projectDir.resolve("schemas/visual-config.schema.json").toFile().readText())
        return test(
            projectDir.resolve("config"),
            nameFilter = { it.startsWith("visual-config") || it.startsWith("visualConfig") },
            parser = { path ->
                jsonSchema.validate(loadConfigFile<JsonObject>(path.toFile())) { error ->
                    throw IllegalArgumentException("Invalid ${path}: $error")
                }
            }
        )
    }

    @TestFactory
    fun testAdvancedJsonExamples() : List<DynamicTest> {
        return test(
            Path("").absolute().parent.parent.parent.resolve("config/_examples/_advanced"),
            nameFilter = { true },
            parser = { path ->
                path.toFile().inputStream().use {
                    TuningRule.listFromInputStream(it)
                }
            }
        )
    }


}