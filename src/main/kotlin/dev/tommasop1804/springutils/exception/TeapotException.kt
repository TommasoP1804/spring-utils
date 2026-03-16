package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.I_AM_A_TEAPOT)
@Suppress("unused")
class TeapotException : ResponseException {
    constructor() : super(HttpStatus.I_AM_A_TEAPOT)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.I_AM_A_TEAPOT, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.I_AM_A_TEAPOT, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.I_AM_A_TEAPOT, message, cause, internalErrorCode)
}