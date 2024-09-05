package org.icpclive.cds.utils

import kotlinx.collections.immutable.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.icpclive.cds.*
import org.icpclive.cds.api.*

internal open class ContestStateWithGroupedRuns<K>(
    val event: ContestUpdate,
    val infoBeforeEvent: ContestInfo?,
    val runs: PersistentMap<K, PersistentList<RunInfo>>,
    val analyticsMessages: PersistentMap<String, CommentaryMessage>,
) {
    val infoAfterEvent: ContestInfo?
        get() = if (event is InfoUpdate) event.newInfo else infoBeforeEvent
}

internal fun <K : Any> Flow<ContestUpdate>.withGroupedRuns(
    selector: (RunInfo) -> K,
    transformGroup: ((key: K, cur: PersistentList<RunInfo>, original: PersistentList<RunInfo>, info: ContestInfo?) -> List<RunInfo>)? = null,
    needUpdateGroup: ((new: ContestInfo, old: ContestInfo?, key: K) -> Boolean)? = null,
): Flow<ContestStateWithGroupedRuns<K>> =
    withGroupedRuns(selector, ::ContestStateWithGroupedRuns, transformGroup, needUpdateGroup)

internal fun <K : Any, S : ContestStateWithGroupedRuns<K>> Flow<ContestUpdate>.withGroupedRuns(
    selector: (RunInfo) -> K,
    provider: (ContestUpdate, ContestInfo?, PersistentMap<K, PersistentList<RunInfo>>, PersistentMap<String, CommentaryMessage>) -> S,
    transformGroup: ((key: K, cur: PersistentList<RunInfo>, original: PersistentList<RunInfo>, info: ContestInfo?) -> List<RunInfo>)? = null,
    needUpdateGroup: ((new: ContestInfo, old: ContestInfo?, key: K) -> Boolean)? = null,
): Flow<S> = flow {
    var curInfo: ContestInfo? = null
    var curRuns = persistentMapOf<K, PersistentList<RunInfo>>()
    var curMessages = persistentMapOf<String, CommentaryMessage>()
    var originalRuns = persistentMapOf<K, PersistentList<RunInfo>>()
    val oldKey = mutableMapOf<RunId, K>()
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
