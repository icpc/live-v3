package org.icpclive.cds

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.approvaltests.Approvals
import org.approvaltests.core.Options
import org.icpclive.api.ContestResultType
import org.icpclive.api.ContestStatus
import org.icpclive.api.tunning.*
import org.icpclive.cds.adapters.applyAdvancedProperties
import org.icpclive.cds.adapters.contestState
import org.icpclive.cds.clics.FeedVersion
import org.icpclive.cds.common.ContestParseResult
import org.icpclive.cds.settings.*
import kotlin.test.Test
import kotlin.text.Regex
import kotlin.time.Duration.Companion.minutes

class CdsLoadersTest {
    @Test
    fun pcmsTest() {
        loaderTest(
            PCMSSettings(
                url = "testData/loaders/pcms.xml"
            )
        )
    }

    @Test
    fun pcmsIOITest() {
        loaderTest(
            PCMSSettings(
                resultType = ContestResultType.IOI,
                url = "testData/loaders/pcms-ioi.xml"
            )
        )
    }


    @Test
    fun clics202003Test() {
        loaderTest(
            ClicsSettings(
                url = "testData/loaders/clics-2020-03",
                feedVersion = FeedVersion.`2020_03`
            )
        )
    }

    @Test
    fun clics202207Test() {
        loaderTest(
            ClicsSettings(
                url = "testData/loaders/clics-2022-07",
                feedVersion = FeedVersion.`2022_07`
            )
        )
    }

    @Test
    fun testSys() {
        loaderTest(
            TestSysSettings(
                url = "testData/loaders/testsys.dat"
            )
        )
    }

    @Test
    fun testSysWithAdvancedOverride() {
        loaderTest(
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

    private fun loaderTest(args: CDSSettings, advanced: AdvancedProperties? = null) {
        val loader = args.toFlow(emptyMap())
        val result = runBlocking {
            val result = withTimeout(1.minutes) {
                loader.contestState().first { it.infoAfterEvent?.status == ContestStatus.FINALIZED }.let {
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
        val options = Options()
        Approvals.verify(json.encodeToString(result), options)
    }
}