package org.icpclive.util

public inline fun <T> Iterable<T>.atMostOne(predicate: (T) -> Boolean) : T? {
    var single: T? = null
    var found = false
    for (element in this) {
        if (predicate(element)) {
            if (found) throw IllegalArgumentException("Collection contains more than one matching element.")
            single = element
            found = true
        }
    }
    return single
}