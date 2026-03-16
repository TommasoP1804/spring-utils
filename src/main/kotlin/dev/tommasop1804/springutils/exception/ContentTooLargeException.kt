package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.CONTENT_TOO_LARGE)
@Suppress("unused")
class ContentTooLargeException : ResponseException {
    constructor() : super(HttpStatus.CONTENT_TOO_LARGE)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.CONTENT_TOO_LARGE, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.CONTENT_TOO_LARGE, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.CONTENT_TOO_LARGE, message, cause, internalErrorCode)
}