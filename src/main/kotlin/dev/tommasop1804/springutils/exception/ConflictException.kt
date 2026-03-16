package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.CONFLICT)
@Suppress("unused")
class ConflictException : ResponseException {
    constructor() : super(HttpStatus.CONFLICT)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.CONFLICT, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.CONFLICT, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.CONFLICT, message, cause, internalErrorCode)
}