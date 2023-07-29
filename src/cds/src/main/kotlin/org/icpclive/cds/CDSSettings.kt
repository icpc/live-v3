@file:Suppress("UNUSED")
package org.icpclive.cds


import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import org.icpclive.api.ContestResultType
import org.icpclive.cds.cats.CATSDataSource
import org.icpclive.cds.clics.ClicsDataSource
import org.icpclive.cds.clics.FeedVersion
import org.icpclive.cds.codeforces.CFDataSource
import org.icpclive.cds.ejudge.EjudgeDataSource
import org.icpclive.cds.krsu.KRSUDataSource
import org.icpclive.cds.noop.NoopDataSource
import org.icpclive.cds.pcms.PCMSDataSource
import org.icpclive.cds.testsys.TestSysDataSource
import org.icpclive.cds.yandex.YandexDataSource
import org.icpclive.util.HumanTimeSerializer
import org.icpclive.util.TimeZoneSerializer

// I'd like to have them in cds files, but then serializing would be much harder

@JvmInline
@Serializable
value class Credential(private val raw: String) {
    fun get(creds: Map<String, String>) : String {
        val prefix = "\$creds."
        return if (raw.startsWith(prefix)) {
            val name = raw.substring(prefix.length)
            creds[name] ?: throw IllegalStateException("Credential $name not found")
        } else {
            raw
        }
    }
}

@Serializable
class EmulationSettings(
    val speed: Double,
    @Serializable(with = HumanTimeSerializer::class)
    val startTime: Instant
)

@Serializable
sealed class CDSSettings {
    abstract val emulation: EmulationSettings?
    override fun toString(): String {
        return json.encodeToString(this)
    }

    internal abstract fun toDataSource(creds: Map<String, String>): RawContestDataSource

    companion object {
        private val json = Json { prettyPrint = true }
    }
}

@Serializable
@SerialName("noop")
class NoopSettings(override val emulation: EmulationSettings? = null) : CDSSettings() {
    override fun toDataSource(creds: Map<String, String>) = NoopDataSource()
}

@Serializable
@SerialName("testsys")
class TestSysSettings(
    val url: String,
    @Serializable(with = TimeZoneSerializer::class)
    val timeZone: TimeZone = TimeZone.of("Europe/Moscow"),
    override val emulation: EmulationSettings? = null,
) : CDSSettings() {
    override fun toDataSource(creds: Map<String, String>) = TestSysDataSource(this)
}

@Serializable
@SerialName("cats")
class CatsSettings(
    val login: Credential,
    val password: Credential,
    val url: String,
    @Serializable(with = TimeZoneSerializer::class)
    val timeZone: TimeZone = TimeZone.of("Asia/Vladivostok"),
    val resultType: ContestResultType = ContestResultType.ICPC,
    val cid: String,
    override val emulation: EmulationSettings? = null,
) : CDSSettings() {
    override fun toDataSource(creds: Map<String, String>) = CATSDataSource(this, creds)
}

@Serializable
@SerialName("krsu")
class KRSUSettings(
    @SerialName("submissions-url")
    val submissionsUrl: String,
    @SerialName("contest-url")
    val contestUrl: String,
    @Serializable(with = TimeZoneSerializer::class)
    val timeZone: TimeZone = TimeZone.of("Asia/Bishkek"),
    override val emulation: EmulationSettings? = null,
) : CDSSettings() {
    override fun toDataSource(creds: Map<String, String>) = KRSUDataSource(this)
}

@Serializable
@SerialName("ejudge")
class EjudgeSettings(
    val url: String,
    val resultType: ContestResultType = ContestResultType.ICPC,
    override val emulation: EmulationSettings? = null
) : CDSSettings() {
    override fun toDataSource(creds: Map<String, String>) = EjudgeDataSource(this)
}


@Serializable
@SerialName("yandex")
class YandexSettings(
    @SerialName("yandex.token")
    val apiKey: Credential,
    @SerialName("yandex.login_prefix")
    val loginRegex: String,
    @SerialName("yandex.contest_id")
    val contestId: Int,
    val resultType: ContestResultType = ContestResultType.ICPC,
    override val emulation: EmulationSettings? = null
) : CDSSettings() {
    override fun toDataSource(creds: Map<String, String>) = YandexDataSource(this, creds)
}

@Serializable
@SerialName("cf")
class CFSettings(
    val contest_id: Int,
    @SerialName("cf.api.key")
    val apiKey: Credential,
    @SerialName("cf.api.secret")
    val apiSecret: Credential,
    override val emulation: EmulationSettings? = null,
) : CDSSettings() {
    override fun toDataSource(creds: Map<String, String>) = CFDataSource(this, creds)
}

@Serializable
@SerialName("pcms")
class PCMSSettings(
    val url: String,
    val login: Credential? = null,
    val password: Credential? = null,
    @SerialName("problems.url")
    val problemsUrl: String? = null,
    val resultType: ContestResultType = ContestResultType.ICPC,
    override val emulation: EmulationSettings? = null
) : CDSSettings() {
    override fun toDataSource(creds: Map<String, String>) = PCMSDataSource(this, creds)
}

@Serializable
class ClicsLoaderSettings(
    val url: String,
    val login: Credential? = null,
    val password: Credential? = null,
    val event_feed_name: String = "event-feed",
    val feed_version: FeedVersion = FeedVersion.`2022_07`
)

@SerialName("clics")
@Serializable
class ClicsSettings(
    private val url: String,
    private val login: Credential? = null,
    private val password: Credential? = null,
    private val event_feed_name: String = "event-feed",
    private val feed_version: FeedVersion = FeedVersion.`2022_07`,
    val additional_feed: ClicsLoaderSettings? = null,
    val use_team_names: Boolean = true,
    val media_base_url: String = "",
    override val emulation: EmulationSettings? = null,
) : CDSSettings() {
    val main_feed get() = ClicsLoaderSettings(url,login, password, event_feed_name, feed_version)

    override fun toDataSource(creds: Map<String, String>) = ClicsDataSource(this, creds)
}
