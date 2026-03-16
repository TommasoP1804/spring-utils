package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
@Suppress("unused")
class NotAcceptableException : ResponseException {
    constructor() : super(HttpStatus.NOT_ACCEPTABLE)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.NOT_ACCEPTABLE, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.NOT_ACCEPTABLE, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.NOT_ACCEPTABLE, message, cause, internalErrorCode)
}