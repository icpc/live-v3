@file:Suppress("UNUSED")
package org.icpclive.cds.settings


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
import org.icpclive.cds.codedrills.CodeDrillsDataSource
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
import javax.swing.text.StyledEditorKit.BoldAction

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

    internal companion object {
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
    val submissionsUrl: String,
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
    val apiKey: Credential,
    val loginRegex: String,
    val contestId: Int,
    val resultType: ContestResultType = ContestResultType.ICPC,
    override val emulation: EmulationSettings? = null
) : CDSSettings() {
    override fun toRawDataSource(creds: Map<String, String>) = YandexDataSource(this, creds)
}

@Serializable
@SerialName("cf")
class CFSettings(
    val contestId: Int,
    val apiKey: Credential,
    val apiSecret: Credential,
    val asManager: Boolean = true,
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
    val eventFeedName: String = "event-feed",
    val feedVersion: FeedVersion = FeedVersion.`2022_07`
)

@SerialName("clics")
@Serializable
class ClicsSettings(
    private val url: String,
    private val login: Credential? = null,
    private val password: Credential? = null,
    private val eventFeedName: String = "event-feed",
    private val feedVersion: FeedVersion = FeedVersion.`2022_07`,
    val additionalFeed: ClicsLoaderSettings? = null,
    val useTeamNames: Boolean = true,
    val mediaBaseUrl: String = "",
    override val emulation: EmulationSettings? = null,
) : CDSSettings() {
    val mainFeed get() = ClicsLoaderSettings(url,login, password, eventFeedName, feedVersion)

    override fun toRawDataSource(creds: Map<String, String>) = ClicsDataSource(this, creds)
}

@SerialName("codedrills")
@Serializable
class CodeDrillsSettings(
    val url: String,
    val port: Int,
    val contestId: String,
    val authKey: Credential,
    override val emulation: EmulationSettings? = null
) : CDSSettings() {
    override fun toRawDataSource(creds: Map<String, String>) = CodeDrillsDataSource(this, creds)
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
            "yandex.token" to "apiKey",
            "yandex.login_prefix" to "loginRegex",
            "yandex.contest_id" to "contestId",
            "cf.api.key" to "apiKey",
            "cf.api.secret" to "apiSecret",
            "problems.url" to "problemsUrl",
            "submissions-url" to "submissionsUrl",
            "contest_id" to "contestId",
            "contest-url" to "contestUrl",
            "timezone" to "timeZone",
            "event_feed_name" to "eventFeedName",
            "feed_version" to "feedVersion",
            "use_team_names" to "useTeamNames",
            "media_base_url" to "mediaBaseUrl",
            "additional_feed.url" to "additionalFeed.url",
            "additional_feed.login" to "additionalFeed.login",
            "additional_feed.password" to "additionalFeed.password",
            "additional_feed.event_feed_name" to "additionalFeed.eventFeedName",
            "additional_feed.feed_version" to "additionalFeed.feedVersion",
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
        properties.getProperty("type")?.let { properties.setProperty("type", it.lowercase()) }
        properties.getProperty("resultType")?.let { properties.setProperty("resultType", it.uppercase()) }
        @Suppress("UNCHECKED_CAST")
        Properties.decodeFromStringMap<CDSSettings>(properties as Map<String, String>)
    } else if (file.name.endsWith(".json")) {
        file.inputStream().use { Json.decodeFromStream<CDSSettings>(it) }
    } else {
        throw IllegalArgumentException("Unknown settings file extension: ${file.path}")
    }
}