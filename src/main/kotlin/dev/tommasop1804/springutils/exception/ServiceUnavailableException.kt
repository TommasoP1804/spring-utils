package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
@Suppress("unused")
class ServiceUnavailableException : ResponseException {
    constructor() : super(HttpStatus.SERVICE_UNAVAILABLE)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.SERVICE_UNAVAILABLE, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.SERVICE_UNAVAILABLE, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.SERVICE_UNAVAILABLE, message, cause, internalErrorCode)
}