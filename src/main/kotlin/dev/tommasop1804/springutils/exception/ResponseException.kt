package dev.tommasop1804.springutils.exception

import dev.tommasop1804.kutils.EMPTY
import dev.tommasop1804.kutils.before

abstract class ResponseException : RuntimeException {
    val internalErrorCode: String?
        get() = message?.before(" @@@ ")?.ifBlank { null }

    constructor() : super()
    constructor(message: String?, internalErrorCode: String? = null) : super((internalErrorCode?.plus(" @@@ ") ?: String.EMPTY) + message)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(internalErrorCode?.plus(" @@@ "), cause)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super((internalErrorCode?.plus(" @@@ ") ?: String.EMPTY) + message, cause)
}