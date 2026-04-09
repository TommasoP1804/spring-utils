/*
 * Copyright © 2026 Tommaso Pastorelli (TommasoP1804) | Spring-Utils
 */

package dev.tommasop1804.springutils.servlet.response

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@ResponseStatus(HttpStatus.PERMANENT_REDIRECT)
@Suppress("unused")
annotation class PermanentRedirect