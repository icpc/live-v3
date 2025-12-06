package org.icpclive.ksp.clics

enum class FeedVersion {
    `2020_03`,
    `2022_07`,
    `2023_06`,
    DRAFT,

    ;

    val packageName: String
        get() = "v${name.replace("_", "")}"
}

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
annotation class SinceClics(val feedVersion: FeedVersion)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class LongMinutesBefore(val feedVersion: FeedVersion)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class SingleBefore(val feedVersion: FeedVersion, val oldName: String)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class InlinedBefore(val feedVersion: FeedVersion, val prefix: String)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class NoEvent

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class Required

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class EventSerialName(vararg val names: String)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class JsonName(val name: String)