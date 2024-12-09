package org.icpclive.cds.tunning

internal fun <T, O, ID> mergeOverrides(
    infos: List<T>,
    overrides: Map<ID, O>?,
    id: T.() -> ID,
    logUnused: (Set<ID>) -> Unit = { },
    merge: (T, O) -> T,
): List<T> {
    return if (overrides == null) {
        infos
    } else {
        val idsSet = infos.map { it.id() }.toSet()
        fun mergeIfNotNull(a: T, b: O?) = if (b == null) a else merge(a, b)
        (overrides.keys - idsSet)
            .takeIf { it.isNotEmpty() }
            ?.let(logUnused)
        infos.map {
            mergeIfNotNull(it, overrides[it.id()])
        }
    }
}

internal fun <K, V> mergeMaps(original: Map<K, V>, override: Map<K, V?>?) = buildMap {
    putAll(original)
    if (override != null) {
        for ((k, v) in override.entries) {
            if (v == null) {
                remove(k)
            } else {
                put(k, v)
            }
        }
    }
}