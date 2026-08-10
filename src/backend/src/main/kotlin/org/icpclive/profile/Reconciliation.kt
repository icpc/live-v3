package org.icpclive.profile

import kotlinx.serialization.json.*
import org.icpclive.cds.api.*

private val whitespace = Regex("\\s+")

// uppercase().lowercase() is a poor man's full case folding: it makes "Straße" and "STRASSE" equal,
// which plain lowercase() does not.
internal fun normalizeName(name: String): String =
    name.trim().split(whitespace).joinToString(" ").uppercase().lowercase()

internal data class Roster(val contestants: List<String>, val coach: String?) {
    val isEmpty: Boolean get() = contestants.isEmpty() && coach == null
}

internal fun extractRoster(team: TeamInfo, persons: List<PersonInfo>, personalMode: Boolean): Roster {
    if (personalMode) return Roster(listOf(team.displayName), null)
    val teamPersons = persons.filter { team.id in it.teamIds }
    return Roster(
        contestants = teamPersons.filter { it.role.equals("contestant", ignoreCase = true) }.map { it.name },
        coach = teamPersons.firstOrNull { it.role.equals("coach", ignoreCase = true) }?.name,
    )
}

private fun stub(name: String): JsonObject = buildJsonObject {
    put("name", name)
    put("altNames", JsonArray(emptyList()))
    put("achievements", JsonArray(emptyList()))
}

private fun JsonObject.primaryName(): String? = (this["name"] as? JsonPrimitive)?.contentOrNull

private fun JsonObject.altNames(): List<String> = (this["altNames"] as? JsonArray)
    ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
    .orEmpty()

private fun takeMatch(normalized: String, pool: MutableList<JsonObject>, names: (JsonObject) -> List<String>): JsonObject? {
    val index = pool.indexOfFirst { person -> names(person).any { normalizeName(it) == normalized } }
    if (index == -1) return null
    return pool.removeAt(index)
}

/**
 * Matches [rosterNames] against [pool], consuming every matched profile person.
 * Primary names are matched first for the whole roster, so a roster name can't be
 * stolen through somebody else's alias before its own primary-name match is tried.
 * Names that normalize to nothing are never matched.
 */
private fun matchRoster(rosterNames: List<String>, pool: MutableList<JsonObject>): List<JsonObject?> {
    val normalized = rosterNames.map { normalizeName(it) }
    val matches = MutableList<JsonObject?>(rosterNames.size) { null }
    for (pass in listOf<(JsonObject) -> List<String>>({ listOfNotNull(it.primaryName()) }, { it.altNames() })) {
        for (i in rosterNames.indices) {
            if (matches[i] != null || normalized[i].isEmpty()) continue
            matches[i] = takeMatch(normalized[i], pool, pass)
        }
    }
    return matches
}

private fun withName(person: JsonObject, name: String): JsonObject =
    JsonObject(person.toMutableMap().apply { put("name", JsonPrimitive(name)) })

internal fun reconcileProfile(
    profile: JsonObject?,
    roster: Roster,
    team: TeamInfo,
    organization: OrganizationInfo?,
): JsonObject {
    if (profile == null) return synthesizeProfile(roster, team, organization)
    if (roster.isEmpty) return profile
    val pool = (profile["contestants"] as? JsonArray)
        ?.filterIsInstance<JsonObject>().orEmpty().toMutableList()
    val contestants = matchRoster(roster.contestants, pool)
        .mapIndexed { i, match ->
            val name = roster.contestants[i]
            match?.let { withName(it, name) } ?: stub(name)
        }
        .sortedBy { (it["name"] as? JsonPrimitive)?.contentOrNull ?: "" }
    val coachPool = (profile["coach"] as? JsonObject)?.let { mutableListOf(it) } ?: mutableListOf()
    val coach = roster.coach?.let { name ->
        matchRoster(listOf(name), coachPool).single()?.let { withName(it, name) } ?: stub(name)
    }
    return JsonObject(profile.toMutableMap().apply {
        put("contestants", JsonArray(contestants))
        put("coach", coach ?: JsonNull)
    })
}

internal fun synthesizeProfile(roster: Roster, team: TeamInfo, organization: OrganizationInfo?): JsonObject =
    buildJsonObject {
        put("id", team.id.value)
        putJsonObject("university") {
            put("fullName", organization?.fullName ?: "")
            put("shortName", organization?.displayName ?: "")
            put("region", JsonNull)
            put("hashTag", team.hashTag?.let { JsonPrimitive(it) } ?: JsonNull)
            put("url", JsonNull)
            put("id", organization?.id?.value?.let { JsonPrimitive(it) } ?: JsonNull)
        }
        putJsonObject("team") {
            put("name", team.displayName)
            put("regionals", JsonNull)
        }
        put("coach", roster.coach?.let { stub(it) } ?: JsonNull)
        put("contestants", JsonArray(roster.contestants.sorted().map { stub(it) }))
    }
