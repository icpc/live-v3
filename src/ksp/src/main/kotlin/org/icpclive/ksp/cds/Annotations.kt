package org.icpclive.ksp.cds

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Builder(val name: String = "")

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class Seconds(val name: String = "")

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class MilliSeconds(val name: String = "")

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class Human(val name: String = "")

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class UnixSeconds(val name: String = "")

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class UnixMilliSeconds(val name: String = "")

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY)
annotation class AlwaysSerialize

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class SerializerProviders(val providerClassName: String)