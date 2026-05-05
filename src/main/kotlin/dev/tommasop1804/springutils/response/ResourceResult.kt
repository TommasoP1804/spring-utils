/*
 * Copyright © 2026 Tommaso Pastorelli (TommasoP1804) | Spring-Utils
 */

package dev.tommasop1804.springutils.response

import com.fasterxml.jackson.annotation.JsonInclude
import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.annotations.*
import dev.tommasop1804.kutils.classes.web.*
import dev.tommasop1804.springutils.*

/**
 * Represents the outcome of an operation associated with a resource, including its status and optional metadata.
 *
 * @property reference The unique reference or identifier related to the resource.
 * @property statusCode The HTTP status associated with the operation result.
 * @property properties Optional metadata or additional properties describing the resource or operation outcome.
 * @since 3.7.2
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Suppress("unused")
data class ResourceResult(
    val reference: Any,
    val statusCode: HttpStatus,
    @property:Since("3.7.8") val properties: DataMap? = null
) {
    constructor(
        reference: Any,
        statusCode: org.springframework.http.HttpStatus,
        properties: DataMap? = null
    ) : this(reference, statusCode.toKutilsHttpStatus(), properties)
}