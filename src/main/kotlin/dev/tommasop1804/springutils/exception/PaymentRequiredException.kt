package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
@Suppress("unused")
class PaymentRequiredException : ResponseException {
    constructor() : super(HttpStatus.PAYMENT_REQUIRED)
    constructor(message: String?, internalErrorCode: String? = null) : super(HttpStatus.PAYMENT_REQUIRED, message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.PAYMENT_REQUIRED, cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(HttpStatus.PAYMENT_REQUIRED, message, cause, internalErrorCode)
}