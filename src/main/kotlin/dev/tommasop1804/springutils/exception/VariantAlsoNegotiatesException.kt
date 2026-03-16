package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.VARIANT_ALSO_NEGOTIATES)
@Suppress("unused")
class VariantAlsoNegotiatesException : ResponseException {
    constructor() : super(HttpStatus.VARIANT_ALSO_NEGOTIATES)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.VARIANT_ALSO_NEGOTIATES, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.VARIANT_ALSO_NEGOTIATES, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.VARIANT_ALSO_NEGOTIATES, message, cause, internalErrorCode)
}