package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.REQUEST_TIMEOUT)
@Suppress("unused")
class RequestTimeoutException : ResponseException {
    constructor() : super(HttpStatus.REQUEST_TIMEOUT)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.REQUEST_TIMEOUT, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.REQUEST_TIMEOUT, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.REQUEST_TIMEOUT, message, cause, internalErrorCode)
}