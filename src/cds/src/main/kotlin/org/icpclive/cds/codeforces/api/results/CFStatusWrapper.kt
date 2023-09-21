package org.icpclive.cds.codeforces.api.results

import kotlinx.serialization.Serializable
import java.io.IOException

@Serializable
internal class CFStatusWrapper<T : Any>(val status: String, val comment: String? = null, val result: T? = null) {
    fun unwrap() : T {
        if (status != "OK") throw IOException("Error from codeforces: $comment")
        return result!!
    }
}