package org.icpclive.cds.adapters

import kotlinx.collections.immutable.*
import kotlinx.coroutines.flow.*
import org.icpclive.api.ContestInfo
import org.icpclive.api.RunInfo
import org.icpclive.cds.*

class ContestEventWithRunsBefore(
    val event: ContestUpdate,
    val infoBeforeEvent: ContestInfo?,
    val runs: PersistentMap<Int, RunInfo>
) {
    val infoAfterEvent: ContestInfo?
        get() = if (event is InfoUpdate) event.newInfo else infoBeforeEvent
}

class ContestEventWithGroupedRuns<K>(
    val event: ContestUpdate,
    val infoBeforeEvent: ContestInfo?,
    val runs: PersistentMap<K, PersistentList<RunInfo>>
) {
    val infoAfterEvent: ContestInfo?
        get() = if (event is InfoUpdate) event.newInfo else infoBeforeEvent
}

open class ContestStateWithGroupedRuns<K>(
    val info: ContestInfo?,
    val runs: PersistentMap<K, PersistentList<RunInfo>>
)

class ContestStateWithRunsByTeam(val info: ContestInfo?, val runs: PersistentMap<Int, PersistentList<RunInfo>>)

fun Flow<ContestUpdate>.withContestInfoBefore() = flow {
    var lastInfo : ContestInfo? = null
    collect {
        emit(it to lastInfo)
        if (it is InfoUpdate) {
            lastInfo = it.newInfo
        }
    }
}

fun Flow<ContestUpdate>.withRunsBefore() = flow {
    var curInfo: ContestInfo? = null
    var curRuns = persistentMapOf<Int, RunInfo>()
    collect {
        emit(ContestEventWithRunsBefore(it, curInfo, curRuns))
        when (it) {
            is RunUpdate -> curRuns = curRuns.put(it.newInfo.id, it.newInfo)
            is InfoUpdate -> curInfo = it.newInfo
            is AnalyticsUpdate -> {}
        }
    }
}

fun Flow<ContestEventWithRunsBefore>.filterUseless() = filter {
    when (it.event) {
        is RunUpdate -> it.runs[it.event.newInfo.id] != it.event.newInfo
        is InfoUpdate -> it.infoBeforeEvent != it.event.newInfo
        is AnalyticsUpdate -> true
    }
}

@JvmName("filterUseless2")
fun Flow<ContestUpdate>.filterUseless() = withRunsBefore().filterUseless().map { it.event }



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

inline fun <K, V> PersistentMap<K, V>.update(k: K, block: (V?) -> V) = put(k, block(get(k)))

private fun <K> PersistentMap<K, PersistentList<RunInfo>>.addAndResort(k: K, info: RunInfo) = update(k) {
    (it ?: persistentListOf()).addAndResort(info)
}
private fun <K> PersistentMap<K, PersistentList<RunInfo>>.updateAndResort(k: K, info: RunInfo) = update(k) {
    val index = it!!.indexOfFirst { run -> run.id == info.id }
    it.setAndResort(index, info)
}
private fun <K> PersistentMap<K, PersistentList<RunInfo>>.removeRun(k: K, info: RunInfo) = update(k) {
    val index = it!!.indexOfFirst { run -> run.id == info.id }
    it.removeAt(index)
}


fun <K: Any> Flow<ContestUpdate>.withGroupedRuns(
    selector: (RunInfo) -> K,
    transformGroup : ((key: K, cur: PersistentList<RunInfo>, original: PersistentList<RunInfo>, info: ContestInfo?) -> List<RunInfo>)? = null,
    needUpdateGroup : ((new: ContestInfo, old: ContestInfo?, key: K) -> Boolean)? = null,
) = flow {
    var curInfo: ContestInfo? = null
    var curRuns = persistentMapOf<K, PersistentList<RunInfo>>()
    var originalRuns = persistentMapOf<K, PersistentList<RunInfo>>()
    val oldKey = mutableMapOf<Int, K>()
    collect { update ->
        suspend fun emit(update: ContestUpdate) = emit(ContestEventWithGroupedRuns(update, curInfo, curRuns))
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
                emit(update)
            }
        }
    }
}

fun <K:Any> Flow<ContestUpdate>.stateWithGroupedRuns(selector: (RunInfo) -> K) = withGroupedRuns(selector).map {
    ContestStateWithGroupedRuns(it.infoAfterEvent, it.runs)
}.conflate()

fun Flow<ContestUpdate>.stateGroupedByTeam() = stateWithGroupedRuns { it.teamId }.map {
    ContestStateWithRunsByTeam(it.info, it.runs)
}