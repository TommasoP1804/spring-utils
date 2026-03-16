package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
@Suppress("unused")
class RangeNotSatisfiableException : ResponseException {
    constructor() : super(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE, message, cause, internalErrorCode)
}