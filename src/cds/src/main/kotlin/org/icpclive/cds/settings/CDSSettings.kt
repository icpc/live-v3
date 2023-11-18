@file:Suppress("UNUSED")
package org.icpclive.cds.settings


import io.github.xn32.json5k.Json5
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.*
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.adapters.toEmulationFlow
import org.icpclive.cds.common.ContestDataSource
import org.icpclive.cds.common.isHttpUrl
import org.icpclive.util.*
import java.nio.file.Path
import java.util.ServiceLoader
import kotlin.io.path.exists
import kotlin.reflect.KClass

@Serializable
public abstract class CDSSettings {
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
        fun serializersModule(credentialProvider: CredentialProvider, path: Path) = SerializersModule {
            postProcess<Credential, String>(
                onDeserialize = {
                    val prefix = "\$creds."
                    if (it.startsWith(prefix)) {
                        val name = it.substring(prefix.length)
                        Credential(it, credentialProvider[name] ?: throw IllegalStateException("Credential $name not found"))
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
                        require(fixedPath.exists()) { "File $fixedPath mentioned in settings doesn't exist"}
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
                for ((clazz, serializer) in loadSettingsSerializers()) {
                    @Suppress("UNCHECKED_CAST")
                    subclass(clazz as KClass<CDSSettings>, serializer as KSerializer<CDSSettings>)
                }
            }
        }
    }
}

public fun parseFileToCdsSettings(path: Path, credentialProvider: Map<String, String>): CDSSettings = parseFileToCdsSettings(path) { credentialProvider[it] }

public fun parseFileToCdsSettings(path: Path, credentialProvider: CredentialProvider) : CDSSettings {
    val file = path.toFile()
    return when {
        !file.exists() -> throw IllegalArgumentException("File ${file.absolutePath} does not exist")
        file.name.endsWith(".properties") -> throw IllegalStateException("Properties format is not supported anymore, use settings.json instead")
        file.name.endsWith(".json") -> {
            file.inputStream().use {
                Json { serializersModule = CDSSettings.serializersModule(credentialProvider, path) }.decodeFromStreamIgnoringComments(it)
            }
        }
        file.name.endsWith(".json5") -> {
            file.inputStream().use {
                Json5 { serializersModule = CDSSettings.serializersModule(credentialProvider, path) }.decodeFromString<CDSSettings>(String(it.readAllBytes()))
            }
        }
        else -> throw IllegalArgumentException("Unknown settings file extension: ${file.path}")
    }
}

private fun loadSettingsSerializers() : Map<KClass<*>, KSerializer<*>> {
    val providers = ServiceLoader.load(CDSSettingsProvider::class.java)
    return buildMap {
        for (provider in providers) {
            put(provider.clazz, provider.serializer)
        }
    }
}

public interface CDSSettingsProvider {
    public val clazz: KClass<*>
    public val serializer: KSerializer<*>
}