package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
@Suppress("unused")
class UnprocessableContentException : ResponseException {
    constructor() : super(HttpStatus.UNPROCESSABLE_CONTENT)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.UNPROCESSABLE_CONTENT, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.UNPROCESSABLE_CONTENT, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.UNPROCESSABLE_CONTENT, message, cause, internalErrorCode)
}