package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_GATEWAY)
@Suppress("unused")
class BadGatewayException : ResponseException {
    constructor() : super(HttpStatus.BAD_GATEWAY)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.BAD_GATEWAY, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.BAD_GATEWAY, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.BAD_GATEWAY, message, cause, internalErrorCode)
}