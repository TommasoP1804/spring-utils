@file:JvmName("ReactiveRequestUtilsKt")
@file:Since("2.0.0")
@file:Suppress("unused")

package dev.tommasop1804.springutils.reactive.function.request

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
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.queryParamOrNull
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlin.reflect.KClass

/**
 * Retrieves all query parameters from the server request and converts them into a `MultiMap`.
 *
 * This method extracts the query parameters from the server request as a `MultiValueMap`
 * and transforms it into a `MultiMap`, preserving the order of keys and their associated values.
 *
 * @return A `MultiMap` representing the query parameters of the request.
 * @since 3.0.0
 */
fun ServerRequest.allParams() = queryParams().toMultiMap()
/**
 * Retrieves all headers from the current server request and converts them 
 * into an instance of `HttpHeaders` from the `kutils` library.
 *
 * This method first accesses the headers of the server request, then converts 
 * the `org.springframework.http.HttpHeaders` instance into the `HttpHeaders` 
 * used by the `kutils` library, ensuring the headers are retained in the process.
 *
 * @receiver The server request from which headers are to be retrieved.
 * @return An instance of `HttpHeaders` containing all headers from the request.
 * @since 3.0.0
 */
fun ServerRequest.allHeaders() = headers().asHttpHeaders().toKutilsHttpHeaders()

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
fun ServerRequest.queryParamOrThrow(
    name: String,
    `class`: KClass<*>,
    lazyException: ThrowableSupplier = { RequiredQueryParamException(name, `class`) }
) = queryParamOrNull(name) ?: throw lazyException()
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
fun ServerRequest.queryParamOrThrow(
    name: String,
    `class`: KClass<*>,
    internalErrorCode: String
) = queryParamOrNull(name) ?: throw RequiredQueryParamException(name, `class`, internalErrorCode)
/**
 * Retrieves the query parameter value associated with the given name or,
 * if not present, returns a default value provided by the given supplier.
 *
 * @param name the name of the query parameter to retrieve
 * @param defaultValue a supplier that provides a default value if the query parameter is not found
 * @return the value of the query parameter as a string, or the supplied default value if not present
 * @since 3.0.0
 */
fun ServerRequest.queryParamOrDefault(
    name: String,
    defaultValue: Supplier<Any>
) = queryParamOrNull(name) ?: defaultValue().toString()

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
fun ServerRequest.pathVariableOrThrow(name: String, `class`: KClass<*>, lazyException: ThrowableSupplier = { RequiredPathVariableException(name, `class`) }) =
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
fun ServerRequest.pathVariableOrThrow(name: String, `class`: KClass<*>, internalErrorCode: String) =
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
fun ServerRequest.headerOrThrow(name: String, `class`: KClass<*>, lazyException: ThrowableSupplier = { RequiredHeaderException(name, `class`) }) =
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
fun ServerRequest.headerOrThrow(name: String, `class`: KClass<*>, internalErrorCode: String) =
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

fun ServerRequest.accept() = header(HttpHeaders.ACCEPT)
fun ServerRequest.acceptCharset() = header(HttpHeaders.ACCEPT_CHARSET)
fun ServerRequest.acceptEncoding() = header(HttpHeaders.ACCEPT_ENCODING)
fun ServerRequest.acceptLanguage() = header(HttpHeaders.ACCEPT_LANGUAGE)
fun ServerRequest.acceptRanges() = header(HttpHeaders.ACCEPT_RANGES)
fun ServerRequest.connection() = header(HttpHeaders.CONNECTION).firstOrNull()?.let(ConnectionBehaviour::of)
fun ServerRequest.contentLength() = header(HttpHeaders.CONTENT_LENGTH).firstOrNull()?.let { it.toInt() ofUnit MeasureUnit.DataSizeUnit.BYTE }
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

fun ServerRequest.queryParamOrThrowAsStringList(name: String) = queryParamOrThrow(name, StringList::class).splitAndTrim(Char.COMMA)
fun ServerRequest.queryParamOrThrowAsInt(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, Int::class) }, includeCause = false, notOverwrite = RequiredQueryParamException::class) { queryParamOrThrow(name, Int::class).toInt() }
fun ServerRequest.queryParamOrThrowAsLong(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, Long::class) }, includeCause = false, notOverwrite = RequiredQueryParamException::class) { queryParamOrThrow(name, Long::class).toLong() }
fun ServerRequest.queryParamOrThrowAsDouble(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, Double::class) }, includeCause = false, notOverwrite = RequiredQueryParamException::class) { queryParamOrThrow(name, Double::class).toDouble() }
fun ServerRequest.queryParamOrThrowAsBoolean(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, Boolean::class) }, includeCause = false, notOverwrite = RequiredQueryParamException::class) { queryParamOrThrow(name, Boolean::class).toBoolean() }
fun ServerRequest.queryParamOrThrowAsUuid(name: String) = queryParamOrThrow(name, Uuid::class).toUuid()() { MalformedQueryParamException(name, Uuid::class) }
fun ServerRequest.queryParamOrThrowAsUlid(name: String) = queryParamOrThrow(name, Ulid::class).toUlid()() { MalformedQueryParamException(name, Ulid::class) }
fun ServerRequest.queryParamOrThrowAsKsuid(name: String) = queryParamOrThrow(name, Ksuid::class).toKsuid()() { MalformedQueryParamException(name, Ksuid::class) }
fun ServerRequest.queryParamOrThrowAsCuid(name: String) = queryParamOrThrow(name, Cuid::class).toCuid()() { MalformedQueryParamException(name, Cuid::class) }
fun ServerRequest.queryParamOrThrowAsTsid(name: String) = queryParamOrThrow(name, Tsid::class).toTsid()() { MalformedQueryParamException(name, Tsid::class) }
fun ServerRequest.queryParamOrThrowAsDate(name: String) = queryParamOrThrow(name, LocalDate::class).parseToLocalDate()() { MalformedQueryParamException(name, LocalDate::class) }
fun ServerRequest.queryParamOrThrowAsDateTime(name: String) = queryParamOrThrow(name, OffsetDateTime::class).parseToOffsetDateTime()() { MalformedQueryParamException(name, OffsetDateTime::class) }
inline fun <reified T : Enum<T>> ServerRequest.queryParamOrThrowAsEnum(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, T::class) }, includeCause = false, notOverwrite = RequiredQueryParamException::class) { queryParamOrThrow(name, T::class).toEnumConst<T>() }

fun ServerRequest.queryParamOrNullAsStringList(name: String) = queryParamOrNull(name)?.splitAndTrim(Char.COMMA)
fun ServerRequest.queryParamOrNullAsInt(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, Int::class) }, includeCause = false) { queryParamOrNull(name)?.toInt() }
fun ServerRequest.queryParamOrNullAsLong(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, Long::class) }, includeCause = false) { queryParamOrNull(name)?.toLong() }
fun ServerRequest.queryParamOrNullAsDouble(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, Double::class) }, includeCause = false) { queryParamOrNull(name)?.toDouble() }
fun ServerRequest.queryParamOrNullAsBoolean(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, Boolean::class) }, includeCause = false) { queryParamOrNull(name)?.toBoolean() }
fun ServerRequest.queryParamOrNullAsUuid(name: String) = queryParamOrNull(name)?.toUuid()?.getOrThrow { MalformedQueryParamException(name, Uuid::class) }
fun ServerRequest.queryParamOrNullAsUlid(name: String) = queryParamOrNull(name)?.toUlid()?.getOrThrow { MalformedQueryParamException(name, Ulid::class) }
fun ServerRequest.queryParamOrNullAsKsuid(name: String) = queryParamOrNull(name)?.toKsuid()?.getOrThrow { MalformedQueryParamException(name, Ksuid::class) }
fun ServerRequest.queryParamOrNullAsCuid(name: String) = queryParamOrNull(name)?.toCuid()?.getOrThrow { MalformedQueryParamException(name, Cuid::class) }
fun ServerRequest.queryParamOrNullAsTsid(name: String) = queryParamOrNull(name)?.toTsid()?.getOrThrow { MalformedQueryParamException(name, Tsid::class) }
fun ServerRequest.queryParamOrNullAsDate(name: String) = queryParamOrNull(name)?.parseToLocalDate()?.getOrThrow { MalformedQueryParamException(name, LocalDate::class) }
fun ServerRequest.queryParamOrNullAsDateTime(name: String) = queryParamOrNull(name)?.parseToOffsetDateTime()?.getOrThrow { MalformedQueryParamException(name, OffsetDateTime::class) }
inline fun <reified T : Enum<T>> ServerRequest.queryParamOrNullAsEnum(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, T::class) }) { queryParamOrNull(name)?.toEnumConst<T>() }

fun ServerRequest.queryParamOrDefaultAsStringList(name: String, defaultValue: Supplier<StringList>) = queryParamOrNull(name)?.splitAndTrim(Char.COMMA) ?: defaultValue()
fun ServerRequest.queryParamOrDefaultAsInt(name: String, defaultValue: Supplier<Int>) = tryOrThrow({ -> MalformedQueryParamException(name, Int::class) }, includeCause = false) { queryParamOrNull(name)?.toInt() ?: defaultValue() }
fun ServerRequest.queryParamOrDefaultAsLong(name: String, defaultValue: Supplier<Long>) = tryOrThrow({ -> MalformedQueryParamException(name, Long::class) }, includeCause = false) { queryParamOrNull(name)?.toLong() ?: defaultValue() }
fun ServerRequest.queryParamOrDefaultAsDouble(name: String, defaultValue: Supplier<Double>) = tryOrThrow({ -> MalformedQueryParamException(name, Double::class) }, includeCause = false) { queryParamOrNull(name)?.toDouble() ?: defaultValue() }
fun ServerRequest.queryParamOrDefaultAsBoolean(name: String, defaultValue: Supplier<Boolean>) = tryOrThrow({ -> MalformedQueryParamException(name, Boolean::class) }, includeCause = false) { queryParamOrNull(name)?.toBoolean() ?: defaultValue() }
fun ServerRequest.queryParamOrDefaultAsUuid(name: String, defaultValue: Supplier<Uuid>) = queryParamOrNull(name)?.toUuid()?.getOrThrow { MalformedQueryParamException(name, Uuid::class) } ?: defaultValue()
fun ServerRequest.queryParamOrDefaultAsUlid(name: String, defaultValue: Supplier<Ulid>) = queryParamOrNull(name)?.toUlid()?.getOrThrow { MalformedQueryParamException(name, Ulid::class) } ?: defaultValue()
fun ServerRequest.queryParamOrDefaultAsKsuid(name: String, defaultValue: Supplier<Ksuid>) = queryParamOrNull(name)?.toKsuid()?.getOrThrow { MalformedQueryParamException(name, Ksuid::class) } ?: defaultValue()
fun ServerRequest.queryParamOrDefaultAsCuid(name: String, defaultValue: Supplier<Cuid>) = queryParamOrNull(name)?.toCuid()?.getOrThrow { MalformedQueryParamException(name, Cuid::class) } ?: defaultValue()
fun ServerRequest.queryParamOrDefaultAsTsid(name: String, defaultValue: Supplier<Tsid>) = queryParamOrNull(name)?.toTsid()?.getOrThrow { MalformedQueryParamException(name, Tsid::class) } ?: defaultValue()
fun ServerRequest.queryParamOrDefaultAsDate(name: String, defaultValue: Supplier<LocalDate>) = queryParamOrNull(name)?.parseToLocalDate()?.getOrThrow { MalformedQueryParamException(name, LocalDate::class) } ?: defaultValue()
fun ServerRequest.queryParamOrDefaultAsDateTime(name: String, defaultValue: Supplier<OffsetDateTime>) = queryParamOrNull(name)?.parseToOffsetDateTime()?.getOrThrow { MalformedQueryParamException(name, OffsetDateTime::class) } ?: defaultValue()
inline fun <reified T : Enum<T>> ServerRequest.queryParamOrDefaultAsEnum(name: String, crossinline defaultValue: () -> T) = tryOrThrow({ -> MalformedQueryParamException(name, T::class) }, includeCause = false) { queryParamOrNull(name)?.toEnumConst<T>() ?: defaultValue() }

fun ServerRequest.pathVariableOrThrowAsStringList(name: String) = pathVariableOrThrow(name, StringList::class).splitAndTrim(Char.COMMA)
fun ServerRequest.pathVariableOrThrowAsInt(name: String) = tryOrThrow({ -> MalformedPathVariableException(name, Int::class) }, includeCause = false, notOverwrite = RequiredPathVariableException::class) { pathVariableOrThrow(name, Int::class).toInt() }
fun ServerRequest.pathVariableOrThrowAsLong(name: String) = tryOrThrow({ -> MalformedPathVariableException(name, Long::class) }, includeCause = false, notOverwrite = RequiredPathVariableException::class) { pathVariableOrThrow(name, Long::class).toLong() }
fun ServerRequest.pathVariableOrThrowAsDouble(name: String) = tryOrThrow({ -> MalformedPathVariableException(name, Double::class) }, includeCause = false, notOverwrite = RequiredPathVariableException::class) { pathVariableOrThrow(name, Double::class).toDouble() }
fun ServerRequest.pathVariableOrThrowAsBoolean(name: String) = tryOrThrow({ -> MalformedPathVariableException(name, Boolean::class) }, includeCause = false, notOverwrite = RequiredPathVariableException::class) { pathVariableOrThrow(name, Boolean::class).toBoolean() }
fun ServerRequest.pathVariableOrThrowAsUuid(name: String) = pathVariableOrThrow(name, Uuid::class).toUuid()() { MalformedPathVariableException(name, Uuid::class) }
fun ServerRequest.pathVariableOrThrowAsUlid(name: String) = pathVariableOrThrow(name, Ulid::class).toUlid()() { MalformedPathVariableException(name, Ulid::class) }
fun ServerRequest.pathVariableOrThrowAsKsuid(name: String) = pathVariableOrThrow(name, Ksuid::class).toKsuid()() { MalformedPathVariableException(name, Ksuid::class) }
fun ServerRequest.pathVariableOrThrowAsCuid(name: String) = pathVariableOrThrow(name, Cuid::class).toCuid()() { MalformedPathVariableException(name, Cuid::class) }
fun ServerRequest.pathVariableOrThrowAsTsid(name: String) = pathVariableOrThrow(name, Tsid::class).toTsid()() { MalformedPathVariableException(name, Tsid::class) }
fun ServerRequest.pathVariableOrThrowAsDate(name: String) = pathVariableOrThrow(name, LocalDate::class).parseToLocalDate()() { MalformedPathVariableException(name, LocalDate::class) }
fun ServerRequest.pathVariableOrThrowAsDateTime(name: String) = pathVariableOrThrow(name, OffsetDateTime::class).parseToOffsetDateTime()() { MalformedPathVariableException(name, OffsetDateTime::class) }
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