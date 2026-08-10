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
    private val templates = listOf("team.svg", "personal.svg").map { Path.of("../../config/_media/$it") }

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
    fun templatesContainExpectedTokens() {
        for (templatePath in templates) {
            val template = templatePath.readText()
            for (token in listOf("{team.json}", "{render.json}", "{mainColor}", "{fontColor}")) {
                assertEquals(1, template.split(token).size - 1, "expected exactly one $token in ${templatePath.name}")
            }
        }
    }

    @Test
    fun templatesContainLiteralAssetTokensExactlyOnce() {
        // {Logo}/{LogoExtension}/{Background} are substituted by the template's own embedded
        // script, not by the backend (see buildProfileCardSvg), but the contract is still that
        // each appears exactly once so the script's own replace-all-occurrences logic is safe.
        for (templatePath in templates) {
            val template = templatePath.readText()
            for (token in listOf("{Logo}", "{LogoExtension}", "{Background}")) {
                assertEquals(1, template.split(token).size - 1, "expected exactly one $token in ${templatePath.name}")
            }
        }
    }

    @Test
    fun substitutedTemplatesAreValidXmlAndTokenFree() {
        val settings = ProfileRenderSettings(
            contestType = ContestType.ICPC,
            finals = ProfileRenderSettings.Finals(include = true, includeEmpty = true),
            fontColor = "#FFFFFF",
        )
        val rawSettings = settingsJson.encodeToJsonElement(ProfileRenderSettings.serializer(), settings) as JsonObject
        for (templatePath in templates) {
            val svg = buildProfileCardSvg(templatePath.readText(), sampleRecord(), rawSettings, settings, teamColor = "#4C83C3")
            for (token in listOf("{team.json}", "{render.json}", "{mainColor}", "{fontColor}")) {
                assertFalse(token in svg, "$token left in substituted ${templatePath.name}")
            }
            DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(ByteArrayInputStream(svg.toByteArray()))   // throws on malformed XML
            val out = Path.of("build/profile-card-sample-${templatePath.name}")
            out.parent.createDirectories()
            out.writeText(svg)
        }
    }
}
