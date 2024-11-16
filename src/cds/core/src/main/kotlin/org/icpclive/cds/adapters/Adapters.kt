package org.icpclive.cds.adapters


import org.icpclive.cds.ContestUpdate
import kotlinx.coroutines.flow.*
import org.icpclive.cds.adapters.impl.*
import org.icpclive.cds.api.*
import org.icpclive.cds.scoreboard.ContestStateWithScoreboard
import org.icpclive.cds.settings.EmulationSettings
import org.icpclive.cds.settings.PreviousDaySettings
import org.icpclive.cds.tunning.AdvancedProperties


public fun Flow<ContestUpdate>.addFirstToSolves(): Flow<ContestUpdate> = addFirstToSolves(this)
public fun Flow<ContestUpdate>.calculateScoreDifferences(): Flow<ContestUpdate> = calculateScoreDifferences(this)
public fun Flow<ContestUpdate>.hideHiddenProblemsRuns(): Flow<ContestUpdate> = hideHiddenProblemsRuns(this)
public fun Flow<ContestUpdate>.hideHiddenGroupsTeams(): Flow<ContestUpdate> = hideHiddenGroupsTeams(this)
public fun Flow<ContestUpdate>.hideHiddenTeamsRuns(): Flow<ContestUpdate> = hideHiddenTeamsRuns(this)
public fun Flow<ContestUpdate>.removeAfterEndSubmissions(): Flow<ContestUpdate> = removeAfterEndSubmissions(this)
public fun Flow<ContestUpdate>.removeFrozenSubmissionsResults(): Flow<ContestUpdate> = removeFrozenSubmissionsResults(this)
public fun Flow<ContestUpdate>.selectProblemColors(): Flow<ContestUpdate> = selectProblemColors(this)

public fun Flow<ContestUpdate>.toEmulationFlow(emulationSettings: EmulationSettings): Flow<ContestUpdate> = toEmulationFlow(this, emulationSettings)

@JvmName("addPreviousDaysByResults")
public fun Flow<ContestUpdate>.addPreviousDays(previousDays: List<ContestState>): Flow<ContestUpdate> = addPreviousDays(this, previousDays)
@JvmName("addPreviousDaysBySettings")
public fun Flow<ContestUpdate>.addPreviousDays(previousDays: List<PreviousDaySettings>): Flow<ContestUpdate> = addPreviousDays(this, previousDays)

public fun Flow<ContestUpdate>.applyAdvancedProperties(advancedPropsFlow: Flow<AdvancedProperties>): Flow<ContestUpdate> = applyAdvancedProperties(this, advancedPropsFlow)
public fun Flow<ContestUpdate>.applyCustomFieldsMap(customFieldsFlow: Flow<Map<TeamId, Map<String, String>>>): Flow<ContestUpdate> = applyCustomFieldsMap(this, customFieldsFlow)

public fun Flow<ContestStateWithScoreboard>.generateCommentary(
    generator: (ContestStateWithScoreboard) -> List<CommentaryMessage>
) : Flow<ContestStateWithScoreboard> = generateCommentary(this, generator)

public fun Flow<ContestUpdate>.processCommentaryTags(): Flow<ContestUpdate> = processCommentaryTags(this)

public class ComputedDataConfig internal constructor() {
    public var firstToSolves: Boolean = true
    public var ioiScoreDifferences: Boolean = true
    public var submissionsAfterEnd: Boolean = false
    public var submissionResultsAfterFreeze: Boolean = false
    public var propagateHidden: Boolean = true
    public var unhideColorWhenSolved: Boolean = true
    public var replaceCommentaryTags: Boolean = true
}

private inline fun Flow<ContestUpdate>.applyIf(cond: Boolean, adapter: Flow<ContestUpdate>.() -> Flow<ContestUpdate>) = if (cond) adapter() else this

public fun Flow<ContestUpdate>.addComputedData(configure: ComputedDataConfig.() -> Unit = {}): Flow<ContestUpdate> {
    val config = ComputedDataConfig().apply(configure)
    return this
        .applyIf(!config.submissionResultsAfterFreeze) { removeFrozenSubmissionsResults() }
        .applyIf(!config.submissionsAfterEnd) { removeAfterEndSubmissions() }
        .applyIf(config.unhideColorWhenSolved) { selectProblemColors() }
        .applyIf(config.propagateHidden) { hideHiddenGroupsTeams() }
        .applyIf(config.propagateHidden) { hideHiddenTeamsRuns() }
        .applyIf(config.propagateHidden) { hideHiddenProblemsRuns() }
        .applyIf(config.firstToSolves) { addFirstToSolves() }
        .applyIf(config.ioiScoreDifferences) { calculateScoreDifferences() }
        .applyIf(config.replaceCommentaryTags) { processCommentaryTags() }
}