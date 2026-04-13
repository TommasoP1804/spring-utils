/*
 * Copyright © 2026 Tommaso Pastorelli (TommasoP1804) | Spring-Utils
 */

@file:JvmName("SecurityUtilsKt")
@file:Suppress("unused")

package dev.tommasop1804.springutils.reactive.security

import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder

/**
 * Retrieves the current authentication object from the reactive security context.
 *
 * This function is used for accessing the security context information associated
 * with the current reactive execution context. It suspends while awaiting the security
 * context and returns the authentication details if available. If no authentication
 * information is present, it returns null.
 *
 * @return The current authentication object, or null if no authentication information is available.
 * @since 2.0.0
 */
suspend fun authentication() = ReactiveSecurityContextHolder.getContext().awaitSingleOrNull()?.authentication

/**
 * Retrieves the username of the currently authenticated user.
 *
 * This function suspends to fetch the security context and obtains the
 * `Authentication` object from it. If the authentication data is available,
 * it extracts and returns the `name` property of the `Authentication` object.
 *
 * @return The username of the authenticated user, or `null` if no user is authenticated.
 * @since 2.0.0
 */
suspend fun username() = authentication()?.name

/**
 * Retrieves the set of authority strings associated with the current authentication context.
 *
 * This method fetches the authentication object from the security context, extracts the list
 * of granted authorities, maps them to their string representation, and returns a set of
 * those authority strings. If the authentication context or authorities are null, an empty
 * set is returned.
 *
 * @return A set of authority strings from the current authentication context, or an empty
 * set if there are no authorities or the context is unavailable.
 * @since 2.0.0
 */
suspend fun authorities() = authentication()?.authorities?.mapNotNull { obj: GrantedAuthority? -> obj?.authority }.orEmpty().toSet()

/**
 * Retrieves the current user's principal from the authentication context.
 *
 * This function suspends to fetch the authentication information from the security context
 * and returns the associated principal object, if available.
 *
 * @return The principal of the currently authenticated user, or null if no authentication is present.
 * @since 2.0.0
 */
suspend fun user() = authentication()?.principal