import org.icpclive.gradle.tasks.TsInterfaceGeneratorTask

plugins {
    id("live.kotlin-conventions")
    id("live.file-sharing")
}

dependencies {
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.datetime)
    api(projects.cds.utils)
    api(projects.cds.core)
}

val generateApiTypeScript by tasks.registering(TsInterfaceGeneratorTask::class) {
    rootClasses.addAll(listOf(
        "org.icpclive.cds.api.ContestInfo",
        "org.icpclive.cds.api.RunInfo",
        "org.icpclive.cds.api.ScoreboardDiff",
        "org.icpclive.api.MainScreenEvent",
        "org.icpclive.api.QueueEvent",
        "org.icpclive.api.AnalyticsEvent",
        "org.icpclive.api.TickerEvent",
        "org.icpclive.api.SolutionsStatistic",
        "org.icpclive.api.ExternalTeamViewSettings",
        "org.icpclive.api.ObjectSettings",
        "org.icpclive.api.WidgetUsageStatistics",
        "org.icpclive.api.TimeLineRunInfo",
        "org.icpclive.api.AddTeamScoreRequest",
        "org.icpclive.api.InterestingTeam"
    ))
    fileName = "api"
}
artifacts.tsInterfacesProvider(generateApiTypeScript)
