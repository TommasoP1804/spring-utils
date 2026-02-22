@file:JvmName("ReactiveRequestUtilsKt")
@file:Since("2.0.0")
@file:Suppress("unused")

package dev.tommasop1804.springutils.reactive.request

import dev.tommasop1804.kutils.Supplier
import dev.tommasop1804.kutils.ThrowableSupplier
import dev.tommasop1804.kutils.annotations.Since
import dev.tommasop1804.kutils.exceptions.RequiredParameterException
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.queryParamOrNull

/**
 * Retrieves the value of the specified query parameter from the server request.
 * If the query parameter is not present, throws the exception provided by the lazyException supplier.
 *
 * @param name The name of the query parameter to retrieve.
 * @param lazyException A lambda returning the exception to be thrown if the query parameter is missing.
 * Defaults to throwing a RequiredParameterException indicating the missing parameter.
 * @return The value of the query parameter if present.
 * @throws Throwable The exception supplied by lazyException when the query parameter is not present.
 * @since 2.0.0
 */
fun ServerRequest.queryParamOrThrow(
    name: String,
    lazyException: ThrowableSupplier = { RequiredParameterException("Missing query parameter '$name'") }
) = queryParamOrNull(name) ?: throw lazyException()

/**
 * Retrieves the value of a query parameter by its name. If the query parameter
 * is not present or is null, the value provided by the defaultValue supplier
 * is returned instead.
 *
 * @param name the name of the query parameter to retrieve
 * @param defaultValue a supplier that provides a default value if the query parameter is not present
 * @return the value of the query parameter if it exists; otherwise, the value provided by the defaultValue supplier
 * @since 2.0.0
 */
fun ServerRequest.queryParamOrDefault(
    name: String,
    defaultValue: Supplier<String>
) = queryParamOrNull(name) ?: defaultValue()