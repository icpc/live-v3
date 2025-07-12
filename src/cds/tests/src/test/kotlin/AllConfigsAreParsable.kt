import kotlinx.serialization.ExperimentalSerializationApi
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
            nameFileter = { it == "settings.json" || it == "settings.json5" },
            parser = { CDSSettings.fromFile(it) { "" } }
        )
    }

    @OptIn(ExperimentalPathApi::class, ExperimentalSerializationApi::class)
    @TestFactory
    fun testAdvancedJson() : List<DynamicTest> {
        return test(
            Path("").absolute().parent.parent.parent.resolve("config"),
            nameFileter = { it == "advanced.json" || it == "settings.json5" },
            parser = { path ->
                path.toFile().inputStream().use {
                    TuningRule.listFromInputStream(it)
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