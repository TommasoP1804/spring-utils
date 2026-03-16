package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.UNAUTHORIZED)
@Suppress("unused")
class UnauthorizedException : ResponseException {
    constructor() : super(HttpStatus.UNAUTHORIZED)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.UNAUTHORIZED, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.UNAUTHORIZED, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.UNAUTHORIZED, message, cause, internalErrorCode)
}