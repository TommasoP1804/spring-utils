package dev.tommasop1804.springutils.response

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@ResponseStatus(HttpStatus.NON_AUTHORITATIVE_INFORMATION)
@Suppress("unused")
annotation class NonAuthoritativeInformation