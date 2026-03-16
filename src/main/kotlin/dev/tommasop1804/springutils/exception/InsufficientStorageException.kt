package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.INSUFFICIENT_STORAGE)
@Suppress("unused")
class InsufficientStorageException : ResponseException {
    constructor() : super(HttpStatus.INSUFFICIENT_STORAGE)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.INSUFFICIENT_STORAGE, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.INSUFFICIENT_STORAGE, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.INSUFFICIENT_STORAGE, message, cause, internalErrorCode)
}