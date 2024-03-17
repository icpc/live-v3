package org.icpclive.cds.plugins.testsys

import org.icpclive.cds.CdsLoadersTest
import org.icpclive.cds.api.toGroupId
import org.icpclive.cds.settings.*
import org.icpclive.cds.tunning.*
import kotlin.test.Test

object TestSysTest : CdsLoadersTest() {
    @Test
    fun testSys() {
        loaderTest(
            goldenDataDir.resolve("testSys.txt"),
            TestSysSettings(
                url = UrlOrLocalPath.Local(testDataDir.resolve("testsys.dat"))
            )
        )
    }

    @Test
    fun testSysWithAdvancedOverride() {
        loaderTest(
            goldenDataDir.resolve("testSysWithAdvancedOverride.txt"),
            TestSysSettings(
                url = UrlOrLocalPath.Local(testDataDir.resolve("testsys.dat"))
            ),
            AdvancedProperties(
                teamNameRegexes = TeamRegexOverrides(
                    groupRegex = mapOf(
                        "outOfContest" to Regex("^\\(вк\\).*"),
                        "firstGrade" to Regex("^\\(1к\\).*"),
                        "school" to Regex("^\\(шк\\).*")
                    ),
                    customFields = mapOf(
                        "funnyName" to RegexSet(mapOf(Regex("^(?:\\(..\\) )?(.*) \\([^)]*\\)") to "$1"))
                    ),
                ),
                groupOverrides = mapOf(
                    "outOfContest".toGroupId() to GroupInfoOverride(isOutOfContest = true)
                ),
                teamOverrideTemplate = TeamOverrideTemplate(
                    displayName = "{funnyName}"
                )
            )
        )
    }

}