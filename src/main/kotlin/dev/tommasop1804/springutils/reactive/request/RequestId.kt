@file:Suppress("unused")

package dev.tommasop1804.springutils.reactive.request

import dev.tommasop1804.springutils.request.RequestId
import kotlinx.coroutines.reactive.awaitSingle
import reactor.core.publisher.Mono
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

class RequestIdContext(val requestId: RequestId) : AbstractCoroutineContextElement(RequestIdContext) {
    companion object Key : CoroutineContext.Key<RequestIdContext>
}

suspend fun requestId(): RequestId = requestIdMono().awaitSingle()

fun requestIdMono(): Mono<RequestId> = Mono.deferContextual { ctx ->
    Mono.justOrEmpty(ctx.getOrEmpty(RequestId::class.java))
}