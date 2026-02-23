@file:Suppress("unused")

package dev.tommasop1804.springutils.log

import dev.tommasop1804.kutils.classes.identifiers.ULID
import kotlinx.coroutines.reactive.awaitSingle
import reactor.core.publisher.Mono
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class LogIdContext(val ulid: ULID) : AbstractCoroutineContextElement(LogIdContext) {
    companion object Key : CoroutineContext.Key<LogIdContext>
}

suspend fun currentLogId(): ULID = currentLogIdMono().awaitSingle()

fun currentLogIdMono(): Mono<ULID> = Mono.deferContextual { ctx ->
    Mono.justOrEmpty(ctx.getOrEmpty(ULID::class.java))
}