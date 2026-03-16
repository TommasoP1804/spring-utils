package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS)
@Suppress("unused")
class UnavailableForLegalReasonsException : ResponseException {
    constructor() : super(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS, message, cause, internalErrorCode)
}