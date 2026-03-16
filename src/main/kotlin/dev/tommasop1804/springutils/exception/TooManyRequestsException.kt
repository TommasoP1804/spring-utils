package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
@Suppress("unused")
class TooManyRequestsException : ResponseException {
    constructor() : super(HttpStatus.TOO_MANY_REQUESTS)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.TOO_MANY_REQUESTS, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.TOO_MANY_REQUESTS, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.TOO_MANY_REQUESTS, message, cause, internalErrorCode)
}