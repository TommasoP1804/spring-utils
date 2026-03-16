package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
@Suppress("unused")
class GatewayTimeoutException : ResponseException {
    constructor() : super(HttpStatus.GATEWAY_TIMEOUT)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.GATEWAY_TIMEOUT, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.GATEWAY_TIMEOUT, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.GATEWAY_TIMEOUT, message, cause, internalErrorCode)
}