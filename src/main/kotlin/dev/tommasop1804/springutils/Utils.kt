/*
 * Copyright © 2026 Tommaso Pastorelli (TommasoP1804) | Spring-Utils
 */

@file:JvmName("UtilsKt")
@file:Suppress("unused", "FunctionName", "UnusedReceiverParameter")

package dev.tommasop1804.springutils

import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.classes.web.*
import dev.tommasop1804.kutils.exceptions.*
import dev.tommasop1804.springutils.annotations.*
import dev.tommasop1804.springutils.exception.*
import dev.tommasop1804.springutils.request.*
import org.springframework.core.env.PropertyResolver
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.security.authentication.LockedException
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MimeType
import org.springframework.util.MultiValueMap
import java.lang.reflect.Method
import java.net.URI

fun ProblemDetail(
    title: String? = null,
    status: HttpStatus,
    type: URI? = null,
    detail: String? = null,
    instance: URI? = null,
    internalErrorCode: String? = null,
    exception: String? = null,
    extensions: DataMapNN = emptyMap()
): ExtendedProblemDetail {
    val result = ProblemDetail.forStatus(status)
    if (title.isNotNull()) result.title = title
    if (type.isNotNull()) result.type = type
    if (detail.isNotNull()) result.detail = detail
    if (instance.isNotNull()) result.instance = instance
    for ((key, value) in extensions) result.setProperty(key, value)
    return ExtendedProblemDetail(result, internalErrorCode, exception)
}
fun ProblemDetail(
    title: String? = null,
    status: Int,
    type: URI? = null,
    detail: String? = null,
    instance: URI? = null,
    internalErrorCode: String? = null,
    exception: String? = null,
    extensions: DataMapNN = emptyMap()
): ExtendedProblemDetail {
    val result = ProblemDetail.forStatus(status)
    if (title.isNotNull()) result.title = title
    if (type.isNotNull()) result.type = type
    if (detail.isNotNull()) result.detail = detail
    if (instance.isNotNull()) result.instance = instance
    for ((key, value) in extensions) result.setProperty(key, value)
    return ExtendedProblemDetail(result, internalErrorCode, exception)
}

internal fun findCallerMethod(): Method? = tryOrNull {
    val stackTrace = Thread.currentThread().stackTrace

    for (i in stackTrace.indices) {
        val element = stackTrace[i]
        try {
            if (element.className.startsWith("java.") || element.className.startsWith("kotlin.")) continue
            val clazz = Class.forName(element.className).let {
                if ($$$"$$EnhancerBySpringCGLIB$$" in it.name || $$$"$$SpringCGLIB$$" in it.name) it.superclass else it
            }
            val methods = clazz.declaredMethods

            val method = methods.find {
                it.name == element.methodName
            }

            if (method?.getAnnotation(Feature::class.java).isNotNull())
                return@tryOrNull method
        } catch (e: Exception) {
            continue
        }
    }
    null
}

/**
 * Converts the current MultiMap instance into a MultiValueMap.
 *
 * @return a new MultiValueMap containing the same key-value pairs as the current MultiMap
 * @since 2.3.1
 */
fun <K : Any, V : Any> MultiMap<K, V>.toMultiValueMap(): MultiValueMap<K, V> = LinkedMultiValueMap<K, V>().also { it.putAll(this) }
/**
 * Converts a `MultiValueMap` to a `MultiMap`.
 *
 * The resulting `MultiMap` preserves the key-value pairs of the `MultiValueMap`,
 * maintaining their order of insertion.
 *
 * @return A `MultiMap` containing the same key-value mappings as the original `MultiValueMap`.
 * @since 2.3.1
 */
fun <K : Any, V : Any> MultiValueMap<K, V>.toMultiMap(): MultiMap<K, V> = LinkedHashMap<K, List<V>>().also { it.putAll(this) }

/**
 * Converts a MediaType object to a new instance with the same type, subtype, and parameters.
 *
 * @receiver The original MediaType object to be converted.
 * @return A new MediaType instance with the same type, subtype, and parameters as the receiver.
 * @since 2.2.0
 */
fun MediaType.toKutilsMediaType() = MediaType(type, subtype, parameters)
/**
 * Converts the current MimeType instance to a new MimeType object with the same type and subtype.
 *
 * @receiver The MimeType instance to be converted.
 * @return A new MimeType instance containing the type and subtype of the original MimeType.
 * @since 2.2.0
 */
fun MimeType.toKutilsMimeType() = MimeType(type, subtype)
/**
 * Converts a custom `MediaType` object from the `dev.tommasop1804.kutils.classes.web` package
 * to a Spring Framework `MediaType` object.
 *
 * @receiver A `MediaType` instance from the custom package.
 * @return A corresponding `MediaType` instance from the Spring Framework,
 * constructed using the type, subtype, and parameters of the receiver.
 * @since 2.2.0
 */
fun dev.tommasop1804.kutils.classes.web.MediaType.toSpringMediaType() = MediaType(type, subtype, parameters)
/**
 * Converts the current instance of `dev.tommasop1804.kutils.classes.web.MimeType`
 * to an instance of Spring's `MimeType` class.
 *
 * @return a new `MimeType` object representing the same type and subtype
 *         as the current `MimeType` instance.
 * @since 2.2.0
 */
fun dev.tommasop1804.kutils.classes.web.MimeType.toSpringMimeType() = MimeType(type, subtype)

/**
 * Converts the current instance of `HttpMethod` to its corresponding
 * Spring Framework `HttpMethod` representation.
 *
 * @return The equivalent `org.springframework.http.HttpMethod` based on the name of the current `HttpMethod`.
 * @throws NoSuchEntryException if no matching Spring HTTP method exists for the current `HttpMethod.name`.
 * @since 3.3.0
 */
fun HttpMethod.toSpringHttpMethod() = tryOrThrow({ -> NoSuchEntryException(HttpMethod::class, name) }) {
    org.springframework.http.HttpMethod.valueOf(name)
}

/**
 * Converts a Spring `HttpMethod` to its corresponding `HttpMethod` in the Kutils library.
 *
 * Maps the `name` of the Spring `HttpMethod` instance to a `HttpMethod` enum value in Kutils.
 *
 * @receiver The Spring `HttpMethod` instance to be converted.
 * @return The corresponding `HttpMethod` enum value from the Kutils library.
 * @throws NoSuchEntryException If the `name` of the Spring `HttpMethod` does not match any Kutils `HttpMethod` value.
 * @since 3.3.0
 */
fun org.springframework.http.HttpMethod.toKutilsHttpMethod() = tryOrThrow({ -> NoSuchEntryException(HttpMethod::class, name()) }) {
    HttpMethod.valueOf(name())
}

internal fun getStatus(e: Throwable) = when (e) {
    is BadGatewayException, is ExternalServiceHttpException -> HttpStatus.BAD_GATEWAY
    is BadRequestException,
    is RequiredPropertyException,
    is RequiredParameterException,
    is RequiredHeaderException,
    is NumberSignException,
    is MalformedInputException -> HttpStatus.BAD_REQUEST
    is ContentTooLargeException -> HttpStatus.CONTENT_TOO_LARGE
    is ConflictException, is ResourceAlreadyExistsException, is ResourceConflictException, is ResourceInUseException -> HttpStatus.CONFLICT
    is ExpectationFailedException -> HttpStatus.EXPECTATION_FAILED
    is FailedDependencyException -> HttpStatus.FAILED_DEPENDENCY
    is ForbiddenException, is InsufficientPermissionsException -> HttpStatus.FORBIDDEN
    is GatewayTimeoutException -> HttpStatus.GATEWAY_TIMEOUT
    is GoneException, is ResourceDeletedException -> HttpStatus.GONE
    is InsufficientStorageException -> HttpStatus.INSUFFICIENT_STORAGE
    is LengthRequiredException -> HttpStatus.LENGTH_REQUIRED
    is LockedException, is ResourceLockedException -> HttpStatus.LOCKED
    is LoopDetectedException -> HttpStatus.LOOP_DETECTED
    is MisdirectedRequestException -> HttpStatus.MISDIRECTED_REQUEST
    is NetworkAuthenticationRequiredException -> HttpStatus.NETWORK_AUTHENTICATION_REQUIRED
    is NotAcceptableException -> HttpStatus.NOT_ACCEPTABLE
    is NotExtendedException -> HttpStatus.NOT_EXTENDED
    is NotFoundException, is ResourceNotFoundException -> HttpStatus.NOT_FOUND
    is NotImplementedException -> HttpStatus.NOT_IMPLEMENTED
    is PaymentRequiredException -> HttpStatus.PAYMENT_REQUIRED
    is PreconditionFailedException -> HttpStatus.PRECONDITION_FAILED
    is PreconditionRequiredException -> HttpStatus.PRECONDITION_REQUIRED
    is RangeNotSatisfiableException -> HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE
    is RequestTimeoutException -> HttpStatus.REQUEST_TIMEOUT
    is ServiceUnavailableException -> HttpStatus.SERVICE_UNAVAILABLE
    is TeapotException -> HttpStatus.I_AM_A_TEAPOT
    is TooEarlyException -> HttpStatus.TOO_EARLY
    is TooManyRequestsException -> HttpStatus.TOO_MANY_REQUESTS
    is UnauthorizedException -> HttpStatus.UNAUTHORIZED
    is UnavailableForLegalReasonsException -> HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS
    is UnprocessableContentException, is ResourceNotAcceptableException -> HttpStatus.UNPROCESSABLE_CONTENT
    is UnsupportedMediaTypeException -> HttpStatus.UNSUPPORTED_MEDIA_TYPE
    is VariantAlsoNegotiatesException -> HttpStatus.VARIANT_ALSO_NEGOTIATES
    else -> HttpStatus.INTERNAL_SERVER_ERROR
}

internal val STATUS_CODE_EXCEPTIONS = arrayOf(
    ExternalServiceHttpException::class,
    RequiredPropertyException::class,
    RequiredParameterException::class,
    RequiredPathVariableException::class,
    RequiredQueryParamException::class,
    RequiredHeaderException::class,
    MalformedPathVariableException::class,
    MalformedQueryParamException::class,
    MalformedHeaderException::class,
    MalformedInputException::class,
    MalformedPropertyException::class,
    MalformedParameterException::class,
    NumberSignException::class,
    ResourceUnaccessibleException::class,
    ResourceAlreadyExistsException::class,
    ResourceConflictException::class,
    InsufficientPermissionsException::class,
    ResourceDeletedException::class,
    ResourceLockedException::class,
    ResourceNotFoundException::class,
    ResourceNotAcceptableException::class,
    ResourceInUseException::class,
    DatabaseOperationException::class
)

internal fun findCorrectException(e: Throwable) =
    if (e::class in STATUS_CODE_EXCEPTIONS) e else e.cause ?: e

/**
 * Represents a custom HTTP header key "From-Service".
 * This header is typically used to identify the originating service
 * in inter-service communication within distributed systems.
 *
 * @since 2.2.6
 */
val HttpHeader.Companion.FROM_SERVICE get() = "From-Service"
/**
 * Represents the HTTP header name "Request-Id", which is typically used to trace and correlate
 * individual requests across distributed systems or microservices.
 *
 * Usage of this header allows for easier debugging and analysis of request flows by tagging
 * each request with a unique identifier.
 * @since 2.2.6
 */
val HttpHeader.Companion.REQUEST_ID get() = "Request-Id"
/**
 * A constant representing the HTTP header key "Feature-Code".
 * Used to specify or retrieve feature-related codes in HTTP requests or responses.
 * @since 2.3.1
 */
val HttpHeader.Companion.FEATURE_CODE get() = "Feature-Code"

/**
 * Retrieves the `RequestId` from the HTTP headers.
 *
 * This function extracts the first occurrence of the `REQUEST_ID` header from the provided
 * `HttpHeaders` instance. The value is then used to create a new instance of the `RequestId` value class.
 *
 * @receiver `HttpHeaders` The HTTP headers from which the `REQUEST_ID` is retrieved.
 * @return A `RequestId` instance corresponding to the value of the `REQUEST_ID` header.
 * @throws NoSuchHeaderException If the `REQUEST_ID` header is missing.
 * @since 2.3.1
 */
fun HttpHeaders.getRequestId() = getFirstOrThrow(HttpHeader.REQUEST_ID).let(::RequestId)
/**
 * Sets the request identifier in the HTTP headers.
 *
 * This method adds or updates the value of the `REQUEST_ID` header with the string representation
 * of the provided [RequestId].
 *
 * @param requestId The request identifier to be set in the headers.
 * @since 2.3.1
 */
fun HttpHeaders.setRequestId(requestId: RequestId) = set(HttpHeader.REQUEST_ID, requestId.value)

/**
 * Converts this instance of `HttpHeaders` into an instance of `org.springframework.http.HttpHeaders`.
 *
 * The method iterates through all header entries in this `HttpHeaders` object and copies
 * each header name and its associated values into the resulting `org.springframework.http.HttpHeaders` object.
 *
 * @receiver The `HttpHeaders` instance to be converted.
 * @return A new instance of `org.springframework.http.HttpHeaders` containing the same header names and values.
 * @since 2.3.1
 */
fun HttpHeaders.toSpringHttpHeaders() = org.springframework.http.HttpHeaders().apply {
    forEach { [name, values] -> addAll(name, values) }
}
/**
 * Converts an instance of `org.springframework.http.HttpHeaders` to a new instance
 * of `HttpHeaders` from the `kutils` library by copying all header entries.
 *
 * For each header key-value pair in the source, the method adds it to the new
 * `HttpHeaders` instance being created.
 *
 * @receiver The `org.springframework.http.HttpHeaders` instance to be converted.
 * @return A new `HttpHeaders` instance containing the same headers as the original.
 * @since 2.3.1
 */
fun org.springframework.http.HttpHeaders.toKutilsHttpHeaders() = HttpHeaders().apply {
    this@toKutilsHttpHeaders.forEach { key, values -> this += key to values }
}

/**
 * Adds the specified HTTP header and its associated values to the current HttpHeaders instance.
 *
 * @param header the HttpHeader containing the header name and values to be added
 * @since 2.3.1
 */
fun org.springframework.http.HttpHeaders.add(header: HttpHeader) {
    addAll(header.name, header.values)
}
/**
 * Adds all header entries from the provided HttpHeaders instance to this HttpHeaders instance.
 *
 * If a header key already exists, the values from the provided headers are appended to the existing values.
 *
 * @param headers the HttpHeaders instance containing the entries to be added
 * @since 2.3.1
 */
fun org.springframework.http.HttpHeaders.addAll(headers: HttpHeaders) {
    headers.forEach { [key, values] -> addAll(key, values) }
}

/**
 * Adds the headers from a given `org.springframework.http.HttpHeaders` instance to the current `HttpHeaders` instance.
 *
 * @param headers The `org.springframework.http.HttpHeaders` instance whose headers are to be added to the current instance.
 * @since 3.3.4
 */
operator fun HttpHeaders.plusAssign(headers: org.springframework.http.HttpHeaders) {
    this += headers.toKutilsHttpHeaders()
}
/**
 * Combines the current instance of `HttpHeaders` with another instance of
 * `org.springframework.http.HttpHeaders` by converting the latter to a `HttpHeaders`
 * object and merging their contents.
 *
 * The resulting `HttpHeaders` instance contains all the header entries from both
 * the original `HttpHeaders` instance and the provided `org.springframework.http.HttpHeaders`
 * instance. If duplicate keys exist, their values are combined.
 *
 * @param headers The `org.springframework.http.HttpHeaders` instance to be merged with
 * the current instance.
 * @return A new `HttpHeaders` instance containing the combined header entries.
 * @since 3.3.4
 */
operator fun HttpHeaders.plus(headers: org.springframework.http.HttpHeaders) = this + headers.toKutilsHttpHeaders()
/**
 * Removes the headers specified in the given `org.springframework.http.HttpHeaders`
 * instance from the current `HttpHeaders` instance.
 *
 * @param headers The `org.springframework.http.HttpHeaders` instance containing
 * the headers to be removed. All key-value pairs in this instance will be
 * removed from the current `HttpHeaders`.
 * @since 3.3.4
 */
operator fun HttpHeaders.minusAssign(headers: org.springframework.http.HttpHeaders) {
    this -= headers.toKutilsHttpHeaders()
}
/**
 * Subtracts the headers from a provided `HttpHeaders` object from the current `HttpHeaders` instance.
 *
 * @param headers The `HttpHeaders` to be subtracted from the current instance.
 * @since 3.3.4
 */
operator fun HttpHeaders.minus(headers: org.springframework.http.HttpHeaders) = this - headers.toKutilsHttpHeaders()

/**
 * Converts the custom HttpStatus instance to a Spring Framework HttpStatus instance.
 *
 * Maps the `value` property of the custom HttpStatus to the corresponding
 * Spring HttpStatus using the `HttpStatus.valueOf` method.
 *
 * @return The corresponding Spring HttpStatus enum constant.
 * @throws NoSuchEntryException If the value does not map to a valid Spring HttpStatus.
 * @since 2.3.1
 */
fun dev.tommasop1804.kutils.classes.web.HttpStatus.toSpringHttpStatus() = tryOrThrow({ -> NoSuchEntryException("No entry found with value $value") }, includeCause = false) {
    HttpStatus.valueOf(value)
}
/**
 * Converts an instance of `HttpStatus` to a corresponding `dev.tommasop1804.kutils.classes.web.HttpStatus`.
 *
 * @receiver The `HttpStatus` to be converted.
 * @return The matching `dev.tommasop1804.kutils.classes.web.HttpStatus`.
 * @throws NoSuchEntryException If no matching entry is found for the value of the `HttpStatus`.
 * @since 2.3.1
 */
fun HttpStatus.toKutilsHttpStatus() = dev.tommasop1804.kutils.classes.web.HttpStatus.of(value()) ?: throw NoSuchEntryException("No entry found with value ${value()}")

/**
 * Retrieves the property value associated with the specified key.
 *
 * @param key The key used to look up the desired property value.
 * @return The property value corresponding to the given key, or null if the key is not found.
 */
operator fun PropertyResolver.get(key: String) = getProperty(key)

/**
 * Retrieves a property value identified by the specified key and casts it to the required type.
 *
 * @param T The expected type of the property value.
 * @param key The key identifying the property to retrieve.
 * @return The property value cast to the specified type, or null if the property is not found or cannot be cast.
 */
inline fun <reified T : Any> PropertyResolver.getProperty(key: String) = getProperty(key, T::class.java)
/**
 * Retrieves the property value associated with the given key.
 * If the property is not found, the value provided by the defaultValue supplier is returned.
 *
 * @param key the key of the property to retrieve
 * @param defaultValue a supplier that provides a default value to return when the property is not found
 * @return the property value associated with the key, or the value from the defaultValue supplier if the key is not present
 */
fun PropertyResolver.getPropertyOrDefault(key: String, defaultValue: Supplier<String>) = getProperty(key) ?: defaultValue()
/**
 * Retrieves the property associated with the given key as the specified type, or returns the default value
 * provided by the supplier if the property is not found or cannot be cast to the specified type.
 *
 * @param T the type of the property to be retrieved
 * @param key the key of the property to retrieve
 * @param defaultValue a supplier providing the default value to return if the property is unavailable
 * @return the property value cast to the specified type, or the default value provided by the supplier
 */
inline fun <reified T : Any> PropertyResolver.getPropertyOrDefault(key: String, defaultValue: Supplier<T>) = getProperty(key, T::class.java) ?: defaultValue()
/**
 * Retrieves the property value associated with the given key.
 * If the property is not found, the provided exception supplier is invoked to throw an exception.
 *
 * @param key The key associated with the property to retrieve.
 * @param lazyException A supplier that provides the exception to throw when the property is not found. Defaults to throwing `NoSuchEnvPropertyException`.
 * @throws Throwable The exception provided by the `lazyException` supplier if the property is not found.
 * @return The value of the property associated with the specified key.
 */
fun PropertyResolver.getPropertyOrThrow(key: String, lazyException: ThrowableSupplier = { NoSuchEnvPropertyException(propertyName = key) }) =
    getProperty(key) ?: lazyException()
/**
 * Retrieves the property value associated with the specified key or throws an exception if the property is not found.
 *
 * @param key the name of the property to retrieve.
 * @param internalErrorCode an optional internal error code to pass to the exception if the property is not found.
 * @throws NoSuchEnvPropertyException if the specified property is not available.
 */
fun PropertyResolver.getPropertyOrThrow(key: String, internalErrorCode: String?) =
    getProperty(key) ?: throw NoSuchEnvPropertyException(propertyName = key, internalErrorCode = internalErrorCode)
/**
 * Retrieves the value of a property with the specified key or throws an exception if the property is not found.
 *
 * @param key The key of the property to retrieve.
 * @param lazyMessage A supplier that provides the exception message in case the property is not found.
 * @throws NoSuchEnvPropertyException If the property with the specified key does not exist.
 */
fun PropertyResolver.getPropertyOrThrow(key: String, lazyMessage: Supplier<String>) =
    getProperty(key) ?: throw NoSuchEnvPropertyException(propertyName = key, message = lazyMessage())
/**
 * Retrieves the property corresponding to the given key from the PropertyResolver.
 * If the property is not found, throws a NoSuchEnvPropertyException with the provided error code and message.
 *
 * @param key The key of the property to retrieve.
 * @param internalErrorCode An optional error code to include in the exception if the property is not found.
 * @param lazyMessage A supplier function that provides a detailed error message when the property is missing.
 * @throws NoSuchEnvPropertyException If the property with the specified key is not found.
 */
fun PropertyResolver.getPropertyOrThrow(key: String, internalErrorCode: String?, lazyMessage: Supplier<String>) =
    getProperty(key) ?: throw NoSuchEnvPropertyException(propertyName = key, internalErrorCode = internalErrorCode, message = lazyMessage())
/**
 * Retrieves a property from the PropertyResolver and casts it to the specified type.
 * If the property is not found, an exception is thrown, created by the provided lazyException supplier.
 *
 * @param key The name of the property to retrieve.
 * @param lazyException A supplier that provides a Throwable to be thrown if the property is not found. Defaults to throwing NoSuchEnvPropertyException.
 * @return The property value cast to the specified type.
 * @throws Throwable If the property is not found in the PropertyResolver.
 */
inline fun <reified T : Any> PropertyResolver.getPropertyOrThrow(key: String, lazyException: ThrowableSupplier = { NoSuchEnvPropertyException(propertyName = key) }) =
    getProperty(key, T::class.java) ?: lazyException()
/**
 * Retrieves a property value of the specified type associated with the provided key.
 * If the property is not found, this method throws a NoSuchEnvPropertyException.
 *
 * @param key The key of the property to retrieve.
 * @param internalErrorCode An optional error code to include in the exception if the property is not found.
 * @throws NoSuchEnvPropertyException if the property with the specified key is not found.
 */
inline fun <reified T : Any> PropertyResolver.getPropertyOrThrow(key: String, internalErrorCode: String?) =
    getProperty(key, T::class.java) ?: throw NoSuchEnvPropertyException(propertyName = key, internalErrorCode = internalErrorCode)
/**
 * Retrieves a property value associated with the specified key and casts it to the expected type.
 * Throws a [NoSuchEnvPropertyException] if the property is not found.
 *
 * @param key The key to retrieve the property value for.
 * @param lazyMessage A lambda supplier that provides the exception message if the property is not found.
 * @return The property value cast to the expected type [T].
 * @throws NoSuchEnvPropertyException if the property is not found for the specified key.
 */
inline fun <reified T : Any> PropertyResolver.getPropertyOrThrow(key: String, lazyMessage: Supplier<String>) =
    getProperty(key, T::class.java) ?: throw NoSuchEnvPropertyException(propertyName = key, message = lazyMessage())
/**
 * Retrieves a property value of the specified type associated with the given key from the PropertyResolver.
 * If the property is not found, a NoSuchEnvPropertyException is thrown with the provided error code and
 * message.
 *
 * @param T The type of the property value to retrieve.
 * @param key The key of the property to retrieve.
 * @param internalErrorCode An optional error code to include in the exception if the property is not found.
 * @param lazyMessage A supplier function that provides a detailed error message for the exception.
 * @throws NoSuchEnvPropertyException if the property is not found.
 */
inline fun <reified T : Any> PropertyResolver.getPropertyOrThrow(key: String, internalErrorCode: String?, lazyMessage: Supplier<String>) =
    getProperty(key, T::class.java) ?: throw NoSuchEnvPropertyException(propertyName = key, internalErrorCode = internalErrorCode, message = lazyMessage())

/**
 * Checks if the specified property key exists in the resolver.
 *
 * @param key The property key to check for existence.
 * @return `true` if the property key exists, `false` otherwise.
 */
operator fun PropertyResolver.contains(key: String) = containsProperty(key)

/**
 * Resolves placeholder values within the string using the provided context of `PropertyResolver`.
 *
 * This function parses the string for placeholder syntax and replaces them with corresponding
 * values resolved from the supplied `PropertyResolver` context. Placeholders are typically
 * denoted in a recognized format, and their values are fetched or substituted based on the
 * configuration and logic defined in the resolver.
 *
 * The function operates in the context of `PropertyResolver`, allowing the resolution process
 * to access any necessary properties, variables, or other data managed by the resolver implementation.
 *
 * @receiver The string in which placeholders are to be resolved.
 * @return A new string with all placeholders replaced by their resolved values.
 * @throws IllegalArgumentException If a placeholder cannot be resolved or is malformed.
 */
context(resolver: PropertyResolver)
fun String.resolvePlaceholders() = resolver.resolvePlaceholders(this)
/**
 * Resolves all placeholders in this string using the provided PropertyResolver context.
 * If any placeholders cannot be resolved, the specified exception supplier is used to
 * throw an exception.
 *
 * @param lazyException A supplier that returns the exception to be thrown if a placeholder
 * cannot be resolved. Defaults to providing a NoSuchEnvPropertyException.
 */
context(resolver: PropertyResolver)
fun String.resolvePlaceholdersOrThrow(lazyException: ThrowableSupplier = { NoSuchEnvPropertyException() }) = tryOrThrow(
    lazyException = lazyException,
    overwriteOnly = IllegalArgumentException::class
) { resolver.resolveRequiredPlaceholders(this) }