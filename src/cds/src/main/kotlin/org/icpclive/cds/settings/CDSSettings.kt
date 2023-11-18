@file:Suppress("UNUSED")
@file:UseContextualSerialization(UrlOrLocalPath::class, Credential::class)
package org.icpclive.cds.settings


import io.github.xn32.json5k.Json5
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.serialization.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
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
import org.icpclive.cds.common.isHttpUrl
import org.icpclive.cds.ejudge.EjudgeDataSource
import org.icpclive.cds.eolymp.EOlympDataSource
import org.icpclive.cds.krsu.KRSUDataSource
import org.icpclive.cds.noop.NoopDataSource
import org.icpclive.cds.pcms.PCMSDataSource
import org.icpclive.cds.testsys.TestSysDataSource
import org.icpclive.cds.yandex.YandexDataSource
import org.icpclive.util.*
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.time.Duration

@Serializable(with = UrlOrLocalPath.Serializer::class)
public class UrlOrLocalPath(public val value: String) {
    internal class Serializer : KSerializer<UrlOrLocalPath> {
        private val delegate = serializer<String>()
        override val descriptor = delegate.descriptor
        override fun deserialize(decoder: Decoder) = UrlOrLocalPath(delegate.deserialize(decoder))
        override fun serialize(encoder: Encoder, value: UrlOrLocalPath) = delegate.serialize(encoder, value.value)
    }

    public override fun toString(): String = value
}

// I'd like to have them in cds files, but then serializing would be much harder

@Serializable(with = Credential.Serializer::class)
public class Credential(public val displayValue: String, public val value: String) {
    internal class Serializer : KSerializer<Credential> {
        private val delegate = serializer<String>()
        override val descriptor = delegate.descriptor
        override fun deserialize(decoder: Decoder) : Credential {
            val raw = delegate.deserialize(decoder)
            return Credential(raw, raw)
        }
        override fun serialize(encoder: Encoder, value: Credential) = delegate.serialize(encoder, value.displayValue)
    }

    override fun toString(): String = displayValue
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

    public fun toFlow(): Flow<ContestUpdate> {
        val raw = toDataSource()
        return when (val emulationSettings = emulation) {
            null -> raw.getFlow()
            else -> raw.getFlow().toEmulationFlow(emulationSettings.startTime, emulationSettings.speed)
        }
    }
    internal abstract fun toDataSource(): ContestDataSource

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
    override fun toDataSource() = NoopDataSource()
}

@Serializable
@SerialName("testsys")
public class TestSysSettings(
    public val url: UrlOrLocalPath,
    @Serializable(with = TimeZoneSerializer::class)
    public val timeZone: TimeZone = TimeZone.of("Europe/Moscow"),
    override val emulation: EmulationSettings? = null,
    override val network: NetworkSettings? = null
) : CDSSettings() {
    override fun toDataSource() = TestSysDataSource(this)
}

@Serializable
@SerialName("cats")
public class CatsSettings(
    public val login: Credential,
    public val password: Credential,
    public val url: UrlOrLocalPath,
    @Serializable(with = TimeZoneSerializer::class)
    public val timeZone: TimeZone = TimeZone.of("Asia/Vladivostok"),
    public val resultType: ContestResultType = ContestResultType.ICPC,
    public val cid: String,
    override val emulation: EmulationSettings? = null,
    override val network: NetworkSettings? = null
) : CDSSettings() {
    override fun toDataSource() = CATSDataSource(this)
}

@Serializable
@SerialName("krsu")
public class KRSUSettings(
    public val submissionsUrl: UrlOrLocalPath,
    public val contestUrl: UrlOrLocalPath,
    @Serializable(with = TimeZoneSerializer::class)
    public val timeZone: TimeZone = TimeZone.of("Asia/Bishkek"),
    override val emulation: EmulationSettings? = null,
    override val network: NetworkSettings? = null
) : CDSSettings() {
    override fun toDataSource() = KRSUDataSource(this)
}

@Serializable
@SerialName("ejudge")
public class EjudgeSettings(
    public val url: UrlOrLocalPath,
    public val resultType: ContestResultType = ContestResultType.ICPC,
    public val timeZone: TimeZone = TimeZone.of("Europe/Moscow"),
    override val emulation: EmulationSettings? = null,
    override val network: NetworkSettings? = null
) : CDSSettings() {
    override fun toDataSource() = EjudgeDataSource(this)
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
    override fun toDataSource() = YandexDataSource(this)
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
    override fun toDataSource() = CFDataSource(this)
}

@Serializable
@SerialName("pcms")
public class PCMSSettings(
    public val url: UrlOrLocalPath,
    public val login: Credential? = null,
    public val password: Credential? = null,
    public val problemsUrl: UrlOrLocalPath? = null,
    public val resultType: ContestResultType = ContestResultType.ICPC,
    override val emulation: EmulationSettings? = null,
    override val network: NetworkSettings? = null,
) : CDSSettings() {
    override fun toDataSource() = PCMSDataSource(this)
}

@Serializable
public class ClicsFeed(
    public val url: UrlOrLocalPath,
    public val contestId: String,
    public val login: Credential? = null,
    public val password: Credential? = null,
    public val eventFeedName: String = "event-feed",
    public val eventFeedPath: String? = null,
    public val feedVersion: ClicsSettings.FeedVersion = ClicsSettings.FeedVersion.`2022_07`
)

@SerialName("clics")
@Serializable
public class ClicsSettings(
    public val feeds: List<ClicsFeed>,
    public val useTeamNames: Boolean = true,
    override val emulation: EmulationSettings? = null,
    override val network: NetworkSettings? = null,
) : CDSSettings() {
    public enum class FeedVersion {
        `2020_03`,
        `2022_07`
    }

    override fun toDataSource() = ClicsDataSource(this)
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
    override fun toDataSource() = CodeDrillsDataSource(this)
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
    override fun toDataSource() = AtcoderDataSource(this)
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
    override fun toDataSource() = CmsDataSource(this)
}

@SerialName("eolymp")
@Serializable
public class EOlympSettings(
    public val url: String,
    public val token: Credential,
    public val contestId: String,
    override val network: NetworkSettings? = null,
    override val emulation: EmulationSettings? = null
) : CDSSettings() {
    override fun toDataSource() = EOlympDataSource(this)
}

public fun interface CredsProvider {
    public operator fun get(s: String) : String?
}
public fun parseFileToCdsSettings(path: Path, creds: Map<String, String>): CDSSettings = parseFileToCdsSettings(path) { creds[it] }

public fun parseFileToCdsSettings(path: Path, creds: CredsProvider) : CDSSettings {
    val file = path.toFile()
    val module = SerializersModule {
        postProcess<Credential>(
            onEncode = {
                val prefix = "\$creds."
                if (it.displayValue.startsWith(prefix)) {
                    val name = it.displayValue.substring(prefix.length)
                    Credential(it.displayValue, creds[name] ?: throw IllegalStateException("Credential $name not found"))
                } else {
                    it
                }
            }
        )
        postProcess<UrlOrLocalPath>(
            onEncode = {
                if (isHttpUrl(it.value)) {
                    it
                } else {
                    val fixedPath = path.parent.resolve(it.value).toAbsolutePath()
                    require(fixedPath.exists()) { "File ${fixedPath} mentioned in settings doesn't exist"}
                    UrlOrLocalPath(fixedPath.toString())
                }
            }
        )
    }
    return if (!file.exists()) {
        throw IllegalArgumentException("File ${file.absolutePath} does not exist")
    } else if (file.name.endsWith(".properties")) {
        throw IllegalStateException("Properties format is not supported anymore, use settings.json instead")
    } else if (file.name.endsWith(".json")) {
        file.inputStream().use {
            Json {
                serializersModule = module
            }.decodeFromStreamIgnoringComments(it)
        }
    } else if (file.name.endsWith(".json5")) {
        file.inputStream().use {
            Json5 {
                serializersModule = module
            }.decodeFromString<CDSSettings>(String(it.readAllBytes()))
        }
    } else {
        throw IllegalArgumentException("Unknown settings file extension: ${file.path}")
    }
}

