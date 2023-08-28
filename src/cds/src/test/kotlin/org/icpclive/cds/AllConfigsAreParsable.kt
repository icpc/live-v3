package org.icpclive.cds

import org.icpclive.cds.settings.parseFileToCdsSettings
import org.junit.jupiter.api.*
import java.nio.file.Path
import kotlin.io.path.*

class AllConfigsAreParsable {
    @OptIn(ExperimentalPathApi::class)
    @TestFactory
    fun test() : List<DynamicTest> {
        val configDir = Path("").absolute().parent.parent.resolve("config")
        return configDir.walk().filter {
            it.name == "settings.json" || it.name == "settings.json5"
        }.map {
            DynamicTest.dynamicTest(it.relativeTo(configDir).toString()) {
                checkFile(it)
            }
        }.toList()
    }
    fun checkFile(path: Path) {
        parseFileToCdsSettings(path)
    }
}