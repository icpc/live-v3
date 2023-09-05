package org.icpclive.cds

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.icpclive.api.ContestResultType
import org.icpclive.api.tunning.*
import org.icpclive.cds.adapters.applyAdvancedProperties
import org.icpclive.cds.adapters.finalContestState
import org.icpclive.cds.clics.FeedVersion
import org.icpclive.cds.common.ContestParseResult
import org.icpclive.cds.settings.*
import org.opentest4j.AssertionFailedError
import java.nio.file.Path
import kotlin.test.Test
import kotlin.text.Regex
import kotlin.time.Duration.Companion.minutes

object CdsLoadersTest {
    private val goldenDataDir = Path.of("").toAbsolutePath().resolve("testData").resolve("loaders").resolve("goldenData")
    private val updateTestData = false

    @Test
    fun pcms() {
        loaderTest(
            goldenDataDir.resolve("pcms.txt"),
            PCMSSettings(
                url = "testData/loaders/pcms.xml"
            )
        )
    }

    @Test
    fun pcmsIOI() {
        loaderTest(
            goldenDataDir.resolve("pcmsIOI.txt"),
            PCMSSettings(
                resultType = ContestResultType.IOI,
                url = "testData/loaders/pcms-ioi.xml"
            )
        )
    }

    @Test
    fun ejudge() {
        loaderTest(
            goldenDataDir.resolve("ejudge.txt"),
            EjudgeSettings(
                url = "testData/loaders/ejudge.xml"
            )
        )
    }


    @Test
    fun clics202003() {
        loaderTest(
            goldenDataDir.resolve("clics202003.txt"),
            ClicsSettings(
                url = "testData/loaders/clics-2020-03",
                feedVersion = FeedVersion.`2020_03`
            )
        )
    }

    @Test
    fun clics202207() {
        loaderTest(
            goldenDataDir.resolve("clics202207.txt"),
            ClicsSettings(
                url = "testData/loaders/clics-2022-07",
                feedVersion = FeedVersion.`2022_07`
            )
        )
    }

    @Test
    fun testSys() {
        loaderTest(
            goldenDataDir.resolve("testSys.txt"),
            TestSysSettings(
                url = "testData/loaders/testsys.dat"
            )
        )
    }

    @Test
    fun testSysWithAdvancedOverride() {
        loaderTest(
            goldenDataDir.resolve("testSysWithAdvancedOverride.txt"),
            TestSysSettings(
                url = "testData/loaders/testsys.dat"
            ),
            AdvancedProperties(
                teamRegexes = TeamRegexOverrides(
                    groupRegex = mapOf(
                        "outOfContest" to Regex("^\\(вк\\).*"),
                        "firstGrade" to Regex("^\\(1к\\).*"),
                        "school" to Regex("^\\(шк\\).*")
                    ),
                    customFields = mapOf(
                        "funnyName" to Regex("^(?:\\(..\\) )?(.*) \\([^)]*\\)")
                    ),
                ),
                groupOverrides = mapOf(
                    "outOfContest" to GroupInfoOverride(isOutOfContest = true)
                ),
                teamOverrideTemplate = TeamOverrideTemplate(
                    displayName = "{funnyName}"
                )
            )
        )
    }



    private val json = Json {
        prettyPrint = true
    }

    private fun loaderTest(expectedFile: Path, args: CDSSettings, advanced: AdvancedProperties? = null) {
        val loader = args.toFlow(emptyMap())
        val result = runBlocking {
            val result = withTimeout(1.minutes) {
                loader.finalContestState().let {
                    ContestParseResult(
                        it.infoAfterEvent!!,
                        it.runs.values.toList(),
                        it.analyticsMessages.values.toList()
                    )
                }
            }
            if (advanced != null) {
                result.copy(
                    contestInfo = applyAdvancedProperties(
                        result.contestInfo,
                        advanced,
                        result.runs.map { it.teamId }.toSet()
                    )
                )
            } else {
                result
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
                "Actual result doesn't match expected in file ${expectedFile}\n" +
                        "Expected: $expected\n" +
                        "Actual: $actual\n",
                expected,
                actual,
            )
        }
    }
}