@file:JvmName("ReactiveRequestUtilsKt")
@file:Since("2.0.0")
@file:Suppress("unused")

package dev.tommasop1804.springutils.reactive.request

import dev.tommasop1804.kutils.COMMA
import dev.tommasop1804.kutils.StringList
import dev.tommasop1804.kutils.Supplier
import dev.tommasop1804.kutils.ThrowableSupplier
import dev.tommasop1804.kutils.UUID
import dev.tommasop1804.kutils.annotations.Since
import dev.tommasop1804.kutils.asSingleList
import dev.tommasop1804.kutils.classes.identifiers.CUID
import dev.tommasop1804.kutils.classes.identifiers.CUID.Companion.toCUID
import dev.tommasop1804.kutils.classes.identifiers.KSUID
import dev.tommasop1804.kutils.classes.identifiers.KSUID.Companion.toKSUID
import dev.tommasop1804.kutils.classes.identifiers.ShortUUID
import dev.tommasop1804.kutils.classes.identifiers.ShortUUID.Companion.toShortUUID
import dev.tommasop1804.kutils.classes.identifiers.ULID
import dev.tommasop1804.kutils.classes.identifiers.ULID.Companion.toULID
import dev.tommasop1804.kutils.exceptions.RequiredHeaderException
import dev.tommasop1804.kutils.exceptions.RequiredParameterException
import dev.tommasop1804.kutils.firstOrThrow
import dev.tommasop1804.kutils.getOrThrow
import dev.tommasop1804.kutils.ifNullOrEmpty
import dev.tommasop1804.kutils.invoke
import dev.tommasop1804.kutils.parseToLocalDate
import dev.tommasop1804.kutils.parseToOffsetDateTime
import dev.tommasop1804.kutils.splitAndTrim
import dev.tommasop1804.kutils.toEnumConst
import dev.tommasop1804.kutils.toUUID
import dev.tommasop1804.kutils.tryOr
import dev.tommasop1804.kutils.tryOrNull
import dev.tommasop1804.kutils.tryOrThrow
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.queryParamOrNull
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.reflect.KClass

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
 * @since 2.0.0
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
 * @since 2.0.0
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
 * @since 2.0.0
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
 * @since 2.0.0
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
 * @since 2.0.0
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
 * @since 2.0.0
 */
fun ServerRequest.pathVariableOrDefault(name: String, defaultValue: Supplier<Any>) =
    tryOr({ defaultValue().toString() }) { pathVariable(name) }
/**
 * Retrieves the value of a path variable with the given name from the server request.
 * If the path variable is not found or an exception occurs, returns null.
 *
 * @param name the name of the path variable to retrieve
 * @return the value of the path variable, or null if not found or an exception occurs
 * @since 2.0.0
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
 * @since 2.0.0
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
 * @since 2.0.0
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
 * @since 2.0.0
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
 * @since 2.0.0
 */
fun ServerRequest.header(name: String): StringList = tryOrNull { headers().header(name) }.orEmpty()

fun ServerRequest.queryParamOrThrowAsStringList(name: String) = queryParamOrThrow(name, StringList::class).splitAndTrim(Char.COMMA)
fun ServerRequest.queryParamOrThrowAsIntOrThrow(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, Int::class) }, includeCause = false, notOverwrite = RequiredQueryParamException::class) { queryParamOrThrow(name, Int::class).toInt() }
fun ServerRequest.queryParamOrThrowAsLongOrThrow(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, Long::class) }, includeCause = false, notOverwrite = RequiredQueryParamException::class) { queryParamOrThrow(name, Long::class).toLong() }
fun ServerRequest.queryParamOrThrowAsDoubleOrThrow(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, Double::class) }, includeCause = false, notOverwrite = RequiredQueryParamException::class) { queryParamOrThrow(name, Double::class).toDouble() }
fun ServerRequest.queryParamOrThrowAsBooleanOrThrow(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, Boolean::class) }, includeCause = false, notOverwrite = RequiredQueryParamException::class) { queryParamOrThrow(name, Boolean::class).toBoolean() }
fun ServerRequest.queryParamOrThrowAsUuidOrThrow(name: String) = queryParamOrThrow(name, UUID::class).toUUID()() { MalformedQueryParamException(name, UUID::class) }
fun ServerRequest.queryParamOrThrowAsUlidOrThrow(name: String) = queryParamOrThrow(name, ULID::class).toULID()() { MalformedQueryParamException(name, ULID::class) }
fun ServerRequest.queryParamOrThrowAsKsuidOrThrow(name: String) = queryParamOrThrow(name, KSUID::class).toKSUID()() { MalformedQueryParamException(name, KSUID::class) }
fun ServerRequest.queryParamOrThrowAsCuidOrThrow(name: String) = queryParamOrThrow(name, CUID::class).toCUID()() { MalformedQueryParamException(name, CUID::class) }
fun ServerRequest.queryParamOrThrowAsDateOrThrow(name: String) = queryParamOrThrow(name, LocalDate::class).parseToLocalDate()() { MalformedQueryParamException(name, LocalDate::class) }
fun ServerRequest.queryParamOrThrowAsDateTimeOrThrow(name: String) = queryParamOrThrow(name, OffsetDateTime::class).parseToOffsetDateTime()() { MalformedQueryParamException(name, OffsetDateTime::class) }
inline fun <reified T : Enum<T>> ServerRequest.queryParamOrThrowAsEnumOrThrow(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, T::class) }, includeCause = false, notOverwrite = RequiredQueryParamException::class) { queryParamOrThrow(name, T::class).toEnumConst<T>() }

fun ServerRequest.queryParamOrThrowAsIntOrNull(name: String) = tryOrNull(notOverwrite = RequiredQueryParamException::class) { queryParamOrThrow(name, Int::class).toInt() }
fun ServerRequest.queryParamOrThrowAsLongOrNull(name: String) = tryOrNull(notOverwrite = RequiredQueryParamException::class) { queryParamOrThrow(name, Long::class).toLong() }
fun ServerRequest.queryParamOrThrowAsDoubleOrNull(name: String) = tryOrNull(notOverwrite = RequiredQueryParamException::class) { queryParamOrThrow(name, Double::class).toDouble() }
fun ServerRequest.queryParamOrThrowAsBooleanOrNull(name: String) = tryOrNull(notOverwrite = RequiredQueryParamException::class) { queryParamOrThrow(name, Boolean::class).toBoolean() }
fun ServerRequest.queryParamOrThrowAsUuidOrNull(name: String) = queryParamOrThrow(name, UUID::class).toUUID().getOrNull()
fun ServerRequest.queryParamOrThrowAsUlidOrNull(name: String) = queryParamOrThrow(name, ULID::class).toULID().getOrNull()
fun ServerRequest.queryParamOrThrowAsKsuidOrNull(name: String) = queryParamOrThrow(name, KSUID::class).toKSUID().getOrNull()
fun ServerRequest.queryParamOrThrowAsCuidOrNull(name: String) = queryParamOrThrow(name, CUID::class).toCUID().getOrNull()
fun ServerRequest.queryParamOrThrowAsDateOrNull(name: String) = queryParamOrThrow(name, LocalDate::class).parseToLocalDate().getOrNull()
fun ServerRequest.queryParamOrThrowAsDateTimeOrNull(name: String) = queryParamOrThrow(name, OffsetDateTime::class).parseToOffsetDateTime().getOrNull()
inline fun <reified T : Enum<T>> ServerRequest.queryParamOrThrowAsEnumOrNull(name: String) = tryOrNull { queryParamOrThrow(name, T::class).toEnumConst<T>() }

fun ServerRequest.queryParamOrNullAsStringList(name: String) = queryParamOrNull(name)?.splitAndTrim(Char.COMMA)
fun ServerRequest.queryParamOrNullAsIntOrNull(name: String) = tryOrNull { queryParamOrNull(name)?.toInt() }
fun ServerRequest.queryParamOrNullAsLongOrNull(name: String) = tryOrNull { queryParamOrNull(name)?.toLong() }
fun ServerRequest.queryParamOrNullAsDoubleOrNull(name: String) = tryOrNull { queryParamOrNull(name)?.toDouble() }
fun ServerRequest.queryParamOrNullAsBooleanOrNull(name: String) = tryOrNull { queryParamOrNull(name)?.toBoolean() }
fun ServerRequest.queryParamOrNullAsUuidOrNull(name: String) = queryParamOrNull(name)?.toUUID()?.getOrNull()
fun ServerRequest.queryParamOrNullAsUlidOrNull(name: String) = queryParamOrNull(name)?.toULID()?.getOrNull()
fun ServerRequest.queryParamOrNullAsKsuidOrNull(name: String) = queryParamOrNull(name)?.toKSUID()?.getOrNull()
fun ServerRequest.queryParamOrNullAsCuidOrNull(name: String) = queryParamOrNull(name)?.toCUID()?.getOrNull()
fun ServerRequest.queryParamOrNullAsDateOrNull(name: String) = queryParamOrNull(name)?.parseToLocalDate()?.getOrNull()
fun ServerRequest.queryParamOrNullAsDateTimeOrNull(name: String) = queryParamOrNull(name)?.parseToOffsetDateTime()?.getOrNull()
inline fun <reified T : Enum<T>> ServerRequest.queryParamOrNullAsEnumOrNull(name: String) = tryOrNull { queryParamOrNull(name)?.toEnumConst<T>() }

fun ServerRequest.queryParamOrNullAsIntOrThrow(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, Int::class) }, includeCause = false) { queryParamOrNull(name)?.toInt() }
fun ServerRequest.queryParamOrNullAsLongOrThrow(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, Long::class) }, includeCause = false) { queryParamOrNull(name)?.toLong() }
fun ServerRequest.queryParamOrNullAsDoubleOrThrow(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, Double::class) }, includeCause = false) { queryParamOrNull(name)?.toDouble() }
fun ServerRequest.queryParamOrNullAsBooleanOrThrow(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, Boolean::class) }, includeCause = false) { queryParamOrNull(name)?.toBoolean() }
fun ServerRequest.queryParamOrNullAsUuidOrThrow(name: String) = queryParamOrNull(name)?.toUUID()?.getOrThrow { MalformedQueryParamException(name, UUID::class) }
fun ServerRequest.queryParamOrNullAsUlidOrThrow(name: String) = queryParamOrNull(name)?.toULID()?.getOrThrow { MalformedQueryParamException(name, ULID::class) }
fun ServerRequest.queryParamOrNullAsKsuidOrThrow(name: String) = queryParamOrNull(name)?.toKSUID()?.getOrThrow { MalformedQueryParamException(name, KSUID::class) }
fun ServerRequest.queryParamOrNullAsCuidOrThrow(name: String) = queryParamOrNull(name)?.toCUID()?.getOrThrow { MalformedQueryParamException(name, CUID::class) }
fun ServerRequest.queryParamOrNullAsDateOrThrow(name: String) = queryParamOrNull(name)?.parseToLocalDate()?.getOrThrow { MalformedQueryParamException(name, LocalDate::class) }
fun ServerRequest.queryParamOrNullAsDateTimeOrThrow(name: String) = queryParamOrNull(name)?.parseToOffsetDateTime()?.getOrThrow { MalformedQueryParamException(name, OffsetDateTime::class) }
inline fun <reified T : Enum<T>> ServerRequest.queryParamOrNullAsEnumOrThrow(name: String) = tryOrThrow({ -> MalformedQueryParamException(name, T::class) }) { queryParamOrNull(name)?.toEnumConst<T>() }

fun ServerRequest.queryParamOrDefaultAsStringList(name: String, defaultValue: Supplier<StringList>) = queryParamOrNull(name)?.splitAndTrim(Char.COMMA) ?: defaultValue()
fun ServerRequest.queryParamOrDefaultAsInt(name: String, defaultValue: Supplier<Int>) = tryOr({ defaultValue() }) { queryParamOrNull(name)?.toInt() ?: defaultValue() }
fun ServerRequest.queryParamOrDefaultAsLong(name: String, defaultValue: Supplier<Long>) = tryOr({ defaultValue() }) { queryParamOrNull(name)?.toLong() ?: defaultValue() }
fun ServerRequest.queryParamOrDefaultAsDouble(name: String, defaultValue: Supplier<Double>) = tryOr({ defaultValue() }) { queryParamOrNull(name)?.toDouble() ?: defaultValue() }
fun ServerRequest.queryParamOrDefaultAsBoolean(name: String, defaultValue: Supplier<Boolean>) = tryOr({ defaultValue() }) { queryParamOrNull(name)?.toBoolean() ?: defaultValue() }
fun ServerRequest.queryParamOrDefaultAsUuid(name: String, defaultValue: Supplier<UUID>) = queryParamOrNull(name)?.toUUID()?.getOrNull() ?: defaultValue()
fun ServerRequest.queryParamOrDefaultAsUlid(name: String, defaultValue: Supplier<ULID>) = queryParamOrNull(name)?.toULID()?.getOrNull() ?: defaultValue()
fun ServerRequest.queryParamOrDefaultAsKsuid(name: String, defaultValue: Supplier<KSUID>) = queryParamOrNull(name)?.toKSUID()?.getOrNull() ?: defaultValue()
fun ServerRequest.queryParamOrDefaultAsCuid(name: String, defaultValue: Supplier<CUID>) = queryParamOrNull(name)?.toCUID()?.getOrNull() ?: defaultValue()
fun ServerRequest.queryParamOrDefaultAsDate(name: String, defaultValue: Supplier<LocalDate>) = queryParamOrNull(name)?.parseToLocalDate()?.getOrNull() ?: defaultValue()
fun ServerRequest.queryParamOrDefaultAsDateTime(name: String, defaultValue: Supplier<OffsetDateTime>) = queryParamOrNull(name)?.parseToOffsetDateTime()?.getOrNull() ?: defaultValue()
inline fun <reified T : Enum<T>> ServerRequest.queryParamOrDefaultAsEnum(name: String, crossinline defaultValue: () -> T) = tryOr({ defaultValue() }) { queryParamOrNull(name)?.toEnumConst<T>() ?: defaultValue() }

fun ServerRequest.queryParamOrDefaultAsIntOrThrow(name: String, defaultValue: Supplier<Int>) = tryOrThrow({ -> MalformedQueryParamException(name, Int::class) }, includeCause = false) { queryParamOrNull(name)?.toInt() ?: defaultValue() }
fun ServerRequest.queryParamOrDefaultAsLongOrThrow(name: String, defaultValue: Supplier<Long>) = tryOrThrow({ -> MalformedQueryParamException(name, Long::class) }, includeCause = false) { queryParamOrNull(name)?.toLong() ?: defaultValue() }
fun ServerRequest.queryParamOrDefaultAsDoubleOrThrow(name: String, defaultValue: Supplier<Double>) = tryOrThrow({ -> MalformedQueryParamException(name, Double::class) }, includeCause = false) { queryParamOrNull(name)?.toDouble() ?: defaultValue() }
fun ServerRequest.queryParamOrDefaultAsBooleanOrThrow(name: String, defaultValue: Supplier<Boolean>) = tryOrThrow({ -> MalformedQueryParamException(name, Boolean::class) }, includeCause = false) { queryParamOrNull(name)?.toBoolean() ?: defaultValue() }
fun ServerRequest.queryParamOrDefaultAsUuidOrThrow(name: String, defaultValue: Supplier<UUID>) = queryParamOrNull(name)?.toUUID()?.getOrThrow { MalformedQueryParamException(name, UUID::class) } ?: defaultValue()
fun ServerRequest.queryParamOrDefaultAsUlidOrThrow(name: String, defaultValue: Supplier<ULID>) = queryParamOrNull(name)?.toULID()?.getOrThrow { MalformedQueryParamException(name, ULID::class) } ?: defaultValue()
fun ServerRequest.queryParamOrDefaultAsKsuidOrThrow(name: String, defaultValue: Supplier<KSUID>) = queryParamOrNull(name)?.toKSUID()?.getOrThrow { MalformedQueryParamException(name, KSUID::class) } ?: defaultValue()
fun ServerRequest.queryParamOrDefaultAsCuidOrThrow(name: String, defaultValue: Supplier<CUID>) = queryParamOrNull(name)?.toCUID()?.getOrThrow { MalformedQueryParamException(name, CUID::class) } ?: defaultValue()
fun ServerRequest.queryParamOrDefaultAsDateOrThrow(name: String, defaultValue: Supplier<LocalDate>) = queryParamOrNull(name)?.parseToLocalDate()?.getOrThrow { MalformedQueryParamException(name, LocalDate::class) } ?: defaultValue()
fun ServerRequest.queryParamOrDefaultAsDateTimeOrThrow(name: String, defaultValue: Supplier<OffsetDateTime>) = queryParamOrNull(name)?.parseToOffsetDateTime()?.getOrThrow { MalformedQueryParamException(name, OffsetDateTime::class) } ?: defaultValue()
inline fun <reified T : Enum<T>> ServerRequest.queryParamOrDefaultAsEnumOrThrow(name: String, crossinline defaultValue: () -> T) = tryOrThrow({ -> MalformedQueryParamException(name, T::class) }, includeCause = false) { queryParamOrNull(name)?.toEnumConst<T>() ?: defaultValue() }

fun ServerRequest.pathVariableOrThrowAsStringList(name: String) = pathVariableOrThrow(name, StringList::class).splitAndTrim(Char.COMMA)
fun ServerRequest.pathVariableOrThrowAsIntOrThrow(name: String) = tryOrThrow({ -> MalformedPathVariableException(name, Int::class) }, includeCause = false, notOverwrite = RequiredPathVariableException::class) { pathVariableOrThrow(name, Int::class).toInt() }
fun ServerRequest.pathVariableOrThrowAsLongOrThrow(name: String) = tryOrThrow({ -> MalformedPathVariableException(name, Long::class) }, includeCause = false, notOverwrite = RequiredPathVariableException::class) { pathVariableOrThrow(name, Long::class).toLong() }
fun ServerRequest.pathVariableOrThrowAsDoubleOrThrow(name: String) = tryOrThrow({ -> MalformedPathVariableException(name, Double::class) }, includeCause = false, notOverwrite = RequiredPathVariableException::class) { pathVariableOrThrow(name, Double::class).toDouble() }
fun ServerRequest.pathVariableOrThrowAsBooleanOrThrow(name: String) = tryOrThrow({ -> MalformedPathVariableException(name, Boolean::class) }, includeCause = false, notOverwrite = RequiredPathVariableException::class) { pathVariableOrThrow(name, Boolean::class).toBoolean() }
fun ServerRequest.pathVariableOrThrowAsUuidOrThrow(name: String) = pathVariableOrThrow(name, UUID::class).toUUID()() { MalformedPathVariableException(name, UUID::class) }
fun ServerRequest.pathVariableOrThrowAsUlidOrThrow(name: String) = pathVariableOrThrow(name, ULID::class).toULID()() { MalformedPathVariableException(name, ULID::class) }
fun ServerRequest.pathVariableOrThrowAsKsuidOrThrow(name: String) = pathVariableOrThrow(name, KSUID::class).toKSUID()() { MalformedPathVariableException(name, KSUID::class) }
fun ServerRequest.pathVariableOrThrowAsCuidOrThrow(name: String) = pathVariableOrThrow(name, CUID::class).toCUID()() { MalformedPathVariableException(name, CUID::class) }
fun ServerRequest.pathVariableOrThrowAsDateOrThrow(name: String) = pathVariableOrThrow(name, LocalDate::class).parseToLocalDate()() { MalformedPathVariableException(name, LocalDate::class) }
fun ServerRequest.pathVariableOrThrowAsDateTimeOrThrow(name: String) = pathVariableOrThrow(name, OffsetDateTime::class).parseToOffsetDateTime()() { MalformedPathVariableException(name, OffsetDateTime::class) }
inline fun <reified T : Enum<T>> ServerRequest.pathVariableOrThrowAsEnumOrThrow(name: String) = tryOrThrow({ -> MalformedPathVariableException(name, T::class) }, includeCause = false, notOverwrite = RequiredPathVariableException::class) { pathVariableOrThrow(name, T::class).toEnumConst<T>() }

fun ServerRequest.pathVariableOrThrowAsIntOrNull(name: String) = tryOrNull(notOverwrite = RequiredPathVariableException::class) { pathVariableOrThrow(name, Int::class).toInt() }
fun ServerRequest.pathVariableOrThrowAsLongOrNull(name: String) = tryOrNull(notOverwrite = RequiredPathVariableException::class) { pathVariableOrThrow(name, Long::class).toLong() }
fun ServerRequest.pathVariableOrThrowAsDoubleOrNull(name: String) = tryOrNull(notOverwrite = RequiredPathVariableException::class) { pathVariableOrThrow(name, Double::class).toDouble() }
fun ServerRequest.pathVariableOrThrowAsBooleanOrNull(name: String) = tryOrNull(notOverwrite = RequiredPathVariableException::class) { pathVariableOrThrow(name, Boolean::class).toBoolean() }
fun ServerRequest.pathVariableOrThrowAsUuidOrNull(name: String) = pathVariableOrThrow(name, UUID::class).toUUID().getOrNull()
fun ServerRequest.pathVariableOrThrowAsUlidOrNull(name: String) = pathVariableOrThrow(name, ULID::class).toULID().getOrNull()
fun ServerRequest.pathVariableOrThrowAsKsuidOrNull(name: String) = pathVariableOrThrow(name, KSUID::class).toKSUID().getOrNull()
fun ServerRequest.pathVariableOrThrowAsCuidOrNull(name: String) = pathVariableOrThrow(name, CUID::class).toCUID().getOrNull()
fun ServerRequest.pathVariableOrThrowAsDateOrNull(name: String) = pathVariableOrThrow(name, LocalDate::class).parseToLocalDate().getOrNull()
fun ServerRequest.pathVariableOrThrowAsDateTimeOrNull(name: String) = pathVariableOrThrow(name, OffsetDateTime::class).parseToOffsetDateTime().getOrNull()
inline fun <reified T : Enum<T>> ServerRequest.pathVariableOrThrowAsEnumOrNull(name: String) = tryOrNull { pathVariableOrThrow(name, T::class).toEnumConst<T>() }

fun ServerRequest.pathVariableOrNullAsStringList(name: String) = pathVariableOrNull(name)?.splitAndTrim(Char.COMMA)
fun ServerRequest.pathVariableOrNullAsIntOrNull(name: String) = tryOrNull { pathVariableOrNull(name)?.toInt() }
fun ServerRequest.pathVariableOrNullAsLongOrNull(name: String) = tryOrNull { pathVariableOrNull(name)?.toLong() }
fun ServerRequest.pathVariableOrNullAsDoubleOrNull(name: String) = tryOrNull { pathVariableOrNull(name)?.toDouble() }
fun ServerRequest.pathVariableOrNullAsBooleanOrNull(name: String) = tryOrNull { pathVariableOrNull(name)?.toBoolean() }
fun ServerRequest.pathVariableOrNullAsUuidOrNull(name: String) = pathVariableOrNull(name)?.toUUID()?.getOrNull()
fun ServerRequest.pathVariableOrNullAsUlidOrNull(name: String) = pathVariableOrNull(name)?.toULID()?.getOrNull()
fun ServerRequest.pathVariableOrNullAsKsuidOrNull(name: String) = pathVariableOrNull(name)?.toKSUID()?.getOrNull()
fun ServerRequest.pathVariableOrNullAsCuidOrNull(name: String) = pathVariableOrNull(name)?.toCUID()?.getOrNull()
fun ServerRequest.pathVariableOrNullAsDateOrNull(name: String) = pathVariableOrNull(name)?.parseToLocalDate()?.getOrNull()
fun ServerRequest.pathVariableOrNullAsDateTimeOrNull(name: String) = pathVariableOrNull(name)?.parseToOffsetDateTime()?.getOrNull()
inline fun <reified T : Enum<T>> ServerRequest.pathVariableOrNullAsEnumOrNull(name: String) = tryOrNull { pathVariableOrNull(name)?.toEnumConst<T>() }

fun ServerRequest.pathVariableOrNullAsIntOrThrow(name: String) = tryOrThrow({ -> MalformedPathVariableException(name, Int::class) }, includeCause = false) { pathVariableOrNull(name)?.toInt() }
fun ServerRequest.pathVariableOrNullAsLongOrThrow(name: String) = tryOrThrow({ -> MalformedPathVariableException(name, Long::class) }, includeCause = false) { pathVariableOrNull(name)?.toLong() }
fun ServerRequest.pathVariableOrNullAsDoubleOrThrow(name: String) = tryOrThrow({ -> MalformedPathVariableException(name, Double::class) }, includeCause = false) { pathVariableOrNull(name)?.toDouble() }
fun ServerRequest.pathVariableOrNullAsBooleanOrThrow(name: String) = tryOrThrow({ -> MalformedPathVariableException(name, Boolean::class) }, includeCause = false) { pathVariableOrNull(name)?.toBoolean() }
fun ServerRequest.pathVariableOrNullAsUuidOrThrow(name: String) = pathVariableOrNull(name)?.toUUID()?.getOrThrow { MalformedPathVariableException(name, UUID::class) }
fun ServerRequest.pathVariableOrNullAsUlidOrThrow(name: String) = pathVariableOrNull(name)?.toULID()?.getOrThrow { MalformedPathVariableException(name, ULID::class) }
fun ServerRequest.pathVariableOrNullAsKsuidOrThrow(name: String) = pathVariableOrNull(name)?.toKSUID()?.getOrThrow { MalformedPathVariableException(name, KSUID::class) }
fun ServerRequest.pathVariableOrNullAsCuidOrThrow(name: String) = pathVariableOrNull(name)?.toCUID()?.getOrThrow { MalformedPathVariableException(name, CUID::class) }
fun ServerRequest.pathVariableOrNullAsDateOrThrow(name: String) = pathVariableOrNull(name)?.parseToLocalDate()?.getOrThrow { MalformedPathVariableException(name, LocalDate::class) }
fun ServerRequest.pathVariableOrNullAsDateTimeOrThrow(name: String) = pathVariableOrNull(name)?.parseToOffsetDateTime()?.getOrThrow { MalformedPathVariableException(name, OffsetDateTime::class) }
inline fun <reified T : Enum<T>> ServerRequest.pathVariableOrNullAsEnumOrThrow(name: String) = tryOrThrow({ -> MalformedPathVariableException(name, T::class) }) { pathVariableOrNull(name)?.toEnumConst<T>() }

fun ServerRequest.pathVariableOrDefaultAsStringList(name: String, defaultValue: Supplier<StringList>) = pathVariableOrNull(name)?.splitAndTrim(Char.COMMA) ?: defaultValue()
fun ServerRequest.pathVariableOrDefaultAsInt(name: String, defaultValue: Supplier<Int>) = tryOr({ defaultValue() }) { pathVariableOrNull(name)?.toInt() ?: defaultValue() }
fun ServerRequest.pathVariableOrDefaultAsLong(name: String, defaultValue: Supplier<Long>) = tryOr({ defaultValue() }) { pathVariableOrNull(name)?.toLong() ?: defaultValue() }
fun ServerRequest.pathVariableOrDefaultAsDouble(name: String, defaultValue: Supplier<Double>) = tryOr({ defaultValue() }) { pathVariableOrNull(name)?.toDouble() ?: defaultValue() }
fun ServerRequest.pathVariableOrDefaultAsBoolean(name: String, defaultValue: Supplier<Boolean>) = tryOr({ defaultValue() }) { pathVariableOrNull(name)?.toBoolean() ?: defaultValue() }
fun ServerRequest.pathVariableOrDefaultAsUuid(name: String, defaultValue: Supplier<UUID>) = pathVariableOrNull(name)?.toUUID()?.getOrNull() ?: defaultValue()
fun ServerRequest.pathVariableOrDefaultAsUlid(name: String, defaultValue: Supplier<ULID>) = pathVariableOrNull(name)?.toULID()?.getOrNull() ?: defaultValue()
fun ServerRequest.pathVariableOrDefaultAsKsuid(name: String, defaultValue: Supplier<KSUID>) = pathVariableOrNull(name)?.toKSUID()?.getOrNull() ?: defaultValue()
fun ServerRequest.pathVariableOrDefaultAsCuid(name: String, defaultValue: Supplier<CUID>) = pathVariableOrNull(name)?.toCUID()?.getOrNull() ?: defaultValue()
fun ServerRequest.pathVariableOrDefaultAsDate(name: String, defaultValue: Supplier<LocalDate>) = pathVariableOrNull(name)?.parseToLocalDate()?.getOrNull() ?: defaultValue()
fun ServerRequest.pathVariableOrDefaultAsDateTime(name: String, defaultValue: Supplier<OffsetDateTime>) = pathVariableOrNull(name)?.parseToOffsetDateTime()?.getOrNull() ?: defaultValue()
inline fun <reified T : Enum<T>> ServerRequest.pathVariableOrDefaultAsEnum(name: String, crossinline defaultValue: () -> T) = tryOr({ defaultValue() }) { pathVariableOrNull(name)?.toEnumConst<T>() ?: defaultValue() }

fun ServerRequest.pathVariableOrDefaultAsIntOrThrow(name: String, defaultValue: Supplier<Int>) = tryOrThrow({ -> MalformedPathVariableException(name, Int::class) }, includeCause = false) { pathVariableOrNull(name)?.toInt() ?: defaultValue() }
fun ServerRequest.pathVariableOrDefaultAsLongOrThrow(name: String, defaultValue: Supplier<Long>) = tryOrThrow({ -> MalformedPathVariableException(name, Long::class) }, includeCause = false) { pathVariableOrNull(name)?.toLong() ?: defaultValue() }
fun ServerRequest.pathVariableOrDefaultAsDoubleOrThrow(name: String, defaultValue: Supplier<Double>) = tryOrThrow({ -> MalformedPathVariableException(name, Double::class) }, includeCause = false) { pathVariableOrNull(name)?.toDouble() ?: defaultValue() }
fun ServerRequest.pathVariableOrDefaultAsBooleanOrThrow(name: String, defaultValue: Supplier<Boolean>) = tryOrThrow({ -> MalformedPathVariableException(name, Boolean::class) }, includeCause = false) { pathVariableOrNull(name)?.toBoolean() ?: defaultValue() }
fun ServerRequest.pathVariableOrDefaultAsUuidOrThrow(name: String, defaultValue: Supplier<UUID>) = pathVariableOrNull(name)?.toUUID()?.getOrThrow { MalformedPathVariableException(name, UUID::class) } ?: defaultValue()
fun ServerRequest.pathVariableOrDefaultAsUlidOrThrow(name: String, defaultValue: Supplier<ULID>) = pathVariableOrNull(name)?.toULID()?.getOrThrow { MalformedPathVariableException(name, ULID::class) } ?: defaultValue()
fun ServerRequest.pathVariableOrDefaultAsKsuidOrThrow(name: String, defaultValue: Supplier<KSUID>) = pathVariableOrNull(name)?.toKSUID()?.getOrThrow { MalformedPathVariableException(name, KSUID::class) } ?: defaultValue()
fun ServerRequest.pathVariableOrDefaultAsCuidOrThrow(name: String, defaultValue: Supplier<CUID>) = pathVariableOrNull(name)?.toCUID()?.getOrThrow { MalformedPathVariableException(name, CUID::class) } ?: defaultValue()
fun ServerRequest.pathVariableOrDefaultAsDateOrThrow(name: String, defaultValue: Supplier<LocalDate>) = pathVariableOrNull(name)?.parseToLocalDate()?.getOrThrow { MalformedPathVariableException(name, LocalDate::class) } ?: defaultValue()
fun ServerRequest.pathVariableOrDefaultAsDateTimeOrThrow(name: String, defaultValue: Supplier<OffsetDateTime>) = pathVariableOrNull(name)?.parseToOffsetDateTime()?.getOrThrow { MalformedPathVariableException(name, OffsetDateTime::class) } ?: defaultValue()
inline fun <reified T : Enum<T>> ServerRequest.pathVariableOrDefaultAsEnumOrThrow(name: String, crossinline defaultValue: () -> T) = tryOrThrow({ -> MalformedPathVariableException(name, T::class) }, includeCause = false) { pathVariableOrNull(name)?.toEnumConst<T>() ?: defaultValue() }