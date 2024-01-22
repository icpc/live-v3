package org.icpclive.cds.plugins.codeforces.api.results

import kotlinx.serialization.Serializable
import java.io.IOException

@Serializable
internal class CFStatusWrapper<T : Any>(val status: String, private val comment: String? = null, val result: T? = null) {
    fun unwrap(): T {
        if (status != "OK") throw IOException("Error from codeforces: $comment")
        return result!!
    }
}