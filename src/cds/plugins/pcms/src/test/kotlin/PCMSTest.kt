package org.icpclive.cds.plugins.pcms

import org.icpclive.cds.CdsLoadersTest
import org.icpclive.cds.api.ContestResultType
import org.icpclive.cds.settings.UrlOrLocalPath
import org.icpclive.cds.tunning.OverrideContestSettings
import kotlin.test.Test
import kotlin.time.Instant

object PCMSTest : CdsLoadersTest() {
    @Test
    fun pcms() {
        loaderTest(
            goldenDataDir.resolve("pcms.txt"),
            PCMSSettings(
                source = UrlOrLocalPath.Local(testDataDir.resolve("pcms.xml"))
            ),
            listOf(OverrideContestSettings(startTime = Instant.fromEpochSeconds(1670397300)))
        )
    }

    @Test
    fun pcmsIOI() {
        loaderTest(
            goldenDataDir.resolve("pcmsIOI.txt"),
            PCMSSettings(
                source = UrlOrLocalPath.Local(testDataDir.resolve("pcms-ioi.xml"))
            ) {
                resultType = ContestResultType.IOI
            },
            listOf(OverrideContestSettings(startTime = Instant.fromEpochSeconds(1670397300)))
        )
    }

    @Test
    fun pcmsLegacy() {
        loaderTest(
            goldenDataDir.resolve("pcmsLegacy.txt"),
            PCMSSettings(
                source = UrlOrLocalPath.Local(testDataDir.resolve("pcms-legacy.xml"))
            ),
            listOf(OverrideContestSettings(startTime = Instant.fromEpochSeconds(1449385200)))
        )
    }
}