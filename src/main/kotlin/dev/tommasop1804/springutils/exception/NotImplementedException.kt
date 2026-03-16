package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
@Suppress("unused")
class NotImplementedException : ResponseException {
    constructor() : super(HttpStatus.NOT_IMPLEMENTED)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.NOT_IMPLEMENTED, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.NOT_IMPLEMENTED, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.NOT_IMPLEMENTED, message, cause, internalErrorCode)
}