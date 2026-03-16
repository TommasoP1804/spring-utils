package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.PRECONDITION_FAILED)
@Suppress("unused")
class PreconditionFailedException : ResponseException {
    constructor() : super(HttpStatus.PRECONDITION_FAILED)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.PRECONDITION_FAILED, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.PRECONDITION_FAILED, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.PRECONDITION_FAILED, message, cause, internalErrorCode)
}