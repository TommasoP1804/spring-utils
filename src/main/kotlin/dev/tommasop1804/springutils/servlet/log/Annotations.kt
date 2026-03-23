package dev.tommasop1804.springutils.servlet.log

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CLASS
)
@Retention(AnnotationRetention.RUNTIME)
annotation class LogExecution(
    val behaviour: Array<Behaviour> = [Behaviour.PATH_BEFORE, Behaviour.BEFORE, Behaviour.AFTER, Behaviour.AFTER_THROWING],
    val basePackage: String = "",
    val includeHighlight: Boolean = true,
    val exclude: Array<Component> = [],
    val includeOnly: Array<Component> = []
) {
    enum class Component {
        FUNCTION_NAME,
        PATH,
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
        PATH_BEFORE,
        BEFORE,
        AFTER,
        AFTER_THROWING,
        @Suppress("unused") NONE
    }
}