/*
 * Copyright © 2026 Tommaso Pastorelli | spring-utils
 */

package dev.tommasop1804.springutils.servlet.response

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@ResponseStatus(HttpStatus.MULTIPLE_CHOICES)
@Suppress("unused")
annotation class MultipleChoices