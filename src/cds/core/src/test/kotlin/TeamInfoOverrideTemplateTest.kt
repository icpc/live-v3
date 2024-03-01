import org.icpclive.cds.adapters.instantiateTemplate
import org.icpclive.cds.api.MediaType
import org.icpclive.cds.api.TeamMediaType
import org.icpclive.cds.tunning.TeamOverrideTemplate
import kotlin.test.*

object TeamInfoOverrideTemplateTest {
    private fun TeamOverrideTemplate.instantiate(map: Map<String, String>) = instantiateTemplate {
        map[it]
    }

    @Test
    fun `check url encodes`() {
        val template = TeamOverrideTemplate(
            displayName = "{teamName}",
            medias = mapOf(
                TeamMediaType.CAMERA to MediaType.Photo("http://photos-server/{teamName}"),
                TeamMediaType.REACTION_VIDEO to MediaType.WebRTCGrabberConnection(
                    url = "{!grabberUrl}",
                    peerName = "{teamName}",
                    streamType = "",
                    credential = null
                )
            )
        )
        val teamName = "Team name with spaces & other / strange : symbols?"
        val teamReplaced = "Team%20name%20with%20spaces%20%26%20other%20%2F%20strange%20%3A%20symbols%3F"
        val url = "http://this-should-not-be-replaced:12345/url"
        val instantiated = template.instantiate(mapOf(
            "teamName" to teamName,
            "grabberUrl" to url
        ))
        assertEquals(teamName, instantiated.displayName)
        assertEquals("http://photos-server/${teamReplaced}", (instantiated.medias?.get(TeamMediaType.CAMERA) as MediaType.Photo).url)
        assertEquals(url, (instantiated.medias?.get(TeamMediaType.REACTION_VIDEO) as MediaType.WebRTCGrabberConnection).url)
        assertEquals(teamName, (instantiated.medias?.get(TeamMediaType.REACTION_VIDEO) as MediaType.WebRTCGrabberConnection).peerName)
    }
}