/*
 * Copyright © 2026 Tommaso Pastorelli (TommasoP1804) | Spring-Utils
 */

@file:JvmName("ReactiveResponseUtilsKt")
@file:Since("3.0.0")
@file:Suppress("unused", "FunctionName", "FunctionName", "UNCHECKED_CAST")

package dev.tommasop1804.springutils.reactive.function.response

import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.annotations.*
import dev.tommasop1804.kutils.classes.measure.*
import dev.tommasop1804.kutils.classes.time.*
import dev.tommasop1804.kutils.classes.web.*
import dev.tommasop1804.kutils.classes.web.HttpHeader.Companion.eTag
import dev.tommasop1804.kutils.classes.web.HttpHeader.Companion.toHeaderDate
import dev.tommasop1804.kutils.classes.web.HttpStatus.Companion.toHttpStatus
import dev.tommasop1804.kutils.exceptions.*
import dev.tommasop1804.springutils.*
import dev.tommasop1804.springutils.annotations.*
import dev.tommasop1804.springutils.config.*
import dev.tommasop1804.springutils.exception.*
import dev.tommasop1804.springutils.reactive.function.*
import dev.tommasop1804.springutils.reactive.function.request.*
import dev.tommasop1804.springutils.request.*
import dev.tommasop1804.springutils.response.*
import dev.tommasop1804.springutils.servlet.EmptyResponse
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueWithTypeAndAwait
import org.springframework.web.reactive.function.server.buildAndAwait
import java.net.URL
import java.time.OffsetDateTime
import java.time.temporal.TemporalAccessor

/**
 * Negotiates the response content type based on the `Accept` header from the client request and sets it accordingly.
 * If a compatible YAML media type is found, the response content type is set to that media type.
 * Finally, the response body is set using the provided body object.
 *
 * @param accept The server request `Accept` header to negotiate content type.
 * @param body The body object to be set in the response.
 * @return The `ServerResponse` with the negotiated content type and provided body.
 * @since 3.7.3
 */
suspend inline fun <reified T : Any> ServerResponse.BodyBuilder.negotiateBodyValueWithTypeAndAwait(
    accept: Collection<org.springframework.http.MediaType>,
    body: T
): ServerResponse {
    val negotiated = YAML_MEDIA_TYPES.firstOrNull { yaml -> accept.any { it.equalsTypeAndSubtype(yaml) } }
        ?: XML_MEDIA_TYPES.firstOrNull { xml -> accept.any { it.equalsTypeAndSubtype(xml) } }
    contentType(negotiated ?: accept.firstOr { org.springframework.http.MediaType.APPLICATION_JSON })
    return bodyValueWithTypeAndAwait(body)
}

/**
 * Evaluates conditional GET preconditions based on request headers and returns an appropriate response status
 * depending on whether the resource has been modified or not.
 *
 * @param resourceETag The ETag of the resource. If provided, it is used in place of the resource's actual ETag.
 * @param lastModifiedDate The last modification date of the resource. It is used to evaluate the "If-Modified-Since" condition.
 * @param requireAtLeastOneValidator If true, ensures at least one of ETag or "If-Modified-Since" validators
 *                                   is present in the request. Throws an exception if neither is provided.
 * @param status The HTTP status to use when the resource is considered modified. Defaults to 200 (OK).
 * @param includeFeatureCode A flag to determine whether to include the "Feature-Code" header in the response. Defaults to true.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param expires The expiration date for the response. If provided, it sets the "Expires" header.
 * @param preferenceApplied A list of preference-applied values to include in the response. If provided, it sets the "Preference-Applied" header.
 * @param refresh The refresh duration for the response. If provided, it sets the "Refresh" header.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Additional headers to include in the response, if provided. HAS LOWER PRIORITY THAN THE NEXT PARAMETES.
 * @param lazyExceptionIfNotPresent A supplier for the exception to throw if validation preconditions are not present (if required).
 * @param body A supplier for the body of the response. The resource body is only included in the response when the
 *             resource is considered modified.
 * @return A [dev.tommasop1804.springutils.reactive.function.Response] entity containing the appropriate HTTP status, headers, and optional response body.
 * @since 3.0.0
 */
context(request: Request)
suspend inline fun <reified T : Any> conditionalGet(
    resourceETag: String? = null,
    lastModifiedDate: OffsetDateTime? = null,
    requireAtLeastOneValidator: Boolean = false,
    status: HttpStatus = HttpStatus.OK,
    includeFeatureCode: Boolean = true,
    includeRequestId: Boolean = true,
    expires: TemporalAccessor? = null,
    preferenceApplied: List<String> = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    contentType: MediaType? = null,
    headers: HttpHeaders = HttpHeaders(),
    lazyExceptionIfNotPresent: ThrowableSupplier = { PreconditionRequiredException("Use one of this or both (based on configuration): If-None-Match, If-Modified-Since") },
    noinline body: Supplier<T>
): Response {
    val eTagNoneMatch = request.ifNoneMatch()
    val ifModifiedSince = request.ifModifiedSince()?.toOffsetDateTime()
    if (requireAtLeastOneValidator && eTagNoneMatch.isEmpty() && ifModifiedSince.isNull())
        throw lazyExceptionIfNotPresent()

    var status = status
    var body: T? = null
    val eTag = if (resourceETag.isNotNullOrBlank()) resourceETag - Char.QUOTATION_MARK else {
        body = body()
        body.eTag
    }
    if (eTagNoneMatch.isNotNullOrEmpty() && eTagNoneMatch.any { (it - Char.QUOTATION_MARK) == eTag || it == String.STAR })
        status = HttpStatus.NOT_MODIFIED
    if (!eTagNoneMatch && ifModifiedSince.isNotNull() && lastModifiedDate.isNotNull() && !lastModifiedDate.isAfter(ifModifiedSince))
        status = HttpStatus.NOT_MODIFIED


    val response = Response.status(status.toSpringHttpStatus())
    if (headers.isNotEmpty()) response.headers { it.addAll(headers.toSpringHttpHeaders()) }
    if (includeFeatureCode) response.featureCode()
    if (expires.isNotNull()) response.expires(expires)
    if (includeRequestId) response.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (preferenceApplied.isNotEmpty()) response.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) response.refresh(refresh)
    if (serverTiming.isNotEmpty()) response.serverTiming(*serverTiming.toTypedArray())
    if (status == HttpStatus.NOT_MODIFIED) return response.buildAndAwait()
    return response.eTag(eTag).negotiateBodyValueWithTypeAndAwait(contentType?.toSpringMediaType()?.asSingleList() ?: request.headers().accept(), body ?: body())
}
/**
 * Processes conditional GET requests based on the provided HTTP headers and configuration,
 * and returns an appropriate response.
 *
 * @param resourceETag The ETag of the resource. If provided, it is used in place of the resource's actual ETag.
 * @param lastModifiedDate The timestamp representing the last modification date of the resource.
 * Used in conjunction with `ifModifiedSince` for conditional validation.
 * @param requireAtLeastOneValidator If true, at least one validator (ETag, If-Modified-Since) must be
 * present in the request. If none are provided, an exception will be thrown.
 * @param status The default HTTP status to return if the request is not conditional or if validation passes.
 * Defaults to HTTP OK (200).
 * @param featureCode A string representing a feature code, which will be added as a "Feature-Code" header
 * in the response.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param expires The expiration date for the response. If provided, it sets the "Expires" header.
 * @param preferenceApplied A list of preference-applied values to include in the response. If provided, it sets the "Preference-Applied" header.
 * @param refresh The refresh duration for the response. If provided, it sets the "Refresh" header.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Any additional HTTP headers to be included in the response.  HAS LOWER PRIORITY THAN THE NEXT PARAMETES.
 * @param lazyExceptionIfNotPresent A lazy supplier for an exception to throw when validators are required
 * but not provided. By default, a PreconditionRequiredException is thrown.
 * @param body A supplier function to provide the body of the response when validation succeeds.
 * @return A [Response] object with an appropriate HTTP status and body, depending on the result
 * of the validation.
 * @since 3.0.0
 */
context(request: Request)
suspend inline fun <reified T : Any> conditionalGet(
    resourceETag: String? = null,
    lastModifiedDate: OffsetDateTime? = null,
    requireAtLeastOneValidator: Boolean = false,
    status: HttpStatus = HttpStatus.OK,
    featureCode: String,
    includeRequestId: Boolean = true,
    expires: TemporalAccessor? = null,
    preferenceApplied: List<String> = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    contentType: MediaType? = null,
    headers: HttpHeaders = HttpHeaders(),
    lazyExceptionIfNotPresent: ThrowableSupplier = { PreconditionRequiredException("Use one of this or both (based on configuration): If-None-Match, If-Modified-Since") },
    noinline body: Supplier<T>
): Response {
    val eTagNoneMatch = request.ifNoneMatch()
    val ifModifiedSince = request.ifModifiedSince()?.toOffsetDateTime()
    if (requireAtLeastOneValidator && eTagNoneMatch.isEmpty() && ifModifiedSince.isNull())
        throw lazyExceptionIfNotPresent()

    var status = status
    var body: T? = null
    val eTag = if (resourceETag.isNotNullOrBlank()) resourceETag - Char.QUOTATION_MARK else {
        body = body()
        body.eTag
    }
    if (eTagNoneMatch.isNotNullOrEmpty() && eTagNoneMatch.any { (it - Char.QUOTATION_MARK) == eTag || it == String.STAR  })
        status = HttpStatus.NOT_MODIFIED
    if (!eTagNoneMatch && ifModifiedSince.isNotNull() && lastModifiedDate.isNotNull() && !lastModifiedDate.isAfter(ifModifiedSince))
        status = HttpStatus.NOT_MODIFIED


    val response = Response.status(status.toSpringHttpStatus())
    if (headers.isNotEmpty()) response.headers { it.addAll(headers.toSpringHttpHeaders()) }
    response.featureCode(featureCode)
    if (includeRequestId) response.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (expires.isNotNull()) response.expires(expires)
    if (preferenceApplied.isNotEmpty()) response.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) response.refresh(refresh)
    if (serverTiming.isNotEmpty()) response.serverTiming(*serverTiming.toTypedArray())
    if (status == HttpStatus.NOT_MODIFIED) return response.buildAndAwait()
    return response.eTag(eTag).negotiateBodyValueWithTypeAndAwait(contentType?.toSpringMediaType()?.asSingleList() ?: request.headers().accept(), body ?: body())
}

/**
 * Attempts to conditionally update a resource based on ETag or If-Unmodified-Since headers.
 *
 * This method checks the provided conditional headers (e.g., ETag, If-Unmodified-Since)
 * against the current or previous state of the resource and only updates if the conditions are met.
 * If the conditions are not met, an exception is thrown, and the response status is not 2xx.
 *
 * @param previousLastModifiedDate The last known modification timestamp of the resource.
 * @param requireAtLeastOneValidator When true, at least one conditional header must be present for the operation to proceed.
 * @param status A custom HTTP status to assign to the response when the update is successful.
 * @param lazyException A supplier that provides an exception to be thrown when validation fails.
 * @param lazyExceptionIfNotPresent A supplier that provides an exception to be thrown when required validators are missing.
 * @param includeFeatureCode If true, includes a "Feature-Code" header in the response based on the calling method's metadata.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param newLastModifiedDate The updated last-modified timestamp to set in the response after a successful update.
 * @param expires The expiration date for the response. If provided, it sets the "Expires" header.
 * @param preferenceApplied A list of preference-applied values to include in the response. If provided, it sets the "Preference-Applied" header.
 * @param refresh The refresh duration for the response. If provided, it sets the "Refresh" header.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Additional HTTP headers to include in the response. HAS LOWER PRIORITY THAN THE NEXT PARAMETES.
 * @param previousValue A supplier callback used to retrieve the previous state of the resource, if available.
 * @param body A supplier callback used to compute or generate the new value for the resource.
 * @return A [Response] containing the newly updated value or additional details as appropriate, with an appropriate HTTP status.
 * @throws PreconditionFailedException Thrown if the ETag or If-Unmodified-Since validation fails.
 * @throws PreconditionRequiredException Thrown if conditional update headers (e.g., If-Match, If-Unmodified-Since) are required but missing.
 * @since 3.0.0
 */
@JvmName("conditionalUpdateNewValue")
context(request: Request)
suspend inline fun <T : Any, reified R : Any> conditionalUpdate(
    previousLastModifiedDate: OffsetDateTime? = null,
    requireAtLeastOneValidator: Boolean = false,
    status: HttpStatus? = null,
    lazyException: ThrowableSupplier = { PreconditionFailedException("ETag not matched or If-Unmodified-Since header failed.") },
    lazyExceptionIfNotPresent: ThrowableSupplier = { PreconditionRequiredException("Use: If-Match, If-Unmodified-Since") },
    includeFeatureCode: Boolean = true,
    includeRequestId: Boolean = true,
    newLastModifiedDate: OffsetDateTime? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: List<String> = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    contentType: MediaType? = null,
    headers: HttpHeaders = HttpHeaders(),
    noinline previousValue: Supplier<T?>?,
    noinline body: Supplier<R>
): Response {
    val eTagIfMatch = request.ifMatch()
    val ifUnmodifiedSince = request.ifUnmodifiedSince()?.toOffsetDateTime()
    if (requireAtLeastOneValidator && eTagIfMatch.isEmpty() && ifUnmodifiedSince.isNull())
        throw lazyExceptionIfNotPresent()

    if (previousValue.isNotNull() && eTagIfMatch.isNotNullOrEmpty()) {
        val previousValue = previousValue()
        if (previousValue.isNotNull()) {
            val eTag = previousValue.eTag
            if (eTag !in eTagIfMatch.map { it - Char.QUOTATION_MARK })
                throw lazyException()
        }
    } else if (previousLastModifiedDate.isNotNull() && ifUnmodifiedSince.isNotNull() && previousLastModifiedDate.isAfter(ifUnmodifiedSince))
        throw lazyException()

    val body = body()
    val status = status ?: (if (body is Unit) HttpStatus.NO_CONTENT else HttpStatus.OK)
    val response = Response.status(status.toSpringHttpStatus())
    if (headers.isNotEmpty()) response.headers { it.addAll(headers.toSpringHttpHeaders()) }
    if (includeFeatureCode) response.featureCode()
    if (newLastModifiedDate.isNotNull()) response.lastModified(newLastModifiedDate.toInstant())
    if (expires.isNotNull()) response.expires(expires)
    if (includeRequestId) response.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (preferenceApplied.isNotEmpty()) response.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) response.refresh(refresh)
    if (serverTiming.isNotEmpty()) response.serverTiming(*serverTiming.toTypedArray())
    if (body !is Unit) response.eTag(body.eTag)
    return response.negotiateBodyValueWithTypeAndAwait(contentType?.toSpringMediaType()?.asSingleList() ?: request.headers().accept(), body)
}
/**
 * Attempts to conditionally update a resource based on ETag or If-Unmodified-Since headers.
 *
 * This method checks the provided conditional headers (e.g., ETag, If-Unmodified-Since)
 * against the current or previous state of the resource and only updates if the conditions are met.
 * If the conditions are not met, an exception is thrown, and the response status is not 2xx.
 *
 * @param previousLastModifiedDate The last known modification timestamp of the resource.
 * @param requireAtLeastOneValidator When true, at least one conditional header must be present for the operation to proceed.
 * @param status A custom HTTP status to assign to the response when the update is successful.
 * @param lazyException A supplier that provides an exception to be thrown when validation fails.
 * @param lazyExceptionIfNotPresent A supplier that provides an exception to be thrown when required validators are missing.
 * @param includeFeatureCode If true, includes a "Feature-Code" header in the response based on the calling method's metadata.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param newLastModifiedDate The updated last-modified timestamp to set in the response after a successful update.
 * @param expires The expiration date for the response. If provided, it sets the "Expires" header.
 * @param preferenceApplied A list of preference-applied values to include in the response. If provided, it sets the "Preference-Applied" header.
 * @param refresh The refresh duration for the response. If provided, it sets the "Refresh" header.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Additional HTTP headers to include in the response. HAS LOWER PRIORITY THAN THE NEXT PARAMETES.
 * @param previousETag The previous ETag of the resource.
 * @param body A supplier callback used to compute or generate the new value for the resource.
 * @return A [Response] containing the newly updated value or additional details as appropriate, with an appropriate HTTP status.
 * @throws PreconditionFailedException Thrown if the ETag or If-Unmodified-Since validation fails.
 * @throws PreconditionRequiredException Thrown if conditional update headers (e.g., If-Match, If-Unmodified-Since) are required but missing.
 * @since 3.0.0
 */
@JvmName("conditionalUpdateNewValue")
context(request: Request)
suspend inline fun <T : Any, reified R : Any> conditionalUpdate(
    previousLastModifiedDate: OffsetDateTime? = null,
    requireAtLeastOneValidator: Boolean = false,
    status: HttpStatus? = null,
    lazyException: ThrowableSupplier = { PreconditionFailedException("ETag not matched or If-Unmodified-Since header failed.") },
    lazyExceptionIfNotPresent: ThrowableSupplier = { PreconditionRequiredException("Use: If-Match, If-Unmodified-Since") },
    includeFeatureCode: Boolean = true,
    includeRequestId: Boolean = true,
    newLastModifiedDate: OffsetDateTime? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: List<String> = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    contentType: MediaType? = null,
    headers: HttpHeaders = HttpHeaders(),
    previousETag: String?,
    noinline body: Supplier<R>
): Response {
    val eTagIfMatch = request.ifMatch()
    val ifUnmodifiedSince = request.ifUnmodifiedSince()?.toOffsetDateTime()
    if (requireAtLeastOneValidator && eTagIfMatch.isEmpty() && ifUnmodifiedSince.isNull())
        throw lazyExceptionIfNotPresent()

    if (previousETag.isNotNull() && eTagIfMatch.isNotNullOrEmpty()) {
        if (previousETag - Char.QUOTATION_MARK !in eTagIfMatch.map { it - Char.QUOTATION_MARK })
            throw lazyException()
    } else if (previousLastModifiedDate.isNotNull() && ifUnmodifiedSince.isNotNull() && previousLastModifiedDate.isAfter(ifUnmodifiedSince))
        throw lazyException()

    val body = body()
    val status = status ?: (if (body is Unit) HttpStatus.NO_CONTENT else HttpStatus.OK)
    val response = Response.status(status.toSpringHttpStatus())
    if (headers.isNotEmpty()) response.headers { it.addAll(headers.toSpringHttpHeaders()) }
    if (includeFeatureCode) response.featureCode()
    if (includeRequestId) response.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (newLastModifiedDate.isNotNull()) response.lastModified(newLastModifiedDate.toInstant())
    if (expires.isNotNull()) response.expires(expires)
    if (preferenceApplied.isNotEmpty()) response.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) response.refresh(refresh)
    if (serverTiming.isNotEmpty()) response.serverTiming(*serverTiming.toTypedArray())
    if (body !is Unit) response.eTag(body.eTag)
    return response.negotiateBodyValueWithTypeAndAwait(contentType?.toSpringMediaType()?.asSingleList() ?: request.headers().accept(), body)
}
/**
 * Conditionally updates a resource based on specified validators such as ETag or last modified date.
 *
 * @param previousLastModifiedDate The last known modification date of the resource, used to compare with
 *        the `ifUnmodifiedSince` parameter.
 * @param requireAtLeastOneValidator If true, at least one validator (e.g., `eTagIfMatch`, `ifUnmodifiedSince`)
 *        must be satisfied for the update to proceed.
 * @param status Optional HTTP status to return for a successful update; defaults to OK or NO_CONTENT based
 *        on the body content.
 * @param lazyException The exception supplier called when the preconditions are not satisfied.
 * @param lazyExceptionIfNotPresent The exception supplier called when required validators are missing.
 * @param featureCode Feature code added as a header to the response.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param newLastModifiedDate Optional new last modified date to include in the response headers if specified.
 * @param expires The expiration date for the response. If provided, it sets the "Expires" header.
 * @param preferenceApplied A list of preference-applied values to include in the response. If provided, it sets the "Preference-Applied" header.
 * @param refresh The refresh duration for the response. If provided, it sets the "Refresh" header.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Optional additional HTTP headers to include in the response. HAS LOWER PRIORITY THAN THE NEXT PARAMETES.
 * @param previousValue Supplier for the previous value of the resource to validate against the
 *        `eTagIfMatch` parameter.
 * @param body Supplier for the new body of the resource, to be used in the response if conditions are met.
 * @return A [Response] object containing the updated body or an appropriate status, headers, and metadata.
 *         If the update is successful, the response will include relevant headers like `Last-Modified`
 *         or `ETag` as applicable.
 * @since 3.0.0
 */
@JvmName("conditionalUpdateNewValue")
context(request: Request)
suspend inline fun <T : Any, reified R : Any> conditionalUpdate(
    previousLastModifiedDate: OffsetDateTime? = null,
    requireAtLeastOneValidator: Boolean = false,
    status: HttpStatus? = null,
    lazyException: ThrowableSupplier = { PreconditionFailedException("ETag not matched or If-Unmodified-Since header failed.") },
    lazyExceptionIfNotPresent: ThrowableSupplier = { PreconditionRequiredException("Use: If-Match, If-Unmodified-Since") },
    featureCode: String,
    includeRequestId: Boolean = true,
    newLastModifiedDate: OffsetDateTime? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: List<String> = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    contentType: MediaType? = null,
    headers: HttpHeaders = HttpHeaders(),
    noinline previousValue: Supplier<T?>?,
    noinline body: Supplier<R>
): Response {
    val eTagIfMatch = request.ifMatch()
    val ifUnmodifiedSince = request.ifUnmodifiedSince()?.toOffsetDateTime()
    if (requireAtLeastOneValidator && eTagIfMatch.isEmpty() && ifUnmodifiedSince.isNull())
        throw lazyExceptionIfNotPresent()

    if (previousValue.isNotNull() && eTagIfMatch.isNotNullOrEmpty()) {
        val previousValue = previousValue()
        if (previousValue.isNotNull()) {
            val eTag = previousValue.eTag
            if (eTag !in eTagIfMatch.map { it - Char.QUOTATION_MARK })
                throw lazyException()
        }
    } else if (previousLastModifiedDate.isNotNull() && ifUnmodifiedSince.isNotNull() && previousLastModifiedDate.isAfter(ifUnmodifiedSince))
        throw lazyException()

    val body = body()
    val status = status ?: (if (body is Unit) HttpStatus.NO_CONTENT else HttpStatus.OK)
    val response = Response.status(status.toSpringHttpStatus())
    if (headers.isNotEmpty()) response.headers { it.addAll(headers.toSpringHttpHeaders()) }
    response.featureCode(featureCode)
    if (newLastModifiedDate.isNotNull()) response.lastModified(newLastModifiedDate.toInstant())
    if (expires.isNotNull()) response.expires(expires)
    if (preferenceApplied.isNotEmpty()) response.preferenceApplied(*preferenceApplied.toTypedArray())
    if (includeRequestId) response.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (refresh.isNotNull()) response.refresh(refresh)
    if (serverTiming.isNotEmpty()) response.serverTiming(*serverTiming.toTypedArray())
    if (body !is Unit) response.eTag(body.eTag)
    return response.negotiateBodyValueWithTypeAndAwait(contentType?.toSpringMediaType()?.asSingleList() ?: request.headers().accept(), body)
}
/**
 * Conditionally updates a resource based on specified validators such as ETag or last modified date.
 *
 * @param previousLastModifiedDate The last known modification date of the resource, used to compare with
 *        the `ifUnmodifiedSince` parameter.
 * @param requireAtLeastOneValidator If true, at least one validator (e.g., `eTagIfMatch`, `ifUnmodifiedSince`)
 *        must be satisfied for the update to proceed.
 * @param status Optional HTTP status to return for a successful update; defaults to OK or NO_CONTENT based
 *        on the body content.
 * @param lazyException The exception supplier called when the preconditions are not satisfied.
 * @param lazyExceptionIfNotPresent The exception supplier called when required validators are missing.
 * @param featureCode Feature code added as a header to the response.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param newLastModifiedDate Optional new last modified date to include in the response headers if specified.
 * @param expires The expiration date for the response. If provided, it sets the "Expires" header.
 * @param preferenceApplied A list of preference-applied values to include in the response. If provided, it sets the "Preference-Applied" header.
 * @param refresh The refresh duration for the response. If provided, it sets the "Refresh" header.
 * @param serverTiming A set of triples containing the "Server-Timing" header label, duration, and description.
 * @param headers Optional additional HTTP headers to include in the response. HAS LOWER PRIORITY THAN THE NEXT PARAMETES.
 * @param previousETag The previous ETag of the resource.
 * @param body Supplier for the new body of the resource, to be used in the response if conditions are met.
 * @return A [Response] object containing the updated body or an appropriate status, headers, and metadata.
 *         If the update is successful, the response will include relevant headers like `Last-Modified`
 *         or `ETag` as applicable.
 * @since 3.0.0
 */
@JvmName("conditionalUpdateNewValue")
context(request: Request)
suspend inline fun <T : Any, reified R : Any> conditionalUpdate(
    previousLastModifiedDate: OffsetDateTime? = null,
    requireAtLeastOneValidator: Boolean = false,
    status: HttpStatus? = null,
    lazyException: ThrowableSupplier = { PreconditionFailedException("ETag not matched or If-Unmodified-Since header failed.") },
    lazyExceptionIfNotPresent: ThrowableSupplier = { PreconditionRequiredException("Use: If-Match, If-Unmodified-Since") },
    featureCode: String,
    includeRequestId: Boolean = true,
    newLastModifiedDate: OffsetDateTime? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: List<String> = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    contentType: MediaType? = null,
    headers: HttpHeaders = HttpHeaders(),
    previousETag: String?,
    noinline body: Supplier<R>
): Response {
    val eTagIfMatch = request.ifMatch()
    val ifUnmodifiedSince = request.ifUnmodifiedSince()?.toOffsetDateTime()
    if (requireAtLeastOneValidator && eTagIfMatch.isEmpty() && ifUnmodifiedSince.isNull())
        throw lazyExceptionIfNotPresent()

    if (previousETag.isNotNull() && eTagIfMatch.isNotNullOrEmpty()) {
        if (previousETag - Char.QUOTATION_MARK !in eTagIfMatch.map { it - Char.QUOTATION_MARK })
            throw lazyException()
    } else if (previousLastModifiedDate.isNotNull() && ifUnmodifiedSince.isNotNull() && previousLastModifiedDate.isAfter(ifUnmodifiedSince))
        throw lazyException()

    val body = body()
    val status = status ?: (if (body is Unit) HttpStatus.NO_CONTENT else HttpStatus.OK)
    val response = Response.status(status.toSpringHttpStatus())
    if (headers.isNotEmpty()) response.headers { it.addAll(headers.toSpringHttpHeaders()) }
    response.featureCode(featureCode)
    if (newLastModifiedDate.isNotNull()) response.lastModified(newLastModifiedDate.toInstant())
    if (expires.isNotNull()) response.expires(expires)
    if (includeRequestId) response.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (preferenceApplied.isNotEmpty()) response.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) response.refresh(refresh)
    if (serverTiming.isNotEmpty()) response.serverTiming(*serverTiming.toTypedArray())
    if (body !is Unit) response.eTag(body.eTag)
    return response.negotiateBodyValueWithTypeAndAwait(contentType?.toSpringMediaType()?.asSingleList() ?: request.headers().accept(), body)
}
/**
 * Executes a conditional update operation based on the provided parameters and validators such as ETag and
 * If-Unmodified-Since headers. It throws exceptions if the preconditions are not met.
 *
 * @param previousLastModifiedDate Optional previous last modified date of the resource.
 * @param requireAtLeastOneValidator If true, at least one validator (ETag or If-Unmodified-Since) must be provided and pass.
 * @param status HTTP status to be used for a successful response. Defaults to HttpStatus.NO_CONTENT.
 * @param lazyException Supplier of the exception to be thrown when validators fail.
 * @param lazyExceptionIfNotPresent Supplier of the exception to be thrown when required validators are not present.
 * @param includeFeatureCode If true, includes a "Feature-Code" header in the response.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param newLastModifiedDate Optional timestamp to set the new last modified date in the response.
 * @param preferenceApplied A list of preference-applied values to include in the response. If provided, it sets the "Preference-Applied" header.
 * @param refresh The refresh duration for the response. If provided, it sets the "Refresh" header.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Optional HTTP headers to include in the response. HAS LOWER PRIORITY THAN THE NEXT PARAMETES.
 * @param previousValue Supplier of the previous value of the resource, utilized for ETag validation.
 * @param action Action to execute as the conditional operation if validators pass.
 * @return An empty [Response] object representing a successful operation.
 * @since 3.0.0
 */
@JvmName("conditionalUpdateAction")
context(request: Request)
suspend fun <T : Any> conditionalUpdate(
    previousLastModifiedDate: OffsetDateTime? = null,
    requireAtLeastOneValidator: Boolean = false,
    status: HttpStatus = HttpStatus.NO_CONTENT,
    lazyException: ThrowableSupplier = { PreconditionFailedException("ETag not matched or If-Unmodified-Since header failed.") },
    lazyExceptionIfNotPresent: ThrowableSupplier = { PreconditionRequiredException("Use: If-Match, If-Unmodified-Since") },
    includeFeatureCode: Boolean = true,
    includeRequestId: Boolean = true,
    newLastModifiedDate: OffsetDateTime? = null,
    preferenceApplied: List<String> = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders = HttpHeaders(),
    previousValue: Supplier<T?>?,
    action: Action? = null
): Response {
    val eTagIfMatch = request.ifMatch()
    val ifUnmodifiedSince = request.ifUnmodifiedSince()?.toOffsetDateTime()
    if (requireAtLeastOneValidator && eTagIfMatch.isEmpty() && ifUnmodifiedSince.isNull())
        throw lazyExceptionIfNotPresent()

    if (previousValue.isNotNull() && eTagIfMatch.isNotNullOrEmpty()) {
        val previousValue = previousValue()
        if (previousValue.isNotNull()) {
            val eTag = previousValue.eTag
            if (eTag !in eTagIfMatch.map { it - Char.QUOTATION_MARK })
                throw lazyException()
        }
    } else if (previousLastModifiedDate.isNotNull() && ifUnmodifiedSince.isNotNull() && previousLastModifiedDate.isAfter(ifUnmodifiedSince))
        throw lazyException()

    action?.invoke()

    val response = Response.status(status.toSpringHttpStatus())
    if (headers.isNotEmpty()) response.headers { it.addAll(headers.toSpringHttpHeaders()) }
    if (includeFeatureCode) response.featureCode()
    if (newLastModifiedDate.isNotNull()) response.lastModified(newLastModifiedDate.toInstant())
    if (includeRequestId) response.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (preferenceApplied.isNotEmpty()) response.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) response.refresh(refresh)
    if (serverTiming.isNotEmpty()) response.serverTiming(*serverTiming.toTypedArray())
    return response.buildAndAwait()
}
/**
 * Executes a conditional update operation based on the provided parameters and validators such as ETag and
 * If-Unmodified-Since headers. It throws exceptions if the preconditions are not met.
 *
 * @param previousLastModifiedDate Optional previous last modified date of the resource.
 * @param requireAtLeastOneValidator If true, at least one validator (ETag or If-Unmodified-Since) must be provided and pass.
 * @param status HTTP status to be used for a successful response. Defaults to HttpStatus.NO_CONTENT.
 * @param lazyException Supplier of the exception to be thrown when validators fail.
 * @param lazyExceptionIfNotPresent Supplier of the exception to be thrown when required validators are not present.
 * @param includeFeatureCode If true, includes a "Feature-Code" header in the response.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param newLastModifiedDate Optional timestamp to set the new last modified date in the response.
 * @param preferenceApplied A list of preference-applied values to include in the response. If provided, it sets the "Preference-Applied" header.
 * @param refresh The refresh duration for the response. If provided, it sets the "Refresh" header.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Optional HTTP headers to include in the response. HAS LOWER PRIORITY THAN THE NEXT PARAMETES.
 * @param previousETag The previous ETag of the resource.
 * @param action Action to execute as the conditional operation if validators pass.
 * @return An empty [Response] object representing a successful operation.
 * @since 3.0.0
 */
@JvmName("conditionalUpdateAction")
context(request: Request)
suspend fun <T : Any> conditionalUpdate(
    previousLastModifiedDate: OffsetDateTime? = null,
    requireAtLeastOneValidator: Boolean = false,
    status: HttpStatus = HttpStatus.NO_CONTENT,
    lazyException: ThrowableSupplier = { PreconditionFailedException("ETag not matched or If-Unmodified-Since header failed.") },
    lazyExceptionIfNotPresent: ThrowableSupplier = { PreconditionRequiredException("Use: If-Match, If-Unmodified-Since") },
    includeFeatureCode: Boolean = true,
    includeRequestId: Boolean = true,
    newLastModifiedDate: OffsetDateTime? = null,
    preferenceApplied: List<String> = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders = HttpHeaders(),
    previousETag: String?,
    action: Action? = null
): Response {
    val eTagIfMatch = request.ifMatch()
    val ifUnmodifiedSince = request.ifUnmodifiedSince()?.toOffsetDateTime()
    if (requireAtLeastOneValidator && eTagIfMatch.isEmpty() && ifUnmodifiedSince.isNull())
        throw lazyExceptionIfNotPresent()

    if (previousETag.isNotNull() && eTagIfMatch.isNotNullOrEmpty()) {
        if (previousETag - Char.QUOTATION_MARK !in eTagIfMatch.map { it - Char.QUOTATION_MARK })
            throw lazyException()
    } else if (previousLastModifiedDate.isNotNull() && ifUnmodifiedSince.isNotNull() && previousLastModifiedDate.isAfter(ifUnmodifiedSince))
        throw lazyException()

    action?.invoke()

    val response = Response.status(status.toSpringHttpStatus())
    if (headers.isNotEmpty()) response.headers { it.addAll(headers.toSpringHttpHeaders()) }
    if (includeFeatureCode) response.featureCode()
    if (newLastModifiedDate.isNotNull()) response.lastModified(newLastModifiedDate.toInstant())
    if (includeRequestId) response.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (preferenceApplied.isNotEmpty()) response.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) response.refresh(refresh)
    if (serverTiming.isNotEmpty()) response.serverTiming(*serverTiming.toTypedArray())
    return response.buildAndAwait()
}
/**
 * Executes a conditional update operation based on the provided validators, such as ETags or last modified date.
 *
 * @param previousLastModifiedDate Previous timestamp of when the resource was last modified, used for comparison.
 * @param requireAtLeastOneValidator Whether at least one validator must be satisfied for the operation to proceed.
 * @param status HTTP status code to return on successful execution of the update.
 * @param lazyException Lazy-initialized exception to throw if one or more conditions fail.
 * @param lazyExceptionIfNotPresent Lazy-initialized exception to throw if no validators are present but required.
 * @param featureCode Feature code to be added to the response as a custom header.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param newLastModifiedDate New timestamp to set as the last modified date in the response headers, if provided.
 * @param preferenceApplied A list of preference-applied values to include in the response. If provided, it sets the "Preference-Applied" header.
 * @param refresh The refresh duration for the response. If provided, it sets the "Refresh" header.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Additional headers to include in the response. HAS LOWER PRIORITY THAN THE NEXT PARAMETES.
 * @param previousValue Supplier to fetch the previous state of the resource for checks (e.g., ETag comparison).
 * @param action Optional action to execute if the update conditions are satisfied.
 * @return [Response] object representing the outcome of the conditional update.
 * @throws PreconditionFailedException if the conditions for update are not satisfied.
 * @throws PreconditionRequiredException if required validators are missing but necessary.
 * @since 3.0.0
 */
@JvmName("conditionalUpdateAction")
context(request: Request)
suspend fun <T : Any> conditionalUpdate(
    previousLastModifiedDate: OffsetDateTime? = null,
    requireAtLeastOneValidator: Boolean = false,
    status: HttpStatus = HttpStatus.NO_CONTENT,
    lazyException: ThrowableSupplier = { PreconditionFailedException("ETag not matched or If-Unmodified-Since header failed.") },
    lazyExceptionIfNotPresent: ThrowableSupplier = { PreconditionRequiredException("Use: If-Match, If-Unmodified-Since") },
    featureCode: String,
    includeRequestId: Boolean = true,
    newLastModifiedDate: OffsetDateTime? = null,
    preferenceApplied: List<String> = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders = HttpHeaders(),
    previousValue: Supplier<T?>?,
    action: Action? = null
): Response {
    val eTagIfMatch = request.ifMatch()
    val ifUnmodifiedSince = request.ifUnmodifiedSince()?.toOffsetDateTime()
    if (requireAtLeastOneValidator && eTagIfMatch.isEmpty() && ifUnmodifiedSince.isNull())
        throw lazyExceptionIfNotPresent()

    if (previousValue.isNotNull() && eTagIfMatch.isNotNullOrEmpty()) {
        val previousValue = previousValue()
        if (previousValue.isNotNull()) {
            val eTag = previousValue.eTag
            if (eTag !in eTagIfMatch.map { it - Char.QUOTATION_MARK })
                throw lazyException()
        }
    } else if (previousLastModifiedDate.isNotNull() && ifUnmodifiedSince.isNotNull() && previousLastModifiedDate.isAfter(ifUnmodifiedSince))
        throw lazyException()

    action?.invoke()

    val response = Response.status(status.toSpringHttpStatus())
    if (headers.isNotEmpty()) response.headers { it.addAll(headers.toSpringHttpHeaders()) }
    response.featureCode(featureCode)
    if (includeRequestId) response.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (newLastModifiedDate.isNotNull()) response.lastModified(newLastModifiedDate.toInstant())
    if (preferenceApplied.isNotEmpty()) response.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) response.refresh(refresh)
    if (serverTiming.isNotEmpty()) response.serverTiming(*serverTiming.toTypedArray())
    return response.buildAndAwait()
}
/**
 * Executes a conditional update operation based on the provided validators, such as ETags or last modified date.
 *
 * @param previousLastModifiedDate Previous timestamp of when the resource was last modified, used for comparison.
 * @param requireAtLeastOneValidator Whether at least one validator must be satisfied for the operation to proceed.
 * @param status HTTP status code to return on successful execution of the update.
 * @param lazyException Lazy-initialized exception to throw if one or more conditions fail.
 * @param lazyExceptionIfNotPresent Lazy-initialized exception to throw if no validators are present but required.
 * @param featureCode Feature code to be added to the response as a custom header.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param newLastModifiedDate New timestamp to set as the last modified date in the response headers, if provided.
 * @param preferenceApplied A list of preference-applied values to include in the response. If provided, it sets the "Preference-Applied" header.
 * @param refresh The refresh duration for the response. If provided, it sets the "Refresh" header.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Additional headers to include in the response. HAS LOWER PRIORITY THAN THE NEXT PARAMETES.
 * @param previousETag The previous ETag of the resource.
 * @param action Optional action to execute if the update conditions are satisfied.
 * @return [Response] object representing the outcome of the conditional update.
 * @throws PreconditionFailedException if the conditions for update are not satisfied.
 * @throws PreconditionRequiredException if required validators are missing but necessary.
 * @since 3.0.0
 */
@JvmName("conditionalUpdateAction")
context(request: Request)
suspend fun <T : Any> conditionalUpdate(
    previousLastModifiedDate: OffsetDateTime? = null,
    requireAtLeastOneValidator: Boolean = false,
    status: HttpStatus = HttpStatus.NO_CONTENT,
    lazyException: ThrowableSupplier = { PreconditionFailedException("ETag not matched or If-Unmodified-Since header failed.") },
    lazyExceptionIfNotPresent: ThrowableSupplier = { PreconditionRequiredException("Use: If-Match, If-Unmodified-Since") },
    featureCode: String,
    includeRequestId: Boolean = true,
    newLastModifiedDate: OffsetDateTime? = null,
    preferenceApplied: List<String> = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders = HttpHeaders(),
    previousETag: String?,
    action: Action? = null
): Response {
    val eTagIfMatch = request.ifMatch()
    val ifUnmodifiedSince = request.ifUnmodifiedSince()?.toOffsetDateTime()
    if (requireAtLeastOneValidator && eTagIfMatch.isEmpty() && ifUnmodifiedSince.isNull())
        throw lazyExceptionIfNotPresent()

    if (previousETag.isNotNull() && eTagIfMatch.isNotNullOrEmpty()) {
        if (previousETag - Char.QUOTATION_MARK !in eTagIfMatch.map { it - Char.QUOTATION_MARK })
            throw lazyException()
    } else if (previousLastModifiedDate.isNotNull() && ifUnmodifiedSince.isNotNull() && previousLastModifiedDate.isAfter(ifUnmodifiedSince))
        throw lazyException()

    action?.invoke()

    val response = Response.status(status.toSpringHttpStatus())
    if (headers.isNotEmpty()) response.headers { it.addAll(headers.toSpringHttpHeaders()) }
    response.featureCode(featureCode)
    if (includeRequestId) response.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (newLastModifiedDate.isNotNull()) response.lastModified(newLastModifiedDate.toInstant())
    if (preferenceApplied.isNotEmpty()) response.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) response.refresh(refresh)
    if (serverTiming.isNotEmpty()) response.serverTiming(*serverTiming.toTypedArray())
    return response.buildAndAwait()
}

/**
 * Constructs an `Response` object with the specified HTTP status, optional headers, action,
 * and feature code header inclusion settings.
 *
 * If an `action` is provided, it is invoked before building the response. By default,
 * the HTTP status is set to `HttpStatus.NO_CONTENT`, and the "Feature-Code" header is included
 * if applicable. Custom headers can also be added through the `headers` parameter.
 *
 * @param status The HTTP status to set for the response. Defaults to `HttpStatus.NO_CONTENT`.
 * @param includeFeatureCode Whether to include the "Feature-Code" header in the response. Defaults to `true`.*
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param eTag Optional ETag value to include in the response. Defaults to `null`.
 * @param preferenceApplied Optional list of preference-applied values to include in the response. Defaults to an empty list.
 * @param refresh Optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to. Defaults to `null`.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Optional HTTP headers to include in the response (overrides any other header parameters of this method). Defaults to `null`.
 * @param action An optional action to execute before building the response. Defaults to `null`.
 * @return A constructed `Response` object with the specified settings.
 * @since 3.0.0
 */
suspend fun EmptyResponse(
    status: HttpStatus = HttpStatus.NO_CONTENT,
    includeFeatureCode: Boolean = true,
    includeRequestId: Boolean = true,
    eTag: String? = null,
    lastModified: OffsetDateTime? = null,
    preferenceApplied: List<String> = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders = HttpHeaders(),
    action: Action? = null
): Response {
    if (action.isNotNull())
        action()

    val re = Response.status(status.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (includeFeatureCode) re.featureCode()
    if (eTag.isNotNull()) re.eTag(eTag)
    if (lastModified.isNotNull()) re.lastModified(lastModified.toInstant())
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    return re.buildAndAwait()
}
/**
 * Constructs an `Response` with the given HTTP status, feature code, optional headers, and action.
 *
 * @param status the HTTP status to set for the response; defaults to `HttpStatus.NO_CONTENT`
 * @param featureCode the feature code to be added as a "Feature-Code" header in the response
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param eTag Optional ETag value to include in the response. Defaults to `null`.
 * @param lastModifiedDate Optional last modified date to include in the response. Defaults to `null`.
 * @param preferenceApplied Optional list of preference-applied values to include in the response. Defaults to an empty list.
 * @param refresh Optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to. Defaults to `null`.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers optional HTTP headers to include in the response (overrides any other header parameters of this method); can be null
 * @param action an optional action to execute; can be null
 * @return an `Response` constructed with the specified parameters
 * @since 3.0.0
 */
suspend fun EmptyResponse(
    status: HttpStatus = HttpStatus.NO_CONTENT,
    featureCode: String,
    includeRequestId: Boolean = true,
    eTag: String? = null,
    lastModifiedDate: OffsetDateTime? = null,
    preferenceApplied: List<String> = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders = HttpHeaders(),
    action: Action? = null
): Response {
    if (action.isNotNull())
        action()

    val re = Response.status(status.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    re.featureCode(featureCode)
    if (eTag.isNotNull()) re.eTag(eTag)
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (lastModifiedDate.isNotNull()) re.lastModified(lastModifiedDate.toInstant())
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    return re.buildAndAwait()
}

/**
 * Generates an HTTP response with status code 200 (OK) and allows for optional customization
 * of headers and body content.
 *
 * @param T The type of the response body.
 * @param includeFeatureCode Indicates whether to include the "Feature-Code" header in the response.
 *                           Defaults to true.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param includeETag Indicates whether to include an ETag header based on the response body. Defaults to true.
 * @param lastModifiedDate The optional last modified date to include in the response headers.
 * @param preferenceApplied Optional list of preference-applied values to include in the response. Defaults to an empty list.
 * @param refresh Optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to. Defaults to `null`.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Additional HTTP headers to include in the response. Defaults to null if no extra headers are needed.
 * @param body A supplier function that provides the response body content. Defaults to null.
 * @return An HTTP response of type `Response` with the specified status, headers, and body content.
 * @since 3.0.0
 */
context(request: Request)
suspend inline fun <reified T : Any> OKResponse(
    includeFeatureCode: Boolean = true,
    includeRequestId: Boolean = true,
    includeETag: Boolean = true,
    lastModifiedDate: OffsetDateTime? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: List<String> = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    contentType: MediaType? = null,
    headers: HttpHeaders = HttpHeaders(),
    noinline body: Supplier<T>? = null
): Response {
    val re = Response.status(HttpStatus.OK.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    if (expires.isNotNull()) re.expires(expires)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (includeFeatureCode) re.featureCode()
    val result = body?.invoke()
    if (includeETag && result.isNotNull()) re.eTag(result.eTag)
    if (lastModifiedDate.isNotNull()) re.lastModified(lastModifiedDate.toInstant())
    if (result.isNotNull()) return re.negotiateBodyValueWithTypeAndAwait(contentType?.toSpringMediaType()?.asSingleList() ?: request.headers().accept(), result)
    return re.buildAndAwait()
}
/**
 * Constructs an HTTP OK response with optional headers and body content.
 *
 * @param featureCode a unique code to be included as a "Feature-Code" header in the response
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param includeETag whether to include an ETag header based on the body content, defaults to true
 * @param lastModifiedDate an optional timestamp to include as a "Last-Modified" header, defaults to null
 * @param expires an optional timestamp to include as an "Expires" header, defaults to null
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param refresh optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to, defaults to null
 * @param serverTiming A set of triples containing the "Server-Timing" header label, duration, and description.
 * @param headers additional headers to include in the response, defaults to null
 * @param body a supplier for the response body content, which can be null
 * @return a Response instance of type T encapsulating the provided configurations
 * @since 3.0.0
 */
context(request: Request)
suspend inline fun <reified T : Any> OKResponse(
    featureCode: String,
    includeRequestId: Boolean = true,
    includeETag: Boolean = true,
    lastModifiedDate: OffsetDateTime? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: List<String> = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    contentType: MediaType? = null,
    headers: HttpHeaders = HttpHeaders(),
    noinline body: Supplier<T>? = null
): Response {
    val re = Response.status(HttpStatus.OK.toSpringHttpStatus())
    val result = body?.invoke()
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    re.featureCode(featureCode)
    if (expires.isNotNull()) re.expires(expires)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (includeETag && result.isNotNull()) re.eTag(result.eTag)
    if (lastModifiedDate.isNotNull()) re.lastModified(lastModifiedDate.toInstant())
    if (result.isNotNull()) return re.negotiateBodyValueWithTypeAndAwait(contentType?.toSpringMediaType()?.asSingleList() ?: request.headers().accept(), result)
    return re.buildAndAwait()
}

/**
 * Constructs a `Response` object with an HTTP status of "201 Created", optionally including additional
 * headers, metadata, and a response body based on the provided parameters.
 *
 * @param T The type of the response body.
 * @param includeFeatureCode Whether to include a "Feature-Code" header based on the `Feature` annotation of the calling method. Defaults to true.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param includeETag Whether to include the ETag header based on the response body content. Defaults to true.
 * @param lastModifiedDate The last modified date to include in the response header, if provided. Defaults to null.
 * @param location The location URI to include in the response header, if applicable. Defaults to null.
 * @param expires An optional timestamp to include as an "Expires" header, defaults to null
 * @param preferenceApplied Optional list of preference-applied values to include in the response. Defaults to an empty list.
 * @param refresh Optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to. Defaults to `null`.
 * @param serverTiming A set of triples containing the "Server-Timing" header label, duration, and description.
 * @param headers Additional headers to include in the response, if specified. Defaults to null.
 * @param includeBody Whether to include the response body. Defaults to `true` if `location` is null.
 * @param body A supplier for the response body. Defaults to null if no body is required.
 * @return A `Response` object containing the constructed HTTP status, headers, and optional body.
 * @since 3.0.0
 */
context(request: Request)
suspend inline fun <reified T : Any> CreatedResponse(
    includeFeatureCode: Boolean = true,
    includeRequestId: Boolean = true,
    includeETag: Boolean = true,
    lastModifiedDate: OffsetDateTime? = null,
    location: Uri? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: List<String> = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    contentType: MediaType? = null,
    headers: HttpHeaders = HttpHeaders(),
    includeBody: Boolean = location.isNull(),
    noinline body: Supplier<T>? = null
): Response {
    val re = Response.status(HttpStatus.CREATED.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    if (includeFeatureCode) re.featureCode()
    val result = body?.invoke()
    if (includeETag && result.isNotNull()) re.eTag(result.eTag)
    if (lastModifiedDate.isNotNull()) re.lastModified(lastModifiedDate.toInstant())
    if (location.isNotNull()) re.location(location)
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (expires.isNotNull()) re.expires(expires)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (result.isNotNull() && includeBody) return re.negotiateBodyValueWithTypeAndAwait(contentType?.toSpringMediaType()?.asSingleList() ?: request.headers().accept(), result)
    return re.buildAndAwait()
}
/**
 * Constructs an HTTP 201 Created response with optional headers, body content, and metadata.
 *
 * @param featureCode the feature code to be added as the "Feature-Code" header in the response
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param includeETag a boolean indicating whether to include an ETag header in the response, defaults to true
 * @param lastModifiedDate the date and time the resource was last modified, included as a Last-Modified header if provided
 * @param location the URI of the created resource, included as a Location header if provided
 * @param expires an optional timestamp to include as an "Expires" header, defaults to null
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param refresh optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to, defaults to null
 * @param serverTiming A set of triples containing the "Server-Timing" header label, duration, and description.
 * @param headers additional headers to be included in the response
 * @param includeBody a boolean indicating whether to include the body in the response, defaults to true if `location` is null
 * @param body a supplier providing the body of the response, invoked if a body is to be included
 * @return a Response object of type T representing the constructed 201 Created response
 * @since 3.0.0
 */
context(request: Request)
suspend inline fun <reified T : Any> CreatedResponse(
    featureCode: String,
    includeRequestId: Boolean = true,
    includeETag: Boolean = true,
    lastModifiedDate: OffsetDateTime? = null,
    location: Uri? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: List<String> = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    contentType: MediaType? = null,
    headers: HttpHeaders = HttpHeaders(),
    includeBody: Boolean = location.isNull(),
    noinline body: Supplier<T>? = null
): Response {
    val re = Response.status(HttpStatus.CREATED.toSpringHttpStatus())
    val result = body?.invoke()
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    re.featureCode(featureCode)
    if (includeETag && result.isNotNull()) re.eTag(result.eTag)
    if (lastModifiedDate.isNotNull()) re.lastModified(lastModifiedDate.toInstant())
    if (location.isNotNull()) re.location(location)
    if (expires.isNotNull()) re.expires(expires)
    if (includeRequestId) re.header("Request-Id", requestId().toString())
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (result.isNotNull() && includeBody) return re.negotiateBodyValueWithTypeAndAwait(contentType?.toSpringMediaType()?.asSingleList() ?: request.headers().accept(), result)
    return re.buildAndAwait()
}

/**
 * Constructs an HTTP response with a status of 202 Accepted and optional configuration
 * for headers, entity tag (ETag), and last modified date.
 *
 * @param T The type of the response body.
 * @param includeFeatureCode Determines whether a "Feature-Code" header should be included in the response.
 *                           Defaults to true.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param includeETag Indicates if the response should include an ETag header determined by the body content.
 *                    Defaults to true.
 * @param lastModifiedDate Specifies the last modified date for the response. Can be null if not applicable.
 * @param expires an optional timestamp to include as an "Expires" header, defaults to null
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param refresh optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to, defaults to null
 * @param serverTiming A set of triples containing the "Server-Timing" header label, duration, and description.
 * @param headers Additional HTTP headers to include in the response. Can be null if no additional headers are needed.
 * @param body A supplier function that provides the response body content. Can be null if no body content is needed.
 * @return The constructed Response object containing the specified HTTP status, headers, and body.
 * @since 3.0.0
 */
context(request: Request)
suspend inline fun <reified T : Any> AcceptedResponse(
    includeFeatureCode: Boolean = true,
    includeRequestId: Boolean = true,
    includeETag: Boolean = true,
    lastModifiedDate: OffsetDateTime? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: List<String> = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    contentType: MediaType? = null,
    headers: HttpHeaders = HttpHeaders(),
    noinline body: Supplier<T>? = null
): Response {
    val re = Response.status(HttpStatus.ACCEPTED.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    if (includeFeatureCode) re.featureCode()
    val result = body?.invoke()
    if (includeETag && result.isNotNull()) re.eTag(result.eTag)
    if (lastModifiedDate.isNotNull()) re.lastModified(lastModifiedDate.toInstant())
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (expires.isNotNull()) re.expires(expires)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (result.isNotNull()) return re.negotiateBodyValueWithTypeAndAwait(contentType?.toSpringMediaType()?.asSingleList() ?: request.headers().accept(), result)
    return re.buildAndAwait()
}
/**
 * Builds a response with an HTTP status of 202 Accepted, optionally including headers and body content.
 *
 * @param featureCode a unique code added to the "Feature-Code" header of the response
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param includeETag indicates whether an ETag header should be included in the response; defaults to true
 * @param lastModifiedDate optional timestamp indicating the last modification date of the resource
 * @param expires an optional timestamp to include as an "Expires" header, defaults to null
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param refresh optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to, defaults to null
 * @param serverTiming A set of triples containing the "Server-Timing" header label, duration, and description.
 * @param headers optional additional headers to include in the response
 * @param body optional supplier for the response body content
 * @return a Response object containing the HTTP status, headers, and optionally body content
 * @since 3.0.0
 */
context(request: Request)
suspend inline fun <reified T : Any> AcceptedResponse(
    featureCode: String,
    includeRequestId: Boolean = true,
    includeETag: Boolean = true,
    lastModifiedDate: OffsetDateTime? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: List<String> = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    contentType: MediaType? = null,
    headers: HttpHeaders = HttpHeaders(),
    noinline body: Supplier<T>? = null
): Response {
    val re = Response.status(HttpStatus.ACCEPTED.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    re.featureCode(featureCode)
    val result = body?.invoke()
    if (expires.isNotNull()) re.expires(expires)
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (includeETag && result.isNotNull()) re.eTag(result.eTag)
    if (lastModifiedDate.isNotNull()) re.lastModified(lastModifiedDate.toInstant())
    if (result.isNotNull()) return re.negotiateBodyValueWithTypeAndAwait(contentType?.toSpringMediaType()?.asSingleList() ?: request.headers().accept(), result)
    return re.buildAndAwait()
}

/**
 * Constructs a `Response` object with a status of `RESET_CONTENT` and optional headers and body content.
 *
 * @param includeFeatureCode Flag indicating whether to include the "Feature-Code" header in the response. Default is `true`.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param eTag Optional ETag header value to include in the response.
 * @param lastModifiedDate Optional timestamp to set the "Last-Modified" header in the response.
 * @param expires an optional timestamp to include as an "Expires" header, defaults to null
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param refresh optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to, defaults to null
 * @param serverTiming A set of triples containing the "Server-Timing" header label, duration, and description.
 * @param headers Optional additional headers to include in the response.
 * @param action An optional action to execute before building the response. Defaults to `null`.
 * @return A `Response` object with the specified settings and content.
 * @since 3.0.0
 */
suspend fun ResetContentResponse(
    includeFeatureCode: Boolean = true,
    includeRequestId: Boolean = true,
    eTag: String? = null,
    lastModifiedDate: OffsetDateTime? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: List<String> = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    contentType: MediaType? = null,
    headers: HttpHeaders = HttpHeaders(),
    action: Action? = null
): Response {
    action?.invoke()
    val re = Response.status(HttpStatus.RESET_CONTENT.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    if (includeFeatureCode) re.featureCode()
    if (eTag.isNotNull()) re.eTag(eTag)
    if (lastModifiedDate.isNotNull()) re.lastModified(lastModifiedDate.toInstant())
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (expires.isNotNull()) re.expires(expires)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    return re.buildAndAwait()
}
/**
 * Constructs a `Response` object with a status of `RESET_CONTENT` and optional headers and body content.
 *
 * @param featureCode "Feature-Code" header value in the response.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param eTag Optional ETag header value to include in the response.
 * @param lastModifiedDate Optional timestamp to set the "Last-Modified" header in the response.
 * @param expires an optional timestamp to include as an "Expires" header, defaults to null
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param refresh optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to, defaults to null
 * @param serverTiming A set of triples containing the "Server-Timing" header label, duration, and description.
 * @param headers Optional additional headers to include in the response.
 * @param action an optional action to execute; can be null
 * @return A `Response` object with the specified settings and content.
 * @since 3.0.0
 */
suspend fun ResetContentResponse(
    featureCode: String,
    includeRequestId: Boolean = true,
    eTag: String? = null,
    lastModifiedDate: OffsetDateTime? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: List<String> = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    contentType: MediaType? = null,
    headers: HttpHeaders = HttpHeaders(),
    action: Action? = null
): Response {
    action?.invoke()
    val re = Response.status(HttpStatus.RESET_CONTENT.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    re.featureCode(featureCode)
    if (expires.isNotNull()) re.expires(expires)
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (eTag.isNotNull()) re.eTag(eTag)
    if (lastModifiedDate.isNotNull()) re.lastModified(lastModifiedDate.toInstant())
    return re.buildAndAwait()
}

/**
 * Constructs a `Response` object with HTTP status 206 (Partial Content) and allows customizing various aspects
 * of the response, including headers, body, and metadata.
 *
 * @param includeFeatureCode Determines whether to include the "Feature-Code" header in the response.
 *        Defaults to true.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param includeETag Determines whether to include the ETag header in the response if the body is not null.
 *        Defaults to true.
 * @param lastModifiedDate Specifies the "Last-Modified" timestamp for the response. Can be null.
 *        Defaults to null.
 * @param expires an optional timestamp to include as an "Expires" header, defaults to null
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param refresh optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to, defaults to null
 * @param serverTiming A set of triples containing the "Server-Timing" header label, duration, and description.
 * @param contentType The content type of the response body. Defaults to `MediaType.APPLICATION_OCTET_STREAM`.
 * @param contentLength The content length of the response body.
 * @param contentRange The content range of the response body.
 * @param contentDisposition The content disposition header value. Can be null. Defaults to null.
 * @param includeAcceptRanges A flag to determine whether to include the "Accept-Ranges" header in the response. Defaults to true.
 * @param headers Custom headers to include in the response. Can be null or empty. Defaults to null.
 * @param body A supplier that provides the body content of the response. Can be null. Defaults to null.
 * @return A `Response` object of type `T` with the specified properties and HTTP status 206.
 * @since 3.0.0
 */
@OptIn(Beta::class)
context(request: Request)
suspend inline fun <reified T : Any> PartialContentResponse(
    includeFeatureCode: Boolean = true,
    includeRequestId: Boolean = true,
    includeETag: Boolean = true,
    lastModifiedDate: OffsetDateTime? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: List<String> = emptyList(),
    refresh: Pair<Duration, Url?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    contentType: MediaType = MediaType.APPLICATION_OCTET_STREAM,
    contentLength: DataSize,
    contentRange: Pair<LongRange, DataSize?>,
    contentDisposition: String? = null,
    includeAcceptRanges: Boolean = true,
    headers: HttpHeaders = HttpHeaders(),
    noinline body: Supplier<T>? = null
): Response {
    val re = Response.status(HttpStatus.PARTIAL_CONTENT.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    if (includeFeatureCode) re.featureCode()
    val result = body?.invoke()
    if (includeETag && result.isNotNull()) re.eTag(result.eTag)
    if (lastModifiedDate.isNotNull()) re.lastModified(lastModifiedDate.toInstant())
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (expires.isNotNull()) re.expires(expires)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    re.contentType(contentType)
    re.contentLength((contentLength convertTo MeasureUnit.DataSizeUnit.BYTES)().value.toLong())
    re.header(HttpHeader(HttpHeader.CONTENT_RANGE, "bytes ${contentRange.first.first}-${contentRange.first.last}${if (contentRange.second.isNotNull()) "/${contentRange.second}" else String.EMPTY}"))
    if (contentDisposition.isNotNull()) re.header(HttpHeader(HttpHeader.CONTENT_DISPOSITION, contentDisposition))
    if (includeAcceptRanges) re.header(HttpHeader(HttpHeader.ACCEPT_RANGES, "bytes"))
    if (result.isNotNull()) return re.negotiateBodyValueWithTypeAndAwait(contentType.toSpringMediaType().asSingleList(), result)
    return re.buildAndAwait()
}
/**
 * Builds and returns a response with HTTP status 206 (Partial Content) and additional metadata or body content.
 *
 * @param T The type of the response body.
 * @param featureCode The feature code to be added as a "Feature-Code" header in the response.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param includeETag A flag indicating whether to include an ETag header based on the response body. Defaults to true.
 * @param lastModifiedDate The date-time to be included in the "Last-Modified" header, if specified.
 * @param expires an optional timestamp to include as an "Expires" header, defaults to null
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param refresh optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to, defaults to null
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Additional HTTP headers to be included in the response, if specified.
 * @param contentType The content type of the response body. Defaults to `MediaType.APPLICATION_OCTET_STREAM`.
 * @param contentLength The content length of the response body.
 * @param contentRange The content range of the response body.
 * @param contentDisposition The content disposition header value. Can be null. Defaults to null.
 * @param includeAcceptRanges A flag to determine whether to include the "Accept-Ranges" header in the response. Defaults to true.
 * @param body A supplier function providing the body content for the response, if specified.
 * @return A built response object containing the given metadata and body content.
 * @since 3.0.0
 */
@OptIn(Beta::class)
context(request: Request)
suspend inline fun <reified T : Any> PartialContentResponse(
    featureCode: String,
    includeRequestId: Boolean = true,
    includeETag: Boolean = true,
    lastModifiedDate: OffsetDateTime? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: List<String> = emptyList(),
    refresh: Pair<Duration, Url?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    contentType: MediaType = MediaType.APPLICATION_OCTET_STREAM,
    contentLength: DataSize,
    contentRange: Pair<LongRange, DataSize?>,
    contentDisposition: String? = null,
    includeAcceptRanges: Boolean = true,
    headers: HttpHeaders = HttpHeaders(),
    noinline body: Supplier<T>? = null
): Response {
    val re = Response.status(HttpStatus.PARTIAL_CONTENT.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    re.featureCode(featureCode)
    if (expires.isNotNull()) re.expires(expires)
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    val result = body?.invoke()
    if (includeETag && result.isNotNull()) re.eTag(result.eTag)
    if (lastModifiedDate.isNotNull()) re.lastModified(lastModifiedDate.toInstant())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    re.contentType(contentType)
    re.contentLength((contentLength convertTo MeasureUnit.DataSizeUnit.BYTES)().value.toLong())
    re.header(HttpHeader(HttpHeader.CONTENT_RANGE, "bytes ${contentRange.first.first}-${contentRange.first.last}${if (contentRange.second.isNotNull()) "/${contentRange.second}" else String.EMPTY}"))
    if (contentDisposition.isNotNull()) re.header(HttpHeader(HttpHeader.CONTENT_DISPOSITION, contentDisposition))
    if (includeAcceptRanges) re.header(HttpHeader(HttpHeader.ACCEPT_RANGES, "bytes"))
    if (result.isNotNull()) return re.negotiateBodyValueWithTypeAndAwait(contentType.toSpringMediaType().asSingleList(), result)
    return re.buildAndAwait()
}

/**
 * Builds and returns a multi-status HTTP response based on the provided parameters and resources.
 *
 * @param includeFeatureCode Flag indicating whether to include the "Feature-Code" header in the response. Defaults to true.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Additional HTTP headers to include in the response. Can be null or empty.
 * @param responseType The format or structure of the response body. Defaults to MultiStatusResponseType.WEBDAV_XML.
 * @param httpVersion The HTTP version to use when formatting the response status. Defaults to "HTTP/1.1".
 * @param resources A list of resource results containing metadata about the operation's outcomes.
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param refresh optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to, defaults to null
 * @return A `Response` instance with a status of HTTP 207 Multi-Status and a body formatted according to the specified `responseType`.
 * @since 3.0.0
 */
context(request: Request)
suspend fun MultiStatusResponse(
    resources: List<ResourceResult>,
    includeFeatureCode: Boolean = true,
    includeRequestId: Boolean = true,
    preferenceApplied: List<String> = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    contentType: MediaType? = null,
    headers: HttpHeaders = HttpHeaders(),
    responseType: MultiStatusResponseType = MultiStatusResponseType.WEBDAV_XML,
    httpVersion: HttpVersion = HttpVersion.HTTP_1_1,
    action: Action? = null
): Response {
    action?.invoke()
    val re = Response.status(HttpStatus.MULTI_STATUS.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    if (includeFeatureCode) re.featureCode()
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    return re.negotiateBodyValueWithTypeAndAwait(contentType?.toSpringMediaType()?.asSingleList() ?: request.headers().accept(), when(responseType) {
        MultiStatusResponseType.WEBDAV_XML -> generateMultiStatusXML(resources, httpVersion.notation)
        MultiStatusResponseType.MAP -> generateMultiStatusMap(resources, httpVersion.notation)
        MultiStatusResponseType.GROUPED_BY_STATUS_MAP -> generateMultiStatusGroupedMap(resources, httpVersion.notation)
        MultiStatusResponseType.GROUPED_BY_SUCCESS_AND_FAILURE_MAP -> generateMultiStatusGroupedByCategoryMap(resources, httpVersion.notation)
    })
}
/**
 * Constructs a multi-status HTTP response based on the provided parameters.
 *
 * @param featureCode A string representing the feature code to be included in the HTTP header.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Optional HTTP headers to be included in the response. Defaults to null.
 * @param responseType The type of the multi-status response content. Defaults to `MultiStatusResponseType.WEBDAV_XML`.
 * @param httpVersion The HTTP version string to be used in the response. Defaults to "HTTP/1.1".
 * @param resources A list of `ResourceResult` objects representing the resource operation results.
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param refresh optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to, defaults to null
 * @return A `Response` object containing the multi-status HTTP response.
 * @since 3.0.0
 */
context(request: Request)
suspend fun MultiStatusResponse(
    resources: List<ResourceResult>,
    featureCode: String,
    includeRequestId: Boolean = true,
    preferenceApplied: List<String> = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    contentType: MediaType? = null,
    headers: HttpHeaders = HttpHeaders(),
    responseType: MultiStatusResponseType = MultiStatusResponseType.WEBDAV_XML,
    httpVersion: HttpVersion = HttpVersion.HTTP_1_1,
): Response {
    val re = Response.status(HttpStatus.MULTI_STATUS.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    re.featureCode(featureCode)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    return re.negotiateBodyValueWithTypeAndAwait(contentType?.toSpringMediaType()?.asSingleList() ?: request.headers().accept(), when(responseType) {
        MultiStatusResponseType.WEBDAV_XML -> generateMultiStatusXML(resources, httpVersion.notation)
        MultiStatusResponseType.MAP -> generateMultiStatusMap(resources, httpVersion.notation)
        MultiStatusResponseType.GROUPED_BY_STATUS_MAP -> generateMultiStatusGroupedMap(resources, httpVersion.notation)
        MultiStatusResponseType.GROUPED_BY_SUCCESS_AND_FAILURE_MAP -> generateMultiStatusGroupedByCategoryMap(resources, httpVersion.notation)
    })
}

internal fun generateMultiStatusMap(results: List<ResourceResult>, httpVersion: String): List<DataMap> {
    val list = emptyMList<DataMap>()
    for (result in results) {
        val map: DataMMap = emptyMMap()
        map["reference"] = result.reference
        map["status"] = "HTTP/" + httpVersion.substringAfter("HTTP/") + Char.SPACE + result.statusCode.value + Char.SPACE + result.statusCode.reasonPhrase
        if (result.description.isNotNullOrBlank()) map["description"] = result.description
        list += map
    }
    return list
}
internal fun generateMultiStatusGroupedMap(results: List<ResourceResult>, httpVersion: String): MultiMap<HttpStatus, DataMap> {
    val grouped = results.groupBy(ResourceResult::statusCode)
    return grouped.mapValues {
        it.value.map { result ->
            val map: DataMMap = emptyMMap()
            map["reference"] = result.reference
            map["status"] = "HTTP/" + httpVersion.substringAfter("HTTP/") + Char.SPACE + result.statusCode.value + Char.SPACE + result.statusCode.reasonPhrase
            if (result.description.isNotNullOrBlank()) map["description"] = result.description
            map
        }
    }
}
internal fun generateMultiStatusGroupedByCategoryMap(results: List<ResourceResult>, httpVersion: String): MultiMap<String, DataMap> {
    val status1x = results.filter { it.statusCode.isInformational }
    val status2x = results.filter { it.statusCode.isSuccessful }
    val status3x = results.filter { it.statusCode.isRedirection }
    val status4x = results.filter { it.statusCode.isClientError }
    val status5x = results.filter { it.statusCode.isServerError }

    val map: MMap<String, List<DataMap>> = emptyMMap()
    if (status1x.isNotEmpty()) map["informational"] = generateMultiStatusMap(status1x, httpVersion)
    if (status2x.isNotEmpty()) map["successful"] = generateMultiStatusMap(status2x, httpVersion)
    if (status3x.isNotEmpty()) map["redirection"] = generateMultiStatusMap(status3x, httpVersion)
    if (status4x.isNotEmpty()) map["clientError"] = generateMultiStatusMap(status4x, httpVersion)
    if (status5x.isNotEmpty()) map["serverError"] = generateMultiStatusMap(status5x, httpVersion)
    return map
}
internal fun generateMultiStatusXML(results: List<ResourceResult>, httpVersion: String) = buildString {
    fun Any.escapeXml() = if (this is String) replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
    else this

    append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n")
    append("<d:multistatus xmlns:d=\"DAV:\">\n")

    for (item in results) {
        append("  <d:response>\n")
        append("    <d:href>${item.reference.escapeXml()}</d:href>\n")

        append("    <d:propstat>\n")
        append("      <d:prop/>\n")

        append("      <d:status>HTTP/${httpVersion.substringAfter("HTTP/")} ${item.statusCode.value} ${item.statusCode.reasonPhrase}</d:status>\n")
        append("    </d:propstat>\n")

        if (item.description.isNotNull()) {
            append("    <d:responsedescription>${item.description.escapeXml()}</d:responsedescription>\n")
        }

        append("  </d:response>\n")
    }

    append("</d:multistatus>")
}

/**
 * Builds an HTTP response with the status code `IM_USED` and allows customization of various
 * response attributes such as headers, ETag, last modified timestamp, and body content.
 *
 * @param T The type of the response body.
 * @param includeFeatureCode Specifies whether to include the "Feature-Code" header in the response.
 *                           Defaults to true.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param newETag The ETag value to be included in the response, if provided.
 *                Defaults to null.
 * @param lastModifiedDate The timestamp to set as the "Last-Modified" header in the response,
 *                         if provided. Defaults to null.
 * @param expires an optional timestamp to include as an "Expires" header, defaults to null
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param refresh optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to, defaults to null
 * @param serverTiming A set of triples containing the "Server-Timing" header label, duration, and description.
 * @param headers Additional headers to include in the response, if provided. Defaults to null.
 * @param body A supplier for generating the response body, if needed. Defaults to null.
 * @return A `Response` instance with the configured attributes.
 * @since 3.0.0
 */
context(request: Request)
suspend inline fun <reified T : Any> IMUsedResponse(
    includeFeatureCode: Boolean = true,
    includeRequestId: Boolean = true,
    newETag: String? = null,
    lastModifiedDate: OffsetDateTime? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: List<String> = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    contentType: MediaType? = null,
    headers: HttpHeaders = HttpHeaders(),
    noinline body: Supplier<T>? = null
): Response {
    val re = Response.status(HttpStatus.IM_USED.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    if (includeFeatureCode) re.featureCode()
    if (expires.isNotNull()) re.expires(expires)
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    val result = body?.invoke()
    if (newETag.isNotNullOrEmpty()) re.eTag(newETag)
    if (lastModifiedDate.isNotNull()) re.lastModified(lastModifiedDate.toInstant())
    if (result.isNotNull()) return re.negotiateBodyValueWithTypeAndAwait(contentType?.toSpringMediaType()?.asSingleList() ?: request.headers().accept(), result)
    return re.buildAndAwait()
}
/**
 * Creates an HTTP response with the status `IM_USED` (226) and optionally includes additional headers, body content,
 * and metadata such as ETag and last modified date.
 *
 * @param featureCode The feature code to be included as the "Feature-Code" header in the response.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param newETag An optional ETag value to include in the response.
 * @param lastModifiedDate An optional last modified date to include in the response.
 * @param expires an optional timestamp to include as an "Expires" header, defaults to null
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param refresh optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to, defaults to null
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Optional additional HTTP headers to be included in the response.
 * @param body An optional supplier for generating the body of the response.
 * @return A `Response` instance representing the constructed HTTP response.
 * @since 3.0.0
 */
context(request: Request)
suspend inline fun <reified T : Any> IMUsedResponse(
    featureCode: String,
    includeRequestId: Boolean = true,
    newETag: String? = null,
    lastModifiedDate: OffsetDateTime? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: List<String> = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    contentType: MediaType? = null,
    headers: HttpHeaders = HttpHeaders(),
    noinline body: Supplier<T>? = null
): Response {
    val re = Response.status(HttpStatus.IM_USED.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    re.featureCode(featureCode)
    if (expires.isNotNull()) re.expires(expires)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    val result = body?.invoke()
    if (newETag.isNotNullOrEmpty()) re.eTag(newETag)
    if (lastModifiedDate.isNotNull()) re.lastModified(lastModifiedDate.toInstant())
    if (result.isNotNull()) return re.negotiateBodyValueWithTypeAndAwait(contentType?.toSpringMediaType()?.asSingleList() ?: request.headers().accept(), result)
    return re.buildAndAwait()
}

/**
 * Creates a `Response` object with an HTTP `303 See Other` status and a specified location.
 *
 * Optionally, this method can include a "Feature-Code" header if `includeFeatureCode` is set to `true`.
 * Additional headers can be added using the `headers` parameter, and an optional custom action can
 * be executed during the response building process.
 *
 * @param includeFeatureCode When `true`, adds a "Feature-Code" header to the response if applicable. Defaults to `true`.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param headers Optional HTTP headers to be included in the response (overrides any other header parameters of this method).
 * @param location The URI to which the user agent should be redirected.
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param action An optional action to be executed during the response building process.
 * @return A `Response` instance with the specified HTTP status, headers, and location.
 * @since 3.0.0
 */
suspend fun SeeOtherResponse(
    includeFeatureCode: Boolean = true,
    includeRequestId: Boolean = true,
    preferenceApplied: List<String> = emptyList(),
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: Duration? = null,
    headers: HttpHeaders = HttpHeaders(),
    location: Uri,
    action: Action? = null
): Response {
    val re = Response.status(HttpStatus.SEE_OTHER.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    re.location(location)
    if (includeFeatureCode) re.featureCode()
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (retryAfter.isNotNull()) re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.buildAndAwait()
}
/**
 * Creates a "See Other" HTTP response with a feature code, optional headers, and a specified location URI.
 *
 * @param featureCode the feature code to be added as a custom header to the response
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers optional HTTP headers to be included in the response (overrides any other header parameters of this method)
 * @param location the URI to which the client should be redirected
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param action an optional lambda that defines additional actions or settings for the response
 * @return a pre-built response entity with the "See Other" HTTP status
 * @since 3.0.0
 */
suspend fun SeeOtherResponse(
    featureCode: String,
    includeRequestId: Boolean = true,
    preferenceApplied: List<String> = emptyList(),
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: Duration? = null,
    headers: HttpHeaders = HttpHeaders(),
    location: Uri,
    action: Action? = null
): Response {
    val re = Response.status(HttpStatus.SEE_OTHER.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    re.featureCode(featureCode).location(location)
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (retryAfter.isNotNull()) re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.buildAndAwait()
}

/**
 * Creates a `Response` object with an HTTP `303 See Other` status and a specified location.
 *
 * Optionally, this method can include a "Feature-Code" header if `includeFeatureCode` is set to `true`.
 * Additional headers can be added using the `headers` parameter, and an optional custom action can
 * be executed during the response building process.
 *
 * @param includeFeatureCode When `true`, adds a "Feature-Code" header to the response if applicable. Defaults to `true`.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param headers Optional HTTP headers to be included in the response (overrides any other header parameters of this method).
 * @param location The URI to which the user agent should be redirected.
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param action An optional action to be executed during the response building process.
 * @return A `Response` instance with the specified HTTP status, headers, and location.
 * @since 3.0.0
 */
suspend fun SeeOtherResponse(
    includeFeatureCode: Boolean = true,
    includeRequestId: Boolean = true,
    preferenceApplied: List<String> = emptyList(),
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: TemporalAccessor,
    headers: HttpHeaders = HttpHeaders(),
    location: Uri,
    action: Action? = null
): Response {
    val re = Response.status(HttpStatus.SEE_OTHER.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    re.location(location)
    if (includeFeatureCode) re.featureCode()
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.buildAndAwait()
}
/**
 * Creates a "See Other" HTTP response with a feature code, optional headers, and a specified location URI.
 *
 * @param featureCode the feature code to be added as a custom header to the response
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers optional HTTP headers to be included in the response (overrides any other header parameters of this method)
 * @param location the URI to which the client should be redirected
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param action an optional lambda that defines additional actions or settings for the response
 * @return a pre-built response entity with the "See Other" HTTP status
 * @since 3.0.0
 */
suspend fun SeeOtherResponse(
    featureCode: String,
    includeRequestId: Boolean = true,
    preferenceApplied: List<String> = emptyList(),
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: TemporalAccessor,
    headers: HttpHeaders = HttpHeaders(),
    location: Uri,
    action: Action? = null
): Response {
    val re = Response.status(HttpStatus.SEE_OTHER.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    re.featureCode(featureCode).location(location)
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.buildAndAwait()
}

/**
 * Constructs a response with a `302 Found` HTTP status, a specified location, and optional headers and actions.
 * Additionally, it can add a "Feature-Code" header based on the calling context, if enabled.
 *
 * @param includeFeatureCode A flag indicating whether to include the "Feature-Code" header in the response. Defaults to `true`.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers Optional headers to include in the response (overrides any other header parameters of this method). Pass `null` or an empty `HttpHeaders` instance for no additional headers.
 * @param location The target URI to which the client is redirected.
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param action An optional lambda function to perform additional customization on the response.
 * @return A `Response` instance with the specified properties and configurations.
 * @since 3.0.0
 */
suspend fun FoundResponse(
    includeFeatureCode: Boolean = true,
    includeRequestId: Boolean = true,
    preferenceApplied: List<String> = emptyList(),
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: Duration? = null,
    headers: HttpHeaders = HttpHeaders(),
    location: Uri,
    action: Action? = null
): Response {
    val re = Response.status(HttpStatus.FOUND.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    if (includeFeatureCode) re.featureCode()
    re.location(location)
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (retryAfter.isNotNull()) re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.buildAndAwait()
}
/**
 * Builds and returns an HTTP 302 Found response with optional headers and an optional action.
 *
 * @param featureCode the value for the "Feature-Code" header in the response
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers optional HTTP headers to include in the response (overrides any other header parameters of this method), can be null
 * @param location the URI to be set in the "Location" header for the response
 * @param action an optional action to execute before building the response, can be null
 * @return a `Response` object with HTTP status 302 (Found) and the provided details
 * @since 3.0.0
 */
suspend fun FoundResponse(
    featureCode: String,
    includeRequestId: Boolean = true,
    preferenceApplied: List<String> = emptyList(),
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: Duration? = null,
    headers: HttpHeaders = HttpHeaders(),
    location: Uri,
    action: Action? = null
): Response {
    val re = Response.status(HttpStatus.FOUND.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    re.featureCode(featureCode).location(location)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (retryAfter.isNotNull()) re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.buildAndAwait()
}

/**
 * Constructs a response with a `302 Found` HTTP status, a specified location, and optional headers and actions.
 * Additionally, it can add a "Feature-Code" header based on the calling context, if enabled.
 *
 * @param includeFeatureCode A flag indicating whether to include the "Feature-Code" header in the response. Defaults to `true`.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers Optional headers to include in the response (overrides any other header parameters of this method). Pass `null` or an empty `HttpHeaders` instance for no additional headers.
 * @param location The target URI to which the client is redirected.
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param action An optional lambda function to perform additional customization on the response.
 * @return A `Response` instance with the specified properties and configurations.
 * @since 3.0.0
 */
suspend fun FoundResponse(
    includeFeatureCode: Boolean = true,
    includeRequestId: Boolean = true,
    preferenceApplied: List<String> = emptyList(),
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: TemporalAccessor,
    headers: HttpHeaders = HttpHeaders(),
    location: Uri,
    action: Action? = null
): Response {
    val re = Response.status(HttpStatus.FOUND.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    if (includeFeatureCode) re.featureCode()
    re.location(location)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.buildAndAwait()
}
/**
 * Builds and returns an HTTP 302 Found response with optional headers and an optional action.
 *
 * @param featureCode the value for the "Feature-Code" header in the response
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers optional HTTP headers to include in the response (overrides any other header parameters of this method), can be null
 * @param location the URI to be set in the "Location" header for the response
 * @param action an optional action to execute before building the response, can be null
 * @return a `Response` object with HTTP status 302 (Found) and the provided details
 * @since 3.0.0
 */
suspend fun FoundResponse(
    featureCode: String,
    includeRequestId: Boolean = true,
    preferenceApplied: List<String> = emptyList(),
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: TemporalAccessor,
    headers: HttpHeaders = HttpHeaders(),
    location: Uri,
    action: Action? = null
): Response {
    val re = Response.status(HttpStatus.FOUND.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    re.featureCode(featureCode).location(location)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.buildAndAwait()
}

/**
 * Creates an HTTP 301 Moved Permanently response.
 *
 * This method generates a response with the HTTP status code 301 (Moved Permanently)
 * and sets the `Location` header to the specified URI. Optionally, it can include
 * feature code metadata in the response headers and execute an additional action.
 *
 * @param includeFeatureCode A boolean indicating whether to include the feature code header in the response. Defaults to `true`.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers Optional HTTP headers to include in the response (overrides any other header parameters of this method). If `null` or empty, no additional headers are included.
 * @param location The target URI where the client should be redirected.
 * @param action An optional additional action to execute while building the response.
 * @return A constructed response with HTTP status 301 (Moved Permanently) and the specified parameters.
 * @since 3.0.0
 */
suspend fun MovedPermanentlyResponse(
    includeFeatureCode: Boolean = true,
    includeRequestId: Boolean = true,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: Duration? = null,
    headers: HttpHeaders = HttpHeaders(),
    location: Uri,
    action: Action? = null
): Response {
    val re = Response.status(HttpStatus.MOVED_PERMANENTLY.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    if (includeFeatureCode) re.featureCode()
    re.location(location)
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (retryAfter.isNotNull()) re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.buildAndAwait()
}
/**
 * Creates a response with HTTP status 301 (Moved Permanently).
 *
 * @param featureCode a string representing the feature code to be added as a custom header in the response.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param headers optional HTTP headers to be included in the response (overrides any other header parameters of this method).
 * @param location a URI indicating the new location of the requested resource.
 * @param action an optional action to be executed during the construction of the response.
 * @return a response object with HTTP status 301 (Moved Permanently) and the specified attributes.
 * @since 3.0.0
 */
suspend fun MovedPermanentlyResponse(
    featureCode: String,
    includeRequestId: Boolean = true,
    headers: HttpHeaders = HttpHeaders(),
    location: Uri,
    action: Action? = null
): Response {
    val re = Response.status(HttpStatus.MOVED_PERMANENTLY.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    re.featureCode(featureCode).location(location)
    if (action.isNotNull()) action()
    return re.buildAndAwait()
}
/**
 * Creates an HTTP 301 Moved Permanently response.
 *
 * This method generates a response with the HTTP status code 301 (Moved Permanently)
 * and sets the `Location` header to the specified URI. Optionally, it can include
 * feature code metadata in the response headers and execute an additional action.
 *
 * @param includeFeatureCode A boolean indicating whether to include the feature code header in the response. Defaults to `true`.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param serverTiming A set of triples containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers Optional HTTP headers to include in the response (overrides any other header parameters of this method). If `null` or empty, no additional headers are included.
 * @param location The target URI where the client should be redirected.
 * @param action An optional additional action to execute while building the response.
 * @return A constructed response with HTTP status 301 (Moved Permanently) and the specified parameters.
 * @since 3.0.0
 */
suspend fun MovedPermanentlyResponse(
    includeFeatureCode: Boolean = true,
    includeRequestId: Boolean = true,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: TemporalAccessor,
    headers: HttpHeaders = HttpHeaders(),
    location: Uri,
    action: Action? = null
): Response {
    val re = Response.status(HttpStatus.MOVED_PERMANENTLY.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    re.location(location)
    if (includeFeatureCode) re.featureCode()
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.buildAndAwait()
}
/**
 * Creates a response with HTTP status 301 (Moved Permanently).
 *
 * @param featureCode a string representing the feature code to be added as a custom header in the response.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param serverTiming A set of triples containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers optional HTTP headers to be included in the response (overrides any other header parameters of this method).
 * @param location a URI indicating the new location of the requested resource.
 * @param action an optional action to be executed during the construction of the response.
 * @return a response object with HTTP status 301 (Moved Permanently) and the specified attributes.
 * @since 3.0.0
 */
suspend fun MovedPermanentlyResponse(
    featureCode: String,
    includeRequestId: Boolean = true,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: TemporalAccessor,
    headers: HttpHeaders = HttpHeaders(),
    location: Uri,
    action: Action? = null
): Response {
    val re = Response.status(HttpStatus.MOVED_PERMANENTLY.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    re.featureCode(featureCode).location(location)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.buildAndAwait()
}

/**
 * Builds and returns an HTTP response with a `308 Permanent Redirect` status code.
 *
 * The response will include the specified `Location` header. Additional headers and
 * an optional action can be included as part of the response. If `includeFeatureCode`
 * is `true`, a `Feature-Code` header will be added to the response based on the
 * calling method's `Feature` annotation, if present.
 *
 * @param includeFeatureCode Indicates whether to include the `Feature-Code` header
 * in the response. Defaults to `true`.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers Optional custom headers to include in the response (overrides any other header parameters of this method). If `null` or empty,
 * no additional headers are added.
 * @param location The URI to set as the `Location` header of the response. Must not be `null`.
 * @param action An optional action to invoke for configuring the `Response` further.
 * If `null`, no action is performed.
 * @return The fully configured `Response` object with a `308 Permanent Redirect` status code.
 * @since 3.0.0
 */
suspend fun PermanentRedirectResponse(
    includeFeatureCode: Boolean = true,
    includeRequestId: Boolean = true,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: Duration? = null,
    headers: HttpHeaders = HttpHeaders(),
    location: Uri,
    action: Action? = null
): Response {
    val re = Response.status(HttpStatus.PERMANENT_REDIRECT.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    re.location(location)
    if (includeFeatureCode) re.featureCode()
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (retryAfter.isNotNull()) re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.buildAndAwait()
}
/**
 * Constructs a response with a 308 Permanent Redirect status, allowing for a new location to be provided.
 *
 * @param featureCode a string representing a feature code to add as a custom header in the response
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param serverTiming A set of triples containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers optional headers to be included in the response (overrides any other header parameters of this method)
 * @param location the URI to which the client is redirected
 * @param action an optional action to execute additional response configuration
 * @return a `Response` object with status 308 Permanent Redirect and the specified configurations
 * @since 3.0.0
 */
suspend fun PermanentRedirectResponse(
    featureCode: String,
    includeRequestId: Boolean = true,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: Duration? = null,
    headers: HttpHeaders = HttpHeaders(),
    location: Uri,
    action: Action? = null
): Response {
    val re = Response.status(HttpStatus.PERMANENT_REDIRECT.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    re.featureCode(featureCode).location(location)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (retryAfter.isNotNull()) re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.buildAndAwait()
}
/**
 * Builds and returns an HTTP response with a `308 Permanent Redirect` status code.
 *
 * The response will include the specified `Location` header. Additional headers and
 * an optional action can be included as part of the response. If `includeFeatureCode`
 * is `true`, a `Feature-Code` header will be added to the response based on the
 * calling method's `Feature` annotation, if present.
 *
 * @param includeFeatureCode Indicates whether to include the `Feature-Code` header
 * in the response. Defaults to `true`.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers Optional custom headers to include in the response (overrides any other header parameters of this method). If `null` or empty,
 * no additional headers are added.
 * @param location The URI to set as the `Location` header of the response. Must not be `null`.
 * @param action An optional action to invoke for configuring the `Response` further.
 * If `null`, no action is performed.
 * @return The fully configured `Response` object with a `308 Permanent Redirect` status code.
 * @since 3.0.0
 */
suspend fun PermanentRedirectResponse(
    includeFeatureCode: Boolean = true,
    includeRequestId: Boolean = true,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: TemporalAccessor,
    headers: HttpHeaders = HttpHeaders(),
    location: Uri,
    action: Action? = null
): Response {
    val re = Response.status(HttpStatus.PERMANENT_REDIRECT.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    re.location(location)
    if (includeFeatureCode) re.featureCode()
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.buildAndAwait()
}
/**
 * Constructs a response with a 308 Permanent Redirect status, allowing for a new location to be provided.
 *
 * @param featureCode a string representing a feature code to add as a custom header in the response
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param serverTiming A set of triples containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers optional headers to be included in the response (overrides any other header parameters of this method)
 * @param location the URI to which the client is redirected
 * @param action an optional action to execute additional response configuration
 * @return a `Response` object with status 308 Permanent Redirect and the specified configurations
 * @since 3.0.0
 */
suspend fun PermanentRedirectResponse(
    featureCode: String,
    includeRequestId: Boolean = true,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: TemporalAccessor,
    headers: HttpHeaders = HttpHeaders(),
    location: Uri,
    action: Action? = null
): Response {
    val re = Response.status(HttpStatus.PERMANENT_REDIRECT.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    re.featureCode(featureCode).location(location)
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.buildAndAwait()
}

/**
 * Creates a response with a `302 Temporary Redirect` status. Allows customization of headers,
 * feature codes, redirection location, and an additional action to modify the response.
 *
 * @param includeFeatureCode Whether to include a feature code in the response headers. Default is `true`.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param serverTiming A set of triples containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers Optional headers to include in the response (overrides any other header parameters of this method). Default is `null`.
 * @param location The URI to which the client is redirected.
 * @param action An optional lambda for additional customization of the response before it is built. Default is `null`.
 * @return A `Response` object with the status set to `302 Temporary Redirect` and the specified attributes.
 * @since 3.0.0
 */
suspend fun TemporaryRedirectResponse(
    includeFeatureCode: Boolean = true,
    includeRequestId: Boolean = true,
    preferenceApplied: List<String> = emptyList(),
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: Duration? = null,
    headers: HttpHeaders = HttpHeaders(),
    location: Uri,
    action: Action? = null
): Response {
    val re = Response.status(HttpStatus.TEMPORARY_REDIRECT.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    if (includeFeatureCode) re.featureCode()
    re.location(location)
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (retryAfter.isNotNull()) re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.buildAndAwait()
}
/**
 * Constructs a response with HTTP status code "307 Temporary Redirect."
 *
 * @param featureCode the feature code to be included as a custom header in the response
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param serverTiming A set of triples containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers optional HTTP headers to include in the response (overrides any other header parameters of this method); if provided, they are added to the response
 * @param location the URI to which the client is redirected
 * @param action an optional action to execute before building the response
 * @return a constructed response with the specified parameters and a "307 Temporary Redirect" status
 * @since 3.0.0
 */
suspend fun TemporaryRedirectResponse(
    featureCode: String,
    includeRequestId: Boolean = true,
    preferenceApplied: List<String> = emptyList(),
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: Duration? = null,
    headers: HttpHeaders = HttpHeaders(),
    location: Uri,
    action: Action? = null
): Response {
    val re = Response.status(HttpStatus.TEMPORARY_REDIRECT.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    re.featureCode(featureCode).location(location)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (retryAfter.isNotNull()) re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.buildAndAwait()
}
/**
 * Creates a response with a `302 Temporary Redirect` status. Allows customization of headers,
 * feature codes, redirection location, and an additional action to modify the response.
 *
 * @param includeFeatureCode Whether to include a feature code in the response headers. Default is `true`.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param serverTiming A set of triples containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers Optional headers to include in the response (overrides any other header parameters of this method). Default is `null`.
 * @param location The URI to which the client is redirected.
 * @param action An optional lambda for additional customization of the response before it is built. Default is `null`.
 * @return A `Response` object with the status set to `302 Temporary Redirect` and the specified attributes.
 * @since 3.0.0
 */
suspend fun TemporaryRedirectResponse(
    includeFeatureCode: Boolean = true,
    includeRequestId: Boolean = true,
    preferenceApplied: List<String> = emptyList(),
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: TemporalAccessor,
    headers: HttpHeaders = HttpHeaders(),
    location: Uri,
    action: Action? = null
): Response {
    val re = Response.status(HttpStatus.TEMPORARY_REDIRECT.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    if (includeFeatureCode) re.featureCode()
    re.location(location)
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.buildAndAwait()
}
/**
 * Constructs a response with HTTP status code "307 Temporary Redirect."
 *
 * @param featureCode the feature code to be included as a custom header in the response
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param serverTiming A set of triples containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers optional HTTP headers to include in the response (overrides any other header parameters of this method); if provided, they are added to the response
 * @param location the URI to which the client is redirected
 * @param action an optional action to execute before building the response
 * @return a constructed response with the specified parameters and a "307 Temporary Redirect" status
 * @since 3.0.0
 */
suspend fun TemporaryRedirectResponse(
    featureCode: String,
    includeRequestId: Boolean = true,
    preferenceApplied: List<String> = emptyList(),
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: TemporalAccessor,
    headers: HttpHeaders = HttpHeaders(),
    location: Uri,
    action: Action? = null
): Response {
    val re = Response.status(HttpStatus.TEMPORARY_REDIRECT.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    re.featureCode(featureCode).location(location)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.buildAndAwait()
}

/**
 * Constructs a 304 Not Modified HTTP response.
 *
 * This method customizes the response by adding optional headers, an ETag, and feature code metadata.
 * It uses the `HttpStatus.NOT_MODIFIED` status to indicate that the resource has not changed since last requested.
 *
 * @param includeFeatureCode Specifies whether to include the "Feature-Code" header in the response. Defaults to `true`.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param eTag An optional ETag value to include in the response for resource versioning. Defaults to `null`.
 * @param expires An optional expiration date for the response. Defaults to `null`.
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param serverTiming A set of triples containing the "Server-Timing" header label, duration, and description.
 * @param headers Optional additional HTTP headers to include in the response (overrides any other header parameters of this method). Defaults to `null`.
 * @param action An optional action to perform before finalizing the response. Defaults to `null`.
 * @return A `Response` instance representing the 304 Not Modified HTTP response.
 * @since 3.0.0
 */
suspend fun NotModifiedResponse(
    includeFeatureCode: Boolean = true,
    includeRequestId: Boolean = true,
    eTag: String? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: List<String> = emptyList(),
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders = HttpHeaders(),
    action: Action? = null
): Response {
    val re = Response.status(HttpStatus.NOT_MODIFIED.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    if (eTag.isNotNull()) re.eTag(eTag)
    if (expires.isNotNull()) re.expires(expires)
    if (includeFeatureCode) re.featureCode()
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (action.isNotNull()) action()
    return re.buildAndAwait()
}
/**
 * Constructs a 304 Not Modified HTTP response.
 *
 * This method customizes the response by adding optional headers, an ETag, and feature code metadata.
 * It uses the `HttpStatus.NOT_MODIFIED` status to indicate that the resource has not changed since last requested.
 *
 * @param featureCode Specifies the "Feature-Code" header in the response.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param eTag An optional ETag value to include in the response for resource versioning. Defaults to `null`.
 * @param expires An optional expiration date for the response. Defaults to `null`.
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param serverTiming A set of triples containing the "Server-Timing" header label, duration, and description.
 * @param headers Optional additional HTTP headers to include in the response (overrides any other header parameters of this method). Defaults to `null`.
 * @param action An optional action to perform before finalizing the response. Defaults to `null`.
 * @return A `Response` instance representing the 304 Not Modified HTTP response.
 * @since 3.0.0
 */
suspend fun NotModifiedResponse(
    featureCode: String,
    includeRequestId: Boolean = true,
    eTag: String? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: List<String> = emptyList(),
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders = HttpHeaders(),
    action: Action? = null
): Response {
    val re = Response.status(HttpStatus.NOT_MODIFIED.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    if (eTag.isNotNull()) re.eTag(eTag)
    if (expires.isNotNull()) re.expires(expires)
    re.featureCode(featureCode)
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (action.isNotNull()) action()
    return re.buildAndAwait()
}

/**
 * Constructs a `Response` object with an HTTP 204 (No Content) status code.
 *
 * This function optionally includes additional metadata such as an `ETag` header, custom headers,
 * and a feature code header derived from the `Feature` annotation of the calling method if applicable.
 * It can also perform a specified action to further customize the response.
 *
 * Deprecated in favor of the [EmptyResponse] function.
 *
 * @param includeFeatureCode A boolean flag indicating whether to include the "Feature-Code" header
 *  based on the `Feature` annotation of the calling method. Defaults to `true`.
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param refresh optional pair of duration and URL for refresh header, defaults to null
 * @param serverTiming A set of triples containing the "Server-Timing" header label, duration, and description.
 * @param headers Optional custom HTTP headers to add to the response (overrides any other header parameters of this method). Can be `null` or empty.
 * @param action An optional action to further customize the response object. Can be `null`.
 * @return A `Response` object with an HTTP 204 (No Content) status code and any specified metadata.
 * @since 3.0.0
 */
@Deprecated("Use EmptyResponse instead")
suspend fun NoContentResponse(
    includeFeatureCode: Boolean = true,
    includeRequestId: Boolean = true,
    preferenceApplied: List<String> = emptyList(),
    refresh: Pair<Duration, Url?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders = HttpHeaders(),
    action: Action? = null
): Response {
    val re = Response.status(HttpStatus.NO_CONTENT.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (includeFeatureCode) re.featureCode()
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (action.isNotNull()) action()
    return re.buildAndAwait()
}
/**
 * Builds an HTTP 204 No Content response with optional headers and eTag.
 *
 * Deprecated in favor of the [EmptyResponse] function.
 *
 * @param featureCode the feature code to include as the "Feature-Code" header in the response
 * @param includeRequestId A flag to determine whether to include the "Request-Id" header in the response. Defaults to true.
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param refresh optional pair of duration and URL for refresh header, defaults to null
 * @param serverTiming A set of triples containing the "Server-Timing" header label, duration, and description.
 * @param headers optional custom headers to include in the response (overrides any other header parameters of this method)
 * @param action an optional action to further modify the response
 * @return a response with an HTTP 204 No Content status
 * @since 3.0.0
 */
@Deprecated("Use EmptyResponse instead")
suspend fun NoContentResponse(
    featureCode: String,
    includeRequestId: Boolean = true,
    preferenceApplied: List<String> = emptyList(),
    refresh: Pair<Duration, Url?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders = HttpHeaders(),
    action: Action? = null
): Response {
    val re = Response.status(HttpStatus.NO_CONTENT.toSpringHttpStatus())
    if (headers.isNotEmpty()) re.headers { it.addAll(headers.toSpringHttpHeaders()) }
    re.featureCode(featureCode)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (includeRequestId) re.header(HttpHeader.REQUEST_ID, requestId().toString())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (action.isNotNull()) action()
    return re.buildAndAwait()
}

/**
 * Adds the "Request-Id" header to the response if a request ID is present in the current logging context.
 *
 * This method retrieves the current request ID from the `LoggingAspect` and appends it as a 
 * header to the response if a non-null value is present. The header key is "Request-Id".
 * If no request ID is available, it returns the builder without modifying the headers.
 *
 * @receiver The response body builder to which the header might be added.
 * @return The modified response body builder with the optional "Request-Id" header.
 * @since 3.0.0
 */
suspend fun ServerResponse.BodyBuilder.requestId() = header("Request-Id", dev.tommasop1804.springutils.reactive.function.request.requestId().toString())
/**
 * Adds a "Request-Id" header to the response with the provided identifier value.
 *
 * @param id The identifier to associate with the "Request-Id" header.
 * @since 3.0.0
 */
fun ServerResponse.BodyBuilder.requestId(id: RequestId) = header("Request-Id", id.toString())

/**
 * Adds a "Feature-Code" header with the provided code to the response.
 *
 * @param code the feature code to be added as the "Feature-Code" header in the response
 * @since 3.0.0
 */
fun ServerResponse.BodyBuilder.featureCode(code: String): ServerResponse.BodyBuilder = header("Feature-Code", code)
/**
 * Adds a header named "Feature-Code" to the response using the value of the `code` property
 * from the `Feature` annotation of the calling method, if present.
 *
 * If the calling method cannot be resolved or does not have a `Feature` annotation, the header
 * value will be an empty string.
 *
 * This method introspects the call stack to identify the method that invoked the annotated
 * functionality and extracts metadata from the `Feature` annotation.
 *
 * @receiver The `Response.BodyBuilder` instance on which this method is invoked.
 * @return The same instance of `Response.BodyBuilder` with the "Feature-Code" header added.
 * @since 3.0.0
 */
fun ServerResponse.BodyBuilder.featureCode(): ServerResponse.BodyBuilder = header("Feature-Code",
    findCallerMethod()?.getAnnotation(Feature::class.java)?.code ?: String.EMPTY
)

/**
 * Sets the `Content-Type` header of the HTTP response to the specified media type.
 *
 * This method converts the provided custom `MediaType` instance to its corresponding
 * Spring `MediaType` equivalent before setting it as the content type of the response.
 *
 * @param mediaType The custom media type to be set as the content type of the response.
 *                  Must be an instance of `MediaType` from the `kutils.classes.web` package.
 * @since 3.6.1
 */
fun ServerResponse.BodyBuilder.contentType(mediaType: dev.tommasop1804.kutils.classes.web.MediaType) = contentType(mediaType.toSpringMediaType())
/**
 * Sets the `Content-Type` header of the HTTP response to the specified media type.
 *
 * Converts the provided custom `MimeType` instance to its corresponding
 * Spring Framework `MediaType` equivalent before setting it as the response's content type.
 *
 * @param mediaType The custom MIME type to be set as the content type of the response.
 * @since 3.6.1
 */
fun ServerResponse.BodyBuilder.contentType(mediaType: MimeType) = contentType(mediaType.toMediaType())

/**
 * Adds an `Expires` header to the HTTP response with the specified expiration time.
 *
 * @param time The expiration time to be set in the `Expires` header. It must be a valid `TemporalAccessor` instance.
 * @return The updated `Response.BodyBuilder` with the `Expires` header added.
 * @since 3.0.0
 */
fun ServerResponse.BodyBuilder.expires(time: TemporalAccessor): ServerResponse.BodyBuilder =
    header("Expires", time.toHeaderDate())

/**
 * Adds a "Preference-Applied" header to the response entity builder with the specified preferences.
 *
 * @param preferences A vararg of preference names to include in the "Preference-Applied" header.
 * @return The updated response entity builder with the added "Preference-Applied" header.
 * @since 3.0.0
 */
fun ServerResponse.BodyBuilder.preferenceApplied(vararg preferences: String): ServerResponse.BodyBuilder =
    header("Preference-Applied", preferences.joinToString(", "))

/**
 * Adds a "Refresh" header to the response, indicating a periodic refresh or redirect to a specified URL.
 *
 * @param time The duration after which the client should refresh or perform the redirect.
 * @param url The optional URL to redirect the client to after the refresh.
 * @return The updated Response.BodyBuilder with the "Refresh" header set.
 * @since 3.0.0
 */
@OptIn(RiskyApproximationOfTemporal::class)
fun ServerResponse.BodyBuilder.refresh(time: Duration, url: Url? = null): ServerResponse.BodyBuilder =
    header("Refresh", "${time.toSeconds()}${url?.let { "; url=$it" } ?: String.EMPTY}")
/**
 * Adds a "Refresh" header to the response, indicating a periodic refresh or redirect to a specified URL.
 *
 * @param timeAndURL A pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to.
 * @return The updated Response.BodyBuilder with the "Refresh" header set.
 * @since 3.0.0
 */
@OptIn(RiskyApproximationOfTemporal::class)
fun ServerResponse.BodyBuilder.refresh(timeAndURL: Pair<Duration, Url?>): ServerResponse.BodyBuilder =
    header("Refresh", "${timeAndURL.first.toSeconds()}${timeAndURL.second?.let { "; url=$it" } ?: String.EMPTY}")
/**
 * Adds a `Refresh` header to the HTTP response, which specifies the interval 
 * after which the client should automatically refresh or redirect to a given URL.
 *
 * @param seconds The number of seconds to wait before the client refreshes. Must be a non-negative integer.
 * @param url The optional URL to which the client will be redirected after the specified interval. 
 * If null, the client will refresh the current URL.
 * @return The current instance of [ServerResponse.BodyBuilder] with the `Refresh` header added.
 * @since 3.0.0
 */
fun ServerResponse.BodyBuilder.refresh(seconds: Int, url: Url? = null): ServerResponse.BodyBuilder =
    header("Refresh", "${seconds}${url?.let { "; url=$it" } ?: String.EMPTY}")

/**
 * Adds a `Retry-After` header to the response with the specified duration in seconds.
 *
 * @param duration The duration to be included in the `Retry-After` header.
 * @return The modified {@link Response.BodyBuilder} with the `Retry-After` header set.
 * @since 3.0.0
 */
@OptIn(RiskyApproximationOfTemporal::class)
fun ServerResponse.BodyBuilder.retryAfter(duration: Duration): ServerResponse.BodyBuilder =
    header("Retry-After", duration.toSeconds().toString())
/**
 * Adds a "Retry-After" header to the HTTP response with the specified delay in seconds.
 *
 * @param seconds The number of seconds to indicate in the "Retry-After" header.
 * @return The updated Response.BodyBuilder with the "Retry-After" header applied.
 * @since 3.0.0
 */
fun ServerResponse.BodyBuilder.retryAfter(seconds: Int): ServerResponse.BodyBuilder =
    header("Retry-After", seconds.toString())
/**
 * Adds a "Retry-After" header to the current HTTP response, using the provided temporal value
 * to determine the delay or time after which the client is allowed to retry the request.
 *
 * @param temporal The temporal accessor representing the time or delay for the "Retry-After" header.
 *                 This value is expected to be formatted in the RFC 1123 date-time format.
 * @return The instance of the response builder with the "Retry-After" header included.
 * @since 3.0.0
 */
fun ServerResponse.BodyBuilder.retryAfter(temporal: TemporalAccessor): ServerResponse.BodyBuilder =
    header("Retry-After", temporal.toHeaderDate())

/**
 * Adds a `Server-Timing` header to the response with the provided timing metrics.
 *
 * Each timing metric is represented as a pair of a name and a duration. The name is used as the metric identifier,
 * and the duration is converted to milliseconds to indicate the elapsed time for the corresponding metric.
 *
 * @param timingMetric Vararg of pairs where each pair consists of a metric name (String) and its duration (Duration).
 *                     The name identifies the metric, and the duration represents the time taken by the metric in milliseconds.
 * @return The modified [ServerResponse.BodyBuilder] instance with the `Server-Timing` header added.
 * @since 3.0.0
 */
@OptIn(RiskyApproximationOfTemporal::class)
fun ServerResponse.BodyBuilder.serverTiming(vararg timingMetric: Pair<String, Duration>): ServerResponse.BodyBuilder =
    header("Server-Timing", timingMetric.joinToString(", ") { "${it.first};dur=${it.second.toMillis()}" })
/**
 * Adds a `Server-Timing` header to the response containing the provided timing metrics.
 * The `Server-Timing` header is used to communicate performance metrics from the server
 * to the client, aiding in performance monitoring and diagnostics.
 *
 * @param timingMetric Vararg of pairs, where each pair contains:
 * - `first`: The metric name (e.g., "db", "cpu").
 * - `second`: The duration of the metric as a number (interpreted as milliseconds).
 * @return The updated `Response.BodyBuilder` instance with the added `Server-Timing` header.
 * @since 3.0.0
 */
@JvmName("serverTimingNumberDuration")
fun ServerResponse.BodyBuilder.serverTiming(vararg timingMetric: Pair<String, Number>): ServerResponse.BodyBuilder =
    header("Server-Timing", timingMetric.joinToString(", ") { "${it.first};dur=${it.second}" })
/**
 * Adds a "Server-Timing" header to the response, providing performance metrics
 * about server-side execution for debugging or monitoring purposes.
 *
 * @param timingMetric A vararg of `Triple` elements where:
 *   - The first element represents the metric name (e.g., "db-query").
 *   - The second element is a `Duration` object indicating the elapsed time for the metric.
 *   - The third element provides an optional description of the metric.
 * @return The updated `Response.BodyBuilder` with the "Server-Timing" header included.
 * @since 3.0.0
 */
@OptIn(RiskyApproximationOfTemporal::class)
fun ServerResponse.BodyBuilder.serverTiming(vararg timingMetric: Triple<String, Duration, String?>): ServerResponse.BodyBuilder =
    header("Server-Timing", timingMetric.joinToString(", ") { "${it.first};dur=${it.second.toMillis()}" + if (it.third.isNotNull()) ";desc=${it.third}" else "" })
/**
 * Adds a `Server-Timing` header to the response with the provided timing metrics.
 *
 * The `Server-Timing` header provides client-side tools with performance metrics, including
 * descriptions and durations of server-side processes.
 *
 * @param timingMetric A vararg of Triple values where:
 * - The first element is the metric name.
 * - The second element is the duration value as a number (e.g., milliseconds).
 * - The third element is the description of the metric.
 * @return The modified instance of [ServerResponse.BodyBuilder] with the `Server-Timing` header added.
 * @since 3.0.0
 */
@JvmName("serverTimingNumberDuration")
fun ServerResponse.BodyBuilder.serverTiming(vararg timingMetric: Triple<String, Number, String?>): ServerResponse.BodyBuilder =
    header("Server-Timing", timingMetric.joinToString(", ") { "${it.first};dur=${it.second}" + if (it.third.isNotNull()) ";desc=${it.third}" else "" })

/**
 * Adds a custom HTTP header to the response.
 *
 * @param header The custom HTTP header containing the name and values to be added.
 * @since 3.0.0
 */
fun ServerResponse.BodyBuilder.header(header: HttpHeader) = header(header.name, *header.values.toTypedArray())

/**
 * Retrieves the HTTP status associated with the response.
 *
 * Converts the internal status code of the response to an HTTP status representation.
 * If the status code cannot be resolved, it throws an `NoSuchEntryException`.
 *
 * @return The HTTP status corresponding to the response's status code.
 * @throws NoSuchEntryException if the status code does not correspond to a valid HTTP status.
 * @since 3.0.0
 */
val Response.status
    get() = statusCode().value().toHttpStatus() ?: throw NoSuchEntryException("Not a valid HTTP status code")