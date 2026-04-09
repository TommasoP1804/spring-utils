/*
 * Copyright © 2026 Tommaso Pastorelli (TommasoP1804) | Spring-Utils
 */

@file:JvmName("AliasesKt")
@file:Since("3.0.0")
@file:Suppress("unused")

package dev.tommasop1804.springutils.servlet.function

import dev.tommasop1804.kutils.annotations.*
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.ServerResponse

/**
 * A type alias for the ServerRequest class, used to simplify and enhance code readability
 * when working with server-side request handling.
 * @since 3.0.0
 */
typealias Request = ServerRequest
/**
 * A typealias for mapping the `ServerResponse` type to a simpler name `Response`.
 * This can be used to enhance readability and simplify references to the `ServerResponse` type.
 * @since 3.0.0
 */
typealias Response = ServerResponse