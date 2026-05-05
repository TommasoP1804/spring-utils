/*
 * Copyright © 2026 Tommaso Pastorelli (TommasoP1804) | Spring-Utils
 */

package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
@Suppress("unused")
class NotAcceptableException : ResponseException {
    override val status = dev.tommasop1804.kutils.classes.web.HttpStatus.NOT_ACCEPTABLE

    constructor() : super()
    constructor(message: String?, internalErrorCode: String? = null) : super(message, internalErrorCode)
    constructor(cause: Throwable?, internalErrorCode: String? = null) : super(cause, internalErrorCode)
    constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(message, cause, internalErrorCode)
}