/*
 * Copyright © 2026 Tommaso Pastorelli (TommasoP1804) | Spring-Utils
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
    val behaviour: Array<Behaviour> = [Behaviour.Before, Behaviour.After, Behaviour.AfterThrowing],
    val basePackage: String = "",
    val includeHighlight: Boolean = true,
    val exclude: Array<Component> = [],
    val includeOnly: Array<Component> = [],
    val customMessages: Array<CustomMessage> = []
) {
    enum class Component {
        FunctionName,
        Path,
        ClassName,
        User,
        Service,
        Id,
        FeatureCode,
        ElapsedTime,
        Status,
        Exception,
        Stacktrace
    }

    enum class Behaviour {
        Before,
        After,
        AfterThrowing,
        @Suppress("unused") None
    }

    @Retention(AnnotationRetention.RUNTIME)
    annotation class CustomMessage(
        val key: String,
        val type: Type,
        val reference: String,
        val effects: Array<Ansi.Effect> = [],
        val textColor: Ansi.TextColor = Ansi.TextColor.Default,
        val bgColor: Ansi.BackgroundColor = Ansi.BackgroundColor.Default,
    ) {
        enum class Type {
            Header,
            QueryParam,
            PathVariable,
            PathIndex,
            Static
        }
    }
}

internal fun applyAnsi(cm: LogExecution.CustomMessage, string: String) = with(cm) { Ansi.compose(
    *(effects.map { it.code } + textColor.code + bgColor.code).toTypedArray()
) + string + Ansi.RESET }