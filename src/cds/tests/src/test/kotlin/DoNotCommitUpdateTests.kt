import org.icpclive.cds.CdsLoadersTest
import kotlin.test.Test
import kotlin.test.assertFalse

object DoNotCommitUpdateTests {
    @Test
    fun `do not commit updateTest = true`() {
        assertFalse((object : CdsLoadersTest() {}).updateTestData)
    }
}