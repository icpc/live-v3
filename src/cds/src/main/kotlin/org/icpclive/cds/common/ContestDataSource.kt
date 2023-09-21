package org.icpclive.cds.common

import kotlinx.coroutines.flow.Flow
import org.icpclive.cds.ContestUpdate

internal interface ContestDataSource {
    fun getFlow(): Flow<ContestUpdate>
}