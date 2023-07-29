import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.approvaltests.Approvals
import org.approvaltests.core.Options
import org.icpclive.api.ContestResultType
import org.icpclive.cds.*
import org.icpclive.cds.clics.FeedVersion
import org.junit.*

class CdsLoadersTest {
    @Test
    fun pcmsTest() {
        loaderTest(
            PCMSSettings(
                url = "testData/loaders/pcms.xml"
            )
        )
    }

    @Test
    fun pcmsIOITest() {
        loaderTest(
            PCMSSettings(
                resultType = ContestResultType.IOI,
                url = "testData/loaders/pcms-ioi.xml"
            )
        )
    }


    @Test
    fun clics202003Test() {
        loaderTest(
            ClicsSettings(
                url = "testData/loaders/clics-2020-03",
                feed_version = FeedVersion.`2020_03`
            )
        )
    }

    @Test
    fun clics202207Test() {
        loaderTest(
            ClicsSettings(
                url = "testData/loaders/clics-2022-07",
                feed_version = FeedVersion.`2022_07`
            )
        )
    }


    private val json = Json {
        prettyPrint = true
    }

    private fun loaderTest(args: CDSSettings) {
        val loader = args.toDataSource(emptyMap())
        val result = runBlocking { loader.loadOnce() }
        val options = Options()
        Approvals.verify(json.encodeToString(result), options)
    }
}