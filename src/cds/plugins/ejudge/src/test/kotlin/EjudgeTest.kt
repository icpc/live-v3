package org.icpclive.cds.plugins.ejudge

import org.icpclive.cds.CdsLoadersTest
import org.icpclive.cds.settings.*
import kotlin.test.Test

object EjudgeTest : CdsLoadersTest() {
    @Test
    fun ejudge() {
        loaderTest(
            goldenDataDir.resolve("ejudge.txt"),
            EjudgeSettings(
                url = UrlOrLocalPath.Local(testDataDir.resolve("ejudge.xml"))
            )
        )
    }

}