/*
 * Copyright © 2026 Tommaso Pastorelli | spring-utils
 */

@file:JvmName("AliasesKt")
@file:Since("1.0.0")
@file:Suppress("unused")

package dev.tommasop1804.springutils.reactive.function

import dev.tommasop1804.kutils.annotations.*
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse

/**
 * Type alias for `ServerRequest`. It provides a more concise and readable way to refer to the `ServerRequest` type.
 * @since 2.0.0
 */
typealias Request = ServerRequest

/**
 * A type alias for `ServerResponse`. This alias can be used to refer to
 * the `ServerResponse` class, providing a shorthand or simplifying usage
 * where appropriate.
 * @since 2.0.0
 */
typealias Response = ServerResponse