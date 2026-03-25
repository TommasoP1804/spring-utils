@file:JvmName("SecurityUtilsKt")
@file:Suppress("unused", "kutils_null_check")

package dev.tommasop1804.springutils.servlet.security

import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.classes.security.*
import dev.tommasop1804.kutils.classes.web.HttpHeader.Companion.AUTHORIZATION
import dev.tommasop1804.springutils.exception.*
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * Represents the current authentication object associated with the security context.
 *
 * This property retrieves the authentication details from the `SecurityContextHolder`.
 * It may contain information about the currently authenticated user or principal.
 * If there is no authenticated user or the security context is empty, this property will return null.
 *
 * @since 1.0.0
 */
val authentication: Authentication?
    get() = SecurityContextHolder.getContext().authentication

/**
 * Represents the username of the currently authenticated user.
 *
 * The value is retrieved from the security context and reflects the name associated
 * with the current authentication. It may be null if no user is authenticated or
 * if the authentication context is not properly set.
 * @since 1.0.0
 */
val username: String?
    get() = SecurityContextHolder.getContext().authentication?.name

/**
 * Represents a set of authorities associated with the currently authenticated user.
 *
 * Each authority is mapped to its string representation and
 * returned as an immutable set of strings.
 *
 * This property dynamically retrieves the authorities from the active
 * authentication object stored in the security context.
 * @since 1.0.0
 */
val authorities: StringSet
    get() {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.authorities?.mapNotNull { obj: GrantedAuthority? -> obj?.authority }.orEmpty().toSet()
    }

/**
 * Represents the currently authenticated user in the security context.
 * The value is retrieved from the security context's authentication principal.
 * It can be null if there is no authenticated user in the current context.
 * @since 1.0.0
 */
val user: Any?
    get() = SecurityContextHolder.getContext().authentication?.principal

/**
 * Extracts a JWT token from the HTTP request header, optionally throws an exception if the token is missing.
 *
 * @param headerName The name of the HTTP header from which the token should be extracted. Defaults to `AUTHORIZATION`.
 * @param lazyException A supplier that provides a throwable to be thrown if the token is missing in the header.
 * @return The extracted JWT object, or null if the token is missing and `throwException` is null.
 * @since 2.2.6
 */
fun token(headerName: String = AUTHORIZATION, lazyException: ThrowableSupplier = { UnauthorizedException("Missing JWT token") }) =
    (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes)
        .request
        .getHeader(headerName)
        .requireNotNullOrThrow(lazyException)
        .let(::Jwt)

/**
 * Extracts and processes a JWT token from the HTTP request header, if available.
 * If the token is not valid or missing, the method returns null instead of throwing an exception.
 *
 * @param headerName the name of the HTTP header that contains the JWT token. Defaults to the "Authorization" header.
 * @since 2.7.2
 */
fun tokenOrNull(headerName: String = AUTHORIZATION) =
    (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes)
        .request
        .getHeader(headerName)
        ?.let { tryOrNull { Jwt(it) } }