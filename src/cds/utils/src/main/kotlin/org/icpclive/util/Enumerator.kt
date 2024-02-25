package org.icpclive.util

public class Enumerator<K> {
    private val keys = mutableMapOf<K, Int>()

    public operator fun get(key: K): Int = keys.getOrPut(key) { keys.size + 1 }
}