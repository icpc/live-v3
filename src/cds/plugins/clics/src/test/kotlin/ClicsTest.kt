package org.icpclive.cds.plugins.clics

import org.icpclive.cds.CdsLoadersTest
import org.icpclive.cds.settings.*
import kotlin.test.Test

object ClicsTest : CdsLoadersTest() {

    @Test
    fun clics202003() {
        loaderTest(
            goldenDataDir.resolve("clics202003.txt"),
            ClicsSettings(
                feeds = listOf(
                    ClicsFeed(
                        url = UrlOrLocalPath.Local(testDataDir.resolve("clics-2020-03")),
                        contestId = "",
                        eventFeedPath = "",
                        feedVersion = FeedVersion.`2020_03`
                    )
                )
            )
        )
    }

    @Test
    fun clics202207() {
        loaderTest(
            goldenDataDir.resolve("clics202207.txt"),
            ClicsSettings(
                feeds = listOf(
                    ClicsFeed(
                        url = UrlOrLocalPath.Local(testDataDir.resolve("clics-2022-07")),
                        contestId = "",
                        eventFeedPath = "",
                        feedVersion = FeedVersion.`2022_07`
                    )
                )
            )
        )
    }


}