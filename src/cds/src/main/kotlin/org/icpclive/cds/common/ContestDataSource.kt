package org.icpclive.cds.common

import kotlinx.coroutines.flow.Flow
import org.icpclive.cds.ContestUpdate

public interface ContestDataSource {
    public fun getFlow(): Flow<ContestUpdate>
}