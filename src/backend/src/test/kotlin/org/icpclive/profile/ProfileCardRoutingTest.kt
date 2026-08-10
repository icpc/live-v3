package org.icpclive.profile

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import org.icpclive.cds.api.*
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.*
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

@OptIn(InefficientContestInfoApi::class)
class ProfileCardRoutingTest {
    @TempDir
    lateinit var root: Path

    private val template = """<svg><script id="t"><![CDATA[{team.json}]]></script>""" +
        """<script id="r"><![CDATA[{render.json}]]></script>""" +
        """<style>:root { --main-color: {mainColor}; --font-color: {fontColor}; }</style></svg>"""

    private val mediaDir: Path get() = root.resolve("media")
    private val profilesDir: Path get() = root.resolve("profiles")

    private fun writeTemplate(name: String = "team.svg") {
        mediaDir.createDirectories()
        mediaDir.resolve(name).writeText(template)
    }

    private fun writeProfile(teamId: String, content: String) {
        val teams = profilesDir.resolve("teams")
        teams.createDirectories()
        teams.resolve("$teamId.json").writeText(content)
    }

    private fun writeSettings(content: String) {
        profilesDir.createDirectories()
        profilesDir.resolve("settings.json").writeText(content)
    }

    private fun team(
        id: String = "1",
        displayName: String = "Team A",
        color: String? = null,
        organizationId: String? = null,
    ) = TeamInfo(
        id = id.toTeamId(),
        fullName = "Uni: $displayName",
        displayName = displayName,
        groups = emptyList(),
        hashTag = null,
        medias = emptyMap(),
        isHidden = false,
        isOutOfContest = false,
        organizationId = organizationId?.toOrganizationId(),
        color = color?.let { Color.normalize(it) },
    )

    private fun contestInfo(
        teams: List<TeamInfo> = listOf(team()),
        persons: List<PersonInfo> = emptyList(),
        organizations: List<OrganizationInfo> = emptyList(),
    ) = ContestInfo(
        name = "test contest",
        resultType = ContestResultType.ICPC,
        startTime = Instant.fromEpochMilliseconds(0),
        contestLength = 5.hours,
        freezeTime = 4.hours,
        problemList = emptyList(),
        teamList = teams,
        groupList = emptyList(),
        organizationList = organizations,
        languagesList = emptyList(),
        penaltyRoundingMode = PenaltyRoundingMode.EACH_SUBMISSION_DOWN_TO_MINUTE,
    ).copy(personsList = persons)

    private fun withCards(info: ContestInfo = contestInfo(), block: suspend ApplicationTestBuilder.() -> Unit) =
        testApplication {
            application {
                routing {
                    route("/profile") {
                        configureProfileCardRouting(mediaDir, profilesDir) { info }
                    }
                }
            }
            block()
        }

    @Test
    fun servesSubstitutedCard() {
        writeTemplate()
        writeProfile(
            "1",
            """{"id":"1","university":{"fullName":"Test University"},"team":{"name":"Team A"},""" +
                """"coach":null,"contestants":[{"name":"Alice Smith","altNames":[],"cfRating":2500}]}""",
        )
        writeSettings("""{"contestType":"ICPC","fontColor":"#FFFFFF"}""")
        withCards(contestInfo(listOf(team(color = "#4C83C3")), listOf(
            PersonInfo("p1".toPersonId(), "Alice Smith", "contestant", teamIds = listOf("1".toTeamId())),
        ))) {
            val response = client.get("/profile/team.svg?teamId=1")
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(ContentType.Image.SVG, response.contentType()?.withoutParameters())
            val body = response.bodyAsText()
            assertTrue(""""cfRating":2500""" in body, "profile data must reach the card")
            assertTrue(""""name":"Alice Smith"""" in body)
            assertTrue(""""contestType":"ICPC"""" in body)
            assertTrue("--main-color: #4c83c3" in body, "team color overrides the main color")
            assertTrue("--font-color: #FFFFFF" in body)
            for (token in listOf("{team.json}", "{render.json}", "{mainColor}", "{fontColor}")) {
                assertFalse(token in body, "$token left unsubstituted")
            }
        }
    }

    @Test
    fun unknownTemplateIsNotFound() {
        writeTemplate()
        withCards {
            val response = client.get("/profile/nope.svg?teamId=1")
            assertEquals(HttpStatusCode.NotFound, response.status)
            assertEquals("Template not found", response.bodyAsText())
        }
    }

    @Test
    fun unknownTeamIsNotFound() {
        writeTemplate()
        withCards {
            val response = client.get("/profile/team.svg?teamId=42")
            assertEquals(HttpStatusCode.NotFound, response.status)
            assertEquals("Unknown team", response.bodyAsText())
        }
    }

    @Test
    fun teamCheckHappensBeforeTemplateResolutionSoUnknownTeamWinsOverUnknownTemplate() {
        writeTemplate()
        withCards {
            val response = client.get("/profile/nope.svg?teamId=42")
            assertEquals(HttpStatusCode.NotFound, response.status)
            assertEquals("Unknown team", response.bodyAsText())
        }
    }

    @Test
    fun missingTeamIdIsBadRequest() {
        writeTemplate()
        withCards {
            assertEquals(HttpStatusCode.BadRequest, client.get("/profile/team.svg").status)
            assertEquals(HttpStatusCode.BadRequest, client.get("/profile/team.svg?teamId=").status)
        }
    }

    @Test
    fun pathTraversalIsNotFound() {
        writeTemplate()
        root.resolve("secret.svg").writeText("TOP SECRET")
        withCards {
            // A plain "../" path segment is routed by ktor as an ordinary literal segment of the
            // "{path...}" tail parameter, so it demonstrably reaches our own resolveInside guard
            // and must fail closed with exactly the same NotFound our handler produces for any
            // other unresolvable template.
            assertEquals(HttpStatusCode.NotFound, client.get("/profile/../secret.svg?teamId=1").status)

            // Percent-encoded variants may never reach our handler at all: ktor's own URL
            // decoding/routing can already reject or normalize these before dispatch. Kept as a
            // defense-in-depth check (not OK, no leaked content) rather than asserting the exact
            // status/body our handler would produce.
            for (path in listOf(
                "/profile/..%2fsecret.svg",
                "/profile/..%2F..%2Fetc%2Fpasswd",
                "/profile/%2e%2e/secret.svg",
            )) {
                val response = client.get("$path?teamId=1")
                assertNotEquals(HttpStatusCode.OK, response.status, path)
                assertFalse("TOP SECRET" in response.bodyAsText(), path)
            }
        }
    }

    @Test
    fun teamIdCanNotEscapeTheProfilesDirectory() {
        writeTemplate()
        root.resolve("outside.json").writeText("""{"marker":"escaped","contestants":[]}""")
        // a contest system could in principle hand out a team id that looks like a path
        withCards(contestInfo(listOf(team(id = "../../outside")))) {
            val response = client.get("/profile/team.svg?teamId=..%2F..%2Foutside")
            assertEquals(HttpStatusCode.OK, response.status)
            assertFalse("escaped" in response.bodyAsText())
        }
    }

    @Test
    fun teamIdWithASlashIsRejectedBeforeAnyPathResolution() {
        writeTemplate()
        // settings.json legitimately reaches {render.json} regardless of this guard (that's the
        // whole point of item 5), so make it fail to parse as settings (bad contestType) to keep
        // {render.json} as a literal, unsubstituted token -- isolating this test to only the
        // profile-file lookup that "../settings" is trying to hijack.
        writeSettings("""{"contestType":"NotAValidType","marker":"escaped-settings"}""")
        withCards(contestInfo(listOf(team(id = "../settings")))) {
            val response = client.get("/profile/team.svg?teamId=..%2Fsettings")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertFalse("escaped-settings" in body, "settings.json must not be readable as this team's profile file")
            assertTrue(""""name":"Team A"""" in body, "falls back to a synthesized profile instead")
        }
    }

    @Test
    fun teamIdWithASlashIsRejectedEvenWhenTheResultingPathStaysInsideTheProfilesDirectory() {
        writeTemplate()
        // Nested under teams/, so resolveInside's own root-containment check alone would happily
        // allow this path (it never leaves the profiles directory); only the dedicated teamId
        // separator guard stops a team id from reaching into a subdirectory of teams/ at all.
        val nested = profilesDir.resolve("teams").resolve("nested")
        nested.createDirectories()
        nested.resolve("evil.json").writeText("""{"marker":"escaped-nested","contestants":[]}""")
        withCards(contestInfo(listOf(team(id = "nested/evil")))) {
            val response = client.get("/profile/team.svg?teamId=nested%2Fevil")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertFalse("escaped-nested" in body)
            assertTrue(""""name":"Team A"""" in body, "falls back to a synthesized profile instead")
        }
    }

    @Test
    fun symlinkedTemplateOutsideMediaIsNotServed() {
        writeTemplate()
        val outside = root.resolve("outside.svg")
        outside.writeText(template)
        Files.createSymbolicLink(mediaDir.resolve("link.svg"), outside)
        withCards {
            assertEquals(HttpStatusCode.NotFound, client.get("/profile/link.svg?teamId=1").status)
        }
    }

    @Test
    fun symlinkedProfileOutsideProfilesDirectoryIsIgnored() {
        writeTemplate()
        writeProfile("2", "{}")
        root.resolve("outside.json").writeText("""{"marker":"escaped","contestants":[]}""")
        Files.createSymbolicLink(profilesDir.resolve("teams").resolve("1.json"), root.resolve("outside.json"))
        withCards {
            val response = client.get("/profile/team.svg?teamId=1")
            assertEquals(HttpStatusCode.OK, response.status)
            assertFalse("escaped" in response.bodyAsText())
        }
    }

    @Test
    fun malformedProfileFallsBackToSynthesizedRecord() {
        writeTemplate()
        writeProfile("1", "{ this is not json")
        withCards(
            contestInfo(
                listOf(team(organizationId = "o1")),
                listOf(PersonInfo("p1".toPersonId(), "Alice Smith", "contestant", teamIds = listOf("1".toTeamId()))),
                listOf(OrganizationInfo("o1".toOrganizationId(), "TU", "Test University", emptyList())),
            )
        ) {
            val response = client.get("/profile/team.svg?teamId=1")
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(""""fullName":"Test University"""" in body)
            assertTrue(""""name":"Alice Smith"""" in body)
        }
    }

    @Test
    fun nonObjectProfileFallsBackToSynthesizedRecord() {
        writeTemplate()
        writeProfile("1", """["not","an","object"]""")
        withCards {
            val response = client.get("/profile/team.svg?teamId=1")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(""""name":"Team A"""" in response.bodyAsText())
        }
    }

    @Test
    fun personalModeUsesTeamDisplayName() {
        writeTemplate("personal.svg")
        writeSettings("""{"contestType":"Personal"}""")
        withCards(contestInfo(listOf(team(displayName = "Solo Person")))) {
            val body = client.get("/profile/personal.svg?teamId=1").bodyAsText()
            assertTrue(
                """"contestants":[{"name":"Solo Person","altNames":[],"achievements":[]}]""" in body,
                "personal mode must produce exactly one contestant taken from the team name",
            )
        }
    }

    @Test
    fun withoutSettingsTokensAreLeftForTheTemplateFallback() {
        writeTemplate()
        withCards {
            val body = client.get("/profile/team.svg?teamId=1").bodyAsText()
            assertFalse("{team.json}" in body)
            assertTrue("{render.json}" in body)
            assertTrue("{mainColor}" in body)
            assertTrue("{fontColor}" in body)
        }
    }

    @Test
    fun brokenSettingsFileIsIgnored() {
        writeTemplate()
        writeSettings("""{"contestType":"Persnal"}""")
        withCards {
            val response = client.get("/profile/team.svg?teamId=1")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue("{render.json}" in response.bodyAsText())
        }
    }

    @Test
    fun oversizedTemplateIsNotServed() {
        mediaDir.createDirectories()
        mediaDir.resolve("huge.svg").writeText("x".repeat(11 * 1024 * 1024))
        withCards {
            assertEquals(HttpStatusCode.NotFound, client.get("/profile/huge.svg?teamId=1").status)
        }
    }

    @Test
    fun oversizedSettingsFileIsIgnored() {
        writeTemplate()
        // Over the 1 MiB JSON limit; a repeated single character is fast to generate and enough
        // to trip the size check regardless of content.
        writeSettings("{\"contestType\":\"ICPC\",\"padding\":\"" + "x".repeat(1024 * 1024 + 1) + "\"}")
        withCards {
            val response = client.get("/profile/team.svg?teamId=1")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue("{render.json}" in response.bodyAsText(), "oversized settings.json must fall back like a missing one")
        }
    }

    @Test
    fun oversizedProfileFileIsIgnored() {
        writeTemplate()
        writeProfile("1", "{\"id\":\"1\",\"padding\":\"" + "x".repeat(1024 * 1024 + 1) + "\"}")
        withCards {
            val response = client.get("/profile/team.svg?teamId=1")
            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(
                """"name":"Team A"""" in response.bodyAsText(),
                "oversized profile file must fall back to a synthesized record",
            )
        }
    }

    @Test
    fun settingsFileIsCachedWhileItsTimestampIsUnchanged() {
        writeTemplate()
        writeSettings("""{"contestType":"ICPC"}""")
        withCards {
            assertTrue(""""contestType":"ICPC"""" in client.get("/profile/team.svg?teamId=1").bodyAsText())
            val file = profilesDir.resolve("settings.json")
            val modifiedAt = Files.getLastModifiedTime(file)
            file.writeText("""{"contestType":"Team"}""")
            Files.setLastModifiedTime(file, modifiedAt)
            assertTrue(
                """"contestType":"ICPC"""" in client.get("/profile/team.svg?teamId=1").bodyAsText(),
                "settings.json must be served from cache while its timestamp and size are unchanged",
            )
        }
    }

    @Test
    fun settingsFileChangesAreSeenAfterModification() {
        writeTemplate()
        writeSettings("""{"contestType":"ICPC"}""")
        withCards {
            assertTrue(""""contestType":"ICPC"""" in client.get("/profile/team.svg?teamId=1").bodyAsText())
            val file = profilesDir.resolve("settings.json")
            file.writeText("""{"contestType":"Team"}""")
            Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis() + 10_000))
            assertTrue(""""contestType":"Team"""" in client.get("/profile/team.svg?teamId=1").bodyAsText())
        }
    }

    @Test
    fun templateIsCachedWhileItsTimestampIsUnchanged() {
        writeTemplate()
        withCards {
            assertTrue("--main-color" in client.get("/profile/team.svg?teamId=1").bodyAsText())
            val file = mediaDir.resolve("team.svg")
            val modifiedAt = Files.getLastModifiedTime(file)
            // Same length as the original template: only the mtime-and-size cache key must stay
            // unchanged, so this is a genuine cache hit rather than an incidental one.
            val replacement = """<svg id="v2">{team.json}</svg>"""
            val padded = replacement + "<!--" + "x".repeat(template.length - replacement.length - 7) + "-->"
            check(padded.length == template.length) { "test fixture must keep the same length as the original template" }
            file.writeText(padded)
            Files.setLastModifiedTime(file, modifiedAt)
            assertFalse(
                """id="v2"""" in client.get("/profile/team.svg?teamId=1").bodyAsText(),
                "template must be served from cache while its timestamp and size are unchanged",
            )
        }
    }

    @Test
    fun templateChangesAreSeenAfterModification() {
        writeTemplate()
        withCards {
            assertTrue("--main-color" in client.get("/profile/team.svg?teamId=1").bodyAsText())
            val file = mediaDir.resolve("team.svg")
            file.writeText("""<svg id="v2">{team.json}</svg>""")
            Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis() + 10_000))
            assertTrue("""id="v2"""" in client.get("/profile/team.svg?teamId=1").bodyAsText())
        }
    }

    @Test
    fun oversizedForCacheTemplateIsServedButNotCached() {
        mediaDir.createDirectories()
        val file = mediaDir.resolve("big.svg")
        // Over the 1 MiB cache threshold but under the 10 MiB hard read limit: this must still
        // be served, just re-read from disk every time instead of pinned in the template cache
        // (team photos/videos live in the same directory and must never be cached indefinitely).
        val big = "x".repeat(2 * 1024 * 1024) + "<svg>{team.json}</svg>"
        file.writeText(big)
        withCards {
            val first = client.get("/profile/big.svg?teamId=1")
            assertEquals(HttpStatusCode.OK, first.status)
            assertTrue("x".repeat(100) in first.bodyAsText(), "first request must return the original content")

            val modifiedAt = Files.getLastModifiedTime(file)
            file.writeText("""<svg id="v2">{team.json}</svg>""")
            Files.setLastModifiedTime(file, modifiedAt)

            assertTrue(
                """id="v2"""" in client.get("/profile/big.svg?teamId=1").bodyAsText(),
                "oversized-for-cache template must be re-read from disk, not served from a stale cache entry",
            )
        }
    }

    @Test
    fun sameMtimeDifferentSizeReplacementBustsTheCache() {
        writeTemplate()
        withCards {
            val file = mediaDir.resolve("team.svg")
            assertTrue("--main-color" in client.get("/profile/team.svg?teamId=1").bodyAsText())

            val modifiedAt = Files.getLastModifiedTime(file)
            // Shorter content than the original template, same mtime: a timestamp-preserving
            // replacement (rsync -a, cp -p) looks like this from the filesystem's point of view.
            file.writeText("""<svg id="v2">{team.json}</svg>""")
            Files.setLastModifiedTime(file, modifiedAt)

            assertTrue(
                """id="v2"""" in client.get("/profile/team.svg?teamId=1").bodyAsText(),
                "a same-mtime replacement with a different size must not be served from the cache",
            )
        }
    }
}
