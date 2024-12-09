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
                source = UrlOrLocalPath.Local(testDataDir.resolve("testsys.dat"))
            )
        )
    }

    @Test
    fun testSysWithAdvancedOverride() {
        loaderTest(
            goldenDataDir.resolve("testSysWithAdvancedOverride.txt"),
            TestSysSettings(
                source = UrlOrLocalPath.Local(testDataDir.resolve("testsys.dat"))
            ),
            listOf(
                AddGroupIfMatches("firstGrade".toGroupId(), "{team.fullName}", Regex("^\\(1к\\).*")),
                AddGroupIfMatches("school".toGroupId(), "{team.fullName}", Regex("^\\(шк\\).*")),
                AddGroupIfMatches("outOfContest".toGroupId(), "{team.fullName}", Regex("^\\(вк\\).*")),
                AddCustomValueByRegex("funnyName", "{team.fullName}", RegexSet(mapOf(Regex("^(?:\\(..\\) )?(.*) \\([^)]*\\)") to "$1"))),
                OverrideGroups(mapOf("outOfContest".toGroupId() to GroupInfoOverride(isOutOfContest = true))),
                OverrideTeamTemplate(displayName = "{funnyName}")
            )
        )
    }

}