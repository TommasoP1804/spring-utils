package dev.tommasop1804.springutils.log

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CLASS
)
@Retention(AnnotationRetention.RUNTIME)
annotation class Logging(
    val basePackage: String = "",
    val includeHighlight: Boolean = true,
    val exclude: Array<LogComponent> = [],
    val includeOnly: Array<LogComponent> = []
)

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CLASS
)
@Retention(AnnotationRetention.RUNTIME)
annotation class LoggingBefore(
    val exclude: Array<LogComponent> = [],
    val includeOnly: Array<LogComponent> = []
)

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CLASS
)
@Retention(AnnotationRetention.RUNTIME)
annotation class LoggingAfter(
    val exclude: Array<LogComponent> = [],
    val includeOnly: Array<LogComponent> = []
)

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CLASS
)
@Retention(AnnotationRetention.RUNTIME)
annotation class LoggingAfterThrowing(
    val basePackage: String = "",
    val includeHighlight: Boolean = true,
    val exclude: Array<LogComponent> = [],
    val includeOnly: Array<LogComponent> = []
)

enum class LogComponent {
    FUNCTION_NAME,
    CLASS_NAME,
    USER,
    SERVICE,
    ID,
    FEATURE_CODE,
    ELAPSED_TIME,
    STATUS,
    EXCEPTION,
    STACKTRACE
}