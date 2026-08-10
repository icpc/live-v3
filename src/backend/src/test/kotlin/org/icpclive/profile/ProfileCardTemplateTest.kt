package org.icpclive.profile

import kotlinx.serialization.json.*
import org.icpclive.cds.api.*
import java.io.ByteArrayInputStream
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.*
import kotlin.test.*

class ProfileCardTemplateTest {
    // gradle test working dir is the module dir (src/backend)
    private val templatePath = Path.of("../../config/_media/team.svg")

    private fun sampleRecord(): JsonObject {
        val team = TeamInfo(
            id = "42".toTeamId(),
            fullName = "Test University: Test Team",
            displayName = "Test Team",
            groups = emptyList(),
            hashTag = "#TEST",
            medias = emptyMap(),
            isHidden = false,
            isOutOfContest = false,
            organizationId = "u1".toOrganizationId(),
        )
        val organization = OrganizationInfo("u1".toOrganizationId(), "TU", "Test University", emptyList())
        val roster = Roster(listOf("Ann Alpha", "Zed Zulu", "Mid Middle"), "Coach Zh")
        return reconcileProfile(null, roster, team, organization)
    }

    @Test
    fun templateContainsExpectedTokens() {
        val template = templatePath.readText()
        for (token in listOf("{team.json}", "{render.json}", "{mainColor}", "{fontColor}")) {
            assertEquals(1, template.split(token).size - 1, "expected exactly one $token")
        }
    }

    @Test
    fun substitutedTemplateIsValidXmlAndTokenFree() {
        val settings = ProfileRenderSettings(
            contestType = "ICPC",
            finals = ProfileRenderSettings.Finals(include = true, includeEmpty = true),
            fontColor = "#FFFFFF",
        )
        val svg = buildProfileCardSvg(templatePath.readText(), sampleRecord(), settings, teamColor = "#4C83C3")
        assertFalse("{team.json}" in svg)
        assertFalse("{render.json}" in svg)
        assertFalse("{mainColor}" in svg)
        assertFalse("{fontColor}" in svg)
        DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(ByteArrayInputStream(svg.toByteArray()))   // throws on malformed XML
        val out = Path.of("build/profile-card-sample.svg")
        out.parent.createDirectories()
        out.writeText(svg)
    }
}
