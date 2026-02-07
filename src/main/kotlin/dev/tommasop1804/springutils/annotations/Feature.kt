package dev.tommasop1804.springutils.annotations

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Feature(
    val code: String
)