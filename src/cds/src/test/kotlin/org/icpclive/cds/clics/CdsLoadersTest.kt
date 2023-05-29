import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.approvaltests.Approvals
import org.approvaltests.core.Options
import org.icpclive.cds.getRawLoader
import org.junit.*
import java.util.Properties

class CdsLoadersTest {
    @Test
    fun pcmsTest() {
        loaderTest(
            mapOf(
                "standings.type" to "PCMS",
                "url" to "testData/loaders/pcms.xml"
            )
        )
    }

    @Test
    fun pcmsIOITest() {
        loaderTest(
            mapOf(
                "standings.type" to "PCMS",
                "standings.resultType" to "IOI",
                "url" to "testData/loaders/pcms-ioi.xml"
            )
        )
    }


    @Test
    fun clics202003Test() {
        loaderTest(
            mapOf(
                "standings.type" to "CLICS",
                "feed_version" to "2020_03",
                "url" to "testData/loaders/clics-2020-03"
            )
        )
    }

    @Test
    fun clics202207Test() {
        loaderTest(
            mapOf(
                "standings.type" to "CLICS",
                "feed_version" to "2022_07",
                "url" to "testData/loaders/clics-2022-07"
            )
        )
    }


    private val json = Json {
        prettyPrint = true
    }

    private fun loaderTest(args: Map<String, String>) {
        val properties = Properties()
        for ((k, v) in args) {
            properties.setProperty(k, v)
        }
        val loader = getRawLoader(properties, emptyMap())
        val result = runBlocking { loader.loadOnce() }
        val options = Options()
        Approvals.verify(json.encodeToString(result), options)
    }
}