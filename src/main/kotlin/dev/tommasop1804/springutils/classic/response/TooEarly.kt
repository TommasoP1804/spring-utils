package dev.tommasop1804.springutils.classic.response

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@ResponseStatus(HttpStatus.TOO_EARLY)
@Suppress("unused")
annotation class TooEarly