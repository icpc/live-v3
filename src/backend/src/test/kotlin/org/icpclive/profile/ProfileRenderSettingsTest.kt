package org.icpclive.profile

import kotlinx.serialization.json.*
import kotlin.test.*

class ProfileRenderSettingsTest {
    private val template = """<svg><script id="t"><![CDATA[{team.json}]]></script>""" +
        """<script id="r"><![CDATA[{render.json}]]></script>""" +
        """<style>:root { --main-color: {mainColor}; }</style>""" +
        """<style>:root { --font-color: {fontColor}; }</style></svg>"""

    @Test
    fun serializationOmitsUnsetKeys() {
        val settings =
            ProfileRenderSettings(contestType = ContestType.ICPC, finals = ProfileRenderSettings.Finals(include = true))
        assertEquals(
            """{"contestType":"ICPC","finals":{"include":true}}""",
            settingsJson.encodeToString(ProfileRenderSettings.serializer(), settings),
        )
    }

    @Test
    fun contestTypeNamesMatchTheDocumentedSpelling() {
        assertEquals(
            ContestType.PERSONAL,
            settingsJson.decodeFromString(
                ProfileRenderSettings.serializer(),
                """{"contestType":"Personal"}""",
            ).contestType,
        )
        assertFails {
            settingsJson.decodeFromString(ProfileRenderSettings.serializer(), """{"contestType":"personal"}""")
        }
    }

    @Test
    fun substitutesBlobsAndScalars() {
        val record = buildJsonObject { put("id", "1"); put("html", "<b>"); put("cdata", "x]]>y") }
        val settings = ProfileRenderSettings(contestType = ContestType.TEAM, fontColor = "#FFFFFF")
        val result = buildProfileCardSvg(template, record, settings, teamColor = "#123456")
        assertFalse("{team.json}" in result)
        assertFalse("{render.json}" in result)
        assertFalse("<b>" in result)                  // raw < must not survive inside the blob
        assertTrue("\\u003cb\\u003e" in result)       // < and > escaped as unicode sequences
        assertEquals(2, Regex("]]>").findAll(result).count())  // only the two template CDATA terminators remain
        assertTrue("--main-color: #123456" in result)
        assertTrue("--font-color: #FFFFFF" in result)
        assertTrue(""""contestType":"Team"""" in result)
    }

    @Test
    fun tokenLiteralsInsideTheRecordSurviveSubstitution() {
        val record = buildJsonObject {
            put("id", "1")
            put("note", "{fontColor} and {render.json} and {mainColor} and {team.json}")
        }
        val settings = ProfileRenderSettings(contestType = ContestType.TEAM, fontColor = "#FFFFFF")
        val result = buildProfileCardSvg(template, record, settings, teamColor = "#123456")
        // the template's own tokens are gone, but every token literal carried by the data is intact
        assertTrue(""""note":"{fontColor} and {render.json} and {mainColor} and {team.json}"""" in result)
        for (token in listOf("{team.json}", "{render.json}", "{mainColor}", "{fontColor}")) {
            assertEquals(1, result.split(token).size - 1, "expected exactly one surviving $token")
        }
    }

    @Test
    fun leavesTokensForFallbackWhenDataAbsent() {
        val result = buildProfileCardSvg(template, buildJsonObject { put("id", "1") }, settings = null, teamColor = null)
        assertTrue("{render.json}" in result)
        assertTrue("{mainColor}" in result)
        assertTrue("{fontColor}" in result)
        assertFalse("{team.json}" in result)
    }

    @Test
    fun settingsPresentButTeamColorMissingLeavesMainColorToken() {
        val result = buildProfileCardSvg(
            template,
            buildJsonObject { put("id", "1") },
            ProfileRenderSettings(contestType = ContestType.ICPC, fontColor = "#FFFFFF"),
            teamColor = null,
        )
        assertTrue("{mainColor}" in result)
        assertFalse("{fontColor}" in result)
        assertFalse("{render.json}" in result)
        assertFalse("{team.json}" in result)
    }

    @Test
    fun teamColorWithoutSettingsSubstitutesOnlyMainColor() {
        val result = buildProfileCardSvg(
            template,
            buildJsonObject { put("id", "1") },
            settings = null,
            teamColor = "#123456",
        )
        assertTrue("--main-color: #123456" in result)
        assertTrue("{fontColor}" in result)
        assertTrue("{render.json}" in result)
        assertFalse("{team.json}" in result)
    }

    @Test
    fun settingsWithoutFontColorLeavesFontColorToken() {
        val result = buildProfileCardSvg(
            template,
            buildJsonObject { put("id", "1") },
            ProfileRenderSettings(contestType = ContestType.ICPC, hideSite = true),
            teamColor = "#123456",
        )
        assertTrue("{fontColor}" in result)
        assertTrue("--main-color: #123456" in result)
        assertFalse("{render.json}" in result)
        assertTrue(""""hideSite":true""" in result)
    }

    @Test
    fun validColorsSurviveValidation() {
        for (color in listOf("#fff", "#FFF", "#123456", "#12345678")) {
            val settings = ProfileRenderSettings(fontColor = color, mainColor = color).withValidatedColors()
            assertEquals(color, settings.fontColor, color)
            assertEquals(color, settings.mainColor, color)
        }
    }

    @Test
    fun invalidColorsAreDropped() {
        val bad = listOf("red", "#12", "#GGGGGG", "#123456789", "#123; } * { display: none", "", "url(x)")
        for (color in bad) {
            val settings = ProfileRenderSettings(fontColor = color, mainColor = color).withValidatedColors()
            assertNull(settings.fontColor, color)
            assertNull(settings.mainColor, color)
        }
    }

    @Test
    fun invalidColorIsNotSubstitutedIntoStyleOrRenderJson() {
        val settings = ProfileRenderSettings(fontColor = "#fff; } * { display: none; } x{y:z").withValidatedColors()
        val result = buildProfileCardSvg(template, buildJsonObject { put("id", "1") }, settings, teamColor = null)
        assertTrue("{fontColor}" in result)
        assertFalse("display: none" in result)
    }
}
