package dev.tommasop1804.springutils.servlet.request

import dev.tommasop1804.kutils.classes.identifiers.ULID
import dev.tommasop1804.kutils.then
import dev.tommasop1804.springutils.request.RequestId
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Bean

@Suppress("unused")
object RequestIdProvider : ApplicationContextAware {
    private lateinit var ctx: ApplicationContext

    private val applicationAcronym: String by lazy {
        ctx.environment.getProperty("spring-utils.application-acronym") ?: "unknown"
    }

    override fun setApplicationContext(context: ApplicationContext) {
        ctx = context
    }

    val requestId = ThreadLocal<RequestId>()

    fun generate() = "REQ:${applicationAcronym}:${ULID(monotonic = true)}".then(::RequestId)
}

@Bean
fun requestIdProvider() = RequestIdProvider