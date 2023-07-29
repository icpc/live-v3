package org.icpclive.cds.common

internal interface RawContestDataSource : ContestDataSource {
    suspend fun loadOnce(): ContestParseResult
}