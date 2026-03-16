package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.LENGTH_REQUIRED)
@Suppress("unused")
class LengthRequiredException : ResponseException {
    constructor() : super(HttpStatus.LENGTH_REQUIRED)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.LENGTH_REQUIRED, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.LENGTH_REQUIRED, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.LENGTH_REQUIRED, message, cause, internalErrorCode)
}