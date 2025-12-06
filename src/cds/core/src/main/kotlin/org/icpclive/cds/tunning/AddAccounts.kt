package org.icpclive.cds.tunning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.icpclive.cds.api.*
import org.icpclive.cds.util.getLogger
import org.icpclive.cds.util.logger

@Serializable
@SerialName("addAccounts")
public data class AddAccounts(public val accounts: List<AccountInfo>): TuningRule {
    @OptIn(InefficientContestInfoApi::class)
    override fun process(info: ContestInfo): ContestInfo {
        val existingIds = info.accountsList.map { it.id }.toMutableSet()
        return info.copy(
            accountsList = buildList {
                addAll(info.accountsList)
                for (account in accounts) {
                    if (existingIds.add(account.id)) {
                        add(account)
                    } else {
                        logger(AddAccounts::class).warning { "Can't add account ${account.id}: it already exists" }
                    }
                }
            }
        )
    }

    private companion object {
        val logger by getLogger()
    }
}