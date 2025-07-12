import org.icpclive.cds.api.*
import org.icpclive.cds.tunning.OverrideTeamTemplate
import kotlin.test.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

object TeamInfoOverrideTemplateTest {

    @Test
    fun `check url encodes`() {
        val teamName = "Team name with spaces & other / strange : symbols?"
        val teamReplaced = "Team%20name%20with%20spaces%20%26%20other%20%2F%20strange%20%3A%20symbols%3F"
        val url = "http://this-should-not-be-replaced:12345/url"

        val info = ContestInfo(
            teamList = listOf(
                TeamInfo(
                    id = "a".toTeamId(),
                    fullName = "WRONG",
                    displayName = "WRONG",
                    groups = emptyList(),
                    medias = emptyMap(),
                    isHidden = false,

                    customFields = mapOf(
                        "teamName" to teamName,
                        "grabberUrl" to url
                    ),
                    hashTag = null,
                    isOutOfContest = false,
                    organizationId = null,
                    color = null
                )
            ),
            name = "",
            resultType = ContestResultType.ICPC,
            startTime = Instant.fromEpochMilliseconds(123456),
            contestLength = 18000.seconds,
            freezeTime = 14400.seconds,
            problemList = emptyList(),
            groupList = emptyList(),
            organizationList = emptyList(),
            languagesList = emptyList(),
            penaltyRoundingMode = PenaltyRoundingMode.EACH_SUBMISSION_DOWN_TO_MINUTE,
            penaltyPerWrongAttempt = 20.minutes,
            cdsSupportsFinalization = false,
        )

        val newInfo = OverrideTeamTemplate(
            displayName = "{teamName}",
            medias = mapOf(
                TeamMediaType.CAMERA to MediaType.Image("http://photos-server/{teamName}"),
                TeamMediaType.REACTION_VIDEO to MediaType.WebRTCGrabberConnection(
                    url = "{!grabberUrl}",
                    peerName = "{teamName}",
                    streamType = "",
                    credential = null
                )
            )
        ).process(info)
        val team = newInfo.teams["a".toTeamId()]
        assertNotNull(team)
        assertEquals(teamName, team.displayName)
        assertEquals("http://photos-server/${teamReplaced}", (team.medias[TeamMediaType.CAMERA] as MediaType.Image).url)
        assertEquals(url, (team.medias[TeamMediaType.REACTION_VIDEO] as MediaType.WebRTCGrabberConnection).url)
        assertEquals(teamName, (team.medias[TeamMediaType.REACTION_VIDEO] as MediaType.WebRTCGrabberConnection).peerName)
    }
}
