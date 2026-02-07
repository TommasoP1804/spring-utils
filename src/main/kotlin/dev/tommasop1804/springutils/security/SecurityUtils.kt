@file:JvmName("SecurityUtilsKt")
@file:Suppress("unused")

package dev.tommasop1804.springutils.security

import dev.tommasop1804.kutils.StringSet
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder

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