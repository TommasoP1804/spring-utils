@file:JvmName("ClientUtilsKt")
@file:Since("2.3.0")
@file:Suppress("unused")

package dev.tommasop1804.springutils.servlet.client

import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.annotations.*
import dev.tommasop1804.kutils.classes.web.*
import dev.tommasop1804.springutils.*
import dev.tommasop1804.springutils.dsl.restclient.*
import io.micrometer.observation.ObservationRegistry
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatusCode
import org.springframework.http.client.ClientHttpRequestInitializer
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.observation.ClientRequestObservationConvention
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.web.client.RestClient

/**
 * Constructs a new `RestClient` instance with customizable configuration parameters.
 *
 * @param baseUrl A base `URL` for the client. If provided, the client will use this URL
 * as the base for all HTTP requests. Defaults to `null`.
 * @param statusHandler A pair consisting of a `Predicate<HttpStatusCode>` to identify error statuses
 * and an associated error handler. Defaults to a predicate identifying error statuses and a no-op handler.
 * @param contentType The default `MediaType` for the `Content-Type` header in requests. Defaults to `MediaType.APPLICATION_JSON`.
 * @param accept The default `MediaType` for the `Accept` header in requests. Defaults to `MediaType.APPLICATION_JSON`.
 * @param fromService An optional string identifying the source service of the requests. Defaults to `null`.
 * @param defaultHeaders Optional default HTTP headers to be included in every request. Defaults to `null`.
 * @param defaultApiVersion An optional default version to be included in requests for API versioning. Defaults to `null`.
 * @param defaultUriVariables A map of default URI variables to be used in URI templates. Defaults to an empty map.
 * @param defaultCookies A map containing default cookies to be included in every request. Defaults to an empty map.
 * @param defaultRequest A consumer to initialize or modify every request before execution. Defaults to `null`.
 * @param requestInterceptors A consumer to configure the list of client request interceptors. Defaults to `null`.
 * @param bufferContent A `BiPredicate` to determine whether content buffering is enabled for particular URIs and HTTP methods. Defaults to `null`.
 * @param requestInizializers A consumer to configure the list of client request initializers. Defaults to `null`.
 * @param messageConverters A consumer to configure the `HttpMessageConverters`. Defaults to `null`.
 * @param observationRegistry An optional `ObservationRegistry` to manage observation and metrics. Defaults to `null`.
 * @param observationConvention An optional `ClientRequestObservationConvention` for observation customization. Defaults to `null`.
 * @since 3.1.0
 */
fun RestClient(
    baseUrl: Url? = null,
    statusHandler: Pair<Predicate<HttpStatusCode>, RestClient.ResponseSpec.ErrorHandler>? = null,
    contentType: MediaType = MediaType.APPLICATION_JSON,
    accept: MediaType = MediaType.APPLICATION_JSON,
    fromService: String? = null,
    defaultHeaders: HttpHeaders? = null,
    defaultApiVersion: Any? = null,
    defaultUriVariables: Map<String, *>? = emptyMap<String, String>(),
    defaultCookies: MultiMap<String, String> = emptyMap(),
    defaultRequest: Consumer<RestClient.RequestHeadersSpec<*>>? = null,
    requestInterceptors: Consumer<List<ClientHttpRequestInterceptor>>? = null,
    bufferContent: BiPredicate<Uri, HttpMethod>? = null,
    requestInizializers: Consumer<List<ClientHttpRequestInitializer>>? = null,
    messageConverters: Consumer<HttpMessageConverters.ClientBuilder>? = null,
    observationRegistry: ObservationRegistry? = null,
    observationConvention: ClientRequestObservationConvention? = null
) = RestClient.builder()
    .also {
        if (baseUrl.isNotNull()) it.baseUrl(baseUrl)
    }
    .contentType(contentType)
    .accept(accept)
    .also { builder ->
        if (fromService.isNotNull()) builder.fromService(fromService)
        if (defaultHeaders.isNotNull()) builder.defaultHeaders { it.addAll(defaultHeaders.toSpringHttpHeaders()) }
        if (defaultApiVersion.isNotNull()) builder.defaultApiVersion(defaultApiVersion)
        if (defaultUriVariables.isNotNull()) builder.defaultUriVariables(defaultUriVariables)
        if (defaultCookies.isNotEmpty()) builder.defaultCookies { it.addAll(defaultCookies.toMultiValueMap()) }
        if (defaultRequest.isNotNull()) builder.defaultRequest(defaultRequest)
        if (requestInterceptors.isNotNull()) builder.requestInterceptors(requestInterceptors)
        if (bufferContent.isNotNull()) builder.bufferContent(bufferContent)
        if (requestInizializers.isNotNull()) builder.requestInitializers(requestInizializers)
        if (messageConverters.isNotNull()) builder.configureMessageConverters(messageConverters)
        if (observationRegistry.isNotNull()) builder.observationRegistry(observationRegistry)
        if (observationConvention.isNotNull()) builder.observationConvention(observationConvention)
    }.run {
        if (statusHandler.isNotNull()) defaultStatusHandler(statusHandler.first, statusHandler.second)
        else defaultStatusHandler(HttpStatusCode::isError) { _, _ -> }
    }.build()

/**
 * Constructs a new `RestClient` instance with customizable configuration parameters.
 *
 * @param baseUrl A base `URL` for the client. If provided, the client will use this URL
 * as the base for all HTTP requests. Defaults to `null`.
 * @param clientName A name for the client, useful for logging and monitoring. Defaults to `null`.
 * @param statusHandler A pair consisting of a `Predicate<HttpStatusCode>` to identify error statuses
 * and an associated error handler. Defaults to a predicate identifying error statuses and a no-op handler.
 * @param contentType The default `MediaType` for the `Content-Type` header in requests. Defaults to `MediaType.APPLICATION_JSON`.
 * @param accept The default `MediaType` for the `Accept` header in requests. Defaults to `MediaType.APPLICATION_JSON`.
 * @param fromService An optional string identifying the source service of the requests. Defaults to `null`.
 * @param defaultHeaders Optional default HTTP headers to be included in every request. Defaults to `null`.
 * @param defaultApiVersion An optional default version to be included in requests for API versioning. Defaults to `null`.
 * @param defaultUriVariables A map of default URI variables to be used in URI templates. Defaults to an empty map.
 * @param defaultCookies A map containing default cookies to be included in every request. Defaults to an empty map.
 * @param defaultRequest A consumer to initialize or modify every request before execution. Defaults to `null`.
 * @param requestInterceptors A consumer to configure the list of client request interceptors. Defaults to `null`.
 * @param bufferContent A `BiPredicate` to determine whether content buffering is enabled for particular URIs and HTTP methods. Defaults to `null`.
 * @param requestInizializers A consumer to configure the list of client request initializers. Defaults to `null`.
 * @param messageConverters A consumer to configure the `HttpMessageConverters`. Defaults to `null`.
 * @param observationRegistry An optional `ObservationRegistry` to manage observation and metrics. Defaults to `null`.
 * @param observationConvention An optional `ClientRequestObservationConvention` for observation customization. Defaults to `null`.
 * @since 3.1.1
 */
fun RestClient(
    baseUrl: Url? = null,
    clientName: String,
    statusHandler: Pair<Predicate<HttpStatusCode>, RestClient.ResponseSpec.ErrorHandler>? = null,
    contentType: MediaType = MediaType.APPLICATION_JSON,
    accept: MediaType = MediaType.APPLICATION_JSON,
    fromService: String? = null,
    defaultHeaders: HttpHeaders? = null,
    defaultApiVersion: Any? = null,
    defaultUriVariables: Map<String, *>? = emptyMap<String, String>(),
    defaultCookies: MultiMap<String, String> = emptyMap(),
    defaultRequest: Consumer<RestClient.RequestHeadersSpec<*>>? = null,
    requestInterceptors: Consumer<List<ClientHttpRequestInterceptor>>? = null,
    bufferContent: BiPredicate<Uri, HttpMethod>? = null,
    requestInizializers: Consumer<List<ClientHttpRequestInitializer>>? = null,
    messageConverters: Consumer<HttpMessageConverters.ClientBuilder>? = null,
    observationRegistry: ObservationRegistry? = null,
    observationConvention: ClientRequestObservationConvention? = null
) = ExtendedRestClient(RestClient(
    baseUrl = baseUrl,
    statusHandler = statusHandler,
    contentType = contentType,
    accept = accept,
    fromService = fromService,
    defaultHeaders = defaultHeaders,
    defaultApiVersion = defaultApiVersion,
    defaultUriVariables = defaultUriVariables,
    defaultCookies = defaultCookies,
    defaultRequest = defaultRequest,
    requestInterceptors = requestInterceptors,
    bufferContent = bufferContent,
    requestInizializers = requestInizializers,
    messageConverters = messageConverters,
    observationRegistry = observationRegistry,
    observationConvention = observationConvention
), clientName)

/**
 * Sets the base URL for the `RestClient.Builder`.
 *
 * This method defines the root URL that will be used for all requests made by the `RestClient`.
 *
 * @param baseUrl The base URL to be set, represented as a `Url` object.
 * @since 3.1.0
 */
fun RestClient.Builder.baseUrl(baseUrl: Url) = baseUrl(baseUrl.toString())

/**
 * Sets the `Content-Type` header for the REST client.
 *
 * This method sets a default `Content-Type` header for all requests prepared by the client.
 * The specified `MediaType` value will be converted to a string and applied as the value
 * of the `Content-Type` header.
 *
 * @param contentType The `MediaType` to be used as the value for the `Content-Type` header.
 * @since 3.1.0
 */
fun RestClient.Builder.contentType(contentType: MediaType) = defaultHeader(HttpHeader.CONTENT_TYPE, contentType.toString())
/**
 * Sets the default "Content-Type" header for the HTTP requests initiated by this RestClient builder.
 *
 * @param contentType The MIME type to set as the value for the "Content-Type" header.
 * @since 3.1.0
 */
fun RestClient.Builder.contentType(contentType: MimeType) = defaultHeader(HttpHeader.CONTENT_TYPE, contentType.toString())

/**
 * Configures the `Accept` header for the `RestClient.Builder` with the specified media types.
 *
 * This method sets the `Accept` header to indicate the media types that the client can handle as a response.
 * The provided `contentType` parameters are converted to their string representations and added as header values.
 *
 * @param contentType A vararg of `MediaType` values representing the media types to be added to the `Accept` header.
 * @since 3.1.0
 */
fun RestClient.Builder.accept(vararg contentType: MediaType) = defaultHeader(HttpHeader.ACCEPT, *contentType.map { it.toString() }.toTypedArray())

/**
 * Adds a `FROM_SERVICE` header to the request with the specified service name.
 *
 * This method is typically used to specify the originating service name for the request,
 * which can be helpful in tracing and identifying the source of API calls across systems.
 *
 * @param service The name of the originating service to be included in the `FROM_SERVICE` header.
 * @since 3.1.0
 */
fun RestClient.Builder.fromService(service: String) = defaultHeader(HttpHeader.FROM_SERVICE, service)

/**
 * Adds a default header to the HTTP client builder. The specified header will be included
 * in all requests made using the client.
 *
 * @param header The HTTP header to be added, consisting of a name and one or more values.
 * @since 3.1.0
 */
fun RestClient.Builder.defaultHeader(header: HttpHeader) = defaultHeader(header.name, *header.values.toTypedArray())
/**
 * Configures default HTTP headers to be used for every request made by the `RestClient`.
 *
 * This method extends the `RestClient.Builder` to allow setting default headers
 * by copying all header entries from the provided `HttpHeaders` instance to the
 * internal configuration of the `RestClient.Builder`.
 *
 * @param headers The `HttpHeaders` instance containing the headers to be set as default.
 * @since 3.1.0
 */
fun RestClient.Builder.defaultHeaders(headers: HttpHeaders) = defaultHeaders {
    it.addAll(headers.toSpringHttpHeaders())
}