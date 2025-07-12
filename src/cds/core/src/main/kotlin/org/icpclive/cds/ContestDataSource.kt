package org.icpclive.cds

import kotlinx.coroutines.flow.Flow

public interface ContestDataSource {
    public fun getFlow(): Flow<ContestUpdate>
}