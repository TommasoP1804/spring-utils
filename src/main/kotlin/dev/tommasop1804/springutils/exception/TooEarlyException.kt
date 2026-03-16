package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.TOO_EARLY)
@Suppress("unused")
class TooEarlyException : ResponseException {
    constructor() : super(HttpStatus.TOO_EARLY)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.TOO_EARLY, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.TOO_EARLY, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.TOO_EARLY, message, cause, internalErrorCode)
}