package org.icpclive.profile

import kotlinx.serialization.json.*
import org.icpclive.cds.api.*
import kotlin.test.*

class ReconciliationTest {
    private fun team(
        id: String = "1",
        displayName: String = "Team A",
        fullName: String = "Uni: Team A",
        hashTag: String? = null,
        org: String? = null,
    ) = TeamInfo(
        id = id.toTeamId(),
        fullName = fullName,
        displayName = displayName,
        groups = emptyList(),
        hashTag = hashTag,
        medias = emptyMap(),
        isHidden = false,
        isOutOfContest = false,
        organizationId = org?.toOrganizationId(),
    )

    private fun org(id: String = "u1", fullName: String = "Test University", displayName: String = "TU") =
        OrganizationInfo(id.toOrganizationId(), displayName, fullName, emptyList())

    private fun person(name: String, role: String, vararg teams: String) =
        PersonInfo(id = name.toPersonId(), name = name, role = role, teamIds = teams.map { it.toTeamId() })

    private fun profilePerson(name: String, altNames: List<String> = emptyList()) = buildJsonObject {
        put("name", name)
        put("altNames", JsonArray(altNames.map { JsonPrimitive(it) }))
        put("cfRating", 2500)
        put("achievements", buildJsonArray {
            addJsonObject { put("achievement", "Some Contest 1st (2020)"); put("priority", 95) }
        })
    }

    private fun profile(contestants: List<JsonObject>, coach: JsonObject? = null) = buildJsonObject {
        put("id", "1")
        putJsonObject("university") { put("fullName", "Uni") }
        putJsonObject("team") { put("name", "Team A") }
        put("coach", coach ?: JsonNull)
        put("contestants", JsonArray(contestants))
    }

    private fun names(record: JsonObject) =
        (record["contestants"] as JsonArray).map { (it.jsonObject["name"] as JsonPrimitive).content }

    @Test
    fun normalizeCollapsesCaseAndWhitespace() {
        assertEquals("alice smith", normalizeName("  Alice   SMITH "))
    }

    @Test
    fun matchedPersonKeepsDataAndTakesContestSpelling() {
        val result = reconcileProfile(
            profile(listOf(profilePerson("Alice Smith"))),
            Roster(listOf("ALICE  SMITH"), null), team(), null,
        )
        val contestant = (result["contestants"] as JsonArray)[0].jsonObject
        assertEquals("ALICE  SMITH", (contestant["name"] as JsonPrimitive).content)
        assertEquals(2500, (contestant["cfRating"] as JsonPrimitive).int)
    }

    @Test
    fun matchesByAltName() {
        val result = reconcileProfile(
            profile(listOf(profilePerson("A. Smith", altNames = listOf("Alice Smith")))),
            Roster(listOf("Alice Smith"), null), team(), null,
        )
        val contestant = (result["contestants"] as JsonArray)[0].jsonObject
        assertEquals("Alice Smith", (contestant["name"] as JsonPrimitive).content)
        assertEquals(2500, (contestant["cfRating"] as JsonPrimitive).int)
    }

    @Test
    fun unmatchedRosterNameBecomesStub() {
        val result = reconcileProfile(
            profile(listOf(profilePerson("Alice Smith"))),
            Roster(listOf("Bob Jones"), null), team(), null,
        )
        val contestant = (result["contestants"] as JsonArray)[0].jsonObject
        assertEquals("Bob Jones", (contestant["name"] as JsonPrimitive).content)
        assertEquals(0, (contestant["achievements"] as JsonArray).size)
        assertNull(contestant["cfRating"])
    }

    @Test
    fun unmatchedProfilePersonsAreDropped() {
        val result = reconcileProfile(
            profile(listOf(profilePerson("Alice Smith"), profilePerson("Carol White"), profilePerson("Dan Black"))),
            Roster(listOf("Alice Smith", "Dan Black"), null), team(), null,
        )
        assertEquals(listOf("Alice Smith", "Dan Black"), names(result))
    }

    @Test
    fun contestantsAreSortedByName() {
        val result = reconcileProfile(
            profile(listOf(profilePerson("Zed Zulu"), profilePerson("Ann Alpha"))),
            Roster(listOf("Zed Zulu", "Ann Alpha"), null), team(), null,
        )
        assertEquals(listOf("Ann Alpha", "Zed Zulu"), names(result))
    }

    @Test
    fun duplicateRosterNamesConsumeMatchesOnce() {
        val result = reconcileProfile(
            profile(listOf(profilePerson("Alice Smith"))),
            Roster(listOf("Alice Smith", "Alice Smith"), null), team(), null,
        )
        val ratings = (result["contestants"] as JsonArray).map { it.jsonObject["cfRating"] }
        assertEquals(1, ratings.count { it != null && it !is JsonNull })
    }

    @Test
    fun emptyRosterReturnsProfileUnchanged() {
        val p = profile(listOf(profilePerson("Alice Smith")), coach = profilePerson("Coach Zh"))
        assertSame(p, reconcileProfile(p, Roster(emptyList(), null), team(), null))
    }

    @Test
    fun coachMatchStubAndDrop() {
        val withCoach = profile(listOf(profilePerson("Alice Smith")), coach = profilePerson("Coach Zh"))
        // match + contest spelling
        val matched = reconcileProfile(withCoach, Roster(listOf("Alice Smith"), "coach ZH"), team(), null)
        assertEquals("coach ZH", ((matched["coach"] as JsonObject)["name"] as JsonPrimitive).content)
        assertEquals(2500, ((matched["coach"] as JsonObject)["cfRating"] as JsonPrimitive).int)
        // roster coach unknown to profile -> stub
        val stubbed = reconcileProfile(withCoach, Roster(listOf("Alice Smith"), "New Coach"), team(), null)
        assertEquals(0, ((stubbed["coach"] as JsonObject)["achievements"] as JsonArray).size)
        // roster has no coach -> profile coach dropped
        val dropped = reconcileProfile(withCoach, Roster(listOf("Alice Smith"), null), team(), null)
        assertIs<JsonNull>(dropped["coach"])
    }

    @Test
    fun extractRosterFiltersByTeamAndRole() {
        val persons = listOf(
            person("Alice Smith", "contestant", "1"),
            person("Bob Jones", "contestant", "1"),
            person("Carol White", "contestant", "2"),
            person("Coach Zh", "coach", "1"),
            person("Reserve Guy", "reserve", "1"),
        )
        val roster = extractRoster(team("1"), persons, personalMode = false)
        assertEquals(listOf("Alice Smith", "Bob Jones"), roster.contestants)
        assertEquals("Coach Zh", roster.coach)
    }

    @Test
    fun personalModeUsesDisplayName() {
        val roster = extractRoster(team(displayName = "Solo Person"), emptyList(), personalMode = true)
        assertEquals(Roster(listOf("Solo Person"), null), roster)
        assertFalse(roster.isEmpty)
    }

    @Test
    fun missingProfileSynthesizedFromContestData() {
        val result = reconcileProfile(
            null,
            Roster(listOf("Zed Zulu", "Ann Alpha"), "Coach Zh"),
            team(hashTag = "#TU", org = "u1"), org(),
        )
        val university = result["university"] as JsonObject
        assertEquals("Test University", (university["fullName"] as JsonPrimitive).content)
        assertEquals("#TU", (university["hashTag"] as JsonPrimitive).content)
        assertEquals("Team A", ((result["team"] as JsonObject)["name"] as JsonPrimitive).content)
        assertEquals(listOf("Ann Alpha", "Zed Zulu"), names(result))
        assertEquals("Coach Zh", ((result["coach"] as JsonObject)["name"] as JsonPrimitive).content)
    }

    @Test
    fun missingProfileWithoutOrgOrPersons() {
        val result = reconcileProfile(null, Roster(emptyList(), null), team(), null)
        assertEquals("", ((result["university"] as JsonObject)["fullName"] as JsonPrimitive).content)
        assertEquals(0, (result["contestants"] as JsonArray).size)
        assertIs<JsonNull>(result["coach"])
    }
}
