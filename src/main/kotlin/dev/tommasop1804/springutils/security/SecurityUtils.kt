/*
 * Copyright © 2026 Tommaso Pastorelli (TommasoP1804) | Spring-Utils
 */

@file:JvmName("SecurityUtilsKt")
@file:Since("4.6.0")
@file:Suppress("unused")
@file:MustUseReturnValues

package dev.tommasop1804.springutils.security

import dev.tommasop1804.kutils.annotations.*
import dev.tommasop1804.kutils.classes.web.*
import org.springframework.security.web.firewall.StrictHttpFirewall

/**
 * Sets the allowed HTTP methods for the strict HTTP firewall.
 *
 * This function configures the HTTP methods that are permitted by the firewall,
 * ensuring that requests with only these methods are accepted. Any request using
 * an HTTP method not included in this list will be rejected.
 *
 * @param methods A vararg of HttpMethod instances representing the allowed HTTP methods.
 * @since 4.6.0
 */
fun StrictHttpFirewall.setAllowedHttpMethods(vararg methods: HttpMethod) =
    setAllowedHttpMethods(methods.map(HttpMethod::value))
/**
 * Sets the allowed HTTP methods for the strict HTTP firewall.
 *
 * @param methods The iterable collection of HttpMethod enums that represent
 *                the HTTP methods to be allowed.
 * @since 4.6.0
 */
fun StrictHttpFirewall.setAllowedHttpMethods(methods: Iterable<HttpMethod>) =
    setAllowedHttpMethods(methods.map(HttpMethod::value))