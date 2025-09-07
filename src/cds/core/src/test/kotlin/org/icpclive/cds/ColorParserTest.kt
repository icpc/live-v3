package org.icpclive.cds

import org.icpclive.cds.api.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class ColorParserTest {
    private fun doTest(data: Map<String, String?>) {
        for ((k, expected) in data) {
            assertEquals(expected, Color.normalize(k)?.value, "Parsing \"$k\" should result in $expected")
        }
    }

    @Test
    fun testStandardCssShorthand() {
        doTest(
            mapOf(
                "#f00" to "#ff0000ff",
                "#F00" to "#ff0000ff",
                "#0f0" to "#00ff00ff",
                "#0F0" to "#00ff00ff",
                "#00f" to "#0000ffff",
                "#00F" to "#0000ffff",
                "#09c" to "#0099ccff",
                "#09C" to "#0099ccff",
            )
        )
    }

    @Test
    fun testStandardCssSixDigit() {
        doTest(
            mapOf(
                "#ff0000" to "#ff0000ff",
                "#FF0000" to "#ff0000ff",
                "#00ff00" to "#00ff00ff",
                "#00FF00" to "#00ff00ff",
                "#0000ff" to "#0000ffff",
                "#0000FF" to "#0000ffff",
                "#123456" to "#123456ff",
            )
        )
    }

    @Test
    fun testStandardCssRgbaShorthand() {
        doTest(
            mapOf(
                "#f008" to "#ff000088",
                "#F008" to "#ff000088",
                "#0f0c" to "#00ff00cc",
                "#0F0C" to "#00ff00cc",
                "#1234" to "#11223344",
            )
        )
    }

    @Test
    fun testStandardCssEightDigitRgba() {
        doTest(
            mapOf(
                "#ff000080" to "#ff000080",
                "#FF000080" to "#ff000080",
                "#00000000" to "#00000000",
                "#FFFFFFFF" to "#ffffffff",
            )
        )
    }

    @Test
    fun testNonstandardPermissive() {
        doTest(
            mapOf(
                "0x11223344" to "#22334411",
                "0xff0000" to "#ff0000ff",
                "0xFF0000" to "#ff0000ff",
            )
        )
    }

    @Test
    fun testInvalidInputs() {
        doTest(
            mapOf(
                "#f" to null,
                "#F" to null,
                "#fffff" to null,
                "#FFFFF" to null,
                "xyz" to null,
                "XYZ" to null,
                "" to null,
                "#ggg" to null,
                "#GGG" to null,
            )
        )
    }
}