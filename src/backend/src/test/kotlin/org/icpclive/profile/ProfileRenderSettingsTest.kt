package org.icpclive.profile

import kotlinx.serialization.json.*
import kotlin.test.*

class ProfileRenderSettingsTest {
    private val template = """<svg><script id="t"><![CDATA[{team.json}]]></script>""" +
        """<script id="r"><![CDATA[{render.json}]]></script>""" +
        """<style>:root { --main-color: {mainColor}; }</style>""" +
        """<style>:root { --font-color: {fontColor}; }</style></svg>"""

    /** Round-trips [settings] through the same Json config production code uses to get a raw object. */
    private fun raw(settings: ProfileRenderSettings): JsonObject =
        settingsJson.encodeToJsonElement(ProfileRenderSettings.serializer(), settings) as JsonObject

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
        val result = buildProfileCardSvg(template, record, raw(settings), settings, teamColor = "#123456")
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
        val result = buildProfileCardSvg(template, record, raw(settings), settings, teamColor = "#123456")
        // the template's own tokens are gone, but every token literal carried by the data is intact
        assertTrue(""""note":"{fontColor} and {render.json} and {mainColor} and {team.json}"""" in result)
        for (token in listOf("{team.json}", "{render.json}", "{mainColor}", "{fontColor}")) {
            assertEquals(1, result.split(token).size - 1, "expected exactly one surviving $token")
        }
    }

    @Test
    fun leavesTokensForFallbackWhenDataAbsent() {
        val result = buildProfileCardSvg(
            template, buildJsonObject { put("id", "1") }, rawSettings = null, settings = null, teamColor = null,
        )
        assertTrue("{render.json}" in result)
        assertTrue("{mainColor}" in result)
        assertTrue("{fontColor}" in result)
        assertFalse("{team.json}" in result)
    }

    @Test
    fun settingsPresentButTeamColorMissingLeavesMainColorToken() {
        val settings = ProfileRenderSettings(contestType = ContestType.ICPC, fontColor = "#FFFFFF")
        val result = buildProfileCardSvg(
            template,
            buildJsonObject { put("id", "1") },
            raw(settings),
            settings,
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
            rawSettings = null,
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
        val settings = ProfileRenderSettings(contestType = ContestType.ICPC, hideSite = true)
        val result = buildProfileCardSvg(
            template,
            buildJsonObject { put("id", "1") },
            raw(settings),
            settings,
            teamColor = "#123456",
        )
        assertTrue("{fontColor}" in result)
        assertTrue("--main-color: #123456" in result)
        assertFalse("{render.json}" in result)
        assertTrue(""""hideSite":true""" in result)
    }

    @Test
    fun rawSettingsPassthroughKeepsUnknownKeysAndDropsInvalidColors() {
        // "futureFlag" is not a field ProfileRenderSettings knows about, and "mainColor" is
        // invalid: {render.json} must still carry the former and must never carry the latter.
        val settingsRaw = buildJsonObject {
            put("contestType", "Team")
            put("fontColor", "#ffffff")
            put("mainColor", "not-a-color")
            put("futureFlag", true)
        }
        val sanitized = sanitizeRawSettings(settingsRaw)
        val typed = settingsJson.decodeFromJsonElement(ProfileRenderSettings.serializer(), settingsRaw)
        val result = buildProfileCardSvg(template, buildJsonObject { put("id", "1") }, sanitized, typed, teamColor = null)
        assertTrue(""""futureFlag":true""" in result, "unknown keys must survive into {render.json}")
        assertFalse("not-a-color" in result, "an invalid color value must never reach the substituted blob")
        assertTrue(""""fontColor":"#ffffff"""" in result)
        assertTrue("\"mainColor\"" !in result, "the invalid mainColor key itself must be dropped, not just its value")
    }

    @Test
    fun validColorsSurviveValidation() {
        for (color in listOf("#fff", "#FFF", "#abcd", "#123456", "#12345678")) {
            val settings = ProfileRenderSettings(fontColor = color, mainColor = color).withValidatedColors()
            assertEquals(color, settings.fontColor, color)
            assertEquals(color, settings.mainColor, color)
        }
    }

    @Test
    fun invalidColorsAreDropped() {
        val bad = listOf(
            "red", "#12", "#GGGGGG", "#123456789", "#123; } * { display: none", "", "url(x)",
            "#12345", "#1234567",
        )
        for (color in bad) {
            val settings = ProfileRenderSettings(fontColor = color, mainColor = color).withValidatedColors()
            assertNull(settings.fontColor, color)
            assertNull(settings.mainColor, color)
        }
    }

    @Test
    fun invalidColorIsNotSubstitutedIntoStyleOrRenderJson() {
        val badColor = "#fff; } * { display: none; } x{y:z"
        val settings = ProfileRenderSettings(fontColor = badColor).withValidatedColors()
        val rawSettings = sanitizeRawSettings(buildJsonObject { put("fontColor", badColor) })
        val result = buildProfileCardSvg(template, buildJsonObject { put("id", "1") }, rawSettings, settings, teamColor = null)
        assertTrue("{fontColor}" in result)
        assertFalse("display: none" in result)
    }

    @Test
    fun sanitizeRawSettingsDropsInvalidColorsKeepsRest() {
        val settingsRaw = buildJsonObject {
            put("mainColor", "#123456")
            put("fontColor", "bad")
            put("futureFlag", true)
        }
        val sanitized = sanitizeRawSettings(settingsRaw)
        assertEquals("#123456", (sanitized["mainColor"] as JsonPrimitive).content)
        assertNull(sanitized["fontColor"])
        assertEquals(true, (sanitized["futureFlag"] as JsonPrimitive).boolean)
    }

    @Test
    fun sanitizeRawSettingsLeavesObjectUntouchedWhenColorsAreValidOrAbsent() {
        val settingsRaw = buildJsonObject { put("contestType", "ICPC") }
        assertSame(settingsRaw, sanitizeRawSettings(settingsRaw))
    }
}
