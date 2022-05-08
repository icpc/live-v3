package org.icpclive.cds.wf2

import kotlinx.datetime.Instant
import kotlinx.serialization.json.*
import org.icpclive.cds.wf2.model.WF2ContestInfo
import org.icpclive.cds.wf2.model.WF2OrganisationInfo
import org.icpclive.cds.wf2.model.WF2ProblemInfo
import org.icpclive.cds.wf2.model.WF2TeamInfo
import java.awt.Color
import kotlin.time.Duration

class WF2Model {
    val problems = mutableMapOf<Int, WF2ProblemInfo>()
    private val organisations = mutableMapOf<String, WF2OrganisationInfo>()
    val teams = mutableMapOf<Int, WF2TeamInfo>()

    val contestInfo: WF2ContestInfo
        get() = WF2ContestInfo(
            problemsMap = problems,
            teams = teams.values.toList(),
            Duration.ZERO,
        )

    fun processProblem(o: JsonObject) {
        val ordinal = o["ordinal"]?.jsonPrimitive?.intOrNull ?: failHasNo("Problem", "ordinal number")

        problems[ordinal] = WF2ProblemInfo(
            id = ordinal,
            letter = o["label"]?.jsonPrimitive?.contentOrNull ?: "",
            name = o["name"]?.jsonPrimitive?.contentOrNull ?: failHasNo("Problem", "name"),
            color = o["rgb"]?.jsonPrimitive?.contentOrNull?.let { Color.decode(it) } ?: Color.GRAY,
            testCount = o["test_data_count"]?.jsonPrimitive?.intOrNull
        )
    }

    fun processOrganisation(o: JsonObject) {
        val id = o["id"]?.jsonPrimitive?.contentOrNull ?: failHasNo("Organisation", "id")
        val name = o["name"]?.jsonPrimitive?.contentOrNull ?: failHasNo("Organisation", "name")
        organisations[id] = WF2OrganisationInfo(
            id = id,
            name = name,
            formalName = o["formal_name"]?.jsonPrimitive?.contentOrNull ?: name,
            country = o["country"]?.jsonPrimitive?.contentOrNull,
            countryFlag = o["country_flag"]?.jsonPrimitive?.contentOrNull,
            logo = o["logo"]?.jsonArray?.lastOrNull()?.jsonObject?.get("href")?.jsonPrimitive?.content,
            hashtag = o["twitter_hashtag"]?.jsonPrimitive?.contentOrNull
        )
        // todo: update team if something changed
    }

    fun processTeam(o: JsonObject) {
        val id = o["id"]?.jsonPrimitive?.intOrNull ?: failHasNo("Team", "id")
        val teamOrganization = o["organization_id"]?.jsonPrimitive?.contentOrNull?.let { organisations[it] }
        val name = o["name"]?.jsonPrimitive?.contentOrNull ?: failHasNo("Team", "name")

        teams[id] = WF2TeamInfo(
            id = id,
            name = teamOrganization?.formalName ?: name,
            shortName = teamOrganization?.name ?: name,
            contestSystemId = id.toString(),
            groups = emptySet(),
            hashTag = teamOrganization?.hashtag,
            photo = o["photo"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.content,
            video = o["video"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.content,
            screens = o["desktop"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
            cameras = o["webcam"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
        )
    }

    private fun failHasNo(type: String, field: String): Nothing =
        throw IllegalArgumentException("$type object hasn't $field")
}
