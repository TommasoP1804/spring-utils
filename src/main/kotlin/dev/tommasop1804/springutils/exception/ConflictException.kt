package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.CONFLICT)
@Suppress("unused")
class ConflictException : ResponseException {
    constructor() : super()
    constructor(message: String?, internalErrorCode: String? = null) : super(message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(message, cause, internalErrorCode)
}