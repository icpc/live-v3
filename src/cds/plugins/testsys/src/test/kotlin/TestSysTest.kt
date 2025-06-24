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
                OverrideTeamTemplate(
                    regexes = mapOf(
                        "groups" to TemplateRegexParser(
                            from = "{team.fullName}",
                            rules = mapOf(
                                Regex("^\\(1к\\).*") to mapOf("id" to "firstGrade"),
                                Regex("^\\(шк\\).*") to mapOf("id" to "school"),
                                Regex("^\\(вк\\).*") to mapOf("id" to "outOfContest"),
                            )
                        ),
                        "custom" to TemplateRegexParser(
                            from = "{team.fullName}",
                            rules = mapOf(
                                Regex("^(?:\\(..\\) )?(.*) \\([^)]*\\)") to mapOf("funnyNameValue" to "$1")
                            )
                        )
                    ),
                    extraGroups = listOf("{regexes.groups.id}"),
                    customFields = mapOf(
                        "funnyName" to "{regexes.custom.funnyNameValue}"
                    )
                ),
                OverrideGroups(mapOf("outOfContest".toGroupId() to OverrideGroups.Override(isOutOfContest = true))),
                OverrideTeamTemplate(displayName = "{funnyName}")
            )
        )
    }

}