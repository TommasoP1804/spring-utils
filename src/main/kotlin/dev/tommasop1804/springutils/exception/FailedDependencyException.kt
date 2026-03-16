package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.FAILED_DEPENDENCY)
@Suppress("unused")
class FailedDependencyException : ResponseException {
    constructor() : super(HttpStatus.FAILED_DEPENDENCY)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.FAILED_DEPENDENCY, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.FAILED_DEPENDENCY, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.FAILED_DEPENDENCY, message, cause, internalErrorCode)
}