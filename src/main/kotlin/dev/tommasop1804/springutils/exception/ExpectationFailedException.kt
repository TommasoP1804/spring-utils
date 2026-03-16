package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.EXPECTATION_FAILED)
@Suppress("unused")
class ExpectationFailedException : ResponseException {
    constructor() : super(HttpStatus.EXPECTATION_FAILED)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.EXPECTATION_FAILED, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.EXPECTATION_FAILED, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.EXPECTATION_FAILED, message, cause, internalErrorCode)
}