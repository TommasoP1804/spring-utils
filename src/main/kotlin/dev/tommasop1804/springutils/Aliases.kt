@file:JvmName("AliasesKt")
@file:Since("1.0.0")
@file:Suppress("unused")

package dev.tommasop1804.springutils

import dev.tommasop1804.kutils.annotations.Since
import org.springframework.http.ResponseEntity

/**
 * A typealias representing an HTTP response with an empty body.
 *
 * Useful for API endpoints where the response has no content but still
 * needs to return an HTTP status code indicating the result of the request.
 * @since 1.0.0
 */
typealias EmptyResponse = ResponseEntity<Unit>

/**
 * Type alias for `ResponseEntity<T>`.
 *
 * Simplifies the usage of `ResponseEntity` for returning responses in
 * a generic way in Spring-based applications. This alias can be used
 * to enhance code readability and reduce verbosity when working with
 * HTTP responses.
 *
 * @param T the type of the body contained in the response
 * @since 1.0.0
 */
typealias Response<T> = ResponseEntity<T>