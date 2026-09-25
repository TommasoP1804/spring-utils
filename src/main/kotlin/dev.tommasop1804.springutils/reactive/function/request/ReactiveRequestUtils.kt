/*
 * Copyright © 2026 Tommaso Pastorelli (TommasoP1804) | Spring-Utils
 */

@file:JvmName("ReactiveRequestUtilsKt")
@file:Since("2.0.0")
@file:Suppress("unused", "MoveLambdaOutsideParentheses")
@file:MustUseReturnValues

package dev.tommasop1804.springutils.reactive.function.request

import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.annotations.*
import dev.tommasop1804.kutils.classes.functional.*
import dev.tommasop1804.kutils.classes.identifiers.*
import dev.tommasop1804.kutils.classes.identifiers.Cuid.Companion.toCuid
import dev.tommasop1804.kutils.classes.identifiers.Ksuid.Companion.toKsuid
import dev.tommasop1804.kutils.classes.identifiers.Tsid.Companion.toTsid
import dev.tommasop1804.kutils.classes.identifiers.Ulid.Companion.toUlid
import dev.tommasop1804.kutils.classes.measure.*
import dev.tommasop1804.kutils.classes.security.*
import dev.tommasop1804.kutils.classes.web.*
import dev.tommasop1804.kutils.classes.web.HttpHeader.Companion.ACCEPT_ENCODING
import dev.tommasop1804.kutils.classes.web.HttpHeader.Companion.EXPECT
import dev.tommasop1804.kutils.classes.web.HttpHeader.Companion.IF_MATCH
import dev.tommasop1804.kutils.classes.web.HttpHeader.Companion.IF_NONE_MATCH
import dev.tommasop1804.kutils.classes.web.HttpHeader.Companion.IF_RANGE
import dev.tommasop1804.kutils.classes.web.HttpHeader.Companion.PREFER
import dev.tommasop1804.kutils.classes.web.HttpHeader.Companion.RANGE
import dev.tommasop1804.kutils.errors.*
import dev.tommasop1804.kutils.exceptions.*
import dev.tommasop1804.springutils.*
import dev.tommasop1804.springutils.exception.*
import dev.tommasop1804.springutils.servlet.function.request.pathVariableOrError
import dev.tommasop1804.springutils.servlet.function.request.pathVariableOrThrow
import org.springframework.core.convert.ConversionService
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.queryParamOrNull
import java.net.InetSocketAddress
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.*
import kotlin.reflect.KClass
import kotlin.toString
import kotlin.uuid.Uuid

/**
 * Retrieves all query parameters from the server request and converts them into a `MultiMap`.
 *
 * This method extracts the query parameters from the server request as a `MultiValueMap`
 * and transforms it into a `MultiMap`, preserving the order of keys and their associated values.
 *
 * @return A `MultiMap` representing the query parameters of the request.
 * @since 3.9.0
 */
val ServerRequest.queryParams get() = queryParams().toMultiMap()
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
 * Retrieves the HTTP method of the current server request.
 *
 * This extension property maps the HTTP method from the underlying servlet request
 * to an instance of the `HttpMethod` enum.
 *
 * @receiver ServerRequest The server request from which the HTTP method is extracted.
 * @return The HTTP method as an `HttpMethod` enum constant.
 * @since 5.2.1
 */
val ServerRequest.method: HttpMethod get() = method().name().toEnumConst()

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
@IgnorableReturnValue
fun ServerRequest.queryParamOrThrow(
    name: String,
    `class`: KClass<*> = String::class,
    lazyException: ThrowableSupplier = { RequiredQueryParamException(name, `class`) }
) = queryParam(name).orElseThrow(lazyException)!!
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
@IgnorableReturnValue
fun ServerRequest.queryParamOrThrow(
    name: String,
    `class`: KClass<*> = String::class,
    internalErrorCode: String
) = queryParam(name).orElseThrow { RequiredQueryParamException(name, `class`, internalErrorCode) }!!
/**
 * Retrieves the value of a query parameter from the `ServerRequest`. If the parameter
 * is missing, it returns an error wrapped in an `Either`.
 *
 * @param name The name of the query parameter to retrieve.
 * @return An `Either` containing the query parameter's value as a `String` if found,
 * or an `IterableError.NotFound` error if the parameter is missing.
 * @since 5.1.0
 */
@IgnorableReturnValue
fun ServerRequest.queryParamOrError(name: String): Either<IterableError.NotFound, String> =
    tryOrError({ IterableError.NotFound(name) }) { queryParamOrThrow(name) }
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
) = queryParam(name).orElseGet { defaultValue().toString() }!!

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
@IgnorableReturnValue
fun ServerRequest.pathVariableOrThrow(name: String, `class`: KClass<*> = String::class, lazyException: ExceptionSupplier = { RequiredPathVariableException(name, `class`) }) =
    tryOrThrow({ lazyException() }, includeCause = false) { pathVariable(name) }
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
@IgnorableReturnValue
fun ServerRequest.pathVariableOrThrow(name: String, `class`: KClass<*> = String::class, internalErrorCode: String) =
    tryOrThrow({ RequiredPathVariableException(name, `class`, internalErrorCode) }, includeCause = false) { pathVariable(name) }
/**
 * Retrieves the value of a path variable with the given name from the server request.
 * If the path variable is not found, returns an error wrapped in an `Either` type.
 *
 * @param name the name of the path variable to retrieve
 * @return the value of the path variable as a `String` if present, or an `Either`
 * wrapping an `IterableError.NotFound` if the path variable is not found
 * @since 5.1.0
 */
fun ServerRequest.pathVariableOrError(name: String): Either<IterableError.NotFound, String> =
    tryOrError({ IterableError.NotFound(name) }) { pathVariable(name) }
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
@IgnorableReturnValue
fun ServerRequest.headerOrThrow(name: String, `class`: KClass<*>, lazyException: ExceptionSupplier = { RequiredHeaderException(name, `class`) }) =
    tryOrThrow({ lazyException() }, includeCause = false) { headers().header(name).ifNullOrEmpty { throw NoSuchElementException() } }
/**
 * Retrieves the value of a specific header from the server request or throws an exception if the header
 * is missing or its value is empty.
 *
 * @param name the name of the header to retrieve
 * @param class the expected class type of the header value
 * @param internalErrorCode the internal error code to associate with the exception if the header is missing or invalid
 * @since 3.0.0
 */
@IgnorableReturnValue
fun ServerRequest.headerOrThrow(name: String, `class`: KClass<*>, internalErrorCode: String): List<String> =
    tryOrThrow({ RequiredHeaderException(name, `class`, internalErrorCode) }, includeCause = false) { headers().header(name).ifNullOrEmpty { throw NoSuchElementException() } }
/**
 * Retrieves the values of the specified header from the server request, or returns an error if
 * the header is not present or contains no values.
 *
 * @param name the name of the header to retrieve.
 * @return an `Either` instance containing a `List` of header values if found, or an
 * `IterableError.NotFound` error if the header is missing or empty.
 * @since 5.1.0
 */
fun ServerRequest.headerOrError(name: String): Either<IterableError.NotFound, List<String>> =
    header(name).orErrorIfEmpty().mapLeft { IterableError.NotFound(name) }
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
): List<String> = headers().header(name).ifEmpty { defaultValue().toString().asSingleList() }
/**
 * Retrieves the values of the specified header from the server request.
 *
 * @param name the name of the header to retrieve.
 * @return a list of values associated with the specified header, or an empty list if the header is not present.
 * @since 3.0.0
 */
fun ServerRequest.header(name: String): List<String> = tryOrNull { headers().header(name) }.orEmpty()

fun ServerRequest.accept() = headers.getAccept()
fun ServerRequest.acceptCharset() = headers.getAcceptCharset()
fun ServerRequest.acceptEncoding() = headers.getOrError(ACCEPT_ENCODING)
fun ServerRequest.acceptLanguage() = headers.getAcceptLanguage()
fun ServerRequest.acceptRanges() = headers.getAcceptRanges()
fun ServerRequest.connection() = headers.getConnection()
fun ServerRequest.contentLength() = headers.getContentLength()
fun ServerRequest.contentType() = headers.getContentType()
fun ServerRequest.expect() = headers.getOrError(EXPECT)
fun ServerRequest.fromService() = headers.getOrError("From-Service")
    .thenMergeWith { it.firstOrError() }
fun ServerRequest.host() = headers.getHost()
fun ServerRequest.ifMatch() = headers.getOrError(IF_MATCH)
fun ServerRequest.ifModifiedSince() = headers.getIfModifiedSince()
fun ServerRequest.ifNoneMatch() = headers.getOrError(IF_NONE_MATCH)
fun ServerRequest.ifRange() = headers.getOrError(IF_RANGE)
fun ServerRequest.ifUnmodifiedSince() = headers.getIfUnmodifiedSince()
fun ServerRequest.jwtToken(headerName: String = HttpHeaders.AUTHORIZATION) = headers.getBearerAuth(headerName)
fun ServerRequest.origin() = headers.getOrigin()
fun ServerRequest.prefer() = headers.getOrError(PREFER)
fun ServerRequest.priority() = headers.getPriority()
fun ServerRequest.range() = headers.getOrError(RANGE)
fun ServerRequest.referer() = headers.getReferer()

fun ServerRequest.acceptOrDefault(default: Supplier<List<String>>) = accept().getOrElse(default)
fun ServerRequest.acceptCharsetOrDefault(default: Supplier<List<String>>) = acceptCharset().getOrElse(default)
fun ServerRequest.acceptEncodingOrDefault(default: Supplier<List<String>>) = acceptEncoding().getOrElse(default)
fun ServerRequest.acceptLanguageOrDefault(default: Supplier<List<Locale.LanguageRange>>) = acceptLanguage().getOrElse(default)
fun ServerRequest.acceptRangesOrDefault(default: Supplier<List<String>>) = acceptRanges().getOrElse(default)
fun ServerRequest.connectionOrDefault(default: Supplier<ConnectionBehaviour>) = connection().getOrElse(default)
fun ServerRequest.contentLengthOrDefault(default: Supplier<DataSize>) = contentLength().getOrElse(default)
fun ServerRequest.contentTypeOrDefault(default: Supplier<MediaType>) = contentType().getOrElse(default)
fun ServerRequest.expectOrDefault(default: Supplier<List<String>>) = expect().getOrElse(default)
fun ServerRequest.fromServiceOrDefault(default: Supplier<String>) = fromService().getOrElse(default)
fun ServerRequest.hostOrDefault(default: Supplier<InetSocketAddress>) = host().getOrElse(default)
fun ServerRequest.ifMatchOrDefault(default: Supplier<List<String>>) = ifMatch().getOrElse(default)
fun ServerRequest.ifModifiedSinceOrDefault(default: Supplier<Instant>) = ifModifiedSince().getOrElse(default)
fun ServerRequest.ifNoneMatchOrDefault(default: Supplier<List<String>>) = ifNoneMatch().getOrElse(default)
fun ServerRequest.ifRangeOrDefault(default: Supplier<List<String>>) = ifRange().getOrElse(default)
fun ServerRequest.ifUnmodifiedSinceOrDefault(default: Supplier<Instant>) = ifUnmodifiedSince().getOrElse(default)
fun ServerRequest.jwtTokenOrDefault(headerName: String = HttpHeaders.AUTHORIZATION, default: Supplier<Jwt>) = jwtToken(headerName).getOrElse(default)
fun ServerRequest.originOrDefault(default: Supplier<Uri>) = origin().getOrElse(default)
fun ServerRequest.preferOrDefault(default: Supplier<List<String>>) = prefer().getOrElse(default)
fun ServerRequest.priorityOrDefault(default: Supplier<Pair<Int, Boolean>>) = priority().getOrElse(default)
fun ServerRequest.rangeOrDefault(default: Supplier<List<String>>) = range().getOrElse(default)
fun ServerRequest.refererOrDefault(default: Supplier<Uri>) = referer().getOrElse(default)

@IgnorableReturnValue
fun ServerRequest.acceptOrThrow(lazyException: ThrowableSupplier = { RequiredHeaderException(HttpHeaders.ACCEPT, List::class) }) = accept().getOrThrow { lazyException() }
@IgnorableReturnValue
fun ServerRequest.acceptCharsetOrThrow(lazyException: ThrowableSupplier = { RequiredHeaderException(HttpHeaders.ACCEPT_CHARSET, List::class) }) = acceptCharset().getOrThrow { lazyException() }
@IgnorableReturnValue
fun ServerRequest.acceptEncodingOrThrow(lazyException: ThrowableSupplier = { RequiredHeaderException(HttpHeaders.ACCEPT_ENCODING, List::class) }) = acceptEncoding().getOrThrow { lazyException() }
@IgnorableReturnValue
fun ServerRequest.acceptLanguageOrThrow(lazyException: ThrowableSupplier = { RequiredHeaderException(HttpHeaders.ACCEPT_LANGUAGE, List::class) }) = acceptLanguage().getOrThrow { lazyException() }
@IgnorableReturnValue
fun ServerRequest.acceptRangesOrThrow(lazyException: ThrowableSupplier = { RequiredHeaderException(HttpHeaders.ACCEPT_RANGES, List::class) }) = acceptRanges().getOrThrow { lazyException() }
@IgnorableReturnValue
fun ServerRequest.connectionOrThrow(lazyException: ThrowableSupplier = { RequiredHeaderException(HttpHeaders.CONNECTION, ConnectionBehaviour::class) }) = connection().getOrThrow { lazyException() }
@IgnorableReturnValue
fun ServerRequest.contentLengthOrThrow(lazyException: ThrowableSupplier = { RequiredHeaderException(HttpHeaders.CONTENT_LENGTH, DataSize::class) }): DataSize = contentLength().getOrThrow { lazyException() }
@IgnorableReturnValue
fun ServerRequest.contentTypeOrThrow(lazyException: ThrowableSupplier = { RequiredHeaderException(HttpHeaders.CONTENT_TYPE, MediaType::class) }) = contentType().getOrThrow { lazyException() }
@IgnorableReturnValue
fun ServerRequest.expectOrThrow(lazyException: ThrowableSupplier = { RequiredHeaderException(HttpHeaders.EXPECT, List::class) }) = expect().getOrThrow { lazyException() }
@IgnorableReturnValue
fun ServerRequest.fromServiceOrThrow(lazyException: ThrowableSupplier = { RequiredHeaderException("From-Service", String::class) }) = fromService().getOrThrow { lazyException() }
@IgnorableReturnValue
fun ServerRequest.hostOrThrow(lazyException: ThrowableSupplier = { RequiredHeaderException(HttpHeaders.HOST, List::class) }) = host().getOrThrow { lazyException() }
@IgnorableReturnValue
fun ServerRequest.ifMatchOrThrow(lazyException: ThrowableSupplier = { RequiredHeaderException(HttpHeaders.IF_MATCH, List::class) }) = ifMatch().getOrThrow { lazyException() }
@IgnorableReturnValue
fun ServerRequest.ifModifiedSinceOrThrow(lazyException: ThrowableSupplier = { RequiredHeaderException(HttpHeaders.IF_MODIFIED_SINCE, Instant::class) }) = ifModifiedSince().getOrThrow { lazyException() }
@IgnorableReturnValue
fun ServerRequest.ifNoneMatchOrThrow(lazyException: ThrowableSupplier = { RequiredHeaderException(HttpHeaders.IF_NONE_MATCH, List::class) }) = ifNoneMatch().getOrThrow { lazyException() }
@IgnorableReturnValue
fun ServerRequest.ifRangeOrThrow(lazyException: ThrowableSupplier = { RequiredHeaderException(HttpHeaders.IF_RANGE, List::class) }) = ifRange().getOrThrow { lazyException() }
@IgnorableReturnValue
fun ServerRequest.ifUnmodifiedSinceOrThrow(lazyException: ThrowableSupplier = { RequiredHeaderException(HttpHeaders.IF_UNMODIFIED_SINCE, Instant::class) }) = ifUnmodifiedSince().getOrThrow { lazyException() }
@IgnorableReturnValue
fun ServerRequest.jwtTokenOrThrow(headerName: String = HttpHeaders.AUTHORIZATION, lazyException: ThrowableSupplier = { RequiredHeaderException(headerName, Jwt::class) }) = jwtToken(headerName).getOrThrow { lazyException() }
@IgnorableReturnValue
fun ServerRequest.originOrThrow(lazyException: ThrowableSupplier = { RequiredHeaderException(HttpHeaders.ORIGIN, Uri::class) }) = origin().getOrThrow { lazyException() }
@IgnorableReturnValue
fun ServerRequest.preferOrThrow(lazyException: ThrowableSupplier = { RequiredHeaderException("Prefer", List::class) }) = prefer().getOrThrow { lazyException() }
@IgnorableReturnValue
fun ServerRequest.priorityOrThrow(lazyException: ThrowableSupplier = { RequiredHeaderException("Priority", List::class) }) = priority().getOrThrow { lazyException() }
@IgnorableReturnValue
fun ServerRequest.rangeOrThrow(lazyException: ThrowableSupplier = { RequiredHeaderException(HttpHeaders.RANGE, List::class) }) = range().getOrThrow { lazyException() }
@IgnorableReturnValue
fun ServerRequest.refererOrThrow(lazyException: ThrowableSupplier = { RequiredHeaderException(HttpHeaders.REFERER, Url::class) }) = referer().getOrThrow { lazyException() }

@IgnorableReturnValue
context(conversionService: ConversionService)
inline fun <reified T : Any> ServerRequest.queryParamOrThrowAs(name: String) = tryOrThrow({ MalformedQueryParamException(name, T::class) }, includeCause = false, except = [RequiredQueryParamException::class]) { conversionService.convert(queryParamOrThrow(name), T::class.java)!! }
@IgnorableReturnValue
fun ServerRequest.queryParamOrThrowAsStringList(name: String) = queryParams[name].ifNullOrEmpty { throw RequiredQueryParamException(name, List::class) }
@IgnorableReturnValue
fun ServerRequest.queryParamOrThrowAsIntList(name: String) = queryParams[name]?.map(String::toInt).ifNullOrEmpty { throw RequiredQueryParamException(name, List::class) }
@IgnorableReturnValue
fun ServerRequest.queryParamOrThrowAsInt(name: String) = tryOrThrow({ MalformedQueryParamException(name, Int::class) }, includeCause = false, except = [RequiredQueryParamException::class]) { queryParamOrThrow(name, Int::class).toInt() }
@IgnorableReturnValue
fun ServerRequest.queryParamOrThrowAsLong(name: String) = tryOrThrow({ MalformedQueryParamException(name, Long::class) }, includeCause = false, except = [RequiredQueryParamException::class]) { queryParamOrThrow(name, Long::class).toLong() }
@IgnorableReturnValue
fun ServerRequest.queryParamOrThrowAsDouble(name: String) = tryOrThrow({ MalformedQueryParamException(name, Double::class) }, includeCause = false, except = [RequiredQueryParamException::class]) { queryParamOrThrow(name, Double::class).toDouble() }
@IgnorableReturnValue
fun ServerRequest.queryParamOrThrowAsBoolean(name: String) = tryOrThrow({ MalformedQueryParamException(name, Boolean::class) }, includeCause = false, except = [RequiredQueryParamException::class]) { queryParamOrThrow(name, Boolean::class).toBoolean() }
@IgnorableReturnValue
fun ServerRequest.queryParamOrThrowAsUuid(name: String) = queryParamOrThrow(name, Uuid::class).toUuid().getOrThrow(lazyException = { MalformedQueryParamException(name, Uuid::class) })
@IgnorableReturnValue
fun ServerRequest.queryParamOrThrowAsUlid(name: String) = queryParamOrThrow(name, Ulid::class).toUlid().getOrThrow(lazyException = { MalformedQueryParamException(name, Ulid::class) })
@IgnorableReturnValue
fun ServerRequest.queryParamOrThrowAsKsuid(name: String) = queryParamOrThrow(name, Ksuid::class).toKsuid().getOrThrow(lazyException = { MalformedQueryParamException(name, Ksuid::class) })
@IgnorableReturnValue
fun ServerRequest.queryParamOrThrowAsCuid(name: String) = queryParamOrThrow(name, Cuid::class).toCuid().getOrThrow(lazyException = { MalformedQueryParamException(name, Cuid::class) })
@IgnorableReturnValue
fun ServerRequest.queryParamOrThrowAsTsid(name: String) = queryParamOrThrow(name, Tsid::class).toTsid().getOrThrow(lazyException = { MalformedQueryParamException(name, Tsid::class) })
@IgnorableReturnValue
fun ServerRequest.queryParamOrThrowAsDate(name: String) = queryParamOrThrow(name, LocalDate::class).parseToLocalDate().getOrThrow(lazyException = { MalformedQueryParamException(name, LocalDate::class) })
@IgnorableReturnValue
fun ServerRequest.queryParamOrThrowAsTime(name: String) = queryParamOrThrow(name, LocalTime::class).parseToLocalTime().getOrThrow(lazyException = { MalformedQueryParamException(name, LocalTime::class) })
@IgnorableReturnValue
fun ServerRequest.queryParamOrThrowAsDateTime(name: String) = queryParamOrThrow(name, OffsetDateTime::class).parseToOffsetDateTime().getOrThrow(lazyException = { MalformedQueryParamException(name, OffsetDateTime::class) })
@IgnorableReturnValue
inline fun <reified T : Enum<T>> ServerRequest.queryParamOrThrowAsEnum(name: String) = tryOrThrow({ MalformedQueryParamException(name, T::class) }, includeCause = false, except = [RequiredQueryParamException::class]) { queryParamOrThrow(name, T::class).toEnumConst<T>() }

context(conversionService: ConversionService)
inline fun <reified T : Any> ServerRequest.queryParamOrNullAs(name: String) = queryParamOrNull(name)?.let { tryOrThrow({ MalformedQueryParamException(name, T::class) }, includeCause = false) { conversionService.convert(it, T::class.java) } }
fun ServerRequest.queryParamOrNullAsStringList(name: String) = queryParams[name]
fun ServerRequest.queryParamOrNullAsIntList(name: String) = queryParams[name]?.map(String::toInt)
fun ServerRequest.queryParamOrNullAsInt(name: String) = tryOrThrow({ MalformedQueryParamException(name, Int::class) }, includeCause = false) { queryParamOrNull(name)?.toInt() }
fun ServerRequest.queryParamOrNullAsLong(name: String) = tryOrThrow({ MalformedQueryParamException(name, Long::class) }, includeCause = false) { queryParamOrNull(name)?.toLong() }
fun ServerRequest.queryParamOrNullAsDouble(name: String) = tryOrThrow({ MalformedQueryParamException(name, Double::class) }, includeCause = false) { queryParamOrNull(name)?.toDouble() }
fun ServerRequest.queryParamOrNullAsBoolean(name: String) = tryOrThrow({ MalformedQueryParamException(name, Boolean::class) }, includeCause = false) { queryParamOrNull(name)?.toBoolean() }
fun ServerRequest.queryParamOrNullAsUuid(name: String) = queryParamOrNull(name)?.toUuid()?.getOrThrow { MalformedQueryParamException(name, Uuid::class) }
fun ServerRequest.queryParamOrNullAsUlid(name: String) = queryParamOrNull(name)?.toUlid()?.getOrThrow { MalformedQueryParamException(name, Ulid::class) }
fun ServerRequest.queryParamOrNullAsKsuid(name: String) = queryParamOrNull(name)?.toKsuid()?.getOrThrow { MalformedQueryParamException(name, Ksuid::class) }
fun ServerRequest.queryParamOrNullAsCuid(name: String) = queryParamOrNull(name)?.toCuid()?.getOrThrow { MalformedQueryParamException(name, Cuid::class) }
fun ServerRequest.queryParamOrNullAsTsid(name: String) = queryParamOrNull(name)?.toTsid()?.getOrThrow { MalformedQueryParamException(name, Tsid::class) }
fun ServerRequest.queryParamOrNullAsDate(name: String) = queryParamOrNull(name)?.parseToLocalDate()?.getOrThrow { MalformedQueryParamException(name, LocalDate::class) }
fun ServerRequest.queryParamOrNullAsTime(name: String) = queryParamOrNull(name)?.parseToLocalTime()?.getOrThrow { MalformedQueryParamException(name, LocalTime::class) }
fun ServerRequest.queryParamOrNullAsDateTime(name: String) = queryParamOrNull(name)?.parseToOffsetDateTime()?.getOrThrow { MalformedQueryParamException(name, OffsetDateTime::class) }
inline fun <reified T : Enum<T>> ServerRequest.queryParamOrNullAsEnum(name: String) = tryOrThrow({ MalformedQueryParamException(name, T::class) }) { queryParamOrNull(name)?.toEnumConst<T>() }

context(conversionService: ConversionService)
inline fun <reified T : Any> ServerRequest.queryParamOrErrorAs(name: String) = queryParamOrError(name).map { tryOrThrow({ MalformedQueryParamException(name, T::class) }, includeCause = false) { conversionService.convert(it, T::class.java)!! } }
fun ServerRequest.queryParamOrErrorAsStringList(name: String) = queryParamOrError(name).map { it.splitAndTrim(Char.COMMA) }
fun ServerRequest.queryParamOrErrorAsInt(name: String) = tryOrThrow({ MalformedQueryParamException(name, Int::class) }, includeCause = false) { queryParamOrError(name).map { it.toInt() } }
fun ServerRequest.queryParamOrErrorAsLong(name: String) = tryOrThrow({ MalformedQueryParamException(name, Long::class) }, includeCause = false) { queryParamOrError(name).map { it.toLong() } }
fun ServerRequest.queryParamOrErrorAsDouble(name: String) = tryOrThrow({ MalformedQueryParamException(name, Double::class) }, includeCause = false) { queryParamOrError(name).map { it.toDouble() } }
fun ServerRequest.queryParamOrErrorAsBoolean(name: String) = tryOrThrow({ MalformedQueryParamException(name, Boolean::class) }, includeCause = false) { queryParamOrError(name).map { it.toBoolean() } }
fun ServerRequest.queryParamOrErrorAsUuid(name: String) = queryParamOrError(name).map { it.toUuid().getOrThrow { MalformedQueryParamException(name, Uuid::class) } }
fun ServerRequest.queryParamOrErrorAsUlid(name: String) = queryParamOrError(name).map { it.toUlid().getOrThrow { MalformedQueryParamException(name, Ulid::class) } }
fun ServerRequest.queryParamOrErrorAsKsuid(name: String) = queryParamOrError(name).map { it.toKsuid().getOrThrow { MalformedQueryParamException(name, Ksuid::class) } }
fun ServerRequest.queryParamOrErrorAsCuid(name: String) = queryParamOrError(name).map { it.toCuid().getOrThrow { MalformedQueryParamException(name, Cuid::class) } }
fun ServerRequest.queryParamOrErrorAsTsid(name: String) = queryParamOrError(name).map { it.toTsid().getOrThrow { MalformedQueryParamException(name, Tsid::class) } }
fun ServerRequest.queryParamOrErrorAsDate(name: String) = queryParamOrError(name).map { it.parseToLocalDate().getOrThrow { MalformedQueryParamException(name, LocalDate::class) } }
fun ServerRequest.queryParamOrErrorAsTime(name: String) = queryParamOrError(name).map { it.parseToLocalTime().getOrThrow { MalformedQueryParamException(name, LocalTime::class) } }
fun ServerRequest.queryParamOrErrorAsDateTime(name: String) = queryParamOrError(name).map { it.parseToOffsetDateTime().getOrThrow { MalformedQueryParamException(name, OffsetDateTime::class) } }
inline fun <reified T : Enum<T>> ServerRequest.queryParamOrErrorAsEnum(name: String) = tryOrThrow({ MalformedQueryParamException(name, T::class) }) { queryParamOrError(name).map { it.toEnumConst<T>() } }

context(conversionService: ConversionService)
inline fun <reified T : Any> ServerRequest.queryParamOrDefaultAs(name: String, defaultValue: Supplier<T>) = tryOrThrow({ MalformedQueryParamException(name, T::class) }, includeCause = false) { conversionService.convert(queryParamOrNull(name), T::class.java) } ?: defaultValue()
fun ServerRequest.paramOrDefaultAsStringList(name: String, defaultValue: Supplier<List<String>>) = queryParams[name] ?: defaultValue()
fun ServerRequest.paramOrDefaultAsIntList(name: String, defaultValue: Supplier<List<Int>>) = queryParams[name]?.map(String::toInt) ?: defaultValue()
fun ServerRequest.paramOrDefaultAsInt(name: String, defaultValue: Supplier<Int>) = tryOrThrow({ MalformedQueryParamException(name, Int::class) }, includeCause = false) { queryParamOrNull(name)?.toInt() ?: defaultValue() }
fun ServerRequest.paramOrDefaultAsLong(name: String, defaultValue: Supplier<Long>) = tryOrThrow({ MalformedQueryParamException(name, Long::class) }, includeCause = false) { queryParamOrNull(name)?.toLong() ?: defaultValue() }
fun ServerRequest.paramOrDefaultAsDouble(name: String, defaultValue: Supplier<Double>) = tryOrThrow({ MalformedQueryParamException(name, Double::class) }, includeCause = false) { queryParamOrNull(name)?.toDouble() ?: defaultValue() }
fun ServerRequest.paramOrDefaultAsBoolean(name: String, defaultValue: Supplier<Boolean>) = tryOrThrow({ MalformedQueryParamException(name, Boolean::class) }, includeCause = false) { queryParamOrNull(name)?.toBoolean() ?: defaultValue() }
fun ServerRequest.paramOrDefaultAsUuid(name: String, defaultValue: Supplier<Uuid>) = queryParamOrNull(name)?.toUuid()?.getOrThrow { MalformedQueryParamException(name, Uuid::class) } ?: defaultValue()
fun ServerRequest.paramOrDefaultAsUlid(name: String, defaultValue: Supplier<Ulid>) = queryParamOrNull(name)?.toUlid()?.getOrThrow { MalformedQueryParamException(name, Ulid::class) } ?: defaultValue()
fun ServerRequest.paramOrDefaultAsKsuid(name: String, defaultValue: Supplier<Ksuid>) = queryParamOrNull(name)?.toKsuid()?.getOrThrow { MalformedQueryParamException(name, Ksuid::class) } ?: defaultValue()
fun ServerRequest.paramOrDefaultAsCuid(name: String, defaultValue: Supplier<Cuid>) = queryParamOrNull(name)?.toCuid()?.getOrThrow { MalformedQueryParamException(name, Cuid::class) } ?: defaultValue()
fun ServerRequest.paramOrDefaultAsTsid(name: String, defaultValue: Supplier<Tsid>) = queryParamOrNull(name)?.toTsid()?.getOrThrow { MalformedQueryParamException(name, Tsid::class) } ?: defaultValue()
fun ServerRequest.paramOrDefaultAsDate(name: String, defaultValue: Supplier<LocalDate>) = queryParamOrNull(name)?.parseToLocalDate()?.getOrThrow { MalformedQueryParamException(name, LocalDate::class) } ?: defaultValue()
fun ServerRequest.paramOrDefaultAsTime(name: String, defaultValue: Supplier<LocalTime>) = queryParamOrNull(name)?.parseToLocalTime()?.getOrThrow { MalformedQueryParamException(name, LocalTime::class) } ?: defaultValue()
fun ServerRequest.paramOrDefaultAsDateTime(name: String, defaultValue: Supplier<OffsetDateTime>) = queryParamOrNull(name)?.parseToOffsetDateTime()?.getOrThrow { MalformedQueryParamException(name, OffsetDateTime::class) } ?: defaultValue()
inline fun <reified T : Enum<T>> ServerRequest.paramOrDefaultAsEnum(name: String, crossinline defaultValue: () -> T) = tryOrThrow({ MalformedQueryParamException(name, T::class) }, includeCause = false) { queryParamOrNull(name)?.toEnumConst<T>() ?: defaultValue() }


@IgnorableReturnValue
context(conversionService: ConversionService)
inline fun <reified T : Any> ServerRequest.pathVariableOrThrowAs(name: String) = tryOrThrow({ MalformedPathVariableException(name, T::class) }, includeCause = false, except = [RequiredPathVariableException::class]) { conversionService.convert(pathVariableOrThrow(name), T::class.java) }
@IgnorableReturnValue
fun ServerRequest.pathVariableOrThrowAsStringList(name: String) = pathVariableOrThrow(name, List::class).splitAndTrim(Char.COMMA)
@IgnorableReturnValue
fun ServerRequest.pathVariableOrThrowAsInt(name: String) = tryOrThrow({ MalformedPathVariableException(name, Int::class) }, includeCause = false, except = [RequiredPathVariableException::class]) { pathVariableOrThrow(name, Int::class).toInt() }
@IgnorableReturnValue
fun ServerRequest.pathVariableOrThrowAsLong(name: String) = tryOrThrow({ MalformedPathVariableException(name, Long::class) }, includeCause = false, except = [RequiredPathVariableException::class]) { pathVariableOrThrow(name, Long::class).toLong() }
@IgnorableReturnValue
fun ServerRequest.pathVariableOrThrowAsDouble(name: String) = tryOrThrow({ MalformedPathVariableException(name, Double::class) }, includeCause = false, except = [RequiredPathVariableException::class]) { pathVariableOrThrow(name, Double::class).toDouble() }
@IgnorableReturnValue
fun ServerRequest.pathVariableOrThrowAsBoolean(name: String) = tryOrThrow({ MalformedPathVariableException(name, Boolean::class) }, includeCause = false, except = [RequiredPathVariableException::class]) { pathVariableOrThrow(name, Boolean::class).toBoolean() }
@IgnorableReturnValue
fun ServerRequest.pathVariableOrThrowAsUuid(name: String) = pathVariableOrThrow(name, Uuid::class).toUuid().getOrThrow(lazyException = { MalformedPathVariableException(name, Uuid::class) })
@IgnorableReturnValue
fun ServerRequest.pathVariableOrThrowAsUlid(name: String) = pathVariableOrThrow(name, Ulid::class).toUlid().getOrThrow(lazyException = { MalformedPathVariableException(name, Ulid::class) })
@IgnorableReturnValue
fun ServerRequest.pathVariableOrThrowAsKsuid(name: String) = pathVariableOrThrow(name, Ksuid::class).toKsuid().getOrThrow(lazyException = { MalformedPathVariableException(name, Ksuid::class) })
@IgnorableReturnValue
fun ServerRequest.pathVariableOrThrowAsCuid(name: String) = pathVariableOrThrow(name, Cuid::class).toCuid().getOrThrow(lazyException = { MalformedPathVariableException(name, Cuid::class) })
@IgnorableReturnValue
fun ServerRequest.pathVariableOrThrowAsTsid(name: String) = pathVariableOrThrow(name, Tsid::class).toTsid().getOrThrow(lazyException = { MalformedPathVariableException(name, Tsid::class) })
@IgnorableReturnValue
fun ServerRequest.pathVariableOrThrowAsDate(name: String) = pathVariableOrThrow(name, LocalDate::class).parseToLocalDate().getOrThrow(lazyException = { MalformedPathVariableException(name, LocalDate::class) })
@IgnorableReturnValue
fun ServerRequest.pathVariableOrThrowAsTime(name: String) = pathVariableOrThrow(name, LocalTime::class).parseToLocalTime().getOrThrow(lazyException = { MalformedPathVariableException(name, LocalTime::class) })
@IgnorableReturnValue
fun ServerRequest.pathVariableOrThrowAsDateTime(name: String) = pathVariableOrThrow(name, OffsetDateTime::class).parseToOffsetDateTime().getOrThrow(lazyException = { MalformedPathVariableException(name, OffsetDateTime::class) })
@IgnorableReturnValue
inline fun <reified T : Enum<T>> ServerRequest.pathVariableOrThrowAsEnum(name: String) = tryOrThrow({ MalformedPathVariableException(name, T::class) }, includeCause = false, except = [RequiredPathVariableException::class]) { pathVariableOrThrow(name, T::class).toEnumConst<T>() }

context(conversionService: ConversionService)
inline fun <reified T : Any> ServerRequest.pathVariableOrNullAs(name: String) = pathVariableOrNull(name)?.let { tryOrThrow({ MalformedPathVariableException(name, T::class) }, includeCause = false) { conversionService.convert(it, T::class.java) } }
fun ServerRequest.pathVariableOrNullAsStringList(name: String) = pathVariableOrNull(name)?.splitAndTrim(Char.COMMA)
fun ServerRequest.pathVariableOrNullAsInt(name: String) = tryOrThrow({ MalformedPathVariableException(name, Int::class) }, includeCause = false) { pathVariableOrNull(name)?.toInt() }
fun ServerRequest.pathVariableOrNullAsLong(name: String) = tryOrThrow({ MalformedPathVariableException(name, Long::class) }, includeCause = false) { pathVariableOrNull(name)?.toLong() }
fun ServerRequest.pathVariableOrNullAsDouble(name: String) = tryOrThrow({ MalformedPathVariableException(name, Double::class) }, includeCause = false) { pathVariableOrNull(name)?.toDouble() }
fun ServerRequest.pathVariableOrNullAsBoolean(name: String) = tryOrThrow({ MalformedPathVariableException(name, Boolean::class) }, includeCause = false) { pathVariableOrNull(name)?.toBoolean() }
fun ServerRequest.pathVariableOrNullAsUuid(name: String) = pathVariableOrNull(name)?.toUuid()?.getOrThrow { MalformedPathVariableException(name, Uuid::class) }
fun ServerRequest.pathVariableOrNullAsUlid(name: String) = pathVariableOrNull(name)?.toUlid()?.getOrThrow { MalformedPathVariableException(name, Ulid::class) }
fun ServerRequest.pathVariableOrNullAsKsuid(name: String) = pathVariableOrNull(name)?.toKsuid()?.getOrThrow { MalformedPathVariableException(name, Ksuid::class) }
fun ServerRequest.pathVariableOrNullAsCuid(name: String) = pathVariableOrNull(name)?.toCuid()?.getOrThrow { MalformedPathVariableException(name, Cuid::class) }
fun ServerRequest.pathVariableOrNullAsTsid(name: String) = pathVariableOrNull(name)?.toTsid()?.getOrThrow { MalformedPathVariableException(name, Tsid::class) }
fun ServerRequest.pathVariableOrNullAsDate(name: String) = pathVariableOrNull(name)?.parseToLocalDate()?.getOrThrow { MalformedPathVariableException(name, LocalDate::class) }
fun ServerRequest.pathVariableOrNullAsTime(name: String) = pathVariableOrNull(name)?.parseToLocalTime()?.getOrThrow { MalformedPathVariableException(name, LocalTime::class) }
fun ServerRequest.pathVariableOrNullAsDateTime(name: String) = pathVariableOrNull(name)?.parseToOffsetDateTime()?.getOrThrow { MalformedPathVariableException(name, OffsetDateTime::class) }
inline fun <reified T : Enum<T>> ServerRequest.pathVariableOrNullAsEnum(name: String) = tryOrThrow({ MalformedPathVariableException(name, T::class) }) { pathVariableOrNull(name)?.toEnumConst<T>() }

context(conversionService: ConversionService)
inline fun <reified T : Any> ServerRequest.pathVariableOrErrorAs(name: String) = pathVariableOrError(name).map { tryOrThrow({ MalformedPathVariableException(name, T::class) }, includeCause = false) { conversionService.convert(it, T::class.java)!! } }
fun ServerRequest.pathVariableOrErrorAsStringList(name: String) = pathVariableOrError(name).map { it.splitAndTrim(Char.COMMA) }
fun ServerRequest.pathVariableOrErrorAsInt(name: String) = tryOrThrow({ MalformedPathVariableException(name, Int::class) }, includeCause = false) { pathVariableOrError(name).map { it.toInt() } }
fun ServerRequest.pathVariableOrErrorAsLong(name: String) = tryOrThrow({ MalformedPathVariableException(name, Long::class) }, includeCause = false) { pathVariableOrError(name).map { it.toLong() } }
fun ServerRequest.pathVariableOrErrorAsDouble(name: String) = tryOrThrow({ MalformedPathVariableException(name, Double::class) }, includeCause = false) { pathVariableOrError(name).map { it.toDouble() } }
fun ServerRequest.pathVariableOrErrorAsBoolean(name: String) = tryOrThrow({ MalformedPathVariableException(name, Boolean::class) }, includeCause = false) { pathVariableOrError(name).map { it.toBoolean() } }
fun ServerRequest.pathVariableOrErrorAsUuid(name: String) = pathVariableOrError(name).map { it.toUuid().getOrThrow { MalformedPathVariableException(name, Uuid::class) } }
fun ServerRequest.pathVariableOrErrorAsUlid(name: String) = pathVariableOrError(name).map { it.toUlid().getOrThrow { MalformedPathVariableException(name, Ulid::class) } }
fun ServerRequest.pathVariableOrErrorAsKsuid(name: String) = pathVariableOrError(name).map { it.toKsuid().getOrThrow { MalformedPathVariableException(name, Ksuid::class) } }
fun ServerRequest.pathVariableOrErrorAsCuid(name: String) = pathVariableOrError(name).map { it.toCuid().getOrThrow { MalformedPathVariableException(name, Cuid::class) } }
fun ServerRequest.pathVariableOrErrorAsTsid(name: String) = pathVariableOrError(name).map { it.toTsid().getOrThrow { MalformedPathVariableException(name, Tsid::class) } }
fun ServerRequest.pathVariableOrErrorAsDate(name: String) = pathVariableOrError(name).map { it.parseToLocalDate().getOrThrow { MalformedPathVariableException(name, LocalDate::class) } }
fun ServerRequest.pathVariableOrErrorAsTime(name: String) = pathVariableOrError(name).map { it.parseToLocalTime().getOrThrow { MalformedPathVariableException(name, LocalTime::class) } }
fun ServerRequest.pathVariableOrErrorAsDateTime(name: String) = pathVariableOrError(name).map { it.parseToOffsetDateTime().getOrThrow { MalformedPathVariableException(name, OffsetDateTime::class) } }
inline fun <reified T : Enum<T>> ServerRequest.pathVariableOrErrorAsEnum(name: String) = tryOrThrow({ MalformedPathVariableException(name, T::class) }) { pathVariableOrError(name).map { it.toEnumConst<T>() } }

context(conversionService: ConversionService)
inline fun <reified T : Any> ServerRequest.pathVariableOrDefaultAs(name: String, defaultValue: Supplier<T>) = tryOrThrow({ MalformedPathVariableException(name, T::class) }, includeCause = false) { conversionService.convert(pathVariableOrNull(name), T::class.java) } ?: defaultValue()
fun ServerRequest.pathVariableOrDefaultAsStringList(name: String, defaultValue: Supplier<List<String>>) = pathVariableOrNull(name)?.splitAndTrim(Char.COMMA) ?: defaultValue()
fun ServerRequest.pathVariableOrDefaultAsInt(name: String, defaultValue: Supplier<Int>) = tryOrThrow({ MalformedPathVariableException(name, Int::class) }, includeCause = false) { pathVariableOrNull(name)?.toInt() ?: defaultValue() }
fun ServerRequest.pathVariableOrDefaultAsLong(name: String, defaultValue: Supplier<Long>) = tryOrThrow({ MalformedPathVariableException(name, Long::class) }, includeCause = false) { pathVariableOrNull(name)?.toLong() ?: defaultValue() }
fun ServerRequest.pathVariableOrDefaultAsDouble(name: String, defaultValue: Supplier<Double>) = tryOrThrow({ MalformedPathVariableException(name, Double::class) }, includeCause = false) { pathVariableOrNull(name)?.toDouble() ?: defaultValue() }
fun ServerRequest.pathVariableOrDefaultAsBoolean(name: String, defaultValue: Supplier<Boolean>) = tryOrThrow({ MalformedPathVariableException(name, Boolean::class) }, includeCause = false) { pathVariableOrNull(name)?.toBoolean() ?: defaultValue() }
fun ServerRequest.pathVariableOrDefaultAsUuid(name: String, defaultValue: Supplier<Uuid>) = pathVariableOrNull(name)?.toUuid()?.getOrThrow { MalformedPathVariableException(name, Uuid::class) } ?: defaultValue()
fun ServerRequest.pathVariableOrDefaultAsUlid(name: String, defaultValue: Supplier<Ulid>) = pathVariableOrNull(name)?.toUlid()?.getOrThrow { MalformedPathVariableException(name, Ulid::class) } ?: defaultValue()
fun ServerRequest.pathVariableOrDefaultAsKsuid(name: String, defaultValue: Supplier<Ksuid>) = pathVariableOrNull(name)?.toKsuid()?.getOrThrow { MalformedPathVariableException(name, Ksuid::class) } ?: defaultValue()
fun ServerRequest.pathVariableOrDefaultAsCuid(name: String, defaultValue: Supplier<Cuid>) = pathVariableOrNull(name)?.toCuid()?.getOrThrow { MalformedPathVariableException(name, Cuid::class) } ?: defaultValue()
fun ServerRequest.pathVariableOrDefaultAsTsid(name: String, defaultValue: Supplier<Tsid>) = pathVariableOrNull(name)?.toTsid()?.getOrThrow { MalformedPathVariableException(name, Tsid::class) } ?: defaultValue()
fun ServerRequest.pathVariableOrDefaultAsDate(name: String, defaultValue: Supplier<LocalDate>) = pathVariableOrNull(name)?.parseToLocalDate()?.getOrThrow { MalformedPathVariableException(name, LocalDate::class) } ?: defaultValue()
fun ServerRequest.pathVariableOrDefaultAsTime(name: String, defaultValue: Supplier<LocalTime>) = pathVariableOrNull(name)?.parseToLocalTime()?.getOrThrow { MalformedPathVariableException(name, LocalTime::class) } ?: defaultValue()
fun ServerRequest.pathVariableOrDefaultAsDateTime(name: String, defaultValue: Supplier<OffsetDateTime>) = pathVariableOrNull(name)?.parseToOffsetDateTime()?.getOrThrow { MalformedPathVariableException(name, OffsetDateTime::class) } ?: defaultValue()
inline fun <reified T : Enum<T>> ServerRequest.pathVariableOrDefaultAsEnum(name: String, crossinline defaultValue: () -> T) = tryOrThrow({ MalformedPathVariableException(name, T::class) }, includeCause = false) { pathVariableOrNull(name)?.toEnumConst<T>() ?: defaultValue() }