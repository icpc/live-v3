import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.icpclive.cds.codeforces.api.data.CFParty
import org.icpclive.cds.codeforces.api.data.CFProblem
import org.icpclive.util.UnixSecondsSerializer

enum class CFHackVerdict {
    HACK_SUCCESSFUL, HACK_UNSUCCESSFUL, INVALID_INPUT, GENERATOR_INCOMPILABLE, GENERATOR_CRASHED, IGNORED, TESTING, OTHER
}

@Serializable
data class CFHack(
    val id:Int,
    @Serializable(UnixSecondsSerializer::class)
    val creationTimeSeconds: Instant,
    val hacker: CFParty,
    val defender: CFParty,
    val verdict: CFHackVerdict? = null,
    val problem: CFProblem,
)