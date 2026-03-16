package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
@Suppress("unused")
class UnsupportedMediaTypeException : ResponseException {
    constructor() : super(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message, cause, internalErrorCode)
}