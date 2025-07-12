package org.icpclive.cds.utils

import kotlinx.collections.immutable.*
import org.icpclive.cds.api.RunInfo

private fun PersistentList<RunInfo>.resort(index_: Int) = builder().apply {
    var index = index_
    val comparator = compareBy(RunInfo::time, { it.id.value })
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

