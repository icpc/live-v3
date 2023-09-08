@file:Suppress("UNUSED")
package org.icpclive.cds.settings


import io.github.xn32.json5k.Json5
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.decodeFromStringMap
import org.icpclive.api.ContestResultType
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.adapters.toEmulationFlow
import org.icpclive.cds.atcoder.AtcoderDataSource
import org.icpclive.cds.cats.CATSDataSource
import org.icpclive.cds.clics.ClicsDataSource
import org.icpclive.cds.cms.CmsDataSource
import org.icpclive.cds.codedrills.CodeDrillsDataSource
import org.icpclive.cds.codeforces.CFDataSource
import org.icpclive.cds.common.ContestDataSource
import org.icpclive.cds.ejudge.EjudgeDataSource
import org.icpclive.cds.krsu.KRSUDataSource
import org.icpclive.cds.noop.NoopDataSource
import org.icpclive.cds.pcms.PCMSDataSource
import org.icpclive.cds.testsys.TestSysDataSource
import org.icpclive.cds.yandex.YandexDataSource
import org.icpclive.util.*
import java.nio.file.Path
import kotlin.time.Duration

// I'd like to have them in cds files, but then serializing would be much harder

@JvmInline
@Serializable
public value class Credential(private val raw: String) {
    public fun get(creds: Map<String, String>) : String {
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
public class EmulationSettings(
    public val speed: Double,
    @Serializable(with = HumanTimeSerializer::class)
    public val startTime: Instant
)

@Serializable
public class NetworkSettings(
    public val allowUnsecureConnections: Boolean = true
)

@Serializable
public sealed class CDSSettings {
    public abstract val emulation: EmulationSettings?
    public abstract val network: NetworkSettings?

    override fun toString(): String {
        return json.encodeToString(this)
    }

    public fun toFlow(creds: Map<String, String>) : Flow<ContestUpdate> {
        val raw = toDataSource(creds)
        return when (val emulationSettings = emulation) {
            null -> raw.getFlow()
            else -> raw.getFlow().toEmulationFlow(emulationSettings.startTime, emulationSettings.speed)
        }
    }
    internal abstract fun toDataSource(creds: Map<String, String>): ContestDataSource

    internal companion object {
        private val json = Json { prettyPrint = true }
    }
}

@Serializable
@SerialName("noop")
public class NoopSettings(
    override val emulation: EmulationSettings? = null,
    override val network: NetworkSettings? = null
) : CDSSettings() {
    override fun toDataSource(creds: Map<String, String>) = NoopDataSource()
}

@Serializable
@SerialName("testsys")
public class TestSysSettings(
    public val url: String,
    @Serializable(with = TimeZoneSerializer::class)
    public val timeZone: TimeZone = TimeZone.of("Europe/Moscow"),
    override val emulation: EmulationSettings? = null,
    override val network: NetworkSettings? = null
) : CDSSettings() {
    override fun toDataSource(creds: Map<String, String>) = TestSysDataSource(this)
}

@Serializable
@SerialName("cats")
public class CatsSettings(
    public val login: Credential,
    public val password: Credential,
    public val url: String,
    @Serializable(with = TimeZoneSerializer::class)
    public val timeZone: TimeZone = TimeZone.of("Asia/Vladivostok"),
    public val resultType: ContestResultType = ContestResultType.ICPC,
    public val cid: String,
    override val emulation: EmulationSettings? = null,
    override val network: NetworkSettings? = null
) : CDSSettings() {
    override fun toDataSource(creds: Map<String, String>) = CATSDataSource(this, creds)
}

@Serializable
@SerialName("krsu")
public class KRSUSettings(
    public val submissionsUrl: String,
    public val contestUrl: String,
    @Serializable(with = TimeZoneSerializer::class)
    public val timeZone: TimeZone = TimeZone.of("Asia/Bishkek"),
    override val emulation: EmulationSettings? = null,
    override val network: NetworkSettings? = null
) : CDSSettings() {
    override fun toDataSource(creds: Map<String, String>) = KRSUDataSource(this)
}

@Serializable
@SerialName("ejudge")
public class EjudgeSettings(
    public val url: String,
    public val resultType: ContestResultType = ContestResultType.ICPC,
    public val timeZone: TimeZone = TimeZone.of("Europe/Moscow"),
    override val emulation: EmulationSettings? = null,
    override val network: NetworkSettings? = null
) : CDSSettings() {
    override fun toDataSource(creds: Map<String, String>) = EjudgeDataSource(this)
}


@Serializable
@SerialName("yandex")
public class YandexSettings(
    public val apiKey: Credential,
    @Serializable(with = RegexSerializer::class) public val loginRegex: Regex,
    public val contestId: Int,
    public val resultType: ContestResultType = ContestResultType.ICPC,
    override val emulation: EmulationSettings? = null,
    override val network: NetworkSettings? = null
) : CDSSettings() {
    override fun toDataSource(creds: Map<String, String>) = YandexDataSource(this, creds)
}

@Serializable
@SerialName("cf")
public class CFSettings(
    public val contestId: Int,
    public val apiKey: Credential,
    public val apiSecret: Credential,
    public val asManager: Boolean = true,
    override val emulation: EmulationSettings? = null,
    override val network: NetworkSettings? = null,
) : CDSSettings() {
    override fun toDataSource(creds: Map<String, String>) = CFDataSource(this, creds)
}

@Serializable
@SerialName("pcms")
public class PCMSSettings(
    public val url: String,
    public val login: Credential? = null,
    public val password: Credential? = null,
    public val problemsUrl: String? = null,
    public val resultType: ContestResultType = ContestResultType.ICPC,
    override val emulation: EmulationSettings? = null,
    override val network: NetworkSettings? = null,
) : CDSSettings() {
    override fun toDataSource(creds: Map<String, String>) = PCMSDataSource(this, creds)
}

@Serializable
public class ClicsFeed(
    public val url: String,
    public val login: Credential? = null,
    public val password: Credential? = null,
    public val eventFeedName: String = "event-feed",
    public val feedVersion: ClicsSettings.FeedVersion = ClicsSettings.FeedVersion.`2022_07`
)

@SerialName("clics")
@Serializable
public class ClicsSettings(
    public val feeds: List<ClicsFeed>,
    public val useTeamNames: Boolean = true,
    public val mediaBaseUrl: String = "",
    override val emulation: EmulationSettings? = null,
    override val network: NetworkSettings? = null,
) : CDSSettings() {
    public enum class FeedVersion {
        `2020_03`,
        `2022_07`
    }

    override fun toDataSource(creds: Map<String, String>) = ClicsDataSource(this, creds)
}

@SerialName("codedrills")
@Serializable
public class CodeDrillsSettings(
    public val url: String,
    public val port: Int,
    public val contestId: String,
    public val authKey: Credential,
    override val emulation: EmulationSettings? = null,
    override val network: NetworkSettings? = null,
) : CDSSettings() {
    override fun toDataSource(creds: Map<String, String>) = CodeDrillsDataSource(this, creds)
}

@SerialName("atcoder")
@Serializable
public class AtcoderSettings(
    public val contestId: String,
    public val sessionCookie: Credential,
    @Serializable(with = HumanTimeSerializer::class)
    public val startTime: Instant,
    @Serializable(with = DurationInSecondsSerializer::class)
    @SerialName("contestLengthSeconds") public val contestLength: Duration,
    override val emulation: EmulationSettings? = null,
    override val network: NetworkSettings? = null
) : CDSSettings() {
    override fun toDataSource(creds: Map<String, String>) = AtcoderDataSource(this, creds)
}

@SerialName("cms")
@Serializable
public class CmsSettings(
    public val url: String,
    public val activeContest: String,
    public val otherContests: List<String>,
    override val network: NetworkSettings? = null,
    override val emulation: EmulationSettings? = null
) : CDSSettings() {
    override fun toDataSource(creds: Map<String, String>) = CmsDataSource(this)
}


@OptIn(ExperimentalSerializationApi::class)
public fun parseFileToCdsSettings(path: Path) : CDSSettings {
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
        file.inputStream().use {
            Json.decodeFromStreamIgnoringComments(it)
        }
    } else if (file.name.endsWith(".json5")) {
        file.inputStream().use {
            Json5.decodeFromString<CDSSettings>(String(it.readAllBytes()))
        }
    } else {
        throw IllegalArgumentException("Unknown settings file extension: ${file.path}")
    }
}

