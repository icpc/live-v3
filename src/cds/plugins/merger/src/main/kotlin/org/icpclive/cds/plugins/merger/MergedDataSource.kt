package org.icpclive.cds.plugins.merger

import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import org.icpclive.cds.*
import org.icpclive.cds.api.*
import org.icpclive.cds.settings.CDSSettings
import org.icpclive.cds.settings.toFlow
import org.icpclive.cds.tunning.RegexSet
import org.icpclive.ksp.cds.Builder


@Serializable
public class SubFeed(
    public val settings: CDSSettings,
    public val teamIdRegex: RegexSet? = null,
    public val problemIdRegex: RegexSet? = null,
    public val groupIdRegex: RegexSet? = null,
    public val organizationIdRegex: RegexSet? = null,
    public val runIdRegex: RegexSet? = null,
    public val languageIdRegex: RegexSet? = null,
)

@Builder("merged")
public sealed interface MergerSettings : CDSSettings {
    public val sources: List<SubFeed>
    override fun toDataSource(): ContestDataSource = MergedDataSource(this)
}

internal class MergedDataSource(private val settings: MergerSettings) : ContestDataSource {

    class IdEncoder(val encoder: Encoder, val regex: RegexSet?) : AbstractEncoder() {
        override val serializersModule: SerializersModule = SerializersModule { }
        override fun encodeValue(value: Any) {
            require(value is String)
            encoder.encodeString(regex?.applyTo(value) ?: value)
        }
    }

    class ToListEncoder(val storage: MutableList<Any?>, val feed: SubFeed) : AbstractEncoder() {
        override val serializersModule: SerializersModule = SerializersModule { }
        override fun encodeValue(value: Any) {
            storage.add(value)
        }
        override fun encodeNull() {
            storage.add(null)
        }

        override fun encodeInline(descriptor: SerialDescriptor): Encoder {
            return when (descriptor.serialName) {
                "org.icpclive.cds.api.TeamId" -> IdEncoder(this, feed.teamIdRegex)
                "org.icpclive.cds.api.ProblemId" -> IdEncoder(this, feed.problemIdRegex)
                "org.icpclive.cds.api.GroupId" -> IdEncoder(this, feed.groupIdRegex)
                "org.icpclive.cds.api.OrganizationId" -> IdEncoder(this, feed.organizationIdRegex)
                "org.icpclive.cds.api.RunId" -> IdEncoder(this, feed.runIdRegex)
                "org.icpclive.cds.api.LanguageId" -> IdEncoder(this, feed.languageIdRegex)
                else -> this
            }
        }

        override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
            storage.add(collectionSize)
            return this
        }
    }

    class FromListDecoder(private val storage: MutableList<Any?>) : AbstractDecoder() {
        private var ptr: Int = 0
        override val serializersModule: SerializersModule = SerializersModule { }
        override fun decodeElementIndex(descriptor: SerialDescriptor) = TODO("shouldn't be called")
        override fun decodeSequentially(): Boolean = true
        override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
            return decodeValue() as Int
        }

        override fun decodeNotNullMark() = storage[ptr] != null
        override fun decodeNull(): Nothing? = null.also {
            require(storage[ptr] == null)
            ptr++
        }
        override fun decodeValue(): Any {
            return storage[ptr++]!!
        }
    }

    inline fun <reified T> T.remap(feed: SubFeed) : T{
        val tempStorage = mutableListOf<Any?>()
        val s = serializer<T>()
        s.serialize(ToListEncoder(tempStorage, feed), this)
        val result = s.deserialize(FromListDecoder(tempStorage))
        return result
    }


    private fun <T, ID> merge(infos: List<List<T>>, id: T.() -> ID) : List<T>{
        val set = mutableSetOf<ID>()
        return buildList {
            for (list in infos) {
                for (info in list) {
                    if (!set.add(id(info))) continue
                    add(info)
                }
            }
        }
    }

    @OptIn(InefficientContestInfoApi::class)
    fun mergeContestInfos(infos: List<ContestInfo>) : ContestInfo = infos.first().copy(
        problemList = merge(infos.map { it.problemList }, ProblemInfo::id),
        teamList = merge(infos.map { it.teamList }, TeamInfo::id),
        groupList = merge(infos.map { it.groupList }, GroupInfo::id),
        organizationList = merge(infos.map { it.organizationList }, OrganizationInfo::id),
        languagesList = merge(infos.map { it.languagesList }, LanguageInfo::id),
    )


    override fun getFlow(): Flow<ContestUpdate> {
        val flows = settings.sources.mapIndexed { index, source ->
            source.settings.toFlow().map {
                index to when (it) {
                    is InfoUpdate -> InfoUpdate(it.newInfo.remap(source))
                    is RunUpdate -> RunUpdate(it.newInfo.remap(source))
                    is CommentaryMessagesUpdate -> CommentaryMessagesUpdate(it.message.remap(source))
                }
            }
        }
        val lastContestInfos = arrayOfNulls<ContestInfo>(flows.size)
        return flows.merge().transform {
            val (index, update) = it
            when (update) {
                is InfoUpdate -> {
                    lastContestInfos[index] = update.newInfo
                    emit(InfoUpdate(mergeContestInfos(lastContestInfos.filterNotNull())))
                }
                is RunUpdate, is CommentaryMessagesUpdate -> { emit(update) }
            }
        }
    }
}