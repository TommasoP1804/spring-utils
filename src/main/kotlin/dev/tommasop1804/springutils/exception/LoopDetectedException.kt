package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.LOOP_DETECTED)
@Suppress("unused")
class LoopDetectedException : ResponseException {
    constructor() : super(HttpStatus.LOOP_DETECTED)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.LOOP_DETECTED, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.LOOP_DETECTED, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.LOOP_DETECTED, message, cause, internalErrorCode)
}