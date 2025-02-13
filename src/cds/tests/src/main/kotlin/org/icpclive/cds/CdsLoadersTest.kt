package org.icpclive.cds

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.icpclive.cds.adapters.*
import org.icpclive.cds.settings.*
import org.icpclive.cds.tunning.*
import org.opentest4j.AssertionFailedError
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.relativeTo
import kotlin.time.Duration.Companion.minutes

abstract class CdsLoadersTest {
    protected val testDataDir: Path = Path.of("").absolute().parent.parent
        .resolve("tests")
        .resolve("testData")
        .resolve("loaders")
        .relativeTo(Path.of("").absolute())

    protected val goldenDataDir: Path = testDataDir.resolve("goldenData")
    internal val updateTestData = false


    private val json = Json {
        prettyPrint = true
    }

    protected fun loaderTest(expectedFile: Path, args: CDSSettings, rules: List<TuningRule> = emptyList()) {
        val loader = args.toFlow()
            .applyTuningRules(flow { emit(rules) })
            .autoCreateMissingGroupsAndOrgs()
        val result = runBlocking {
            withTimeout(1.minutes) {
                loader.finalContestState().let {
                    ContestParseResult(
                        it.infoAfterEvent!!,
                        it.runsAfterEvent.values.toList(),
                        it.commentaryMessagesAfterEvent.values.toList()
                    )
                }
            }
        }
        fun sanitize(s: String) = s.trim().replace(" +$", "").replace(System.lineSeparator(), "\n")
        val actual = sanitize(json.encodeToString(result))
        val expected = sanitize(expectedFile.toFile().takeIf { it.exists() }?.readText() ?: "")
        if (actual != expected) {
            if (updateTestData) {
                expectedFile.toFile().printWriter().use {
                    it.print(actual.replace("\n", System.lineSeparator()))
                }
            }
            throw AssertionFailedError(
                "Actual result doesn't match expected in file ${expectedFile}\n",
                expected,
                actual,
            )
        }
    }
}
