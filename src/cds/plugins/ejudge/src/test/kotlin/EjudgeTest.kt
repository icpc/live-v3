package org.icpclive.cds.plugins.ejudge

import org.icpclive.cds.CdsLoadersTest
import org.icpclive.cds.api.ContestResultType
import org.icpclive.cds.settings.PreviousDaySettings
import org.icpclive.cds.settings.UrlOrLocalPath
import kotlin.test.Test

object EjudgeTest : CdsLoadersTest() {
    @Test
    fun ejudge() {
        loaderTest(
            goldenDataDir.resolve("ejudge.txt"),
            EjudgeSettings(
                source = UrlOrLocalPath.Local(testDataDir.resolve("ejudge.xml"))
            )
        )
    }

    @Test
    fun ejudgeTwoDays() {
        loaderTest(
            goldenDataDir.resolve("ejudgeTwoDays.txt"),
            EjudgeSettings(
                source = UrlOrLocalPath.Local(testDataDir.resolve("ejudge-ioi-day2.xml")),
            ) {
                resultType = ContestResultType.IOI
                previousDays = listOf(
                    PreviousDaySettings(
                        EjudgeSettings(
                            source = UrlOrLocalPath.Local(testDataDir.resolve("ejudge-ioi-day1.xml"))
                        ) {
                            resultType = ContestResultType.IOI
                            problemScoreLimit = mapOf(
                                "5" to 71.0,
                                "8" to 84.0,
                            )
                        })
                )
            }
        )
    }

}