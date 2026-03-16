package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_EXTENDED)
@Suppress("unused")
class NotExtendedException : ResponseException {
    constructor() : super(HttpStatus.NOT_EXTENDED)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.NOT_EXTENDED, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.NOT_EXTENDED, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.NOT_EXTENDED, message, cause, internalErrorCode)
}