/*
 * Copyright © 2026 Tommaso Pastorelli (TommasoP1804) | Spring-Utils
 */

package dev.tommasop1804.springutils.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.MISDIRECTED_REQUEST)
@Suppress("unused")
class MisdirectedRequestException : ResponseException {
        constructor() : super()
        constructor(message: String?, internalErrorCode: String? = null) : super(message, internalErrorCode)
        constructor(cause: Throwable?, internalErrorCode: String? = null) : super(cause, internalErrorCode)
        constructor(message: String?, cause: Throwable?, internalErrorCode: String? = null) : super(message, cause, internalErrorCode)
}