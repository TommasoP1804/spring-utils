/*
 * Copyright © 2026 Tommaso Pastorelli (TommasoP1804) | Spring-Utils
 */

package dev.tommasop1804.springutils.response

import dev.tommasop1804.kutils.classes.web.*
import dev.tommasop1804.springutils.*

/**
 * Represents the result of a resource operation containing metadata about the operation's outcome.
 *
 * @property reference The reference related to the resource (if XML -> the href URI).
 * @property statusCode The HTTP status code representing the outcome of the operation.
 * @property description Optional textual description providing additional details about the operation result.
 * @since 3.7.2
 */
@Suppress("unused")
data class ResourceResult(
    val reference: Any,
    val statusCode: HttpStatus,
    val description: String? = null
) {
    constructor(
        reference: Any,
        statusCode: org.springframework.http.HttpStatus,
        description: String? = null
    ) : this(reference, statusCode.toKutilsHttpStatus(), description)
}