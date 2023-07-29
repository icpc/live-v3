@file:Suppress("UNUSED")
package org.icpclive.cds


import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap
import org.icpclive.api.ContestResultType
import org.icpclive.cds.adapters.EmulationAdapter
import org.icpclive.cds.cats.CATSDataSource
import org.icpclive.cds.clics.ClicsDataSource
import org.icpclive.cds.clics.FeedVersion
import org.icpclive.cds.codeforces.CFDataSource
import org.icpclive.cds.common.ContestDataSource
import org.icpclive.cds.common.RawContestDataSource
import org.icpclive.cds.ejudge.EjudgeDataSource
import org.icpclive.cds.krsu.KRSUDataSource
import org.icpclive.cds.noop.NoopDataSource
import org.icpclive.cds.pcms.PCMSDataSource
import org.icpclive.cds.testsys.TestSysDataSource
import org.icpclive.cds.yandex.YandexDataSource
import org.icpclive.util.HumanTimeSerializer
import org.icpclive.util.TimeZoneSerializer
import org.icpclive.util.getLogger
import java.nio.file.Path

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

    fun toFlow(creds: Map<String, String>) = toDataSource(creds).getFlow()

    internal fun toDataSource(creds: Map<String, String>) : ContestDataSource {
        val raw = toRawDataSource(creds)
        return when (val emulationSettings = emulation) {
            null -> raw
            else -> EmulationAdapter(emulationSettings.startTime, emulationSettings.speed, raw)
        }
    }
    internal abstract fun toRawDataSource(creds: Map<String, String>): RawContestDataSource

    companion object {
        private val json = Json { prettyPrint = true }
    }
}

@Serializable
@SerialName("noop")
class NoopSettings(override val emulation: EmulationSettings? = null) : CDSSettings() {
    override fun toRawDataSource(creds: Map<String, String>) = NoopDataSource()
}

@Serializable
@SerialName("testsys")
class TestSysSettings(
    val url: String,
    @Serializable(with = TimeZoneSerializer::class)
    val timeZone: TimeZone = TimeZone.of("Europe/Moscow"),
    override val emulation: EmulationSettings? = null,
) : CDSSettings() {
    override fun toRawDataSource(creds: Map<String, String>) = TestSysDataSource(this)
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
    override fun toRawDataSource(creds: Map<String, String>) = CATSDataSource(this, creds)
}

@Serializable
@SerialName("krsu")
class KRSUSettings(
    @SerialName("submissions_url")
    val submissionsUrl: String,
    @SerialName("contest_url")
    val contestUrl: String,
    @Serializable(with = TimeZoneSerializer::class)
    val timeZone: TimeZone = TimeZone.of("Asia/Bishkek"),
    override val emulation: EmulationSettings? = null,
) : CDSSettings() {
    override fun toRawDataSource(creds: Map<String, String>) = KRSUDataSource(this)
}

@Serializable
@SerialName("ejudge")
class EjudgeSettings(
    val url: String,
    val resultType: ContestResultType = ContestResultType.ICPC,
    override val emulation: EmulationSettings? = null
) : CDSSettings() {
    override fun toRawDataSource(creds: Map<String, String>) = EjudgeDataSource(this)
}


@Serializable
@SerialName("yandex")
class YandexSettings(
    @SerialName("api_key")
    val apiKey: Credential,
    @SerialName("login_regex")
    val loginRegex: String,
    @SerialName("contest_id")
    val contestId: Int,
    val resultType: ContestResultType = ContestResultType.ICPC,
    override val emulation: EmulationSettings? = null
) : CDSSettings() {
    override fun toRawDataSource(creds: Map<String, String>) = YandexDataSource(this, creds)
}

@Serializable
@SerialName("cf")
class CFSettings(
    @SerialName("contest_id")
    val contestId: Int,
    @SerialName("api_key")
    val apiKey: Credential,
    @SerialName("api_secret")
    val apiSecret: Credential,
    override val emulation: EmulationSettings? = null,
) : CDSSettings() {
    override fun toRawDataSource(creds: Map<String, String>) = CFDataSource(this, creds)
}

@Serializable
@SerialName("pcms")
class PCMSSettings(
    val url: String,
    val login: Credential? = null,
    val password: Credential? = null,
    @SerialName("problems_url")
    val problemsUrl: String? = null,
    val resultType: ContestResultType = ContestResultType.ICPC,
    override val emulation: EmulationSettings? = null
) : CDSSettings() {
    override fun toRawDataSource(creds: Map<String, String>) = PCMSDataSource(this, creds)
}

@Serializable
class ClicsLoaderSettings(
    val url: String,
    val login: Credential? = null,
    val password: Credential? = null,
    @SerialName("event_feed_name")
    val eventFeedName: String = "event-feed",
    @SerialName("feed_version")
    val feedVersion: FeedVersion = FeedVersion.`2022_07`
)

@SerialName("clics")
@Serializable
class ClicsSettings(
    private val url: String,
    private val login: Credential? = null,
    private val password: Credential? = null,
    @SerialName("event_feed_name")
    private val eventFeedName: String = "event-feed",
    @SerialName("feed_version")
    private val feedVersion: FeedVersion = FeedVersion.`2022_07`,
    @SerialName("additional_feed")
    val additionalFeed: ClicsLoaderSettings? = null,
    @SerialName("use_team_names")
    val useTeamNames: Boolean = true,
    @SerialName("media_base_url")
    val mediaBaseUrl: String = "",
    override val emulation: EmulationSettings? = null,
) : CDSSettings() {
    val mainFeed get() = ClicsLoaderSettings(url,login, password, eventFeedName, feedVersion)

    override fun toRawDataSource(creds: Map<String, String>) = ClicsDataSource(this, creds)
}

@OptIn(ExperimentalSerializationApi::class)
fun parseFileToCdsSettings(path: Path) : CDSSettings {
    val file = path.toFile()
    return if (!file.exists()) {
        throw java.lang.IllegalArgumentException("File ${file.absolutePath} does not exist")
    } else if (file.name.endsWith(".properties")) {
        val properties = java.util.Properties()
        file.inputStream().use { properties.load(it) }
        val legacyMap = mapOf(
            "standings.type" to "type",
            "standings.resultType" to "resultType",
            "yandex.token" to "api_key",
            "yandex.login_prefix" to "login_regex",
            "yandex.contest_id" to "contest_id",
            "cf.api.key" to "api_key",
            "cf.api.secret" to "api_secret",
            "problems.url" to "problems_url",
            "submissions-url" to "submissions_url",
            "contest-url" to "contest_url",
            "timezone" to "timeZone"
        )
        for ((k, v) in legacyMap) {
            properties.getProperty(k)?.let {
                getLogger(CDSSettings::class).info(
                    "Deprecated event.properties key $k is used. Use $v instead."
                )
                properties.setProperty(v, it)
                properties.remove(k)
            }
        }
        @Suppress("UNCHECKED_CAST")
        Properties.decodeFromStringMap<CDSSettings>(properties as Map<String, String>)
    } else if (file.name.endsWith(".json")) {
        file.inputStream().use { Json.decodeFromStream<CDSSettings>(it) }
    } else {
        throw IllegalArgumentException("Unknown settings file extension: ${file.path}")
    }
}