package dev.tommasop1804.springutils.exception

import dev.tommasop1804.kutils.EMPTY
import dev.tommasop1804.kutils.before
import dev.tommasop1804.kutils.isNotNull
import dev.tommasop1804.kutils.isNotNullOrBlank
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

abstract class ResponseException : ResponseStatusException {
    val internalErrorCode: String?
        get() = reason?.before(" @@@ ")?.ifBlank { null }

    constructor(status: HttpStatus) : super(status)
    constructor(status: HttpStatus, message: String?, internalErrorCode: String? = null) : super(status, (if (internalErrorCode.isNotNullOrBlank()) internalErrorCode.plus(" @@@ ") else String.EMPTY) + if (message.isNotNullOrBlank()) message else String.EMPTY)
    constructor(status: HttpStatus, cause: Throwable?, internalErrorCode: String? = null) : super(status, if (internalErrorCode.isNotNullOrBlank()) internalErrorCode.plus(" @@@ ") else String.EMPTY, cause)
    constructor(status: HttpStatus, message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(status, (if (internalErrorCode.isNotNullOrBlank()) internalErrorCode.plus(" @@@ ") else String.EMPTY) + if (message.isNotNullOrBlank()) message else String.EMPTY, cause)
}