package dev.tommasop1804.springutils.log

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CLASS
)
@Retention(AnnotationRetention.RUNTIME)
annotation class LogExecution(
    val behaviour: Behaviour = Behaviour.ALL,
    val basePackage: String = "",
    val includeHighlight: Boolean = true,
    val exclude: Array<Component> = [],
    val includeOnly: Array<Component> = []
) {
    enum class Component {
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

    enum class Behaviour {
        BEFORE,
        AFTER,
        AFTER_THROWING,
        ALL,
        @Suppress("unused") NONE
    }
}