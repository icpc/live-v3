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
        val settings = ProfileRenderSettings(contestType = "ICPC", finals = ProfileRenderSettings.Finals(include = true))
        assertEquals(
            """{"contestType":"ICPC","finals":{"include":true}}""",
            settingsJson.encodeToString(ProfileRenderSettings.serializer(), settings),
        )
    }

    @Test
    fun substitutesBlobsAndScalars() {
        val record = buildJsonObject { put("id", "1"); put("html", "<b>") }
        val settings = ProfileRenderSettings(contestType = "Team", fontColor = "#FFFFFF")
        val result = buildProfileCardSvg(template, record, settings, teamColor = "#123456")
        assertFalse("{team.json}" in result)
        assertFalse("{render.json}" in result)
        assertFalse("<b>" in result)              // raw < must not survive inside the blob
        assertTrue("\\u003cb>" in result)         // < escaped as <
        assertTrue("--main-color: #123456" in result)
        assertTrue("--font-color: #FFFFFF" in result)
        assertTrue(""""contestType":"Team"""" in result)
    }

    @Test
    fun leavesTokensForFallbackWhenDataAbsent() {
        val result = buildProfileCardSvg(template, buildJsonObject { put("id", "1") }, settings = null, teamColor = null)
        assertTrue("{render.json}" in result)
        assertTrue("{mainColor}" in result)
        assertTrue("{fontColor}" in result)
        assertFalse("{team.json}" in result)
    }
}
