/*
 * Copyright © 2026 Tommaso Pastorelli (TommasoP1804) | Spring-Utils
 */

package dev.tommasop1804.springutils.exception

import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.classes.web.*

abstract class ResponseException : RuntimeException {
    val internalErrorCode: String?
        get() = message?.before(" @@@ ")?.ifBlank { null }
    abstract val status: HttpStatus

    constructor() : super()
    constructor(message: String?, internalErrorCode: String? = null) : super((if (internalErrorCode.isNotNullOrBlank()) internalErrorCode.plus(" @@@ ") else String.EMPTY) + if (message.isNotNullOrBlank()) message else String.EMPTY)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(if (internalErrorCode.isNotNullOrBlank()) internalErrorCode.plus(" @@@ ") else String.EMPTY, cause)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super((if (internalErrorCode.isNotNullOrBlank()) internalErrorCode.plus(" @@@ ") else String.EMPTY) + if (message.isNotNullOrBlank()) message else String.EMPTY, cause)
}