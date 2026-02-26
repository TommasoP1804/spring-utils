package dev.tommasop1804.springutils.servlet.request

import dev.tommasop1804.kutils.classes.identifiers.ULID
import dev.tommasop1804.kutils.then
import dev.tommasop1804.springutils.request.RequestId
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

/**
 * A provider responsible for generating and managing request identifiers within an application's context.
 *
 * This class integrates with the Spring `Environment` to dynamically retrieve configuration properties
 * and construct unique request identifiers based on those properties. The generated identifiers
 * typically include an application-specific acronym and a unique ID compliant with the ULID standard.
 *
 * This class is designed to support thread-local storage of request identifiers, enabling safe
 * association of identifiers with individual threads during the lifecycle of a request. This is useful
 * for logging, debugging, and request tracking purposes.
 *
 * @since 2.1.0
 * @author Tommaso Pastorelli
 */
@Suppress("unused")
@Component
class RequestIdProvider(
    val environment: Environment
) {
    companion object {
        /**
         * A thread-local variable used to store and manage the current request's unique identifier.
         *
         * This variable is specifically designed to hold an instance of [RequestId], which represents
         * a structured and detailed identifier corresponding to the current HTTP request or operation.
         * It allows for thread-safe association of a request ID within the lifecycle of a single thread.
         *
         * The `ThreadLocal` provides isolation of the `RequestId` value to the thread it is associated with,
         * preventing leakage to other threads and ensuring thread-specific context management.
         *
         * This is typically used in conjunction with request processing mechanisms to associate metadata
         * or identifiers with individual requests for tracking, logging, or debugging purposes.
         * @since 2.1.0
         */
        @JvmStatic
        internal val requestIdThreadLocal = ThreadLocal<RequestId>()
        /**
         * Retrieves the current `RequestId` value stored in the thread-local context.
         *
         * This property provides access to the `RequestId` associated with the current thread. The `RequestId`
         * may be used to uniquely identify a request within an application or service.
         *
         * The value of `requestId` is managed internally by using a thread-local variable, ensuring that it
         * maintains thread confinement and does not interfere with other threads.
         *
         * @return The `RequestId` associated with the current thread, or `null` if no `RequestId` has been set.
         * @since 2.1.0
         */
        @JvmStatic
        val requestId: RequestId? get() = requestIdThreadLocal.get()
    }

    private val applicationAcronym by lazy { environment.getProperty("spring-utils.application-acronym") ?: "" }

    fun generate() = "REQ:${applicationAcronym}:${ULID(monotonic = true)}".then(::RequestId)
}