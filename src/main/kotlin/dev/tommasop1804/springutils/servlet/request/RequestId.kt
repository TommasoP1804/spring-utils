package dev.tommasop1804.springutils.servlet.request

import dev.tommasop1804.kutils.classes.identifiers.ULID
import dev.tommasop1804.kutils.then
import dev.tommasop1804.springutils.request.RequestId
import org.springframework.context.annotation.Bean

@Suppress("unused")
object RequestIdProvider {
    private val applicationAcronym: String by lazy {
        System.getProperty("spring-utils.application-acronym")
            ?: System.getenv("spring-utils.application-acronym")
            ?: System.getenv("APPLICATION_ACRONYM")
            ?: System.getenv("SPRING_UTILS_APPLICATION_ACRONYM")
            ?: ""
    }
    internal val requestIdThreadLocal = ThreadLocal<RequestId>()
    val requestId: RequestId? get() = requestIdThreadLocal.get()

    fun generate() = "REQ:${applicationAcronym}:${ULID(monotonic = true)}".then(::RequestId)
}

@Bean
fun requestIdProvider() = RequestIdProvider