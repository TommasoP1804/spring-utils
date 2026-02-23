package dev.tommasop1804.springutils.log

import dev.tommasop1804.kutils.classes.identifiers.ULID
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.CoroutineContext.Element

/**
 * A [CoroutineContext.Element] that carries the current log ULID through the coroutine context.
 *
 * In suspend functions intercepted by [LoggingAspect], this element is added to the
 * coroutine context, allowing code inside the coroutine to access the log identifier via:
 * ```
 * val logId = coroutineContext[LogId]?.id
 * ```
 *
 * @since 2.0.0
 * @author Tommaso Pastorelli
 */
@Suppress("unused")
class LogId(val id: ULID) : AbstractCoroutineContextElement(LogId) {
    companion object Key : CoroutineContext.Key<LogId>
}