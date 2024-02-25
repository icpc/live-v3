package org.icpclive.ksp.clics

enum class FeedVersion {
    `2020_03`,
    `2022_07`,
    `2023_06`,

    ;

    val packageName: String
        get() = "v${name.replace("_", "")}"
}

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class SinceClics(val feedVersion: FeedVersion)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class InlinedBefore(val feedVersion: FeedVersion, val prefix: String)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class RequiredSince(val feedVersion: FeedVersion)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class NameIn(val name: String, vararg val feedVersions: FeedVersion)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class UpdateContestEvent

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class UpdateRunEvent

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class NoEvent

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class Required

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class EventSerialName(vararg val names: String)