package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.FORBIDDEN)
@Suppress("unused")
class ForbiddenException : ResponseException {
    constructor() : super(HttpStatus.FORBIDDEN)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.FORBIDDEN, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.FORBIDDEN, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.FORBIDDEN, message, cause, internalErrorCode)
}