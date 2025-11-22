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
                "#f00" to "#ff0000",
                "#F00" to "#ff0000",
                "#0f0" to "#00ff00",
                "#0F0" to "#00ff00",
                "#00f" to "#0000ff",
                "#00F" to "#0000ff",
                "#09c" to "#0099cc",
                "#09C" to "#0099cc",
            )
        )
    }

    @Test
    fun testStandardCssSixDigit() {
        doTest(
            mapOf(
                "#ff0000" to "#ff0000",
                "#FF0000" to "#ff0000",
                "#00ff00" to "#00ff00",
                "#00FF00" to "#00ff00",
                "#0000ff" to "#0000ff",
                "#0000FF" to "#0000ff",
                "#123456" to "#123456",
            )
        )
    }

    @Test
    fun testStandardCssRgbaShorthand() {
        doTest(
            mapOf(
                "#f008" to "#ff0000",
                "#F008" to "#ff0000",
                "#0f0c" to "#00ff00",
                "#0F0C" to "#00ff00",
                "#1234" to "#112233",
            )
        )
    }

    @Test
    fun testStandardCssEightDigitRgba() {
        doTest(
            mapOf(
                "#ff000080" to "#ff0000",
                "#FF000080" to "#ff0000",
                "#00000000" to "#000000",
                "#FFFFFFFF" to "#ffffff",
            )
        )
    }

    @Test
    fun testNonstandardPermissive() {
        doTest(
            mapOf(
                "0x11223344" to "#223344",
                "0xff0000" to "#ff0000",
                "0xFF0000" to "#ff0000",
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