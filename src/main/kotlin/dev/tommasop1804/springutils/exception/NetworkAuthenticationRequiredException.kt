package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NETWORK_AUTHENTICATION_REQUIRED)
@Suppress("unused")
class NetworkAuthenticationRequiredException : ResponseException {
    constructor() : super(HttpStatus.NETWORK_AUTHENTICATION_REQUIRED)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.NETWORK_AUTHENTICATION_REQUIRED, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.NETWORK_AUTHENTICATION_REQUIRED, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.NETWORK_AUTHENTICATION_REQUIRED, message, cause, internalErrorCode)
}