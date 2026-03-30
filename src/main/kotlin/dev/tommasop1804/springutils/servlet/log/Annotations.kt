/*
 * Copyright © 2026 Tommaso Pastorelli | spring-utils
 */

package dev.tommasop1804.springutils.servlet.log

import dev.tommasop1804.kutils.*

@Target(
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CLASS
)
@Retention(AnnotationRetention.RUNTIME)
annotation class LogExecution(
    val behaviour: Array<Behaviour> = [Behaviour.BEFORE, Behaviour.AFTER, Behaviour.AFTER_THROWING],
    val basePackage: String = "",
    val includeHighlight: Boolean = true,
    val exclude: Array<Component> = [],
    val includeOnly: Array<Component> = [],
    val customMessages: Array<CustomMessage> = []
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
        BEFORE,
        AFTER,
        AFTER_THROWING,
        @Suppress("unused") NONE
    }

    @Retention(AnnotationRetention.RUNTIME)
    annotation class CustomMessage(
        val key: String,
        val type: Type,
        val reference: String,
        val effects: Array<Ansi.Effect> = [],
        val textColor: Ansi.TextColor = Ansi.TextColor.DEFAULT,
        val bgColor: Ansi.BackgroundColor = Ansi.BackgroundColor.DEFAULT,
    ) {
        enum class Type {
            HEADER,
            QUERY_PARAM,
            PATH_VARIABLE,
            PATH_INDEX,
            STATIC
        }
    }
}

internal fun applyAnsi(cm: LogExecution.CustomMessage, string: String) = with(cm) { Ansi.compose(
    *(effects.map { it.code } + textColor.code + bgColor.code).toTypedArray()
) + string + Ansi.RESET }