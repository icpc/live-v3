package org.icpclive.cds.plugins.pcms

import kotlinx.datetime.Instant
import org.icpclive.cds.CdsLoadersTest
import org.icpclive.cds.api.ContestResultType
import org.icpclive.cds.settings.*
import org.icpclive.cds.tunning.*
import kotlin.test.Test

object PCMSTest : CdsLoadersTest() {
    @Test
    fun pcms() {
        loaderTest(
            goldenDataDir.resolve("pcms.txt"),
            PCMSSettings(
                url = UrlOrLocalPath.Local(testDataDir.resolve("pcms.xml"))
            ),
            AdvancedProperties(startTime = Instant.fromEpochSeconds(1670397300))
        )
    }

    @Test
    fun pcmsIOI() {
        loaderTest(
            goldenDataDir.resolve("pcmsIOI.txt"),
            PCMSSettings(
                url = UrlOrLocalPath.Local(testDataDir.resolve("pcms-ioi.xml"))
            ) {
                resultType = ContestResultType.IOI
            },
            AdvancedProperties(startTime = Instant.fromEpochSeconds(1670397300))
        )
    }

    @Test
    fun pcmsLegacy() {
        loaderTest(
            goldenDataDir.resolve("pcmsLegacy.txt"),
            PCMSSettings(
                url = UrlOrLocalPath.Local(testDataDir.resolve("pcms-legacy.xml"))
            ),
            AdvancedProperties(startTime = Instant.fromEpochSeconds(1449385200))
        )
    }
}