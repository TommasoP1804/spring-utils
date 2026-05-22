/*
 * Copyright © 2026 Tommaso Pastorelli | spring-utils
 */

package dev.tommasop1804.springutils.servlet.function

import dev.tommasop1804.kutils.*
import dev.tommasop1804.springutils.exception.*
import dev.tommasop1804.springutils.servlet.function.request.*
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.ConverterNotFoundException
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.paramOrNull

/**
 * Abstract class providing enhanced handling of query parameters and path variables
 * for HTTP requests in a type-safe and conversion-aware manner.
 *
 * This class leverages a `ConversionService` to convert input values retrieved
 * from a `ServerRequest` into specific types, throwing exceptions for invalid or
 * malformed inputs. It provides utility functions to handle required, nullable,
 * and default values for both query parameters and path variables.
 *
 * @constructor Initializes an instance of `EnhancedHandler` with the provided `ConversionService`.
 * @property conversionService The conversion service used to transform inputs to the required types.
 * @since 3.10.0
 * @author Tommaso Pastorelli
 */
@Suppress("unused")
abstract class EnhancedHandler {
    protected abstract val conversionService: ConversionService

    /**
     * Retrieves the query parameter with the specified name from the ServerRequest, converts it to the expected type [T],
     * and returns it. If the parameter is missing or cannot be converted, an exception is thrown.
     *
     * @param T The expected type of the query parameter.
     * @param name The name of the query parameter to retrieve.
     * @throws RequiredQueryParamException If the query parameter is not present in the request.
     * @throws MalformedQueryParamException If the query parameter is present but cannot be converted to the expected type [T].
     * @throws ConverterNotFoundException if no suitable converter is found for the specified type [T].
     * @since 3.10.0
     */
    protected inline fun <reified T : Any> ServerRequest.paramOrThrowAs(name: String) = paramOrThrow(name).let {
        tryOrThrow({ -> MalformedQueryParamException(name, T::class) }, includeCause = false, notOverwrite = ConverterNotFoundException::class) {
            conversionService.convert(it, T::class.java)!!
        }
    }

    /**
     * Attempts to retrieve the value of a query parameter with the specified name from the `ServerRequest`
     * and converts it to the specified type, returning `null` if the parameter is not found or is invalid.
     *
     * @param T The target type to which the parameter value will be converted. Must be a class type.
     * @param name The name of the query parameter to retrieve.
     * @throws MalformedQueryParamException If the parameter value cannot be converted to the specified type.
     * @throws ConverterNotFoundException if no suitable converter is found for the specified type [T].
     * @return The converted parameter value, or `null` if the parameter is not present.
     * @since 3.10.0
     */
    protected inline fun <reified T : Any> ServerRequest.paramOrNullAs(name: String) = paramOrNull(name).let {
        tryOrThrow({ -> MalformedQueryParamException(name, T::class) }, includeCause = false, notOverwrite = ConverterNotFoundException::class) {
            conversionService.convert(it, T::class.java)
        }
    }

    /**
     * Retrieves the value of a query parameter from the server request, attempts to convert it to the specified type,
     * and provides a fallback default value if the parameter is not present or cannot be converted.
     *
     * @param name The name of the query parameter to retrieve.
     * @param defaultValue A supplier function that provides the default value if the parameter is absent or invalid.
     * @throws MalformedQueryParamException If the query parameter exists but cannot be converted to the required type.
     * @throws ConverterNotFoundException if no suitable converter is found for the specified type [T].
     * @return The value of the query parameter converted to the specified type, or the default value provided by the supplier.
     * @since 3.10.0
     */
    protected inline fun <reified T : Any> ServerRequest.paramOrDefaultAs(name: String, defaultValue: Supplier<T>) = paramOrNull(name).let {
        tryOrThrow({ -> MalformedQueryParamException(name, T::class) }, includeCause = false, notOverwrite = ConverterNotFoundException::class) {
            conversionService.convert(it, T::class.java)
        } ?: defaultValue()
    }

    /**
     * Attempts to extract a path variable from the `ServerRequest` and convert it to the specified type [T],
     * throwing an appropriate exception if the conversion fails or if the variable is absent.
     *
     * @param name The name of the path variable to retrieve and convert.
     * @throws MalformedPathVariableException If the path variable cannot be converted to the specified type [T].
     * @throws RequiredPathVariableException If the path variable is not present in the request.
     * @throws ConverterNotFoundException if no suitable converter is found for the specified type [T].
     * @return The converted path variable of type [T].
     * @since 3.10.0
     */
    protected inline fun <reified T : Any> ServerRequest.pathVariableOrThrowAs(name: String) = pathVariableOrThrow(name).let {
        tryOrThrow({ -> MalformedPathVariableException(name, T::class) }, includeCause = false, notOverwrite = ConverterNotFoundException::class) {
            conversionService.convert(it, T::class.java)!!
        }
    }

    /**
     * Attempts to retrieve a path variable by its name from the server request, convert it to a specified type `T`,
     * and return the result. If the path variable is not found or the conversion fails, returns null.
     *
     * @param name the name of the path variable to retrieve and convert
     * @return the converted value of the path variable, or null if the variable is not found or conversion fails
     * @throws MalformedPathVariableException if the conversion fails due to an invalid path variable
     * or when the value retrieved is incompatible with the specified type `T`
     * @throws ConverterNotFoundException if no suitable converter is found for the specified type [T].
     * @since 3.10.0
     */
    protected inline fun <reified T : Any> ServerRequest.pathVariableOrNullAs(name: String) = pathVariableOrNull(name).let {
        tryOrThrow({ -> MalformedPathVariableException(name, T::class) }, includeCause = false, notOverwrite = ConverterNotFoundException::class) {
            conversionService.convert(it, T::class.java)
        }
    }

    /**
     * Safely retrieves a path variable from the `ServerRequest` and attempts to convert it to the specified type `T`.
     * If the conversion fails or the path variable is not found, the provided `defaultValue` is returned.
     *
     * @param name the name of the path variable to retrieve from the request
     * @param defaultValue a supplier that provides a default value of type `T` if the path variable is null or invalid
     * @return the converted value of type `T` or the default value provided by the `defaultValue` supplier
     * @throws MalformedPathVariableException if the path variable cannot be converted to the specified type `T`
     * @throws ConverterNotFoundException if no suitable converter is found for the specified type [T].
     * @since 3.10.0
     */
    protected inline fun <reified T : Any> ServerRequest.pathVariableOrDefaultAs(name: String, defaultValue: Supplier<T>) = pathVariableOrNull(name).let {
        tryOrThrow({ -> MalformedPathVariableException(name, T::class) }, includeCause = false, notOverwrite = ConverterNotFoundException::class) {
            conversionService.convert(it, T::class.java)
        } ?: defaultValue()
    }
}