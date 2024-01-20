package org.icpclive.cds.adapters

import kotlinx.collections.immutable.*
import kotlinx.coroutines.flow.*
import org.icpclive.api.*
import org.icpclive.cds.*

internal open class ContestStateWithGroupedRuns<K>(
    public val event: ContestUpdate,
    public val infoBeforeEvent: ContestInfo?,
    public val runs: PersistentMap<K, PersistentList<RunInfo>>,
    public val analyticsMessages: PersistentMap<String, AnalyticsMessage>
) {
    public val infoAfterEvent: ContestInfo?
        get() = if (event is InfoUpdate) event.newInfo else infoBeforeEvent
}

internal class ContestStateWithRunsByTeam(
    event: ContestUpdate,
    infoBeforeEvent: ContestInfo?,
    runs: PersistentMap<Int, PersistentList<RunInfo>>,
    analyticsMessages: PersistentMap<String, AnalyticsMessage>
) : ContestStateWithGroupedRuns<Int>(event, infoBeforeEvent, runs, analyticsMessages)

private fun PersistentList<RunInfo>.resort(index_: Int) = builder().apply {
    var index = index_
    val comparator = compareBy(RunInfo::time, RunInfo::id)
    while (index > 0 && comparator.compare(get(index - 1), get(index)) > 0) {
        val t = get(index)
        set(index, get(index - 1))
        set(index - 1, t)
        index--
    }
    while (index + 1 < size && comparator.compare(get(index), get(index + 1)) > 0) {
        val t = get(index)
        set(index, get(index + 1))
        set(index + 1, t)
        index++
    }
}.build()
private fun PersistentList<RunInfo>.addAndResort(info: RunInfo) = add(info).resort(size)
private fun PersistentList<RunInfo>.setAndResort(index: Int, info: RunInfo) = set(index, info).resort(index)

internal inline fun <K, V> PersistentMap<K, V>.update(k: K, block: (V?) -> V) = put(k, block(get(k)))

internal fun <K> PersistentMap<K, PersistentList<RunInfo>>.addAndResort(k: K, info: RunInfo) = update(k) {
    (it ?: persistentListOf()).addAndResort(info)
}
internal fun <K> PersistentMap<K, PersistentList<RunInfo>>.updateAndResort(k: K, info: RunInfo) = update(k) {
    val index = it!!.indexOfFirst { run -> run.id == info.id }
    it.setAndResort(index, info)
}
internal fun <K> PersistentMap<K, PersistentList<RunInfo>>.removeRun(k: K, info: RunInfo) = update(k) {
    val index = it!!.indexOfFirst { run -> run.id == info.id }
    it.removeAt(index)
}

internal fun <K: Any> Flow<ContestUpdate>.withGroupedRuns(
    selector: (RunInfo) -> K,
    transformGroup: ((key: K, cur: PersistentList<RunInfo>, original: PersistentList<RunInfo>, info: ContestInfo?) -> List<RunInfo>)? = null,
    needUpdateGroup: ((new: ContestInfo, old: ContestInfo?, key: K) -> Boolean)? = null,
): Flow<ContestStateWithGroupedRuns<K>> = withGroupedRuns(selector, ::ContestStateWithGroupedRuns, transformGroup, needUpdateGroup)

internal fun <K: Any, S : ContestStateWithGroupedRuns<K>> Flow<ContestUpdate>.withGroupedRuns(
    selector: (RunInfo) -> K,
    provider: (ContestUpdate, ContestInfo?, PersistentMap<K, PersistentList<RunInfo>>, PersistentMap<String, AnalyticsMessage>) -> S,
    transformGroup: ((key: K, cur: PersistentList<RunInfo>, original: PersistentList<RunInfo>, info: ContestInfo?) -> List<RunInfo>)? = null,
    needUpdateGroup: ((new: ContestInfo, old: ContestInfo?, key: K) -> Boolean)? = null
): Flow<S> = flow {
    var curInfo: ContestInfo? = null
    var curRuns = persistentMapOf<K, PersistentList<RunInfo>>()
    var curMessages = persistentMapOf<String, AnalyticsMessage>()
    var originalRuns = persistentMapOf<K, PersistentList<RunInfo>>()
    val oldKey = mutableMapOf<Int, K>()
    collect { update ->
        suspend fun emit(update: ContestUpdate) = emit(provider(update, curInfo, curRuns, curMessages))
        suspend fun updateGroup(key: K, newRun: RunInfo? = null) {
            var plist = curRuns[key] ?: persistentListOf()
            if (transformGroup == null) {
                newRun?.let { emit(RunUpdate(it)) }
                return
            }
            val newList = transformGroup(key, plist, originalRuns[key] ?: persistentListOf(), curInfo)
            if (newList === plist) {
                newRun?.let { emit(RunUpdate(it)) }
                return
            }
            for (i in newList.indices) {
                if (newList[i] != plist[i] || newList[i].id == newRun?.id) {
                    plist = plist.set(i, newList[i])
                    curRuns = curRuns.put(key, plist)
                    emit(RunUpdate(newList[i]))
                }
            }
        }
        when (update) {
            is RunUpdate -> {
                val k = selector(update.newInfo)
                val oldK = oldKey[update.newInfo.id]
                oldKey[update.newInfo.id] = k
                if (oldK != k) {
                    if (oldK != null) {
                        curRuns = curRuns.removeRun(oldK, update.newInfo)
                        originalRuns = originalRuns.removeRun(oldK, update.newInfo)
                        updateGroup(oldK)
                    }
                    curRuns = curRuns.addAndResort(k, update.newInfo)
                    originalRuns = originalRuns.addAndResort(k, update.newInfo)
                    updateGroup(k, update.newInfo)
                } else {
                    curRuns = curRuns.updateAndResort(k, update.newInfo)
                    originalRuns = originalRuns.updateAndResort(k, update.newInfo)
                    updateGroup(k, update.newInfo)
                }
            }
            is InfoUpdate -> {
                emit(update)
                val oldInfo = curInfo
                curInfo = update.newInfo
                if (needUpdateGroup != null) {
                    for (k in curRuns.keys) {
                        if (needUpdateGroup(update.newInfo, oldInfo, k)) {
                            updateGroup(k)
                        }
                    }
                }
            }
            is AnalyticsUpdate -> {
                curMessages = curMessages.put(update.message.id, update.message)
                emit(update)
            }
        }
    }
}

internal fun Flow<ContestUpdate>.stateGroupedByTeam(): Flow<ContestStateWithRunsByTeam> =
    withGroupedRuns({ it.teamId }, ::ContestStateWithRunsByTeam)
