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
import org.icpclive.ksp.cds.SerializerProviders
import org.icpclive.cds.util.postProcess
import java.nio.file.Path
import java.util.*
import kotlin.reflect.KClass

private fun Flow<ContestUpdate>.processEmulation(emulationSettings: EmulationSettings?) = when {
    emulationSettings == null -> this
    else -> toEmulationFlow(emulationSettings)
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
    @Contextual public val advancedJsonPath: UrlOrLocalPath.Local? = null,
)

@SerializerProviders("org.icpclive.cds.settings.CDSSettingsProvider")
public interface CDSSettings {
    public val emulation: EmulationSettings?
        get() = null
    public val previousDays: List<PreviousDaySettings>
        get() = emptyList()

    public fun toDataSource(): ContestDataSource

    public companion object {
        private val json = Json { prettyPrint = true }
        internal fun serializersModule() = serializersModule({ it }, Path.of(""))
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
                val urlSerializer = UrlOrLocalPathSerializer(path)
                contextual(UrlOrLocalPath::class, urlSerializer)
                postProcess<UrlOrLocalPath.Url, UrlOrLocalPath>(
                    urlSerializer,
                    onSerialize = { it },
                    onDeserialize = { it as? UrlOrLocalPath.Url ?: throw SerializationException("Local path is not allowed") }
                )
                postProcess<UrlOrLocalPath.Local, UrlOrLocalPath>(
                    urlSerializer.raw,
                    onSerialize = { it },
                    onDeserialize = { it as? UrlOrLocalPath.Local ?: throw SerializationException("Url is not allowed") }
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
