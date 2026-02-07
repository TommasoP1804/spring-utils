@file:JvmName("ResponseUtilsKt")
@file:Since("1.0.0")
@file:Suppress("unused", "FunctionName", "FunctionName", "UNCHECKED_CAST")

package dev.tommasop1804.springutils.response

import dev.tommasop1804.springutils.EmptyResponse
import dev.tommasop1804.springutils.Response
import dev.tommasop1804.springutils.annotations.Feature
import dev.tommasop1804.springutils.eTag
import dev.tommasop1804.springutils.exception.PreconditionFailedException
import dev.tommasop1804.springutils.exception.PreconditionRequiredException
import dev.tommasop1804.springutils.findCallerMethod
import dev.tommasop1804.springutils.toHeaderDate
import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.annotations.Since
import dev.tommasop1804.kutils.classes.time.Duration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.net.URI
import java.net.URL
import java.time.OffsetDateTime
import java.time.temporal.TemporalAccessor

/**
 * Evaluates conditional GET preconditions based on request headers and returns an appropriate response status
 * depending on whether the resource has been modified or not.
 *
 * @param eTagNoneMatch A list of ETag values to match against. If provided, the resource's ETag is compared
 *                      to these values to determine if it has been modified. Has higher priority than `ifModifiedSince`.
 * @param ifModifiedSince An instant in time representing the "If-Modified-Since" header from the request. This is
 *                        used to check if the resource has been updated after the specified time. If eTagNoneMatch is present,
 *                        this is not considered.
 * @param resourceETag The ETag of the resource. If provided, it is used in place of the resource's actual ETag.
 * @param lastModifiedDate The last modification date of the resource. It is used to evaluate the "If-Modified-Since" condition.
 * @param requireAtLeastOneValidator If true, ensures at least one of ETag or "If-Modified-Since" validators
 *                                   is present in the request. Throws an exception if neither is provided.
 * @param status The HTTP status to use when the resource is considered modified. Defaults to 200 (OK).
 * @param includeFeatureCode A flag to determine whether to include the "Feature-Code" header in the response. Defaults to true.
 * 
 * @param expires The expiration date for the response. If provided, it sets the "Expires" header.
 * @param preferenceApplied A list of preference-applied values to include in the response. If provided, it sets the "Preference-Applied" header.
 * @param refresh The refresh duration for the response. If provided, it sets the "Refresh" header.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Additional headers to include in the response, if provided. HAS LOWER PRIORITY THAN THE NEXT PARAMETES.
 * @param lazyExceptionIfNotPresent A supplier for the exception to throw if validation preconditions are not met.
 *                                   Defaults to an exception indicating that preconditions are required.
 * @param body A supplier for the body of the response. The resource body is only included in the response when the
 *             resource is considered modified.
 * @return A response entity containing the appropriate HTTP status, headers, and optional response body.
 * @since 1.0.0
 */
fun <T : Any> conditionalGet(
    eTagNoneMatch: StringList? = null,
    ifModifiedSince: OffsetDateTime? = null,
    resourceETag: String? = null,
    lastModifiedDate: OffsetDateTime? = null,
    requireAtLeastOneValidator: Boolean = false,
    status: HttpStatus = HttpStatus.OK,
    includeFeatureCode: Boolean = true,
    expires: TemporalAccessor? = null,
    preferenceApplied: StringList = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders? = null,
    lazyExceptionIfNotPresent: ThrowableSupplier = { PreconditionRequiredException("Use one of this or both (based on configuration): If-None-Match, If-Modified-Since") },
    body: Supplier<T>
): Response<T> {
    if (requireAtLeastOneValidator && eTagNoneMatch.isNull() && ifModifiedSince.isNull())
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


    val response = ResponseEntity.status(status)
    if (headers.isNotNull() && !headers.isEmpty) response.headers(headers)
    if (includeFeatureCode) response.featureCode()
    if (expires.isNotNull()) response.expires(expires)
    if (preferenceApplied.isNotEmpty()) response.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) response.refresh(refresh)
    if (serverTiming.isNotEmpty()) response.serverTiming(*serverTiming.toTypedArray())
    if (status == HttpStatus.NOT_MODIFIED) return response.build()
    return response.eTag(eTag).body(body ?: body())
}
/**
 * Processes conditional GET requests based on the provided HTTP headers and configuration,
 * and returns an appropriate response.
 *
 * @param eTagNoneMatch A list of ETag values to validate against. If the resource's ETag matches any of
 * these values, a 304 Not Modified response will be returned. Has higher priority than `ifModifiedSince`.
 * @param ifModifiedSince A timestamp to validate whether the resource has been modified after this date.
 * If the resource was not modified, a 304 Not Modified response will be returned. If `eTagNoneMatch` is present, this is not considered.
 * @param resourceETag The ETag of the resource. If provided, it is used in place of the resource's actual ETag.
 * @param lastModifiedDate The timestamp representing the last modification date of the resource.
 * Used in conjunction with `ifModifiedSince` for conditional validation.
 * @param requireAllValidators If true, all validators (ETag, If-Modified-Since) must pass
 * for the request to proceed.
 * @param requireAtLeastOneValidator If true, at least one validator (ETag, If-Modified-Since) must be
 * present in the request. If none are provided, an exception will be thrown.
 * @param status The default HTTP status to return if the request is not conditional or if validation passes.
 * Defaults to HTTP OK (200).
 * @param featureCode A string representing a feature code, which will be added as a "Feature-Code" header
 * in the response.
 * @param expires The expiration date for the response. If provided, it sets the "Expires" header.
 * @param preferenceApplied A list of preference-applied values to include in the response. If provided, it sets the "Preference-Applied" header.
 * @param refresh The refresh duration for the response. If provided, it sets the "Refresh" header.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Any additional HTTP headers to be included in the response.  HAS LOWER PRIORITY THAN THE NEXT PARAMETES.
 * @param lazyExceptionIfNotPresent A lazy supplier for an exception to throw when validators are required
 * but not provided. By default, a PreconditionRequiredException is thrown.
 * @param body A supplier function to provide the body of the response when validation succeeds.
 * @return A Response object with an appropriate HTTP status and body, depending on the result
 * of the validation.
 * @since 1.0.0
 */
fun <T : Any> conditionalGet(
    eTagNoneMatch: StringList? = null,
    ifModifiedSince: OffsetDateTime? = null,
    resourceETag: String? = null,
    lastModifiedDate: OffsetDateTime? = null,
    requireAllValidators: Boolean = false,
    requireAtLeastOneValidator: Boolean = false,
    status: HttpStatus = HttpStatus.OK,
    featureCode: String,
    expires: TemporalAccessor? = null,
    preferenceApplied: StringList = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders? = null,
    lazyExceptionIfNotPresent: ThrowableSupplier = { PreconditionRequiredException("Use one of this or both (based on configuration): If-None-Match, If-Modified-Since") },
    body: Supplier<T>
): Response<T> {
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


    val response = ResponseEntity.status(status)
    if (headers.isNotNull() && !headers.isEmpty) response.headers(headers)
    response.featureCode(featureCode)
    if (expires.isNotNull()) response.expires(expires)
    if (preferenceApplied.isNotEmpty()) response.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) response.refresh(refresh)
    if (serverTiming.isNotEmpty()) response.serverTiming(*serverTiming.toTypedArray())
    if (status == HttpStatus.NOT_MODIFIED) return response.build()
    return response.eTag(eTag).body(body ?: body())
}

/**
 * Attempts to conditionally update a resource based on ETag or If-Unmodified-Since headers.
 *
 * This method checks the provided conditional headers (e.g., ETag, If-Unmodified-Since)
 * against the current or previous state of the resource and only updates if the conditions are met.
 * If the conditions are not met, an exception is thrown, and the response status is not 2xx.
 *
 * @param eTagIfMatch A list of ETag values to be checked against the resource's current ETag. Has higher priority than `ifUnmodifiedSince`.
 * @param ifUnmodifiedSince A timestamp indicating that the resource will only be updated if it has not been modified since this time. If eTagIfMatch is present, this is not considered.
 * @param previousLastModifiedDate The last known modification timestamp of the resource.
 * @param requireAtLeastOneValidator When true, at least one conditional header must be present for the operation to proceed.
 * @param status A custom HTTP status to assign to the response when the update is successful.
 * @param lazyException A supplier that provides an exception to be thrown when validation fails.
 * @param lazyExceptionIfNotPresent A supplier that provides an exception to be thrown when required validators are missing.
 * @param includeFeatureCode If true, includes a "Feature-Code" header in the response based on the calling method's metadata.
 * @param newLastModifiedDate The updated last-modified timestamp to set in the response after a successful update.
 * @param expires The expiration date for the response. If provided, it sets the "Expires" header.
 * @param preferenceApplied A list of preference-applied values to include in the response. If provided, it sets the "Preference-Applied" header.
 * @param refresh The refresh duration for the response. If provided, it sets the "Refresh" header.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Additional HTTP headers to include in the response. HAS LOWER PRIORITY THAN THE NEXT PARAMETES.
 * @param previousValue A supplier callback used to retrieve the previous state of the resource, if available.
 * @param body A supplier callback used to compute or generate the new value for the resource.
 * @return A response containing the newly updated value or additional details as appropriate, with an appropriate HTTP status.
 * @throws PreconditionFailedException Thrown if the ETag or If-Unmodified-Since validation fails.
 * @throws PreconditionRequiredException Thrown if conditional update headers (e.g., If-Match, If-Unmodified-Since) are required but missing.
 * @since 1.0.0
 */
@JvmName("conditionalUpdateNewValue")
fun <T : Any, R : Any> conditionalUpdate(
    eTagIfMatch: StringList? = null,
    ifUnmodifiedSince: OffsetDateTime? = null,
    previousLastModifiedDate: OffsetDateTime? = null,
    requireAtLeastOneValidator: Boolean = false,
    status: HttpStatus? = null,
    lazyException: ThrowableSupplier = { PreconditionFailedException("ETag not matched or If-Unmodified-Since header failed.") },
    lazyExceptionIfNotPresent: ThrowableSupplier = { PreconditionRequiredException("Use: If-Match, If-Unmodified-Since") },
    includeFeatureCode: Boolean = true,
    newLastModifiedDate: OffsetDateTime? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: StringList = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders? = null,
    previousValue: Supplier<T?>?,
    body: Supplier<R>
): Response<R> {
    if (requireAtLeastOneValidator && eTagIfMatch.isNull() && ifUnmodifiedSince.isNull())
        throw lazyExceptionIfNotPresent()

    if (previousValue.isNotNull() && eTagIfMatch.isNotNull()) {
        val previousValue = previousValue()
        if (previousValue.isNotNull()) {
            val eTag = previousValue.eTag
            if (eTag !in eTagIfMatch mappedTo { it - Char.QUOTATION_MARK })
                throw lazyException()
        }
    } else if (previousLastModifiedDate.isNotNull() && ifUnmodifiedSince.isNotNull() && previousLastModifiedDate.isAfter(ifUnmodifiedSince))
        throw lazyException()

    val body = body()
    val status = status ?: (if (body is Unit) HttpStatus.NO_CONTENT else HttpStatus.OK)
    val response = ResponseEntity.status(status)
    if (headers.isNotNull() && !headers.isEmpty) response.headers(headers)
    if (includeFeatureCode) response.featureCode()
    if (newLastModifiedDate.isNotNull()) response.lastModified(newLastModifiedDate.toInstant())
    if (expires.isNotNull()) response.expires(expires)
    if (preferenceApplied.isNotEmpty()) response.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) response.refresh(refresh)
    if (serverTiming.isNotEmpty()) response.serverTiming(*serverTiming.toTypedArray())
    if (body !is Unit) response.eTag(body.eTag)
    return response.body(body)
}
/**
 * Attempts to conditionally update a resource based on ETag or If-Unmodified-Since headers.
 *
 * This method checks the provided conditional headers (e.g., ETag, If-Unmodified-Since)
 * against the current or previous state of the resource and only updates if the conditions are met.
 * If the conditions are not met, an exception is thrown, and the response status is not 2xx.
 *
 * @param eTagIfMatch A list of ETag values to be checked against the resource's current ETag. Has higher priority than `ifUnmodifiedSince`.
 * @param ifUnmodifiedSince A timestamp indicating that the resource will only be updated if it has not been modified since this time. If eTagIfMatch is present, this is not considered.
 * @param previousLastModifiedDate The last known modification timestamp of the resource.
 * @param requireAtLeastOneValidator When true, at least one conditional header must be present for the operation to proceed.
 * @param status A custom HTTP status to assign to the response when the update is successful.
 * @param lazyException A supplier that provides an exception to be thrown when validation fails.
 * @param lazyExceptionIfNotPresent A supplier that provides an exception to be thrown when required validators are missing.
 * @param includeFeatureCode If true, includes a "Feature-Code" header in the response based on the calling method's metadata.
 * @param newLastModifiedDate The updated last-modified timestamp to set in the response after a successful update.
 * @param expires The expiration date for the response. If provided, it sets the "Expires" header.
 * @param preferenceApplied A list of preference-applied values to include in the response. If provided, it sets the "Preference-Applied" header.
 * @param refresh The refresh duration for the response. If provided, it sets the "Refresh" header.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Additional HTTP headers to include in the response. HAS LOWER PRIORITY THAN THE NEXT PARAMETES.
 * @param previousETag The previous ETag of the resource.
 * @param body A supplier callback used to compute or generate the new value for the resource.
 * @return A response containing the newly updated value or additional details as appropriate, with an appropriate HTTP status.
 * @throws PreconditionFailedException Thrown if the ETag or If-Unmodified-Since validation fails.
 * @throws PreconditionRequiredException Thrown if conditional update headers (e.g., If-Match, If-Unmodified-Since) are required but missing.
 * @since 1.0.0
 */
@JvmName("conditionalUpdateNewValue")
fun <T : Any, R : Any> conditionalUpdate(
    eTagIfMatch: StringList? = null,
    ifUnmodifiedSince: OffsetDateTime? = null,
    previousLastModifiedDate: OffsetDateTime? = null,
    requireAtLeastOneValidator: Boolean = false,
    status: HttpStatus? = null,
    lazyException: ThrowableSupplier = { PreconditionFailedException("ETag not matched or If-Unmodified-Since header failed.") },
    lazyExceptionIfNotPresent: ThrowableSupplier = { PreconditionRequiredException("Use: If-Match, If-Unmodified-Since") },
    includeFeatureCode: Boolean = true,
    newLastModifiedDate: OffsetDateTime? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: StringList = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders? = null,
    previousETag: String?,
    body: Supplier<R>
): Response<R> {
    if (requireAtLeastOneValidator && eTagIfMatch.isNull() && ifUnmodifiedSince.isNull())
        throw lazyExceptionIfNotPresent()

    if (previousETag.isNotNull() && eTagIfMatch.isNotNull()) {
        if (previousETag - Char.QUOTATION_MARK !in eTagIfMatch mappedTo { it - Char.QUOTATION_MARK })
            throw lazyException()
    } else if (previousLastModifiedDate.isNotNull() && ifUnmodifiedSince.isNotNull() && previousLastModifiedDate.isAfter(ifUnmodifiedSince))
        throw lazyException()

    val body = body()
    val status = status ?: (if (body is Unit) HttpStatus.NO_CONTENT else HttpStatus.OK)
    val response = ResponseEntity.status(status)
    if (headers.isNotNull() && !headers.isEmpty) response.headers(headers)
    if (includeFeatureCode) response.featureCode()
    if (newLastModifiedDate.isNotNull()) response.lastModified(newLastModifiedDate.toInstant())
    if (expires.isNotNull()) response.expires(expires)
    if (preferenceApplied.isNotEmpty()) response.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) response.refresh(refresh)
    if (serverTiming.isNotEmpty()) response.serverTiming(*serverTiming.toTypedArray())
    if (body !is Unit) response.eTag(body.eTag)
    return response.body(body)
}
/**
 * Conditionally updates a resource based on specified validators such as ETag or last modified date.
 *
 * @param eTagIfMatch Optional list of ETags that the resource must match for the update to proceed. Has higher priority than `ifUnmodifiedSince`.
 * @param ifUnmodifiedSince Optional timestamp indicating that the update should proceed only if the resource
 *        has not been modified since this time. If eTagIfMatch is present, this is not considered.
 * @param previousLastModifiedDate The last known modification date of the resource, used to compare with
 *        the `ifUnmodifiedSince` parameter.
 * @param requireAtLeastOneValidator If true, at least one validator (e.g., `eTagIfMatch`, `ifUnmodifiedSince`)
 *        must be satisfied for the update to proceed.
 * @param status Optional HTTP status to return for a successful update; defaults to OK or NO_CONTENT based
 *        on the body content.
 * @param lazyException The exception supplier called when the preconditions are not satisfied.
 * @param lazyExceptionIfNotPresent The exception supplier called when required validators are missing.
 * @param featureCode Feature code added as a header to the response.
 * @param newLastModifiedDate Optional new last modified date to include in the response headers if specified.
 * @param expires The expiration date for the response. If provided, it sets the "Expires" header.
 * @param preferenceApplied A list of preference-applied values to include in the response. If provided, it sets the "Preference-Applied" header.
 * @param refresh The refresh duration for the response. If provided, it sets the "Refresh" header.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Optional additional HTTP headers to include in the response. HAS LOWER PRIORITY THAN THE NEXT PARAMETES.
 * @param previousValue Supplier for the previous value of the resource to validate against the
 *        `eTagIfMatch` parameter.
 * @param body Supplier for the new body of the resource, to be used in the response if conditions are met.
 * @return A Response object containing the updated body or an appropriate status, headers, and metadata.
 *         If the update is successful, the response will include relevant headers like `Last-Modified`
 *         or `ETag` as applicable.
 * @since 1.0.0
 */
@JvmName("conditionalUpdateNewValue")
fun <T : Any, R : Any> conditionalUpdate(
    eTagIfMatch: StringList? = null,
    ifUnmodifiedSince: OffsetDateTime? = null,
    previousLastModifiedDate: OffsetDateTime? = null,
    requireAtLeastOneValidator: Boolean = false,
    status: HttpStatus? = null,
    lazyException: ThrowableSupplier = { PreconditionFailedException("ETag not matched or If-Unmodified-Since header failed.") },
    lazyExceptionIfNotPresent: ThrowableSupplier = { PreconditionRequiredException("Use: If-Match, If-Unmodified-Since") },
    featureCode: String,
    newLastModifiedDate: OffsetDateTime? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: StringList = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders? = null,
    previousValue: Supplier<T?>?,
    body: Supplier<R>
): Response<R> {
    if (requireAtLeastOneValidator && eTagIfMatch.isNull() && ifUnmodifiedSince.isNull())
        throw lazyExceptionIfNotPresent()

    if (previousValue.isNotNull() && eTagIfMatch.isNotNull()) {
        val previousValue = previousValue()
        if (previousValue.isNotNull()) {
            val eTag = previousValue.eTag
            if (eTag !in eTagIfMatch mappedTo { it - Char.QUOTATION_MARK })
                throw lazyException()
        }
    } else if (previousLastModifiedDate.isNotNull() && ifUnmodifiedSince.isNotNull() && previousLastModifiedDate.isAfter(ifUnmodifiedSince))
        throw lazyException()

    val body = body()
    val status = status ?: (if (body is Unit) HttpStatus.NO_CONTENT else HttpStatus.OK)
    val response = ResponseEntity.status(status)
    if (headers.isNotNull() && !headers.isEmpty) response.headers(headers)
    response.featureCode(featureCode)
    if (newLastModifiedDate.isNotNull()) response.lastModified(newLastModifiedDate.toInstant())
    if (expires.isNotNull()) response.expires(expires)
    if (preferenceApplied.isNotEmpty()) response.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) response.refresh(refresh)
    if (serverTiming.isNotEmpty()) response.serverTiming(*serverTiming.toTypedArray())
    if (body !is Unit) response.eTag(body.eTag)
    return response.body(body)
}
/**
 * Conditionally updates a resource based on specified validators such as ETag or last modified date.
 *
 * @param eTagIfMatch Optional list of ETags that the resource must match for the update to proceed. Has higher priority than `ifUnmodifiedSince`.
 * @param ifUnmodifiedSince Optional timestamp indicating that the update should proceed only if the resource
 *        has not been modified since this time. If eTagIfMatch is present, this is not considered.
 * @param previousLastModifiedDate The last known modification date of the resource, used to compare with
 *        the `ifUnmodifiedSince` parameter.
 * @param requireAtLeastOneValidator If true, at least one validator (e.g., `eTagIfMatch`, `ifUnmodifiedSince`)
 *        must be satisfied for the update to proceed.
 * @param status Optional HTTP status to return for a successful update; defaults to OK or NO_CONTENT based
 *        on the body content.
 * @param lazyException The exception supplier called when the preconditions are not satisfied.
 * @param lazyExceptionIfNotPresent The exception supplier called when required validators are missing.
 * @param featureCode Feature code added as a header to the response.
 * @param newLastModifiedDate Optional new last modified date to include in the response headers if specified.
 * @param expires The expiration date for the response. If provided, it sets the "Expires" header.
 * @param preferenceApplied A list of preference-applied values to include in the response. If provided, it sets the "Preference-Applied" header.
 * @param refresh The refresh duration for the response. If provided, it sets the "Refresh" header.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Optional additional HTTP headers to include in the response. HAS LOWER PRIORITY THAN THE NEXT PARAMETES.
 * @param previousETag The previous ETag of the resource.
 * @param body Supplier for the new body of the resource, to be used in the response if conditions are met.
 * @return A Response object containing the updated body or an appropriate status, headers, and metadata.
 *         If the update is successful, the response will include relevant headers like `Last-Modified`
 *         or `ETag` as applicable.
 * @since 1.0.0
 */
@JvmName("conditionalUpdateNewValue")
fun <T : Any, R : Any> conditionalUpdate(
    eTagIfMatch: StringList? = null,
    ifUnmodifiedSince: OffsetDateTime? = null,
    previousLastModifiedDate: OffsetDateTime? = null,
    requireAtLeastOneValidator: Boolean = false,
    status: HttpStatus? = null,
    lazyException: ThrowableSupplier = { PreconditionFailedException("ETag not matched or If-Unmodified-Since header failed.") },
    lazyExceptionIfNotPresent: ThrowableSupplier = { PreconditionRequiredException("Use: If-Match, If-Unmodified-Since") },
    featureCode: String,
    newLastModifiedDate: OffsetDateTime? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: StringList = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders? = null,
    previousETag: String?,
    body: Supplier<R>
): Response<R> {
    if (requireAtLeastOneValidator && eTagIfMatch.isNull() && ifUnmodifiedSince.isNull())
        throw lazyExceptionIfNotPresent()

    if (previousETag.isNotNull() && eTagIfMatch.isNotNull()) {
        if (previousETag - Char.QUOTATION_MARK !in eTagIfMatch mappedTo { it - Char.QUOTATION_MARK })
            throw lazyException()
    } else if (previousLastModifiedDate.isNotNull() && ifUnmodifiedSince.isNotNull() && previousLastModifiedDate.isAfter(ifUnmodifiedSince))
        throw lazyException()

    val body = body()
    val status = status ?: (if (body is Unit) HttpStatus.NO_CONTENT else HttpStatus.OK)
    val response = ResponseEntity.status(status)
    if (headers.isNotNull() && !headers.isEmpty) response.headers(headers)
    response.featureCode(featureCode)
    if (newLastModifiedDate.isNotNull()) response.lastModified(newLastModifiedDate.toInstant())
    if (expires.isNotNull()) response.expires(expires)
    if (preferenceApplied.isNotEmpty()) response.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) response.refresh(refresh)
    if (serverTiming.isNotEmpty()) response.serverTiming(*serverTiming.toTypedArray())
    if (body !is Unit) response.eTag(body.eTag)
    return response.body(body)
}
/**
 * Executes a conditional update operation based on the provided parameters and validators such as ETag and
 * If-Unmodified-Since headers. It throws exceptions if the preconditions are not met.
 *
 * @param eTagIfMatch Optional list of ETag values to match against the existing resource's ETag. Has higher priority than `ifUnmodifiedSince`.
 * @param ifUnmodifiedSince Optional timestamp to ensure the resource has not been modified after this time. If eTagIfMatch is present, this is not considered.
 * @param previousLastModifiedDate Optional previous last modified date of the resource.
 * @param requireAtLeastOneValidator If true, at least one validator (ETag or If-Unmodified-Since) must be provided and pass.
 * @param status HTTP status to be used for a successful response. Defaults to HttpStatus.NO_CONTENT.
 * @param lazyException Supplier of the exception to be thrown when validators fail.
 * @param lazyExceptionIfNotPresent Supplier of the exception to be thrown when required validators are not present.
 * @param includeFeatureCode If true, includes a "Feature-Code" header in the response.
 * @param newLastModifiedDate Optional timestamp to set the new last modified date in the response.
 * @param preferenceApplied A list of preference-applied values to include in the response. If provided, it sets the "Preference-Applied" header.
 * @param refresh The refresh duration for the response. If provided, it sets the "Refresh" header.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Optional HTTP headers to include in the response. HAS LOWER PRIORITY THAN THE NEXT PARAMETES.
 * @param previousValue Supplier of the previous value of the resource, utilized for ETag validation.
 * @param action Action to execute as the conditional operation if validators pass.
 * @return An empty response object representing a successful operation.
 * @since 1.0.0
 */
@JvmName("conditionalUpdateAction")
fun <T : Any> conditionalUpdate(
    eTagIfMatch: StringList? = null,
    ifUnmodifiedSince: OffsetDateTime? = null,
    previousLastModifiedDate: OffsetDateTime? = null,
    requireAtLeastOneValidator: Boolean = false,
    status: HttpStatus = HttpStatus.NO_CONTENT,
    lazyException: ThrowableSupplier = { PreconditionFailedException("ETag not matched or If-Unmodified-Since header failed.") },
    lazyExceptionIfNotPresent: ThrowableSupplier = { PreconditionRequiredException("Use: If-Match, If-Unmodified-Since") },
    includeFeatureCode: Boolean = true,
    newLastModifiedDate: OffsetDateTime? = null,
    preferenceApplied: StringList = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders? = null,
    previousValue: Supplier<T?>?,
    action: Action? = null
): EmptyResponse {
    if (requireAtLeastOneValidator && eTagIfMatch.isNull() && ifUnmodifiedSince.isNull())
        throw lazyExceptionIfNotPresent()

    if (previousValue.isNotNull() && eTagIfMatch.isNotNull()) {
        val previousValue = previousValue()
        if (previousValue.isNotNull()) {
            val eTag = previousValue.eTag
            if (eTag !in eTagIfMatch mappedTo { it - Char.QUOTATION_MARK })
                throw lazyException()
        }
    } else if (previousLastModifiedDate.isNotNull() && ifUnmodifiedSince.isNotNull() && previousLastModifiedDate.isAfter(ifUnmodifiedSince))
        throw lazyException()

    action?.invoke()

    val response = ResponseEntity.status(status)
    if (headers.isNotNull() && !headers.isEmpty) response.headers(headers)
    if (includeFeatureCode) response.featureCode()
    if (newLastModifiedDate.isNotNull()) response.lastModified(newLastModifiedDate.toInstant())
    if (preferenceApplied.isNotEmpty()) response.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) response.refresh(refresh)
    if (serverTiming.isNotEmpty()) response.serverTiming(*serverTiming.toTypedArray())
    return response.build()
}
/**
 * Executes a conditional update operation based on the provided parameters and validators such as ETag and
 * If-Unmodified-Since headers. It throws exceptions if the preconditions are not met.
 *
 * @param eTagIfMatch Optional list of ETag values to match against the existing resource's ETag. Has higher priority than `ifUnmodifiedSince`.
 * @param ifUnmodifiedSince Optional timestamp to ensure the resource has not been modified after this time. If eTagIfMatch is present, this is not considered.
 * @param previousLastModifiedDate Optional previous last modified date of the resource.
 * @param requireAtLeastOneValidator If true, at least one validator (ETag or If-Unmodified-Since) must be provided and pass.
 * @param status HTTP status to be used for a successful response. Defaults to HttpStatus.NO_CONTENT.
 * @param lazyException Supplier of the exception to be thrown when validators fail.
 * @param lazyExceptionIfNotPresent Supplier of the exception to be thrown when required validators are not present.
 * @param includeFeatureCode If true, includes a "Feature-Code" header in the response.
 * @param newLastModifiedDate Optional timestamp to set the new last modified date in the response.
 * @param preferenceApplied A list of preference-applied values to include in the response. If provided, it sets the "Preference-Applied" header.
 * @param refresh The refresh duration for the response. If provided, it sets the "Refresh" header.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Optional HTTP headers to include in the response. HAS LOWER PRIORITY THAN THE NEXT PARAMETES.
 * @param previousETag The previous ETag of the resource.
 * @param action Action to execute as the conditional operation if validators pass.
 * @return An empty response object representing a successful operation.
 * @since 1.0.0
 */
@JvmName("conditionalUpdateAction")
fun <T : Any> conditionalUpdate(
    eTagIfMatch: StringList? = null,
    ifUnmodifiedSince: OffsetDateTime? = null,
    previousLastModifiedDate: OffsetDateTime? = null,
    requireAtLeastOneValidator: Boolean = false,
    status: HttpStatus = HttpStatus.NO_CONTENT,
    lazyException: ThrowableSupplier = { PreconditionFailedException("ETag not matched or If-Unmodified-Since header failed.") },
    lazyExceptionIfNotPresent: ThrowableSupplier = { PreconditionRequiredException("Use: If-Match, If-Unmodified-Since") },
    includeFeatureCode: Boolean = true,
    newLastModifiedDate: OffsetDateTime? = null,
    preferenceApplied: StringList = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders? = null,
    previousETag: String?,
    action: Action? = null
): EmptyResponse {
    if (requireAtLeastOneValidator && eTagIfMatch.isNull() && ifUnmodifiedSince.isNull())
        throw lazyExceptionIfNotPresent()

    if (previousETag.isNotNull() && eTagIfMatch.isNotNull()) {
        if (previousETag - Char.QUOTATION_MARK !in eTagIfMatch mappedTo { it - Char.QUOTATION_MARK })
            throw lazyException()
    } else if (previousLastModifiedDate.isNotNull() && ifUnmodifiedSince.isNotNull() && previousLastModifiedDate.isAfter(ifUnmodifiedSince))
        throw lazyException()

    action?.invoke()

    val response = ResponseEntity.status(status)
    if (headers.isNotNull() && !headers.isEmpty) response.headers(headers)
    if (includeFeatureCode) response.featureCode()
    if (newLastModifiedDate.isNotNull()) response.lastModified(newLastModifiedDate.toInstant())
    if (preferenceApplied.isNotEmpty()) response.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) response.refresh(refresh)
    if (serverTiming.isNotEmpty()) response.serverTiming(*serverTiming.toTypedArray())
    return response.build()
}
/**
 * Executes a conditional update operation based on the provided validators, such as ETags or last modified date.
 *
 * @param eTagIfMatch List of ETags that must match for the update to proceed. If null, this aspect is ignored. Has higher priority than `ifUnmodifiedSince`.
 * @param ifUnmodifiedSince Date and time indicating the resource must not have been modified since this timestamp to proceed. If eTagIfMatch is present, this is not considered.
 * @param previousLastModifiedDate Previous timestamp of when the resource was last modified, used for comparison.
 * @param requireAtLeastOneValidator Whether at least one validator must be satisfied for the operation to proceed.
 * @param status HTTP status code to return on successful execution of the update.
 * @param lazyException Lazy-initialized exception to throw if one or more conditions fail.
 * @param lazyExceptionIfNotPresent Lazy-initialized exception to throw if no validators are present but required.
 * @param featureCode Feature code to be added to the response as a custom header.
 * @param newLastModifiedDate New timestamp to set as the last modified date in the response headers, if provided.
 * @param preferenceApplied A list of preference-applied values to include in the response. If provided, it sets the "Preference-Applied" header.
 * @param refresh The refresh duration for the response. If provided, it sets the "Refresh" header.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Additional headers to include in the response. HAS LOWER PRIORITY THAN THE NEXT PARAMETES.
 * @param previousValue Supplier to fetch the previous state of the resource for checks (e.g., ETag comparison).
 * @param action Optional action to execute if the update conditions are satisfied.
 * @return EmptyResponse object representing the outcome of the conditional update.
 * @throws PreconditionFailedException if the conditions for update are not satisfied.
 * @throws PreconditionRequiredException if required validators are missing but necessary.
 * @since 1.0.0
 */
@JvmName("conditionalUpdateAction")
fun <T : Any> conditionalUpdate(
    eTagIfMatch: StringList? = null,
    ifUnmodifiedSince: OffsetDateTime? = null,
    previousLastModifiedDate: OffsetDateTime? = null,
    requireAtLeastOneValidator: Boolean = false,
    status: HttpStatus = HttpStatus.NO_CONTENT,
    lazyException: ThrowableSupplier = { PreconditionFailedException("ETag not matched or If-Unmodified-Since header failed.") },
    lazyExceptionIfNotPresent: ThrowableSupplier = { PreconditionRequiredException("Use: If-Match, If-Unmodified-Since") },
    featureCode: String,
    newLastModifiedDate: OffsetDateTime? = null,
    preferenceApplied: StringList = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders? = null,
    previousValue: Supplier<T?>?,
    action: Action? = null
): EmptyResponse {
    if (requireAtLeastOneValidator && eTagIfMatch.isNull() && ifUnmodifiedSince.isNull())
        throw lazyExceptionIfNotPresent()

    if (previousValue.isNotNull() && eTagIfMatch.isNotNull()) {
        val previousValue = previousValue()
        if (previousValue.isNotNull()) {
            val eTag = previousValue.eTag
            if (eTag !in eTagIfMatch mappedTo { it - Char.QUOTATION_MARK })
                throw lazyException()
        }
    } else if (previousLastModifiedDate.isNotNull() && ifUnmodifiedSince.isNotNull() && previousLastModifiedDate.isAfter(ifUnmodifiedSince))
        throw lazyException()

    action?.invoke()

    val response = ResponseEntity.status(status)
    if (headers.isNotNull() && !headers.isEmpty) response.headers(headers)
    response.featureCode(featureCode)
    if (newLastModifiedDate.isNotNull()) response.lastModified(newLastModifiedDate.toInstant())
    if (preferenceApplied.isNotEmpty()) response.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) response.refresh(refresh)
    if (serverTiming.isNotEmpty()) response.serverTiming(*serverTiming.toTypedArray())
    return response.build()
}
/**
 * Executes a conditional update operation based on the provided validators, such as ETags or last modified date.
 *
 * @param eTagIfMatch List of ETags that must match for the update to proceed. If null, this aspect is ignored. Has higher priority than `ifUnmodifiedSince`.
 * @param ifUnmodifiedSince Date and time indicating the resource must not have been modified since this timestamp to proceed. If eTagIfMatch is present, this is not considered.
 * @param previousLastModifiedDate Previous timestamp of when the resource was last modified, used for comparison.
 * @param requireAtLeastOneValidator Whether at least one validator must be satisfied for the operation to proceed.
 * @param status HTTP status code to return on successful execution of the update.
 * @param lazyException Lazy-initialized exception to throw if one or more conditions fail.
 * @param lazyExceptionIfNotPresent Lazy-initialized exception to throw if no validators are present but required.
 * @param featureCode Feature code to be added to the response as a custom header.
 * @param newLastModifiedDate New timestamp to set as the last modified date in the response headers, if provided.
 * @param preferenceApplied A list of preference-applied values to include in the response. If provided, it sets the "Preference-Applied" header.
 * @param refresh The refresh duration for the response. If provided, it sets the "Refresh" header.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Additional headers to include in the response. HAS LOWER PRIORITY THAN THE NEXT PARAMETES.
 * @param previousETag The previous ETag of the resource.
 * @param action Optional action to execute if the update conditions are satisfied.
 * @return EmptyResponse object representing the outcome of the conditional update.
 * @throws PreconditionFailedException if the conditions for update are not satisfied.
 * @throws PreconditionRequiredException if required validators are missing but necessary.
 * @since 1.0.0
 */
@JvmName("conditionalUpdateAction")
fun <T : Any> conditionalUpdate(
    eTagIfMatch: StringList? = null,
    ifUnmodifiedSince: OffsetDateTime? = null,
    previousLastModifiedDate: OffsetDateTime? = null,
    requireAtLeastOneValidator: Boolean = false,
    status: HttpStatus = HttpStatus.NO_CONTENT,
    lazyException: ThrowableSupplier = { PreconditionFailedException("ETag not matched or If-Unmodified-Since header failed.") },
    lazyExceptionIfNotPresent: ThrowableSupplier = { PreconditionRequiredException("Use: If-Match, If-Unmodified-Since") },
    featureCode: String,
    newLastModifiedDate: OffsetDateTime? = null,
    preferenceApplied: StringList = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders? = null,
    previousETag: String?,
    action: Action? = null
): EmptyResponse {
    if (requireAtLeastOneValidator && eTagIfMatch.isNull() && ifUnmodifiedSince.isNull())
        throw lazyExceptionIfNotPresent()

    if (previousETag.isNotNull() && eTagIfMatch.isNotNull()) {
        if (previousETag - Char.QUOTATION_MARK !in eTagIfMatch mappedTo { it - Char.QUOTATION_MARK })
            throw lazyException()
    } else if (previousLastModifiedDate.isNotNull() && ifUnmodifiedSince.isNotNull() && previousLastModifiedDate.isAfter(ifUnmodifiedSince))
        throw lazyException()

    action?.invoke()

    val response = ResponseEntity.status(status)
    if (headers.isNotNull() && !headers.isEmpty) response.headers(headers)
    response.featureCode(featureCode)
    if (newLastModifiedDate.isNotNull()) response.lastModified(newLastModifiedDate.toInstant())
    if (preferenceApplied.isNotEmpty()) response.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) response.refresh(refresh)
    if (serverTiming.isNotEmpty()) response.serverTiming(*serverTiming.toTypedArray())
    return response.build()
}

/**
 * Constructs an `EmptyResponse` object with the specified HTTP status, optional headers, action,
 * and feature code header inclusion settings.
 *
 * If an `action` is provided, it is invoked before building the response. By default,
 * the HTTP status is set to `HttpStatus.NO_CONTENT`, and the "Feature-Code" header is included
 * if applicable. Custom headers can also be added through the `headers` parameter.
 *
 * @param status The HTTP status to set for the response. Defaults to `HttpStatus.NO_CONTENT`.
 * @param includeFeatureCode Whether to include the "Feature-Code" header in the response. Defaults to `true`.*
 * @param eTag Optional ETag value to include in the response. Defaults to `null`.
 * @param preferenceApplied Optional list of preference-applied values to include in the response. Defaults to an empty list.
 * @param refresh Optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to. Defaults to `null`.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Optional HTTP headers to include in the response (overrides any other header parameters of this method). Defaults to `null`.
 * @param action An optional action to execute before building the response. Defaults to `null`.
 * @return A constructed `EmptyResponse` object with the specified settings.
 * @since 1.0.0
 */
fun EmptyResponse(
    status: HttpStatus = HttpStatus.NO_CONTENT,
    includeFeatureCode: Boolean = true,
    eTag: String? = null,
    lastModified: OffsetDateTime? = null,
    preferenceApplied: StringList = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders? = null,
    action: Action? = null
): EmptyResponse {
    if (action.isNotNull())
        action()

    val re = Response.status(status)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    if (includeFeatureCode) re.featureCode()
    if (eTag.isNotNull()) re.eTag(eTag)
    if (lastModified.isNotNull()) re.lastModified(lastModified.toInstant())
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    return re.build()
}
/**
 * Constructs an `EmptyResponse` with the given HTTP status, feature code, optional headers, and action.
 *
 * @param status the HTTP status to set for the response; defaults to `HttpStatus.NO_CONTENT`
 * @param featureCode the feature code to be added as a "Feature-Code" header in the response
 * @param eTag Optional ETag value to include in the response. Defaults to `null`.
 * @param lastModifiedDate Optional last modified date to include in the response. Defaults to `null`.
 * @param preferenceApplied Optional list of preference-applied values to include in the response. Defaults to an empty list.
 * @param refresh Optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to. Defaults to `null`.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers optional HTTP headers to include in the response (overrides any other header parameters of this method); can be null
 * @param action an optional action to execute; can be null
 * @return an `EmptyResponse` constructed with the specified parameters
 * @since 1.0.0
 */
fun EmptyResponse(
    status: HttpStatus = HttpStatus.NO_CONTENT,
    featureCode: String,
    eTag: String? = null,
    lastModifiedDate: OffsetDateTime? = null,
    preferenceApplied: StringList = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders? = null,
    action: Action? = null
): EmptyResponse {
    if (action.isNotNull())
        action()

    val re = Response.status(status)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    re.featureCode(featureCode)
    if (eTag.isNotNull()) re.eTag(eTag)
    if (lastModifiedDate.isNotNull()) re.lastModified(lastModifiedDate.toInstant())
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    return re.build()
}

/**
 * Generates an HTTP response with status code 200 (OK) and allows for optional customization
 * of headers and body content.
 *
 * @param T The type of the response body.
 * @param includeFeatureCode Indicates whether to include the "Feature-Code" header in the response.
 *                           Defaults to true.
 * @param includeETag Indicates whether to include an ETag header based on the response body. Defaults to true.
 * @param lastModifiedDate The optional last modified date to include in the response headers.
 * @param preferenceApplied Optional list of preference-applied values to include in the response. Defaults to an empty list.
 * @param refresh Optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to. Defaults to `null`.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Additional HTTP headers to include in the response. Defaults to null if no extra headers are needed.
 * @param body A supplier function that provides the response body content. Defaults to null.
 * @return An HTTP response of type `Response<T>` with the specified status, headers, and body content.
 * @since 1.0.0
 */
fun <T : Any> OKResponse(
    includeFeatureCode: Boolean = true,
    includeETag: Boolean = true,
    lastModifiedDate: OffsetDateTime? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: StringList = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders? = null,
    body: Supplier<T>? = null
): Response<T> {
    val re = Response.status(HttpStatus.OK)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    if (expires.isNotNull()) re.expires(expires)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (includeFeatureCode) re.featureCode()
    val result = body?.invoke()
    if (includeETag && result.isNotNull()) re.eTag(result.eTag)
    if (lastModifiedDate.isNotNull()) re.lastModified(lastModifiedDate.toInstant())
    if (result.isNotNull()) return re.body(result)
    return re.build()
}
/**
 * Constructs an HTTP OK response with optional headers and body content.
 *
 * @param featureCode a unique code to be included as a "Feature-Code" header in the response
 * @param includeETag whether to include an ETag header based on the body content, defaults to true
 * @param lastModifiedDate an optional timestamp to include as a "Last-Modified" header, defaults to null
 * @param expires an optional timestamp to include as an "Expires" header, defaults to null
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param refresh optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to, defaults to null
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers additional headers to include in the response, defaults to null
 * @param body a supplier for the response body content, which can be null
 * @return a Response instance of type T encapsulating the provided configurations
 * @since 1.0.0
 */
fun <T : Any> OKResponse(
    featureCode: String,
    includeETag: Boolean = true,
    lastModifiedDate: OffsetDateTime? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: StringList = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders? = null,
    body: Supplier<T>? = null
): Response<T> {
    val re = Response.status(HttpStatus.OK)
    val result = body?.invoke()
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    re.featureCode(featureCode)
    if (expires.isNotNull()) re.expires(expires)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (includeETag && result.isNotNull()) re.eTag(result.eTag)
    if (lastModifiedDate.isNotNull()) re.lastModified(lastModifiedDate.toInstant())
    if (result.isNotNull()) return re.body(result)
    return re.build()
}

/**
 * Constructs a `Response` object with an HTTP status of "201 Created", optionally including additional
 * headers, metadata, and a response body based on the provided parameters.
 *
 * @param T The type of the response body.
 * @param includeFeatureCode Whether to include a "Feature-Code" header based on the `Feature` annotation of the calling method. Defaults to true.
 * @param includeETag Whether to include the ETag header based on the response body content. Defaults to true.
 * @param lastModifiedDate The last modified date to include in the response header, if provided. Defaults to null.
 * @param location The location URI to include in the response header, if applicable. Defaults to null.
 * @param expires An optional timestamp to include as an "Expires" header, defaults to null
 * @param preferenceApplied Optional list of preference-applied values to include in the response. Defaults to an empty list.
 * @param refresh Optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to. Defaults to `null`.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Additional headers to include in the response, if specified. Defaults to null.
 * @param includeBody Whether to include the response body. Defaults to `true` if `location` is null.
 * @param body A supplier for the response body. Defaults to null if no body is required.
 * @return A `Response` object containing the constructed HTTP status, headers, and optional body.
 * @since 1.0.0
 */
fun <T : Any> CreatedResponse(
    includeFeatureCode: Boolean = true,
    includeETag: Boolean = true,
    lastModifiedDate: OffsetDateTime? = null,
    location: URI? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: StringList = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders? = null,
    includeBody: Boolean = location.isNull(),
    body: Supplier<T>? = null
): Response<T> {
    val re = Response.status(HttpStatus.CREATED)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    if (includeFeatureCode) re.featureCode()
    val result = body?.invoke()
    if (includeETag && result.isNotNull()) re.eTag(result.eTag)
    if (lastModifiedDate.isNotNull()) re.lastModified(lastModifiedDate.toInstant())
    if (location.isNotNull()) re.location(location)
    if (expires.isNotNull()) re.expires(expires)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (result.isNotNull() && includeBody) return re.body(result)
    return re.build()
}
/**
 * Constructs an HTTP 201 Created response with optional headers, body content, and metadata.
 *
 * @param featureCode the feature code to be added as the "Feature-Code" header in the response
 * @param includeETag a boolean indicating whether to include an ETag header in the response, defaults to true
 * @param lastModifiedDate the date and time the resource was last modified, included as a Last-Modified header if provided
 * @param location the URI of the created resource, included as a Location header if provided
 * @param expires an optional timestamp to include as an "Expires" header, defaults to null
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param refresh optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to, defaults to null
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers additional headers to be included in the response
 * @param includeBody a boolean indicating whether to include the body in the response, defaults to true if `location` is null
 * @param body a supplier providing the body of the response, invoked if a body is to be included
 * @return a Response object of type T representing the constructed 201 Created response
 * @since 1.0.0
 */
fun <T : Any> CreatedResponse(
    featureCode: String,
    includeETag: Boolean = true,
    lastModifiedDate: OffsetDateTime? = null,
    location: URI? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: StringList = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders? = null,
    includeBody: Boolean = location.isNull(),
    body: Supplier<T>? = null
): Response<T> {
    val re = Response.status(HttpStatus.CREATED)
    val result = body?.invoke()
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    re.featureCode(featureCode)
    if (includeETag && result.isNotNull()) re.eTag(result.eTag)
    if (lastModifiedDate.isNotNull()) re.lastModified(lastModifiedDate.toInstant())
    if (location.isNotNull()) re.location(location)
    if (expires.isNotNull()) re.expires(expires)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (result.isNotNull() && includeBody) return re.body(result)
    return re.build()
}

/**
 * Constructs an HTTP response with a status of 202 Accepted and optional configuration
 * for headers, entity tag (ETag), and last modified date.
 *
 * @param T The type of the response body.
 * @param includeFeatureCode Determines whether a "Feature-Code" header should be included in the response.
 *                           Defaults to true.
 * @param includeETag Indicates if the response should include an ETag header determined by the body content.
 *                    Defaults to true.
 * @param lastModifiedDate Specifies the last modified date for the response. Can be null if not applicable.
 * @param expires an optional timestamp to include as an "Expires" header, defaults to null
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param refresh optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to, defaults to null
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Additional HTTP headers to include in the response. Can be null if no additional headers are needed.
 * @param body A supplier function that provides the response body content. Can be null if no body content is needed.
 * @return The constructed response object containing the specified HTTP status, headers, and body.
 * @since 1.0.0
 */
fun <T : Any> AcceptedResponse(
    includeFeatureCode: Boolean = true,
    includeETag: Boolean = true,
    lastModifiedDate: OffsetDateTime? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: StringList = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders? = null,
    body: Supplier<T>? = null
): Response<T> {
    val re = Response.status(HttpStatus.ACCEPTED)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    if (includeFeatureCode) re.featureCode()
    val result = body?.invoke()
    if (includeETag && result.isNotNull()) re.eTag(result.eTag)
    if (lastModifiedDate.isNotNull()) re.lastModified(lastModifiedDate.toInstant())
    if (expires.isNotNull()) re.expires(expires)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (result.isNotNull()) return re.body(result)
    return re.build()
}
/**
 * Builds a response with an HTTP status of 202 Accepted, optionally including headers and body content.
 *
 * @param featureCode a unique code added to the "Feature-Code" header of the response
 * @param includeETag indicates whether an ETag header should be included in the response; defaults to true
 * @param lastModifiedDate optional timestamp indicating the last modification date of the resource
 * @param expires an optional timestamp to include as an "Expires" header, defaults to null
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param refresh optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to, defaults to null
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers optional additional headers to include in the response
 * @param body optional supplier for the response body content
 * @return a Response object containing the HTTP status, headers, and optionally body content
 * @since 1.0.0
 */
fun <T : Any> AcceptedResponse(
    featureCode: String,
    includeETag: Boolean = true,
    lastModifiedDate: OffsetDateTime? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: StringList = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders? = null,
    body: Supplier<T>? = null
): Response<T> {
    val re = Response.status(HttpStatus.ACCEPTED)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    re.featureCode(featureCode)
    val result = body?.invoke()
    if (expires.isNotNull()) re.expires(expires)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (includeETag && result.isNotNull()) re.eTag(result.eTag)
    if (lastModifiedDate.isNotNull()) re.lastModified(lastModifiedDate.toInstant())
    if (result.isNotNull()) return re.body(result)
    return re.build()
}

/**
 * Constructs a `Response` object with a status of `RESET_CONTENT` and optional headers and body content.
 *
 * @param includeFeatureCode Flag indicating whether to include the "Feature-Code" header in the response. Default is `true`.
 * @param eTag Optional ETag header value to include in the response.
 * @param lastModifiedDate Optional timestamp to set the "Last-Modified" header in the response.
 * @param expires an optional timestamp to include as an "Expires" header, defaults to null
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param refresh optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to, defaults to null
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Optional additional headers to include in the response.
 * @param action An optional action to execute before building the response. Defaults to `null`.
 * @return A `Response` object with the specified settings and content.
 * @since 1.0.0
 */
fun ResetContentResponse(
    includeFeatureCode: Boolean = true,
    eTag: String? = null,
    lastModifiedDate: OffsetDateTime? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: StringList = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders? = null,
    action: Action? = null
): EmptyResponse {
    action?.invoke()
    val re = Response.status(HttpStatus.RESET_CONTENT)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    if (includeFeatureCode) re.featureCode()
    if (eTag.isNotNull()) re.eTag(eTag)
    if (lastModifiedDate.isNotNull()) re.lastModified(lastModifiedDate.toInstant())
    if (expires.isNotNull()) re.expires(expires)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    return re.build()
}
/**
 * Constructs a `Response` object with a status of `RESET_CONTENT` and optional headers and body content.
 *
 * @param featureCode "Feature-Code" header value in the response.
 * @param eTag Optional ETag header value to include in the response.
 * @param lastModifiedDate Optional timestamp to set the "Last-Modified" header in the response.
 * @param expires an optional timestamp to include as an "Expires" header, defaults to null
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param refresh optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to, defaults to null1
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Optional additional headers to include in the response.
 * @param action an optional action to execute; can be null
 * @return A `Response` object with the specified settings and content.
 * @since 1.0.0
 */
fun ResetContentResponse(
    featureCode: String,
    eTag: String? = null,
    lastModifiedDate: OffsetDateTime? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: StringList = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders? = null,
    action: Action? = null
): EmptyResponse {
    action?.invoke()
    val re = Response.status(HttpStatus.RESET_CONTENT)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    re.featureCode(featureCode)
    if (expires.isNotNull()) re.expires(expires)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (eTag.isNotNull()) re.eTag(eTag)
    if (lastModifiedDate.isNotNull()) re.lastModified(lastModifiedDate.toInstant())
    return re.build()
}

/**
 * Constructs a `Response` object with HTTP status 206 (Partial Content) and allows customizing various aspects
 * of the response, including headers, body, and metadata.
 *
 * @param includeFeatureCode Determines whether to include the "Feature-Code" header in the response.
 *        Defaults to true.
 * @param includeETag Determines whether to include the ETag header in the response if the body is not null.
 *        Defaults to true.
 * @param lastModifiedDate Specifies the "Last-Modified" timestamp for the response. Can be null.
 *        Defaults to null.
*  @param expires an optional timestamp to include as an "Expires" header, defaults to null
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param refresh optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to, defaults to null
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Custom headers to include in the response. Can be null or empty. Defaults to null.
 * @param body A supplier that provides the body content of the response. Can be null. Defaults to null.
 * @return A `Response` object of type `T` with the specified properties and HTTP status 206.
 * @since 1.0.0
 */
fun <T : Any> PartialContentResponse(
    includeFeatureCode: Boolean = true,
    includeETag: Boolean = true,
    lastModifiedDate: OffsetDateTime? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: StringList = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders? = null,
    body: Supplier<T>? = null
): Response<T> {
    val re = Response.status(HttpStatus.PARTIAL_CONTENT)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    if (includeFeatureCode) re.featureCode()
    val result = body?.invoke()
    if (includeETag && result.isNotNull()) re.eTag(result.eTag)
    if (lastModifiedDate.isNotNull()) re.lastModified(lastModifiedDate.toInstant())
    if (expires.isNotNull()) re.expires(expires)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (result.isNotNull()) return re.body(result)
    return re.build()
}
/**
 * Builds and returns a response with HTTP status 206 (Partial Content) and additional metadata or body content.
 *
 * @param T The type of the response body.
 * @param featureCode The feature code to be added as a "Feature-Code" header in the response.
 * @param includeETag A flag indicating whether to include an ETag header based on the response body. Defaults to true.
 * @param lastModifiedDate The date-time to be included in the "Last-Modified" header, if specified.
 * @param expires an optional timestamp to include as an "Expires" header, defaults to null
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param refresh optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to, defaults to null
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Additional HTTP headers to be included in the response, if specified.
 * @param body A supplier function providing the body content for the response, if specified.
 * @return A built response object containing the given metadata and body content.
 * @since 1.0.0
 */
fun <T : Any> PartialContentResponse(
    featureCode: String,
    includeETag: Boolean = true,
    lastModifiedDate: OffsetDateTime? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: StringList = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders? = null,
    body: Supplier<T>? = null
): Response<T> {
    val re = Response.status(HttpStatus.PARTIAL_CONTENT)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    re.featureCode(featureCode)
    if (expires.isNotNull()) re.expires(expires)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    val result = body?.invoke()
    if (includeETag && result.isNotNull()) re.eTag(result.eTag)
    if (lastModifiedDate.isNotNull()) re.lastModified(lastModifiedDate.toInstant())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (result.isNotNull()) return re.body(result)
    return re.build()
}

enum class MultiStatusResponseType {
    WEBDAV_XML,
    MAP,
    GROUPED_BY_STATUS_MAP,
    GROUPED_BY_SUCCESS_AND_FAILURE_MAP,
}

/**
 * Builds and returns a multi-status HTTP response based on the provided parameters and resources.
 *
 * @param includeFeatureCode Flag indicating whether to include the "Feature-Code" header in the response. Defaults to true.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Additional HTTP headers to include in the response. Can be null or empty.
 * @param responseType The format or structure of the response body. Defaults to MultiStatusResponseType.WEBDAV_XML.
 * @param httpVersion The HTTP version to use when formatting the response status. Defaults to "HTTP/1.1".
 * @param resources A list of resource results containing metadata about the operation's outcomes.
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param refresh optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to, defaults to null
 * @return A `Response` instance with a status of HTTP 207 Multi-Status and a body formatted according to the specified `responseType`.
 * @since 1.0.0
 */
fun MultiStatusResponse(
    resources: List<ResourceResult>,
    includeFeatureCode: Boolean = true,
    preferenceApplied: StringList = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders? = null,
    responseType: MultiStatusResponseType = MultiStatusResponseType.WEBDAV_XML,
    httpVersion: String = "HTTP/1.1",
    action: Action? = null
): Response<Any> {
    action?.invoke()
    val re = Response.status(HttpStatus.MULTI_STATUS)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    if (includeFeatureCode) re.featureCode()
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    return re.body(when(responseType) {
        MultiStatusResponseType.WEBDAV_XML -> generateMultiStatusXML(resources, httpVersion)
        MultiStatusResponseType.MAP -> generateMultiStatusMap(resources, httpVersion)
        MultiStatusResponseType.GROUPED_BY_STATUS_MAP -> generateMultiStatusGroupedMap(resources, httpVersion)
        MultiStatusResponseType.GROUPED_BY_SUCCESS_AND_FAILURE_MAP -> generateMultiStatusGroupedByCategoryMap(resources, httpVersion)
    })
}
/**
 * Constructs a multi-status HTTP response based on the provided parameters.
 *
 * @param featureCode A string representing the feature code to be included in the HTTP header.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Optional HTTP headers to be included in the response. Defaults to null.
 * @param responseType The type of the multi-status response content. Defaults to `MultiStatusResponseType.WEBDAV_XML`.
 * @param httpVersion The HTTP version string to be used in the response. Defaults to "HTTP/1.1".
 * @param resources A list of `ResourceResult` objects representing the resource operation results.
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param refresh optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to, defaults to null
 * @return A `Response` object containing the multi-status HTTP response.
 * @since 1.0.0
 */
fun MultiStatusResponse(
    resources: List<ResourceResult>,
    featureCode: String,
    preferenceApplied: StringList = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders? = null,
    responseType: MultiStatusResponseType = MultiStatusResponseType.WEBDAV_XML,
    httpVersion: String = "HTTP/1.1",
): Response<Any> {
    val re = Response.status(HttpStatus.MULTI_STATUS)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    re.featureCode(featureCode)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    return re.body(when(responseType) {
        MultiStatusResponseType.WEBDAV_XML -> generateMultiStatusXML(resources, httpVersion)
        MultiStatusResponseType.MAP -> generateMultiStatusMap(resources, httpVersion)
        MultiStatusResponseType.GROUPED_BY_STATUS_MAP -> generateMultiStatusGroupedMap(resources, httpVersion)
        MultiStatusResponseType.GROUPED_BY_SUCCESS_AND_FAILURE_MAP -> generateMultiStatusGroupedByCategoryMap(resources, httpVersion)
    })
}

internal fun generateMultiStatusMap(results: List<ResourceResult>, httpVersion: String): List<DataMap> {
    val list = emptyMList<DataMap>()
    for (result in results) {
        val map: DataMMap = emptyMMap()
        map["reference"] = result.reference
        map["status"] = "HTTP/" + httpVersion.substringAfter("HTTP/") + Char.SPACE + result.statusCode.value() + Char.SPACE + result.statusCode.reasonPhrase
        if (result.description.isNotNullOrBlank()) map["description"] = result.description
        list += map
    }
    return list
}
internal fun generateMultiStatusGroupedMap(results: List<ResourceResult>, httpVersion: String): MultiMap<HttpStatus, DataMap> {
    val grouped = results groupedBy ResourceResult::statusCode
    return grouped valuesMappedTo {
        it.value mappedTo { result ->
            val map: DataMMap = emptyMMap()
            map["reference"] = result.reference
            map["status"] = "HTTP/" + httpVersion.substringAfter("HTTP/") + Char.SPACE + result.statusCode.value() + Char.SPACE + result.statusCode.reasonPhrase
            if (result.description.isNotNullOrBlank()) map["description"] = result.description
            map
        }
    }
}
internal fun generateMultiStatusGroupedByCategoryMap(results: List<ResourceResult>, httpVersion: String): MultiMap<String, DataMap> {
    val status1x = results { it.statusCode.is1xxInformational }
    val status2x = results { it.statusCode.is2xxSuccessful }
    val status3x = results { it.statusCode.is3xxRedirection }
    val status4x = results { it.statusCode.is4xxClientError }
    val status5x = results { it.statusCode.is5xxServerError }

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

        append("      <d:status>HTTP/${httpVersion.substringAfter("HTTP/")} ${item.statusCode.value()} ${item.statusCode.reasonPhrase}</d:status>\n")
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
 * @param newETag The ETag value to be included in the response, if provided.
 *                Defaults to null.
 * @param lastModifiedDate The timestamp to set as the "Last-Modified" header in the response,
 *                         if provided. Defaults to null.
 * @param expires an optional timestamp to include as an "Expires" header, defaults to null
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param refresh optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to, defaults to null
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Additional headers to include in the response, if provided. Defaults to null.
 * @param body A supplier for generating the response body, if needed. Defaults to null.
 * @return A `Response` instance with the configured attributes.
 * @since 1.0.0
 */
fun <T : Any> IMUsedResponse(
    includeFeatureCode: Boolean = true,
    newETag: String? = null,
    lastModifiedDate: OffsetDateTime? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: StringList = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders? = null,
    body: Supplier<T>? = null
): Response<T> {
    val re = Response.status(HttpStatus.IM_USED)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    if (includeFeatureCode) re.featureCode()
    if (expires.isNotNull()) re.expires(expires)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    val result = body?.invoke()
    if (newETag.isNotNullOrEmpty()) re.eTag(newETag)
    if (lastModifiedDate.isNotNull()) re.lastModified(lastModifiedDate.toInstant())
    if (result.isNotNull()) return re.body(result)
    return re.build()
}
/**
 * Creates an HTTP response with the status `IM_USED` (226) and optionally includes additional headers, body content,
 * and metadata such as ETag and last modified date.
 *
 * @param featureCode The feature code to be included as the "Feature-Code" header in the response.
 * @param newETag An optional ETag value to include in the response.
 * @param lastModifiedDate An optional last modified date to include in the response.
 * @param expires an optional timestamp to include as an "Expires" header, defaults to null
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param refresh optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to, defaults to null
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Optional additional HTTP headers to be included in the response.
 * @param body An optional supplier for generating the body of the response.
 * @return A `Response<T>` instance representing the constructed HTTP response.
 * @since 1.0.0
 */
fun <T : Any> IMUsedResponse(
    featureCode: String,
    newETag: String? = null,
    lastModifiedDate: OffsetDateTime? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: StringList = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders? = null,
    body: Supplier<T>? = null
): Response<T> {
    val re = Response.status(HttpStatus.IM_USED)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    re.featureCode(featureCode)
    if (expires.isNotNull()) re.expires(expires)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    val result = body?.invoke()
    if (newETag.isNotNullOrEmpty()) re.eTag(newETag)
    if (lastModifiedDate.isNotNull()) re.lastModified(lastModifiedDate.toInstant())
    if (result.isNotNull()) return re.body(result)
    return re.build()
}

/**
 * Creates a `Response` object with an HTTP `303 See Other` status and a specified location.
 *
 * Optionally, this method can include a "Feature-Code" header if `includeFeatureCode` is set to `true`.
 * Additional headers can be added using the `headers` parameter, and an optional custom action can
 * be executed during the response building process.
 *
 * @param includeFeatureCode When `true`, adds a "Feature-Code" header to the response if applicable. Defaults to `true`.
 * @param headers Optional HTTP headers to be included in the response (overrides any other header parameters of this method).
 * @param location The URI to which the user agent should be redirected.
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param refresh optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to, defaults to null
 * @param retryAfter optional duration after which the client should retry the request
 * @param action An optional action to be executed during the response building process.
 * @return A `Response` instance with the specified HTTP status, headers, and location.
 * @since 1.0.0
 */
fun SeeOtherResponse(
    includeFeatureCode: Boolean = true,
    preferenceApplied: StringList = emptyList(),
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: Duration? = null,
    headers: HttpHeaders? = null,
    location: URI,
    action: Action? = null
): EmptyResponse {
    val re = Response.status(HttpStatus.SEE_OTHER)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    re.location(location)
    if (includeFeatureCode) re.featureCode()
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (retryAfter.isNotNull()) re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.build()
}
/**
 * Creates a "See Other" HTTP response with a feature code, optional headers, and a specified location URI.
 *
 * @param featureCode the feature code to be added as a custom header to the response
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers optional HTTP headers to be included in the response (overrides any other header parameters of this method)
 * @param location the URI to which the client should be redirected
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param action an optional lambda that defines additional actions or settings for the response
 * @return a pre-built response entity with the "See Other" HTTP status
 * @since 1.0.0
 */
fun SeeOtherResponse(
    featureCode: String,
    preferenceApplied: StringList = emptyList(),
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: Duration? = null,
    headers: HttpHeaders? = null,
    location: URI,
    action: Action? = null
): EmptyResponse {
    val re = Response.status(HttpStatus.SEE_OTHER)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    re.featureCode(featureCode).location(location)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (retryAfter.isNotNull()) re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.build()
}

/**
 * Creates a `Response` object with an HTTP `303 See Other` status and a specified location.
 *
 * Optionally, this method can include a "Feature-Code" header if `includeFeatureCode` is set to `true`.
 * Additional headers can be added using the `headers` parameter, and an optional custom action can
 * be executed during the response building process.
 *
 * @param includeFeatureCode When `true`, adds a "Feature-Code" header to the response if applicable. Defaults to `true`.
 * @param headers Optional HTTP headers to be included in the response (overrides any other header parameters of this method).
 * @param location The URI to which the user agent should be redirected.
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param refresh optional pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to, defaults to null
 * @param retryAfter optional duration after which the client should retry the request
 * @param action An optional action to be executed during the response building process.
 * @return A `Response` instance with the specified HTTP status, headers, and location.
 * @since 1.0.0
 */
fun SeeOtherResponse(
    includeFeatureCode: Boolean = true,
    preferenceApplied: StringList = emptyList(),
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: TemporalAccessor,
    headers: HttpHeaders? = null,
    location: URI,
    action: Action? = null
): EmptyResponse {
    val re = Response.status(HttpStatus.SEE_OTHER)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    re.location(location)
    if (includeFeatureCode) re.featureCode()
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.build()
}
/**
 * Creates a "See Other" HTTP response with a feature code, optional headers, and a specified location URI.
 *
 * @param featureCode the feature code to be added as a custom header to the response
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers optional HTTP headers to be included in the response (overrides any other header parameters of this method)
 * @param location the URI to which the client should be redirected
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param action an optional lambda that defines additional actions or settings for the response
 * @return a pre-built response entity with the "See Other" HTTP status
 * @since 1.0.0
 */
fun SeeOtherResponse(
    featureCode: String,
    preferenceApplied: StringList = emptyList(),
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: TemporalAccessor,
    headers: HttpHeaders? = null,
    location: URI,
    action: Action? = null
): EmptyResponse {
    val re = Response.status(HttpStatus.SEE_OTHER)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    re.featureCode(featureCode).location(location)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.build()
}

/**
 * Constructs a response with a `302 Found` HTTP status, a specified location, and optional headers and actions.
 * Additionally, it can add a "Feature-Code" header based on the calling context, if enabled.
 *
 * @param includeFeatureCode A flag indicating whether to include the "Feature-Code" header in the response. Defaults to `true`.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers Optional headers to include in the response (overrides any other header parameters of this method). Pass `null` or an empty `HttpHeaders` instance for no additional headers.
 * @param location The target URI to which the client is redirected.
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param action An optional lambda function to perform additional customization on the response.
 * @return A `Response<T>` instance with the specified properties and configurations.
 * @since 1.0.0
 */
fun FoundResponse(
    includeFeatureCode: Boolean = true,
    preferenceApplied: StringList = emptyList(),
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: Duration? = null,
    headers: HttpHeaders? = null,
    location: URI,
    action: Action? = null
): EmptyResponse {
    val re = Response.status(HttpStatus.FOUND)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    if (includeFeatureCode) re.featureCode()
    re.location(location)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (retryAfter.isNotNull()) re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.build()
}
/**
 * Builds and returns an HTTP 302 Found response with optional headers and an optional action.
 *
 * @param featureCode the value for the "Feature-Code" header in the response
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers optional HTTP headers to include in the response (overrides any other header parameters of this method), can be null
 * @param location the URI to be set in the "Location" header for the response
 * @param action an optional action to execute before building the response, can be null
 * @return a `Response` object with HTTP status 302 (Found) and the provided details
 * @since 1.0.0
 */
fun FoundResponse(
    featureCode: String,
    preferenceApplied: StringList = emptyList(),
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: Duration? = null,
    headers: HttpHeaders? = null,
    location: URI,
    action: Action? = null
): EmptyResponse {
    val re = Response.status(HttpStatus.FOUND)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    re.featureCode(featureCode).location(location)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (retryAfter.isNotNull()) re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.build()
}

/**
 * Constructs a response with a `302 Found` HTTP status, a specified location, and optional headers and actions.
 * Additionally, it can add a "Feature-Code" header based on the calling context, if enabled.
 *
 * @param includeFeatureCode A flag indicating whether to include the "Feature-Code" header in the response. Defaults to `true`.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers Optional headers to include in the response (overrides any other header parameters of this method). Pass `null` or an empty `HttpHeaders` instance for no additional headers.
 * @param location The target URI to which the client is redirected.
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param action An optional lambda function to perform additional customization on the response.
 * @return A `Response<T>` instance with the specified properties and configurations.
 * @since 1.0.0
 */
fun FoundResponse(
    includeFeatureCode: Boolean = true,
    preferenceApplied: StringList = emptyList(),
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: TemporalAccessor,
    headers: HttpHeaders? = null,
    location: URI,
    action: Action? = null
): EmptyResponse {
    val re = Response.status(HttpStatus.FOUND)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    if (includeFeatureCode) re.featureCode()
    re.location(location)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.build()
}
/**
 * Builds and returns an HTTP 302 Found response with optional headers and an optional action.
 *
 * @param featureCode the value for the "Feature-Code" header in the response
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers optional HTTP headers to include in the response (overrides any other header parameters of this method), can be null
 * @param location the URI to be set in the "Location" header for the response
 * @param action an optional action to execute before building the response, can be null
 * @return a `Response` object with HTTP status 302 (Found) and the provided details
 * @since 1.0.0
 */
fun FoundResponse(
    featureCode: String,
    preferenceApplied: StringList = emptyList(),
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: TemporalAccessor,
    headers: HttpHeaders? = null,
    location: URI,
    action: Action? = null
): EmptyResponse {
    val re = Response.status(HttpStatus.FOUND)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    re.featureCode(featureCode).location(location)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.build()
}

/**
 * Creates an HTTP 301 Moved Permanently response.
 *
 * This method generates a response with the HTTP status code 301 (Moved Permanently)
 * and sets the `Location` header to the specified URI. Optionally, it can include
 * feature code metadata in the response headers and execute an additional action.
 *
 * @param includeFeatureCode A boolean indicating whether to include the feature code header in the response. Defaults to `true`.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers Optional HTTP headers to include in the response (overrides any other header parameters of this method). If `null` or empty, no additional headers are included.
 * @param location The target URI where the client should be redirected.
 * @param action An optional additional action to execute while building the response.
 * @return A constructed response with HTTP status 301 (Moved Permanently) and the specified parameters.
 * @since 1.0.0
 */
fun MovedPermanentlyResponse(
    includeFeatureCode: Boolean = true,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: Duration? = null,
    headers: HttpHeaders? = null,
    location: URI,
    action: Action? = null
): EmptyResponse {
    val re = Response.status(HttpStatus.MOVED_PERMANENTLY)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    re.location(location)
    if (includeFeatureCode) re.featureCode()
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (retryAfter.isNotNull()) re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.build()
}
/**
 * Creates a response with HTTP status 301 (Moved Permanently).
 *
 * @param featureCode a string representing the feature code to be added as a custom header in the response.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers optional HTTP headers to be included in the response (overrides any other header parameters of this method).
 * @param location a URI indicating the new location of the requested resource.
 * @param action an optional action to be executed during the construction of the response.
 * @return a response object with HTTP status 301 (Moved Permanently) and the specified attributes.
 * @since 1.0.0
 */
fun MovedPermanentlyResponse(
    featureCode: String,
    headers: HttpHeaders? = null,
    location: URI,
    action: Action? = null
): EmptyResponse {
    val re = Response.status(HttpStatus.MOVED_PERMANENTLY)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    re.featureCode(featureCode).location(location)
    if (action.isNotNull()) action()
    return re.build()
}
/**
 * Creates an HTTP 301 Moved Permanently response.
 *
 * This method generates a response with the HTTP status code 301 (Moved Permanently)
 * and sets the `Location` header to the specified URI. Optionally, it can include
 * feature code metadata in the response headers and execute an additional action.
 *
 * @param includeFeatureCode A boolean indicating whether to include the feature code header in the response. Defaults to `true`.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers Optional HTTP headers to include in the response (overrides any other header parameters of this method). If `null` or empty, no additional headers are included.
 * @param location The target URI where the client should be redirected.
 * @param action An optional additional action to execute while building the response.
 * @return A constructed response with HTTP status 301 (Moved Permanently) and the specified parameters.
 * @since 1.0.0
 */
fun MovedPermanentlyResponse(
    includeFeatureCode: Boolean = true,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: TemporalAccessor,
    headers: HttpHeaders? = null,
    location: URI,
    action: Action? = null
): EmptyResponse {
    val re = Response.status(HttpStatus.MOVED_PERMANENTLY)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    re.location(location)
    if (includeFeatureCode) re.featureCode()
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.build()
}
/**
 * Creates a response with HTTP status 301 (Moved Permanently).
 *
 * @param featureCode a string representing the feature code to be added as a custom header in the response.
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers optional HTTP headers to be included in the response (overrides any other header parameters of this method).
 * @param location a URI indicating the new location of the requested resource.
 * @param action an optional action to be executed during the construction of the response.
 * @return a response object with HTTP status 301 (Moved Permanently) and the specified attributes.
 * @since 1.0.0
 */
fun MovedPermanentlyResponse(
    featureCode: String,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: TemporalAccessor,
    headers: HttpHeaders? = null,
    location: URI,
    action: Action? = null
): EmptyResponse {
    val re = Response.status(HttpStatus.MOVED_PERMANENTLY)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    re.featureCode(featureCode).location(location)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.build()
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
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers Optional custom headers to include in the response (overrides any other header parameters of this method). If `null` or empty,
 * no additional headers are added.
 * @param location The URI to set as the `Location` header of the response. Must not be `null`.
 * @param action An optional action to invoke for configuring the `Response` further.
 * If `null`, no action is performed.
 * @return The fully configured `Response` object with a `308 Permanent Redirect` status code.
 * @since 1.0.0
 */
fun PermanentRedirectResponse(
    includeFeatureCode: Boolean = true,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: Duration? = null,
    headers: HttpHeaders? = null,
    location: URI,
    action: Action? = null
): EmptyResponse {
    val re = Response.status(HttpStatus.PERMANENT_REDIRECT)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    re.location(location)
    if (includeFeatureCode) re.featureCode()
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (retryAfter.isNotNull()) re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.build()
}
/**
 * Constructs a response with a 308 Permanent Redirect status, allowing for a new location to be provided.
 *
 * @param featureCode a string representing a feature code to add as a custom header in the response
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers optional headers to be included in the response (overrides any other header parameters of this method)
 * @param location the URI to which the client is redirected
 * @param action an optional action to execute additional response configuration
 * @return a `Response` object with status 308 Permanent Redirect and the specified configurations
 * @since 1.0.0
 */
fun PermanentRedirectResponse(
    featureCode: String,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: Duration? = null,
    headers: HttpHeaders? = null,
    location: URI,
    action: Action? = null
): EmptyResponse {
    val re = Response.status(HttpStatus.PERMANENT_REDIRECT)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    re.featureCode(featureCode).location(location)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (retryAfter.isNotNull()) re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.build()
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
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers Optional custom headers to include in the response (overrides any other header parameters of this method). If `null` or empty,
 * no additional headers are added.
 * @param location The URI to set as the `Location` header of the response. Must not be `null`.
 * @param action An optional action to invoke for configuring the `Response` further.
 * If `null`, no action is performed.
 * @return The fully configured `Response` object with a `308 Permanent Redirect` status code.
 * @since 1.0.0
 */
fun PermanentRedirectResponse(
    includeFeatureCode: Boolean = true,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: TemporalAccessor,
    headers: HttpHeaders? = null,
    location: URI,
    action: Action? = null
): EmptyResponse {
    val re = Response.status(HttpStatus.PERMANENT_REDIRECT)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    re.location(location)
    if (includeFeatureCode) re.featureCode()
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.build()
}
/**
 * Constructs a response with a 308 Permanent Redirect status, allowing for a new location to be provided.
 *
 * @param featureCode a string representing a feature code to add as a custom header in the response
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers optional headers to be included in the response (overrides any other header parameters of this method)
 * @param location the URI to which the client is redirected
 * @param action an optional action to execute additional response configuration
 * @return a `Response` object with status 308 Permanent Redirect and the specified configurations
 * @since 1.0.0
 */
fun PermanentRedirectResponse(
    featureCode: String,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: TemporalAccessor,
    headers: HttpHeaders? = null,
    location: URI,
    action: Action? = null
): EmptyResponse {
    val re = Response.status(HttpStatus.PERMANENT_REDIRECT)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    re.featureCode(featureCode).location(location)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.build()
}

/**
 * Creates a response with a `302 Temporary Redirect` status. Allows customization of headers,
 * feature codes, redirection location, and an additional action to modify the response.
 *
 * @param includeFeatureCode Whether to include a feature code in the response headers. Default is `true`.
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers Optional headers to include in the response (overrides any other header parameters of this method). Default is `null`.
 * @param location The URI to which the client is redirected.
 * @param action An optional lambda for additional customization of the response before it is built. Default is `null`.
 * @return A `Response` object with the status set to `302 Temporary Redirect` and the specified attributes.
 * @since 1.0.0
 */
fun TemporaryRedirectResponse(
    includeFeatureCode: Boolean = true,
    preferenceApplied: StringList = emptyList(),
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: Duration? = null,
    headers: HttpHeaders? = null,
    location: URI,
    action: Action? = null
): EmptyResponse {
    val re = Response.status(HttpStatus.TEMPORARY_REDIRECT)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    if (includeFeatureCode) re.featureCode()
    re.location(location)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (retryAfter.isNotNull()) re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.build()
}
/**
 * Constructs a response with HTTP status code "307 Temporary Redirect."
 *
 * @param featureCode the feature code to be included as a custom header in the response
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers optional HTTP headers to include in the response (overrides any other header parameters of this method); if provided, they are added to the response
 * @param location the URI to which the client is redirected
 * @param action an optional action to execute before building the response
 * @return a constructed response with the specified parameters and a "307 Temporary Redirect" status
 * @since 1.0.0
 */
fun TemporaryRedirectResponse(
    featureCode: String,
    preferenceApplied: StringList = emptyList(),
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: Duration? = null,
    headers: HttpHeaders? = null,
    location: URI,
    action: Action? = null
): EmptyResponse {
    val re = Response.status(HttpStatus.TEMPORARY_REDIRECT)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    re.featureCode(featureCode).location(location)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (retryAfter.isNotNull()) re.retryAfter(retryAfter)
    if (retryAfter.isNotNull()) re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.build()
}
/**
 * Creates a response with a `302 Temporary Redirect` status. Allows customization of headers,
 * feature codes, redirection location, and an additional action to modify the response.
 *
 * @param includeFeatureCode Whether to include a feature code in the response headers. Default is `true`.
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers Optional headers to include in the response (overrides any other header parameters of this method). Default is `null`.
 * @param location The URI to which the client is redirected.
 * @param action An optional lambda for additional customization of the response before it is built. Default is `null`.
 * @return A `Response` object with the status set to `302 Temporary Redirect` and the specified attributes.
 * @since 1.0.0
 */
fun TemporaryRedirectResponse(
    includeFeatureCode: Boolean = true,
    preferenceApplied: StringList = emptyList(),
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: TemporalAccessor,
    headers: HttpHeaders? = null,
    location: URI,
    action: Action? = null
): EmptyResponse {
    val re = Response.status(HttpStatus.TEMPORARY_REDIRECT)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    if (includeFeatureCode) re.featureCode()
    re.location(location)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.build()
}
/**
 * Constructs a response with HTTP status code "307 Temporary Redirect."
 *
 * @param featureCode the feature code to be included as a custom header in the response
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param retryAfter optional duration after which the client should retry the request
 * @param headers optional HTTP headers to include in the response (overrides any other header parameters of this method); if provided, they are added to the response
 * @param location the URI to which the client is redirected
 * @param action an optional action to execute before building the response
 * @return a constructed response with the specified parameters and a "307 Temporary Redirect" status
 * @since 1.0.0
 */
fun TemporaryRedirectResponse(
    featureCode: String,
    preferenceApplied: StringList = emptyList(),
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    retryAfter: TemporalAccessor,
    headers: HttpHeaders? = null,
    location: URI,
    action: Action? = null
): EmptyResponse {
    val re = Response.status(HttpStatus.TEMPORARY_REDIRECT)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    re.featureCode(featureCode).location(location)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    re.retryAfter(retryAfter)
    if (action.isNotNull()) action()
    return re.build()
}

/**
 * Constructs a 304 Not Modified HTTP response.
 *
 * This method customizes the response by adding optional headers, an ETag, and feature code metadata.
 * It uses the `HttpStatus.NOT_MODIFIED` status to indicate that the resource has not changed since last requested.
 *
 * @param includeFeatureCode Specifies whether to include the "Feature-Code" header in the response. Defaults to `true`.
 * @param eTag An optional ETag value to include in the response for resource versioning. Defaults to `null`.
 * @param expires An optional expiration date for the response. Defaults to `null`.
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Optional additional HTTP headers to include in the response (overrides any other header parameters of this method). Defaults to `null`.
 * @param action An optional action to perform before finalizing the response. Defaults to `null`.
 * @return A `Response<T>` instance representing the 304 Not Modified HTTP response.
 * @since 1.0.0
 */
fun NotModifiedResponse(
    includeFeatureCode: Boolean = true,
    eTag: String? = null,
    expires: TemporalAccessor? = null,
    preferenceApplied: StringList = emptyList(),
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders? = null,
    action: Action? = null
): EmptyResponse {
    val re = Response.status(HttpStatus.NOT_MODIFIED)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    if (eTag.isNotNull()) re.eTag(eTag)
    if (expires.isNotNull()) re.expires(expires)
    if (includeFeatureCode) re.featureCode()
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (action.isNotNull()) action()
    return re.build()
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
 *  based on the `Feature` annotation of the calling method. Defaults to `true`.*
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param refresh optional pair of duration and URL for refresh header, defaults to null
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers Optional custom HTTP headers to add to the response (overrides any other header parameters of this method). Can be `null` or empty.
 * @param action An optional action to further customize the response object. Can be `null`.
 * @return A `Response` object with an HTTP 204 (No Content) status code and any specified metadata.
 * @since 1.0.0
 */
@Deprecated("Use EmptyResponse instead")
fun NoContentResponse(
    includeFeatureCode: Boolean = true,
    preferenceApplied: StringList = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders? = null,
    action: Action? = null
): EmptyResponse {
    val re = Response.status(HttpStatus.NO_CONTENT)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (includeFeatureCode) re.featureCode()
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (action.isNotNull()) action()
    return re.build()
}
/**
 * Builds an HTTP 204 No Content response with optional headers and eTag.
 *
 * Deprecated in favor of the [EmptyResponse] function.
 *
 * @param featureCode the feature code to include as the "Feature-Code" header in the response
 * @param preferenceApplied optional list of preference-applied values to include in the response, defaults to an empty list
 * @param refresh optional pair of duration and URL for refresh header, defaults to null
 * @param serverTiming A list of triple containing the "Server-Timing" header label, duration, and description.
 * @param headers optional custom headers to include in the response (overrides any other header parameters of this method)
 * @param action an optional action to further modify the response
 * @return a response with an HTTP 204 No Content status
 * @since 1.0.0
 */
@Deprecated("Use EmptyResponse instead")
fun NoContentResponse(
    featureCode: String,
    preferenceApplied: StringList = emptyList(),
    refresh: Pair<Duration, URL?>? = null,
    serverTiming: Set<Triple<String, Duration, String?>> = emptySet(),
    headers: HttpHeaders? = null,
    action: Action? = null
): EmptyResponse {
    val re = Response.status(HttpStatus.NO_CONTENT)
    if (headers.isNotNull() && !headers.isEmpty) re.headers(headers)
    re.featureCode(featureCode)
    if (preferenceApplied.isNotEmpty()) re.preferenceApplied(*preferenceApplied.toTypedArray())
    if (refresh.isNotNull()) re.refresh(refresh)
    if (serverTiming.isNotEmpty()) re.serverTiming(*serverTiming.toTypedArray())
    if (action.isNotNull()) action()
    return re.build()
}

/**
 * Adds a "Feature-Code" header with the provided code to the response.
 *
 * @param code the feature code to be added as the "Feature-Code" header in the response
 * @since 1.0.0
 */
fun ResponseEntity.BodyBuilder.featureCode(code: String): ResponseEntity.BodyBuilder = header("Feature-Code", code)
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
 * @receiver The `ResponseEntity.BodyBuilder` instance on which this method is invoked.
 * @return The same instance of `ResponseEntity.BodyBuilder` with the "Feature-Code" header added.
 * @since 1.0.0
 */
fun ResponseEntity.BodyBuilder.featureCode(): ResponseEntity.BodyBuilder = header("Feature-Code",
    findCallerMethod()?.getAnnotation(Feature::class.java)?.code ?: String.EMPTY
)

/**
 * Adds an `Expires` header to the HTTP response with the specified expiration time.
 *
 * @param time The expiration time to be set in the `Expires` header. It must be a valid `TemporalAccessor` instance.
 * @return The updated `ResponseEntity.BodyBuilder` with the `Expires` header added.
 * @since 1.0.0
 */
fun ResponseEntity.BodyBuilder.expires(time: TemporalAccessor): ResponseEntity.BodyBuilder =
    header("Expires", time.toHeaderDate())

/**
 * Adds a "Preference-Applied" header to the response entity builder with the specified preferences.
 *
 * @param preferences A vararg of preference names to include in the "Preference-Applied" header.
 * @return The updated response entity builder with the added "Preference-Applied" header.
 * @since 1.0.0
 */
fun ResponseEntity.BodyBuilder.preferenceApplied(vararg preferences: String): ResponseEntity.BodyBuilder =
    header("Preference-Applied", preferences.joinToString(", "))

/**
 * Adds a "Refresh" header to the response, indicating a periodic refresh or redirect to a specified URL.
 *
 * @param time The duration after which the client should refresh or perform the redirect.
 * @param url The optional URL to redirect the client to after the refresh.
 * @return The updated ResponseEntity.BodyBuilder with the "Refresh" header set.
 * @since 1.0.0
 */
@OptIn(RiskyApproximationOfTemporal::class)
fun ResponseEntity.BodyBuilder.refresh(time: Duration, url: URL? = null): ResponseEntity.BodyBuilder =
    header("Refresh", "${time.toSeconds()}${url?.let { "; url=$it" } ?: String.EMPTY}")
/**
 * Adds a "Refresh" header to the response, indicating a periodic refresh or redirect to a specified URL.
 *
 * @param timeAndURL A pair containing the duration after which the client should refresh or perform the redirect and the optional URL to redirect to.
 * @return The updated ResponseEntity.BodyBuilder with the "Refresh" header set.
 * @since 1.0.0
 */
@OptIn(RiskyApproximationOfTemporal::class)
fun ResponseEntity.BodyBuilder.refresh(timeAndURL: Pair<Duration, URL?>): ResponseEntity.BodyBuilder =
    header("Refresh", "${timeAndURL.first.toSeconds()}${timeAndURL.second?.let { "; url=$it" } ?: String.EMPTY}")
/**
 * Adds a `Refresh` header to the HTTP response, which specifies the interval 
 * after which the client should automatically refresh or redirect to a given URL.
 *
 * @param seconds The number of seconds to wait before the client refreshes. Must be a non-negative integer.
 * @param url The optional URL to which the client will be redirected after the specified interval. 
 * If null, the client will refresh the current URL.
 * @return The current instance of [ResponseEntity.BodyBuilder] with the `Refresh` header added.
 * @since 1.0.0
 */
fun ResponseEntity.BodyBuilder.refresh(seconds: Int, url: URL? = null): ResponseEntity.BodyBuilder =
    header("Refresh", "${seconds}${url?.let { "; url=$it" } ?: String.EMPTY}")

/**
 * Adds a `Retry-After` header to the response with the specified duration in seconds.
 *
 * @param duration The duration to be included in the `Retry-After` header.
 * @return The modified {@link ResponseEntity.BodyBuilder} with the `Retry-After` header set.
 * @since 1.0.0
 */
@OptIn(RiskyApproximationOfTemporal::class)
fun ResponseEntity.BodyBuilder.retryAfter(duration: Duration): ResponseEntity.BodyBuilder =
    header("Retry-After", duration.toSeconds().toString())
/**
 * Adds a "Retry-After" header to the HTTP response with the specified delay in seconds.
 *
 * @param seconds The number of seconds to indicate in the "Retry-After" header.
 * @return The updated ResponseEntity.BodyBuilder with the "Retry-After" header applied.
 * @since 1.0.0
 */
fun ResponseEntity.BodyBuilder.retryAfter(seconds: Int): ResponseEntity.BodyBuilder =
    header("Retry-After", seconds.toString())
/**
 * Adds a "Retry-After" header to the current HTTP response, using the provided temporal value
 * to determine the delay or time after which the client is allowed to retry the request.
 *
 * @param temporal The temporal accessor representing the time or delay for the "Retry-After" header.
 *                 This value is expected to be formatted in the RFC 1123 date-time format.
 * @return The instance of the response builder with the "Retry-After" header included.
 * @since 1.0.0
 */
fun ResponseEntity.BodyBuilder.retryAfter(temporal: TemporalAccessor): ResponseEntity.BodyBuilder =
    header("Retry-After", temporal.toHeaderDate())

/**
 * Adds a `Server-Timing` header to the response with the provided timing metrics.
 *
 * Each timing metric is represented as a pair of a name and a duration. The name is used as the metric identifier,
 * and the duration is converted to milliseconds to indicate the elapsed time for the corresponding metric.
 *
 * @param timingMetric Vararg of pairs where each pair consists of a metric name (String) and its duration (Duration).
 *                     The name identifies the metric, and the duration represents the time taken by the metric in milliseconds.
 * @return The modified [ResponseEntity.BodyBuilder] instance with the `Server-Timing` header added.
 * @since 1.0.0
 */
@OptIn(RiskyApproximationOfTemporal::class)
fun ResponseEntity.BodyBuilder.serverTiming(vararg timingMetric: Pair<String, Duration>): ResponseEntity.BodyBuilder =
    header("Server-Timing", timingMetric.joinToString(", ") { "${it.first};dur=${it.second.toMillis()}" })
/**
 * Adds a `Server-Timing` header to the response containing the provided timing metrics.
 * The `Server-Timing` header is used to communicate performance metrics from the server
 * to the client, aiding in performance monitoring and diagnostics.
 *
 * @param timingMetric Vararg of pairs, where each pair contains:
 * - `first`: The metric name (e.g., "db", "cpu").
 * - `second`: The duration of the metric as a number (interpreted as milliseconds).
 * @return The updated `ResponseEntity.BodyBuilder` instance with the added `Server-Timing` header.
 * @since 1.0.0
 */
@JvmName("serverTimingNumberDuration")
fun ResponseEntity.BodyBuilder.serverTiming(vararg timingMetric: Pair<String, Number>): ResponseEntity.BodyBuilder =
    header("Server-Timing", timingMetric.joinToString(", ") { "${it.first};dur=${it.second}" })
/**
 * Adds a "Server-Timing" header to the response, providing performance metrics
 * about server-side execution for debugging or monitoring purposes.
 *
 * @param timingMetric A vararg of `Triple` elements where:
 *   - The first element represents the metric name (e.g., "db-query").
 *   - The second element is a `Duration` object indicating the elapsed time for the metric.
 *   - The third element provides an optional description of the metric.
 * @return The updated `ResponseEntity.BodyBuilder` with the "Server-Timing" header included.
 * @since 1.0.0
 */
@OptIn(RiskyApproximationOfTemporal::class)
fun ResponseEntity.BodyBuilder.serverTiming(vararg timingMetric: Triple<String, Duration, String?>): ResponseEntity.BodyBuilder =
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
 * @return The modified instance of [ResponseEntity.BodyBuilder] with the `Server-Timing` header added.
 * @since 1.0.0
 */
@JvmName("serverTimingNumberDuration")
fun ResponseEntity.BodyBuilder.serverTiming(vararg timingMetric: Triple<String, Number, String?>): ResponseEntity.BodyBuilder =
    header("Server-Timing", timingMetric.joinToString(", ") { "${it.first};dur=${it.second}" + if (it.third.isNotNull()) ";desc=${it.third}" else "" })