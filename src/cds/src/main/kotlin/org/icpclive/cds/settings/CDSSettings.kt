@file:Suppress("UNUSED")

package org.icpclive.cds.settings


import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.adapters.toEmulationFlow
import org.icpclive.cds.common.ContestDataSource
import org.icpclive.util.postProcess
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists
import kotlin.reflect.KClass

public fun CDSSettings.toFlow(): Flow<ContestUpdate> {
    val raw = toDataSource()
    return when (val emulationSettings = emulation) {
        null -> raw.getFlow()
        else -> raw.getFlow().toEmulationFlow(emulationSettings.startTime, emulationSettings.speed)
    }
}

public interface CDSSettings {
    public val emulation: EmulationSettings?
    public val network: NetworkSettings?

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
