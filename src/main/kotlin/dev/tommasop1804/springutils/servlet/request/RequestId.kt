package dev.tommasop1804.springutils.servlet.request

import dev.tommasop1804.kutils.classes.identifiers.ULID
import dev.tommasop1804.kutils.then
import dev.tommasop1804.springutils.request.RequestId
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Bean

@Suppress("unused")
object RequestIdProvider : ApplicationContextAware {
    private lateinit var applicationName: String

    override fun setApplicationContext(ctx: ApplicationContext) {
        applicationName = ctx.environment.getProperty("springutils.application-name") ?: "unknown"
    }

    val requestId = ThreadLocal<RequestId>()

    fun generate() = "REQ:$applicationName:${ULID(monotonic = true)}".then(::RequestId)
}

@Bean
fun requestIdProvider() = RequestIdProvider