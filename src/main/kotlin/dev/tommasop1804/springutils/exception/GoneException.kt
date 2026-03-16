package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.GONE)
@Suppress("unused")
class GoneException : ResponseException {
    constructor() : super(HttpStatus.GONE)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.GONE, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.GONE, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.GONE, message, cause, internalErrorCode)
}