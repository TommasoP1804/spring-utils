package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
@Suppress("unused")
class NotFoundException : ResponseException {
    constructor() : super(HttpStatus.NOT_FOUND)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.NOT_FOUND, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.NOT_FOUND, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.NOT_FOUND, message, cause, internalErrorCode)
}