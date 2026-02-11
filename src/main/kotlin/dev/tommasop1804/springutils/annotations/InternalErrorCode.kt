package dev.tommasop1804.springutils.annotations

import kotlin.reflect.KClass

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class InternalErrorCode(
    val ifMissing: String = "",
    val ifInvalid: Array<InvalidMapping> = []
) {
    @Target()
    @Retention(AnnotationRetention.RUNTIME)
    annotation class InvalidMapping(
        val exceptions: Array<KClass<out Throwable>>,
        val code: String
    )
}