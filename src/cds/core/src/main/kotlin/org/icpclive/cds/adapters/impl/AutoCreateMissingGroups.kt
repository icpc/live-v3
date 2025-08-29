package org.icpclive.cds.adapters.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.icpclive.cds.ContestUpdate
import org.icpclive.cds.InfoUpdate
import org.icpclive.cds.api.*

@OptIn(InefficientContestInfoApi::class)
internal fun ContestInfo.autoCreateGroupsAndOrgs() : ContestInfo {
    val newGroups = teams.flatMap { it.value.groups }.toSet() - groups.keys
    val newOrgs = teams.mapNotNull { it.value.organizationId }.toSet() - organizations.keys
    if (newGroups.isEmpty() && newOrgs.isEmpty()) return this
    return copy(
        groupList = groupList + newGroups.map { GroupInfo(it, it.value, false, false) },
        organizationList = organizationList + newOrgs.map { OrganizationInfo(it, it.value, it.value, emptyList()) },
    )
}

internal fun autoCreateMissingGroupsAndOrgs(flow: Flow<ContestUpdate>) = flow.map {
    if (it !is InfoUpdate) return@map it
    InfoUpdate(it.newInfo.autoCreateGroupsAndOrgs())
}