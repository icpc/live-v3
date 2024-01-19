@file:Suppress("UNUSED")
package org.icpclive.cds.settings


import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.serialization.*
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
import org.icpclive.cds.ejudge.EjudgeDataSource
import org.icpclive.cds.eolymp.EOlympDataSource
import org.icpclive.cds.krsu.KRSUDataSource
import org.icpclive.cds.nsu.NSUDataSource
import org.icpclive.cds.noop.NoopDataSource
import org.icpclive.cds.pcms.PCMSDataSource
import org.icpclive.cds.testsys.TestSysDataSource
import org.icpclive.cds.yandex.YandexDataSource
import org.icpclive.util.*
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.time.Duration

public sealed class UrlOrLocalPath {
    public abstract fun subDir(s: String): UrlOrLocalPath
    public data class Url(public val value: String) : UrlOrLocalPath() {
        public override fun subDir(s: String): UrlOrLocalPath = Url("$value/$s")
        override fun toString(): String = value
    }
    public data class Local(public val value: Path) : UrlOrLocalPath() {
        public override fun subDir(s: String): UrlOrLocalPath = Local(value.resolve(s))
        override fun toString(): String = value.toString()
    }
}

// I'd like to have them in cds files, but then serializing would be much harder

public class Credential(public val displayValue: String, public val value: String) {
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
        internal fun serializersModule() = serializersModule({ it }, Path.of(""))
        internal fun isHttpUrl(text: String) = text.startsWith("http://") || text.startsWith("https://")
        fun serializersModule(credentialProvider: CredentialProvider, path: Path) = SerializersModule {
            postProcess<Credential, String>(
                onDeserialize = {
                    val prefix = "\$creds."
                    if (it.startsWith(prefix)) {
                        val name = it.substring(prefix.length)
                        Credential(
                            it,
                            credentialProvider[name] ?: throw IllegalStateException("Credential $name not found")
                        )
                    } else {
                        Credential(it, it)
                    }
                },
                onSerialize = { it.displayValue }
            )
            postProcess<UrlOrLocalPath, String>(
                onDeserialize = {
                    if (isHttpUrl(it)) {
                        UrlOrLocalPath.Url(it)
                    } else {
                        val fixedPath = path.parent.resolve(it).toAbsolutePath()
                        require(fixedPath.exists()) { "File $fixedPath mentioned in settings doesn't exist" }
                        UrlOrLocalPath.Local(fixedPath)
                    }
                },
                onSerialize = {
                    when (it) {
                        is UrlOrLocalPath.Url -> it.value
                        is UrlOrLocalPath.Local -> it.value.toString()
                    }
                }
            )
        }
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
    @Contextual public val url: UrlOrLocalPath,
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
    @Contextual public val login: Credential,
    @Contextual public val password: Credential,
    public val url: String,
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
    public val submissionsUrl: String,
    public val contestUrl: String,
    @Serializable(with = TimeZoneSerializer::class)
    public val timeZone: TimeZone = TimeZone.of("Asia/Bishkek"),
    override val emulation: EmulationSettings? = null,
    override val network: NetworkSettings? = null
) : CDSSettings() {
    override fun toDataSource() = KRSUDataSource(this)
}

@Serializable
@SerialName("nsu")
public class NSUSettings(
    public val url: String,
    public val olympiadId: Int,
    public val tourId: Int,
    @Contextual public val email: Credential,
    @Contextual public val password: Credential,
    @Serializable(with = TimeZoneSerializer::class)
    public val timeZone: TimeZone = TimeZone.of("Asia/Novosibirsk"),
    override val emulation: EmulationSettings? = null,
    override val network: NetworkSettings? = null
) : CDSSettings() {
    override fun toDataSource() = NSUDataSource(this)
}

@Serializable
@SerialName("ejudge")
public class EjudgeSettings(
    @Contextual public val url: UrlOrLocalPath,
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
    @Contextual public val apiKey: Credential,
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
    @Contextual public val apiKey: Credential,
    @Contextual public val apiSecret: Credential,
    public val asManager: Boolean = true,
    override val emulation: EmulationSettings? = null,
    override val network: NetworkSettings? = null,
) : CDSSettings() {
    override fun toDataSource() = CFDataSource(this)
}

@Serializable
@SerialName("pcms")
public class PCMSSettings(
    @Contextual public val url: UrlOrLocalPath,
    @Contextual public val login: Credential? = null,
    @Contextual public val password: Credential? = null,
    @Contextual public val problemsUrl: UrlOrLocalPath? = null,
    public val resultType: ContestResultType = ContestResultType.ICPC,
    override val emulation: EmulationSettings? = null,
    override val network: NetworkSettings? = null,
) : CDSSettings() {
    override fun toDataSource() = PCMSDataSource(this)
}

@Serializable
public class ClicsFeed(
    @Contextual public val url: UrlOrLocalPath,
    public val contestId: String,
    @Contextual public val login: Credential? = null,
    @Contextual public val password: Credential? = null,
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
    @Contextual public val authKey: Credential,
    override val emulation: EmulationSettings? = null,
    override val network: NetworkSettings? = null,
) : CDSSettings() {
    override fun toDataSource() = CodeDrillsDataSource(this)
}

@SerialName("atcoder")
@Serializable
public class AtcoderSettings(
    public val contestId: String,
    @Contextual public val sessionCookie: Credential,
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
    @Contextual public val token: Credential,
    public val contestId: String,
    public val previousDaysContestIds: List<String> = emptyList(),
    override val network: NetworkSettings? = null,
    override val emulation: EmulationSettings? = null
) : CDSSettings() {
    override fun toDataSource() = EOlympDataSource(this)
}

