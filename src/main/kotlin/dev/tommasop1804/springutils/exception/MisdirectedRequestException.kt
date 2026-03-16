package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

    @ResponseStatus(HttpStatus.MISDIRECTED_REQUEST)
@Suppress("unused")
class MisdirectedRequestException : ResponseException {
        constructor() : super(HttpStatus.MISDIRECTED_REQUEST)
        constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.MISDIRECTED_REQUEST, message, internalErrorCode)
        constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.MISDIRECTED_REQUEST, cause, internalErrorCode)
        constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.MISDIRECTED_REQUEST, message, cause, internalErrorCode)
}