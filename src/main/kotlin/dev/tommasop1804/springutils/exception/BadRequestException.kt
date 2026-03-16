package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
@Suppress("unused")
class BadRequestException : ResponseException {
    constructor() : super(HttpStatus.BAD_REQUEST)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.BAD_REQUEST, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.BAD_REQUEST, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.BAD_REQUEST, message, cause, internalErrorCode)
}