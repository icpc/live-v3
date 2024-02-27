@file:Suppress("UNUSED")

package org.icpclive.cds.settings


import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.adapters.toEmulationFlow
import org.icpclive.cds.ContestDataSource
import org.icpclive.cds.adapters.addPreviousDays
import org.icpclive.cds.ksp.SerializerProviders
import org.icpclive.util.postProcess
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists
import kotlin.reflect.KClass

private fun Flow<ContestUpdate>.processEmulation(emulationSettings: EmulationSettings?) = when {
    emulationSettings == null -> this
    else -> toEmulationFlow(emulationSettings.startTime, emulationSettings.speed)
}

public fun CDSSettings.toFlow(): Flow<ContestUpdate> {
    return toDataSource()
        .getFlow()
        .addPreviousDays(previousDays)
        .processEmulation(emulation)
}

@Serializable
public class PreviousDaySettings(
    public val settings: CDSSettings,
    @Contextual public val advancedJsonPath: UrlOrLocalPath? = null,
)

@SerializerProviders("org.icpclive.cds.settings.CDSSettingsProvider")
public interface CDSSettings {
    public val emulation: EmulationSettings?
        get() = null
    public val network: NetworkSettings?
        get() = null
    public val previousDays: List<PreviousDaySettings>
        get() = emptyList()

    public fun toDataSource(): ContestDataSource

    public companion object {
        private val json = Json { prettyPrint = true }
        internal fun serializersModule() = serializersModule({ it }, Path.of(""))
        private fun isHttpUrl(text: String) = text.startsWith("http://") || text.startsWith("https://")
        public fun serializersModule(credentialProvider: CredentialProvider, path: Path): SerializersModule =
            SerializersModule {
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
                polymorphic(CDSSettings::class) {
                    for (provider in loadSettingsSerializers()) {
                        fun <T : CDSSettings> CDSSettingsProvider<T>.register() = subclass(clazz, serializer)
                        provider.register()
                    }
                }
            }
    }
}

private fun loadSettingsSerializers(): List<CDSSettingsProvider<*>> {
    val providers = ServiceLoader.load(CDSSettingsProvider::class.java)
    return providers.toList()
}

public interface CDSSettingsProvider<T : CDSSettings> {
    public val clazz: KClass<T>
    public val serializer: KSerializer<T>
}
