/*
 * Copyright © 2026 Tommaso Pastorelli | spring-utils
 */

@file:JvmName("ServletRequestUtilsKt")
@file:Since("3.0.0")
@file:Suppress("unused", "FunctionName", "FunctionName", "UNCHECKED_CAST", "MoveLambdaOutsideParentheses")

package dev.tommasop1804.springutils.servlet.function.request

import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.annotations.*
import dev.tommasop1804.kutils.classes.identifiers.*
import dev.tommasop1804.kutils.classes.identifiers.Cuid.Companion.toCuid
import dev.tommasop1804.kutils.classes.identifiers.Ksuid.Companion.toKsuid
import dev.tommasop1804.kutils.classes.identifiers.Tsid.Companion.toTsid
import dev.tommasop1804.kutils.classes.identifiers.Ulid.Companion.toUlid
import dev.tommasop1804.kutils.classes.measure.*
import dev.tommasop1804.kutils.classes.measure.RMeasurement.Companion.ofUnit
import dev.tommasop1804.kutils.classes.security.Jwt.Companion.toJwt
import dev.tommasop1804.kutils.classes.web.*
import dev.tommasop1804.kutils.classes.web.HttpHeader.Companion.headerDateToInstant
import dev.tommasop1804.kutils.exceptions.*
import dev.tommasop1804.springutils.*
import dev.tommasop1804.springutils.exception.*
import dev.tommasop1804.springutils.servlet.request.*
import org.springframework.http.HttpHeaders
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.paramOrNull
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlin.reflect.KClass

/**
 * Retrieves all request parameters from the server request and converts them to a `MultiMap`.
 *
 * This function extracts the parameters from the current `ServerRequest` as a `MultiValueMap`
 * and then converts it to a `MultiMap` using the `toMultiMap` function. The resulting `MultiMap`
 * preserves the key-value mappings and their order from the original parameter collection.
 *
 * @receiver The `ServerRequest` from which parameters are extracted.
 * @return A `MultiMap` containing all request parameters and their associated values.
 * @since 3.0.2
 */
val ServerRequest.params get() = params().toMultiMap()
/**
 * Retrieves all headers from the current `ServerRequest` instance and converts them
 * into a `HttpHeaders` structure compatible with the `kutils` library.
 *
 * This method accesses the headers associated with the request, converts them from
 * `org.springframework.http.HttpHeaders` to the `HttpHeaders` class provided by 
 * the `kutils` library by leveraging the `toKutilsHttpHeaders` extension method.
 *
 * @receiver The `ServerRequest` instance from which to extract the headers.
 * @return A `HttpHeaders` instance containing all headers from the request, converted
 *         to the `kutils` library's format.
 * @since 3.0.2
 */
val ServerRequest.headers get() = headers().asHttpHeaders().toKutilsHttpHeaders()
/**
 * Provides a map of path variables extracted from the current server request.
 *
 * This property retrieves the path variable mappings as a `StringMap`, where the keys represent
 * the variable names and the values represent their corresponding values in the path. It is often
 * used in the context of handling requests with dynamic path segments.
 * @since 3.0.2
 */
val ServerRequest.pathVariables: StringMap get() = pathVariables()

/**
 * Retrieves the value of a query parameter by its name or throws an exception if it is missing.
 *
 * This method attempts to fetch the specified query parameter from the request. If the parameter is not found,
 * it throws a lazily created exception provided by the `lazyException` parameter.
 *
 * @param name The name of the query parameter to retrieve.
 * @param `class` The expected type class of the query parameter, used for informational purposes when the exception is thrown.
 * @param lazyException A supplier function to create the exception to be thrown if the query parameter is missing. Defaults to
 *                      creating a `RequiredQueryParamException` with the provided `name` and `class`.
 * @throws Throwable The exception provided by the `lazyException` supplier, if the query parameter is not found.
 * @return The value of the query parameter if it exists.
 * @since 3.0.0
 */
fun ServerRequest.paramOrThrow(
    name: String,
    `class`: KClass<*> = String::class,
    lazyException: ThrowableSupplier = { RequiredQueryParamException(name, `class`) }
) = paramOrNull(name) ?: throw lazyException()
/**
 * Retrieves the value of a query parameter by its name or throws a RequiredQueryParamException
 * if the parameter is missing.
 *
 * This function is used to ensure that a specific query parameter is present in the request
 * and satisfies the required conditions. If the parameter is missing, a descriptive exception
 * is thrown to indicate the issue.
 *
 * @param name The name of the query parameter to retrieve. It cannot be null or blank.
 * @param class The expected type of the query parameter. Used for constructing the exception message
 * when the parameter is missing.
 * @param internalErrorCode An internal error code that adds context to the thrown exception. Typically
 * used for error classification or logging.
 * @throws RequiredQueryParamException if the query parameter is not present in the request.
 * @since 3.0.0
 */
fun ServerRequest.paramOrThrow(
    name: String,
    `class`: KClass<*> = String::class,
    internalErrorCode: String
) = paramOrNull(name) ?: throw RequiredQueryParamException(name, `class`, internalErrorCode)
/**
 * Retrieves the query parameter value associated with the given name or,
 * if not present, returns a default value provided by the given supplier.
 *
 * @param name the name of the query parameter to retrieve
 * @param defaultValue a supplier that provides a default value if the query parameter is not found
 * @return the value of the query parameter as a string, or the supplied default value if not present
 * @since 3.0.0
 */
fun ServerRequest.paramOrDefault(
    name: String,
    defaultValue: Supplier<Any>
) = paramOrNull(name) ?: defaultValue().toString()

/**
 * Retrieves the specified path variable from the `ServerRequest` or throws an exception if not found or invalid.
 *
 * @param name The name of the path variable to retrieve.
 * @param `class` The expected type of the path variable, used for validation and error reporting.
 * @param lazyException A lambda that supplies the exception to be thrown if the path variable is missing
 *                      or invalid. Defaults to throwing a `RequiredPathVariableException` with details about
 *                      the missing variable.
 * @throws Throwable The exception supplied by `lazyException` if the path variable is missing or invalid.
 * @return The path variable as a string if the retrieval is successful.
 * @since 3.0.0
 */
fun ServerRequest.pathVariableOrThrow(name: String, `class`: KClass<*> = String::class, lazyException: ThrowableSupplier = { RequiredPathVariableException(name, `class`) }) =
    tryOrThrow(lazyException, includeCause = false) { pathVariable(name) }
/**
 * Extracts a required path variable from the server request. If the specified path variable
 * is missing or invalid, a `RequiredPathVariableException` is thrown with details about the
 * missing variable, its expected type, and an optional internal error code. The exception
 * can provide helpful diagnostic information for troubleshooting.
 *
 * @param name The name of the path variable to extract from the server request.
 * @param `class` The Kotlin class representing the expected type of the path variable.
 * @param internalErrorCode An internal error code for contextualizing the exception, appended in the exception message.
 *                          This is especially useful for providing additional diagnostic information.
 * @throws RequiredPathVariableException If the path variable is not present or invalid.
 * @since 3.0.0
 */
fun ServerRequest.pathVariableOrThrow(name: String, `class`: KClass<*> = String::class, internalErrorCode: String) =
    tryOrThrow({ -> RequiredPathVariableException(name, `class`, internalErrorCode) }, includeCause = false) { pathVariable(name) }
/**
 * Retrieves the value of a path variable by the given name from the ServerRequest.
 * If the path variable is not found or an error occurs during retrieval,
 * the default value provided by the supplier is used.
 *
 * @param name The name of the path variable to retrieve.
 * @param defaultValue A supplier providing the default value to use if the path variable is not found or an error occurs.
 * @return The value of the path variable if found, otherwise the default value supplied.
 * @since 3.0.0
 */
fun ServerRequest.pathVariableOrDefault(name: String, defaultValue: Supplier<Any>) =
    tryOr({ defaultValue().toString() }) { pathVariable(name) }
/**
 * Retrieves the value of a path variable with the given name from the server request.
 * If the path variable is not found or an exception occurs, returns null.
 *
 * @param name the name of the path variable to retrieve
 * @return the value of the path variable, or null if not found or an exception occurs
 * @since 3.0.0
 */
fun ServerRequest.pathVariableOrNull(name: String): String? = tryOrNull { pathVariable(name) }

/**
 * Retrieves the value of a specified header from the server request, or throws an exception
 * if the header is not present or its value is empty.
 *
 * @param name the name of the header to be retrieved.
 * @param class the expected type of the header's value.
 * @param lazyException a lambda function that supplies the exception to be thrown if the header
 * is missing or its value is empty. Defaults to producing a `RequiredHeaderException` with the
 * header name and expected type.
 * @throws Throwable if the header is not present or has an empty value, as provided by the
 * `lazyException` parameter.
 * @since 3.0.0
 */
fun ServerRequest.headerOrThrow(name: String, `class`: KClass<*> = StringList::class, lazyException: ThrowableSupplier = { RequiredHeaderException(name, `class`) }): StringList =
    tryOrThrow(lazyException, includeCause = false) { headers().header(name).ifNullOrEmpty { throw NoSuchElementException() } }
/**
 * Retrieves the value of a specific header from the server request or throws an exception if the header
 * is missing or its value is empty.
 *
 * @param name the name of the header to retrieve
 * @param class the expected class type of the header value
 * @param internalErrorCode the internal error code to associate with the exception if the header is missing or invalid
 * @since 3.0.0
 */
fun ServerRequest.headerOrThrow(name: String, `class`: KClass<*> = String::class, internalErrorCode: String): StringList =
    tryOrThrow({ -> RequiredHeaderException(name, `class`, internalErrorCode) }, includeCause = false) { headers().header(name).ifNullOrEmpty { throw NoSuchElementException() } }
/**
 * Retrieves the header values associated with the given header name from the request. If no values are found,
 * a default value provided by the specified supplier is returned as a single-element list.
 *
 * @param name the name of the header to retrieve
 * @param defaultValue a supplier that provides a default value if the header is not present or has no values
 * @return a list of strings containing the header values, or a single-element list with the default value
 * @since 3.0.0
 */
fun ServerRequest.headerOrDefault(
    name: String,
    defaultValue: Supplier<Any>
): StringList = headers().header(name).ifEmpty { defaultValue().toString().asSingleList() }
/**
 * Retrieves the values of the specified header from the server request.
 *
 * @param name the name of the header to retrieve.
 * @return a list of values associated with the specified header, or an empty list if the header is not present.
 * @since 3.0.0
 */
fun ServerRequest.header(name: String): StringList = tryOrNull { headers().header(name) }.orEmpty()

/**
 * Retrieves the specified header from the server request, ensuring it contains only a single value,
 * or throws an exception if the header is not found, its value is empty, or it contains multiple elements.
 *
 * @param name the name of the header to retrieve.
 * @param class the expected type of the header's value. Defaults to `StringList::class`.
 * @param lazyException a lambda function that supplies the exception to be thrown if the header
 * is missing, its value is empty, or it contains multiple elements. Defaults to producing a `RequiredHeaderException`
 * with the header name and expected type.
 * @return the single value of the header as a `String`.
 * @since 3.0.2
 */
fun ServerRequest.headerOrThrowOnlyElement(name: String, `class`: KClass<*> = StringList::class, lazyException: ThrowableSupplier = { RequiredHeaderException(name, `class`) }): String =
    headerOrThrow(name, `class`, lazyException).onlyElement()
/**
 * Retrieves the value of a specific header from the server request and ensures that it contains
 * only a single element. Throws an exception if the header is missing, empty, or contains multiple elements.
 *
 * @param name the name of the header to retrieve
 * @param class the expected class type of the header value
 * @param internalErrorCode the internal error code to associate with the exception if the header is invalid or contains multiple elements
 * @return the single value of the specified header
 * @since 3.0.2
 */
fun ServerRequest.headerOrThrowOnlyElement(name: String, `class`: KClass<*> = String::class, internalErrorCode: String): String =
    headerOrThrow(name, `class`, internalErrorCode).onlyElement()
/**
 * Retrieves the only element of the specified header from the request headers.
 * If the header is not present or contains multiple elements, the provided default value is returned as a string.
 *
 * @param name the name of the header to retrieve
 * @param defaultValue a supplier that provides a default value if the header is absent or contains multiple elements
 * @return the single value of the specified header or the default value as a string
 * @since 3.0.2
 */
fun ServerRequest.headerOrDefaultOnlyElement(
    name: String,
    defaultValue: Supplier<Any>
): String = headers().header(name).onlyElementOrNull() ?: defaultValue().toString()
/**
 * Retrieves the only element from the values of the specified header in the server request.
 *
 * @param name the name of the header whose single value is to be retrieved
 * @return the single value of the specified header
 * @throws IllegalArgumentException if the header contains zero or multiple values
 * @since 3.0.2
 */
fun ServerRequest.headerOnlyElement(name: String): String = headers().header(name).onlyElement()

fun ServerRequest.accept() = header(HttpHeaders.ACCEPT)
fun ServerRequest.acceptCharset() = header(HttpHeaders.ACCEPT_CHARSET)
fun ServerRequest.acceptEncoding() = header(HttpHeaders.ACCEPT_ENCODING)
fun ServerRequest.acceptLanguage() = header(HttpHeaders.ACCEPT_LANGUAGE)
fun ServerRequest.acceptRanges() = header(HttpHeaders.ACCEPT_RANGES)
fun ServerRequest.connection() = header(HttpHeaders.CONNECTION).firstOrNull()?.let(ConnectionBehaviour::of)
fun ServerRequest.contentLength() = header(HttpHeaders.CONTENT_LENGTH).firstOrNull()?.let { it.toInt() ofUnit MeasureUnit.DataSizeUnit.BYTES }
fun ServerRequest.contentType() = header(HttpHeaders.CONTENT_TYPE).firstOrNull()?.let { MediaType.parse(it)() }
fun ServerRequest.expect() = header(HttpHeaders.EXPECT)
fun ServerRequest.fromService() = header("From-Service").firstOrNull()
fun ServerRequest.host() = header(HttpHeaders.HOST)
fun ServerRequest.ifMatch() = header(HttpHeaders.IF_MATCH)
fun ServerRequest.ifModifiedSince() = header(HttpHeaders.IF_MODIFIED_SINCE).firstOrNull()?.headerDateToInstant()
fun ServerRequest.ifNoneMatch() = header(HttpHeaders.IF_NONE_MATCH)
fun ServerRequest.ifRange() = header(HttpHeaders.IF_RANGE)
fun ServerRequest.ifUnmodifiedSince() = header(HttpHeaders.IF_UNMODIFIED_SINCE).firstOrNull()?.headerDateToInstant()
fun ServerRequest.jwtToken(headerName: String = HttpHeaders.AUTHORIZATION) = header(headerName).firstOrNull()?.toJwt()?.getOrThrow()
fun ServerRequest.origin() = header(HttpHeaders.ORIGIN)
fun ServerRequest.prefer() = header("Prefer")
fun ServerRequest.priority() = header("Priority")
fun ServerRequest.range() = header(HttpHeaders.RANGE)
fun ServerRequest.referer() = header(HttpHeaders.REFERER).firstOrNull()?.toUrl()?.getOrThrow()

fun ServerRequest.paramOrThrowAsStringList(name: String) = paramOrThrow(name, StringList::class).splitAndTrim(Char.COMMA)
fun ServerRequest.paramOrThrowAsInt(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, Int::class) }, includeCause = false, notOverwrite = RequiredQueryParamException::class) { paramOrThrow(name, Int::class).toInt() }
fun ServerRequest.paramOrThrowAsLong(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, Long::class) }, includeCause = false, notOverwrite = RequiredQueryParamException::class) { paramOrThrow(name, Long::class).toLong() }
fun ServerRequest.paramOrThrowAsDouble(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, Double::class) }, includeCause = false, notOverwrite = RequiredQueryParamException::class) { paramOrThrow(name, Double::class).toDouble() }
fun ServerRequest.paramOrThrowAsBoolean(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, Boolean::class) }, includeCause = false, notOverwrite = RequiredQueryParamException::class) { paramOrThrow(name, Boolean::class).toBoolean() }
fun ServerRequest.paramOrThrowAsUuid(name: String) = paramOrThrow(name, Uuid::class).toUuid().getOrThrow(lazyException = { MalformedQueryParamException(name, Uuid::class) })
fun ServerRequest.paramOrThrowAsUlid(name: String) = paramOrThrow(name, Ulid::class).toUlid().getOrThrow(lazyException = { MalformedQueryParamException(name, Ulid::class) })
fun ServerRequest.paramOrThrowAsKsuid(name: String) = paramOrThrow(name, Ksuid::class).toKsuid().getOrThrow(lazyException = { MalformedQueryParamException(name, Ksuid::class) })
fun ServerRequest.paramOrThrowAsCuid(name: String) = paramOrThrow(name, Cuid::class).toCuid().getOrThrow(lazyException = { MalformedQueryParamException(name, Cuid::class) })
fun ServerRequest.paramOrThrowAsTsid(name: String) = paramOrThrow(name, Tsid::class).toTsid().getOrThrow(lazyException = { MalformedQueryParamException(name, Tsid::class) })
fun ServerRequest.paramOrThrowAsDate(name: String) = paramOrThrow(name, LocalDate::class).parseToLocalDate().getOrThrow(lazyException = { MalformedQueryParamException(name, LocalDate::class) })
fun ServerRequest.paramOrThrowAsDateTime(name: String) = paramOrThrow(name, OffsetDateTime::class).parseToOffsetDateTime().getOrThrow(lazyException = { MalformedQueryParamException(name, OffsetDateTime::class) })
inline fun <reified T : Enum<T>> ServerRequest.paramOrThrowAsEnum(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, T::class) }, includeCause = false, notOverwrite = RequiredQueryParamException::class) { paramOrThrow(name, T::class).toEnumConst<T>() }

fun ServerRequest.paramOrNullAsStringList(name: String) = paramOrNull(name)?.splitAndTrim(Char.COMMA)
fun ServerRequest.paramOrNullAsInt(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, Int::class) }, includeCause = false) { paramOrNull(name)?.toInt() }
fun ServerRequest.paramOrNullAsLong(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, Long::class) }, includeCause = false) { paramOrNull(name)?.toLong() }
fun ServerRequest.paramOrNullAsDouble(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, Double::class) }, includeCause = false) { paramOrNull(name)?.toDouble() }
fun ServerRequest.paramOrNullAsBoolean(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, Boolean::class) }, includeCause = false) { paramOrNull(name)?.toBoolean() }
fun ServerRequest.paramOrNullAsUuid(name: String) = paramOrNull(name)?.toUuid()?.getOrThrow { MalformedQueryParamException(name, Uuid::class) }
fun ServerRequest.paramOrNullAsUlid(name: String) = paramOrNull(name)?.toUlid()?.getOrThrow { MalformedQueryParamException(name, Ulid::class) }
fun ServerRequest.paramOrNullAsKsuid(name: String) = paramOrNull(name)?.toKsuid()?.getOrThrow { MalformedQueryParamException(name, Ksuid::class) }
fun ServerRequest.paramOrNullAsCuid(name: String) = paramOrNull(name)?.toCuid()?.getOrThrow { MalformedQueryParamException(name, Cuid::class) }
fun ServerRequest.paramOrNullAsTsid(name: String) = paramOrNull(name)?.toTsid()?.getOrThrow { MalformedQueryParamException(name, Tsid::class) }
fun ServerRequest.paramOrNullAsDate(name: String) = paramOrNull(name)?.parseToLocalDate()?.getOrThrow { MalformedQueryParamException(name, LocalDate::class) }
fun ServerRequest.paramOrNullAsDateTime(name: String) = paramOrNull(name)?.parseToOffsetDateTime()?.getOrThrow { MalformedQueryParamException(name, OffsetDateTime::class) }
inline fun <reified T : Enum<T>> ServerRequest.paramOrNullAsEnum(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, T::class) }) { paramOrNull(name)?.toEnumConst<T>() }

fun ServerRequest.paramOrDefaultAsStringList(name: String, defaultValue: Supplier<StringList>) = paramOrNull(name)?.splitAndTrim(Char.COMMA) ?: defaultValue()
fun ServerRequest.paramOrDefaultAsInt(name: String, defaultValue: Supplier<Int>) = tryOrThrow({ -> MalformedQueryParamException(name, Int::class) }, includeCause = false) { paramOrNull(name)?.toInt() ?: defaultValue() }
fun ServerRequest.paramOrDefaultAsLong(name: String, defaultValue: Supplier<Long>) = tryOrThrow({ -> MalformedQueryParamException(name, Long::class) }, includeCause = false) { paramOrNull(name)?.toLong() ?: defaultValue() }
fun ServerRequest.paramOrDefaultAsDouble(name: String, defaultValue: Supplier<Double>) = tryOrThrow({ -> MalformedQueryParamException(name, Double::class) }, includeCause = false) { paramOrNull(name)?.toDouble() ?: defaultValue() }
fun ServerRequest.paramOrDefaultAsBoolean(name: String, defaultValue: Supplier<Boolean>) = tryOrThrow({ -> MalformedQueryParamException(name, Boolean::class) }, includeCause = false) { paramOrNull(name)?.toBoolean() ?: defaultValue() }
fun ServerRequest.paramOrDefaultAsUuid(name: String, defaultValue: Supplier<Uuid>) = paramOrNull(name)?.toUuid()?.getOrThrow { MalformedQueryParamException(name, Uuid::class) } ?: defaultValue()
fun ServerRequest.paramOrDefaultAsUlid(name: String, defaultValue: Supplier<Ulid>) = paramOrNull(name)?.toUlid()?.getOrThrow { MalformedQueryParamException(name, Ulid::class) } ?: defaultValue()
fun ServerRequest.paramOrDefaultAsKsuid(name: String, defaultValue: Supplier<Ksuid>) = paramOrNull(name)?.toKsuid()?.getOrThrow { MalformedQueryParamException(name, Ksuid::class) } ?: defaultValue()
fun ServerRequest.paramOrDefaultAsCuid(name: String, defaultValue: Supplier<Cuid>) = paramOrNull(name)?.toCuid()?.getOrThrow { MalformedQueryParamException(name, Cuid::class) } ?: defaultValue()
fun ServerRequest.paramOrDefaultAsTsid(name: String, defaultValue: Supplier<Tsid>) = paramOrNull(name)?.toTsid()?.getOrThrow { MalformedQueryParamException(name, Tsid::class) } ?: defaultValue()
fun ServerRequest.paramOrDefaultAsDate(name: String, defaultValue: Supplier<LocalDate>) = paramOrNull(name)?.parseToLocalDate()?.getOrThrow { MalformedQueryParamException(name, LocalDate::class) } ?: defaultValue()
fun ServerRequest.paramOrDefaultAsDateTime(name: String, defaultValue: Supplier<OffsetDateTime>) = paramOrNull(name)?.parseToOffsetDateTime()?.getOrThrow { MalformedQueryParamException(name, OffsetDateTime::class) } ?: defaultValue()
inline fun <reified T : Enum<T>> ServerRequest.paramOrDefaultAsEnum(name: String, crossinline defaultValue: () -> T) = tryOrThrow({ -> MalformedQueryParamException(name, T::class) }, includeCause = false) { paramOrNull(name)?.toEnumConst<T>() ?: defaultValue() }

fun ServerRequest.pathVariableOrThrowAsStringList(name: String) = pathVariableOrThrow(name, StringList::class).splitAndTrim(Char.COMMA)
fun ServerRequest.pathVariableOrThrowAsInt(name: String) = tryOrThrow({ -> MalformedPathVariableException(name, Int::class) }, includeCause = false, notOverwrite = RequiredPathVariableException::class) { pathVariableOrThrow(name, Int::class).toInt() }
fun ServerRequest.pathVariableOrThrowAsLong(name: String) = tryOrThrow({ -> MalformedPathVariableException(name, Long::class) }, includeCause = false, notOverwrite = RequiredPathVariableException::class) { pathVariableOrThrow(name, Long::class).toLong() }
fun ServerRequest.pathVariableOrThrowAsDouble(name: String) = tryOrThrow({ -> MalformedPathVariableException(name, Double::class) }, includeCause = false, notOverwrite = RequiredPathVariableException::class) { pathVariableOrThrow(name, Double::class).toDouble() }
fun ServerRequest.pathVariableOrThrowAsBoolean(name: String) = tryOrThrow({ -> MalformedPathVariableException(name, Boolean::class) }, includeCause = false, notOverwrite = RequiredPathVariableException::class) { pathVariableOrThrow(name, Boolean::class).toBoolean() }
fun ServerRequest.pathVariableOrThrowAsUuid(name: String) = pathVariableOrThrow(name, Uuid::class).toUuid().getOrThrow(lazyException = { MalformedPathVariableException(name, Uuid::class) })
fun ServerRequest.pathVariableOrThrowAsUlid(name: String) = pathVariableOrThrow(name, Ulid::class).toUlid().getOrThrow(lazyException = { MalformedPathVariableException(name, Ulid::class) })
fun ServerRequest.pathVariableOrThrowAsKsuid(name: String) = pathVariableOrThrow(name, Ksuid::class).toKsuid().getOrThrow(lazyException = { MalformedPathVariableException(name, Ksuid::class) })
fun ServerRequest.pathVariableOrThrowAsCuid(name: String) = pathVariableOrThrow(name, Cuid::class).toCuid().getOrThrow(lazyException = { MalformedPathVariableException(name, Cuid::class) })
fun ServerRequest.pathVariableOrThrowAsTsid(name: String) = pathVariableOrThrow(name, Tsid::class).toTsid().getOrThrow(lazyException = { MalformedPathVariableException(name, Tsid::class) })
fun ServerRequest.pathVariableOrThrowAsDate(name: String) = pathVariableOrThrow(name, LocalDate::class).parseToLocalDate().getOrThrow(lazyException = { MalformedPathVariableException(name, LocalDate::class) })
fun ServerRequest.pathVariableOrThrowAsDateTime(name: String) = pathVariableOrThrow(name, OffsetDateTime::class).parseToOffsetDateTime().getOrThrow(lazyException = { MalformedPathVariableException(name, OffsetDateTime::class) })
inline fun <reified T : Enum<T>> ServerRequest.pathVariableOrThrowAsEnum(name: String) = tryOrThrow({ -> MalformedPathVariableException(name, T::class) }, includeCause = false, notOverwrite = RequiredPathVariableException::class) { pathVariableOrThrow(name, T::class).toEnumConst<T>() }

fun ServerRequest.pathVariableOrNullAsStringList(name: String) = pathVariableOrNull(name)?.splitAndTrim(Char.COMMA)
fun ServerRequest.pathVariableOrNullAsInt(name: String) = tryOrThrow({ -> MalformedPathVariableException(name, Int::class) }, includeCause = false) { pathVariableOrNull(name)?.toInt() }
fun ServerRequest.pathVariableOrNullAsLong(name: String) = tryOrThrow({ -> MalformedPathVariableException(name, Long::class) }, includeCause = false) { pathVariableOrNull(name)?.toLong() }
fun ServerRequest.pathVariableOrNullAsDouble(name: String) = tryOrThrow({ -> MalformedPathVariableException(name, Double::class) }, includeCause = false) { pathVariableOrNull(name)?.toDouble() }
fun ServerRequest.pathVariableOrNullAsBoolean(name: String) = tryOrThrow({ -> MalformedPathVariableException(name, Boolean::class) }, includeCause = false) { pathVariableOrNull(name)?.toBoolean() }
fun ServerRequest.pathVariableOrNullAsUuid(name: String) = pathVariableOrNull(name)?.toUuid()?.getOrThrow { MalformedPathVariableException(name, Uuid::class) }
fun ServerRequest.pathVariableOrNullAsUlid(name: String) = pathVariableOrNull(name)?.toUlid()?.getOrThrow { MalformedPathVariableException(name, Ulid::class) }
fun ServerRequest.pathVariableOrNullAsKsuid(name: String) = pathVariableOrNull(name)?.toKsuid()?.getOrThrow { MalformedPathVariableException(name, Ksuid::class) }
fun ServerRequest.pathVariableOrNullAsCuid(name: String) = pathVariableOrNull(name)?.toCuid()?.getOrThrow { MalformedPathVariableException(name, Cuid::class) }
fun ServerRequest.pathVariableOrNullAsTsid(name: String) = pathVariableOrNull(name)?.toTsid()?.getOrThrow { MalformedPathVariableException(name, Tsid::class) }
fun ServerRequest.pathVariableOrNullAsDate(name: String) = pathVariableOrNull(name)?.parseToLocalDate()?.getOrThrow { MalformedPathVariableException(name, LocalDate::class) }
fun ServerRequest.pathVariableOrNullAsDateTime(name: String) = pathVariableOrNull(name)?.parseToOffsetDateTime()?.getOrThrow { MalformedPathVariableException(name, OffsetDateTime::class) }
inline fun <reified T : Enum<T>> ServerRequest.pathVariableOrNullAsEnum(name: String) = tryOrThrow({ -> MalformedPathVariableException(name, T::class) }) { pathVariableOrNull(name)?.toEnumConst<T>() }

fun ServerRequest.pathVariableOrDefaultAsStringList(name: String, defaultValue: Supplier<StringList>) = pathVariableOrNull(name)?.splitAndTrim(Char.COMMA) ?: defaultValue()
fun ServerRequest.pathVariableOrDefaultAsInt(name: String, defaultValue: Supplier<Int>) = tryOrThrow({ -> MalformedPathVariableException(name, Int::class) }, includeCause = false) { pathVariableOrNull(name)?.toInt() ?: defaultValue() }
fun ServerRequest.pathVariableOrDefaultAsLong(name: String, defaultValue: Supplier<Long>) = tryOrThrow({ -> MalformedPathVariableException(name, Long::class) }, includeCause = false) { pathVariableOrNull(name)?.toLong() ?: defaultValue() }
fun ServerRequest.pathVariableOrDefaultAsDouble(name: String, defaultValue: Supplier<Double>) = tryOrThrow({ -> MalformedPathVariableException(name, Double::class) }, includeCause = false) { pathVariableOrNull(name)?.toDouble() ?: defaultValue() }
fun ServerRequest.pathVariableOrDefaultAsBoolean(name: String, defaultValue: Supplier<Boolean>) = tryOrThrow({ -> MalformedPathVariableException(name, Boolean::class) }, includeCause = false) { pathVariableOrNull(name)?.toBoolean() ?: defaultValue() }
fun ServerRequest.pathVariableOrDefaultAsUuid(name: String, defaultValue: Supplier<Uuid>) = pathVariableOrNull(name)?.toUuid()?.getOrThrow { MalformedPathVariableException(name, Uuid::class) } ?: defaultValue()
fun ServerRequest.pathVariableOrDefaultAsUlid(name: String, defaultValue: Supplier<Ulid>) = pathVariableOrNull(name)?.toUlid()?.getOrThrow { MalformedPathVariableException(name, Ulid::class) } ?: defaultValue()
fun ServerRequest.pathVariableOrDefaultAsKsuid(name: String, defaultValue: Supplier<Ksuid>) = pathVariableOrNull(name)?.toKsuid()?.getOrThrow { MalformedPathVariableException(name, Ksuid::class) } ?: defaultValue()
fun ServerRequest.pathVariableOrDefaultAsCuid(name: String, defaultValue: Supplier<Cuid>) = pathVariableOrNull(name)?.toCuid()?.getOrThrow { MalformedPathVariableException(name, Cuid::class) } ?: defaultValue()
fun ServerRequest.pathVariableOrDefaultAsTsid(name: String, defaultValue: Supplier<Tsid>) = pathVariableOrNull(name)?.toTsid()?.getOrThrow { MalformedPathVariableException(name, Tsid::class) } ?: defaultValue()
fun ServerRequest.pathVariableOrDefaultAsDate(name: String, defaultValue: Supplier<LocalDate>) = pathVariableOrNull(name)?.parseToLocalDate()?.getOrThrow { MalformedPathVariableException(name, LocalDate::class) } ?: defaultValue()
fun ServerRequest.pathVariableOrDefaultAsDateTime(name: String, defaultValue: Supplier<OffsetDateTime>) = pathVariableOrNull(name)?.parseToOffsetDateTime()?.getOrThrow { MalformedPathVariableException(name, OffsetDateTime::class) } ?: defaultValue()
inline fun <reified T : Enum<T>> ServerRequest.pathVariableOrDefaultAsEnum(name: String, crossinline defaultValue: () -> T) = tryOrThrow({ -> MalformedPathVariableException(name, T::class) }, includeCause = false) { pathVariableOrNull(name)?.toEnumConst<T>() ?: defaultValue() }