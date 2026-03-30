/*
 * Copyright © 2026 Tommaso Pastorelli | spring-utils
 */

package dev.tommasop1804.springutils.servlet.response

import dev.tommasop1804.kutils.classes.web.HttpStatus
import dev.tommasop1804.springutils.toKutilsHttpStatus

/**
 * Represents the result of a resource operation containing metadata about the operation's outcome.
 *
 * @property reference The reference related to the resource (if XML -> the href URI).
 * @property statusCode The HTTP status code representing the outcome of the operation.
 * @property description Optional textual description providing additional details about the operation result.
 * @since 1.0.0
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