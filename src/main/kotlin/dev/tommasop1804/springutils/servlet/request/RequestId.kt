package dev.tommasop1804.springutils.servlet.request

import dev.tommasop1804.kutils.classes.identifiers.ULID
import dev.tommasop1804.kutils.then
import dev.tommasop1804.springutils.request.RequestId
import org.springframework.context.annotation.Bean

@Suppress("unused")
object RequestIdProvider {
    private val applicationAcronym: String by lazy {
        System.getProperty("spring.profiles.active")
            ?: System.getenv("spring.profiles.active")
            ?: System.getenv("SPRING_PROFILES_ACTIVE")
            ?: ""
    }
    val requestId = ThreadLocal<RequestId>()

    fun generate() = "REQ:${applicationAcronym}:${ULID(monotonic = true)}".then(::RequestId)
}

@Bean
fun requestIdProvider() = RequestIdProvider