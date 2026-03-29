@file:JvmName("ClientUtilsKt")
@file:Since("2.3.0")
@file:Suppress("unused")

package dev.tommasop1804.springutils.reactive.client

import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.annotations.*
import dev.tommasop1804.kutils.classes.coding.*
import dev.tommasop1804.kutils.classes.coding.Json.*
import dev.tommasop1804.kutils.classes.measure.*
import dev.tommasop1804.kutils.classes.web.*
import dev.tommasop1804.springutils.*
import io.micrometer.observation.ObservationRegistry
import org.springframework.core.ResolvableType
import org.springframework.http.HttpStatusCode
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.http.codec.json.JacksonJsonDecoder
import org.springframework.http.codec.json.JacksonJsonEncoder
import org.springframework.web.reactive.function.client.*
import reactor.core.publisher.Mono
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule

class JsonSupportingDecoder(mapper: JsonMapper) : JacksonJsonDecoder(mapper) {
    override fun canDecode(
        elementType: ResolvableType,
        mimeType: org.springframework.util.MimeType?
    ): Boolean {
        if (elementType.toClass() == Json::class.java)
            return true
        return super.canDecode(elementType, mimeType)
    }
}

/**
 * Constructs a fully customized instance of `WebClient` with the provided parameters.
 *
 * @param baseUrl The base URL to be used for all requests made by the WebClient. Default is `null`.
 * @param statusHandler A pair specifying a predicate to match HTTP status codes and a transformer to handle responses. Default is `HttpStatusCode::isError` paired with a transformer
 *  returning `Mono.empty()`.
 * @param contentType The default `Content-Type` header to be used, e.g., `application/json`. Default is `MediaType.APPLICATION_JSON`.
 * @param accept The `Accept` header indicating the media type expected in the response. Default is `MediaType.APPLICATION_JSON`.
 * @param fromService Identifies the service making the request for inclusion in the "From-Service" header. Default is `null`.
 * @param defaultHeaders The `HttpHeaders` to be included in every request. Default is `null`.
 * @param defaultApiVersion The API version to be used for all requests. Default is `null`.
 * @param defaultUriVariables The URI variables to pre-configure all default URIs. Default is an empty map.
 * @param defaultFilters A list of `ExchangeFilterFunction` objects applied to every request. Default is an empty list.
 * @param defaultCookies The default cookies to include in every request, represented as a `MultiMap`. Default is an empty map.
 * @param defaultRequest A consumer to customize default configurations for request headers. Default is `null`.
 * @param clientConnector A custom `ClientHttpConnector` to be used by the WebClient. Default is `null`.
 * @param codecs A consumer to configure codecs using the `ClientCodecConfigurer`. Default is `null`.
 * @param exchangeStrategies A custom `ExchangeStrategies` to define response strategies. Default is `null`.
 * @param exchangeFunction A custom `ExchangeFunction` for processing requests and responses. Default is `null`.
 * @param observationRegistry An `ObservationRegistry` for monitoring and observation purposes. Default is `null`.
 * @param observationConvention A `ClientRequestObservationConvention` to standardize conventions for request observations. Default is `null`.
 * @param maxInMemorySize The maximum size of the in-memory buffer for decoding responses. Default is `null`.
 * @since 2.3.6
 */
fun WebClient(
    baseUrl: Url? = null,
    statusHandler: Pair<Predicate<HttpStatusCode>, Transformer<ClientResponse, Mono<out Throwable>>> = HttpStatusCode::isError to { _ -> Mono.empty() },
    contentType: MediaType = MediaType.APPLICATION_JSON,
    accept: MediaType = MediaType.APPLICATION_JSON,
    fromService: String? = null,
    defaultHeaders: HttpHeaders? = null,
    defaultApiVersion: Any? = null,
    defaultUriVariables: Map<String, *>? = emptyMap<String, String>(),
    defaultFilters: List<ExchangeFilterFunction> = emptyList(),
    defaultCookies: MultiMap<String, String> = emptyMap(),
    defaultRequest: Consumer<WebClient.RequestHeadersSpec<*>>? = null,
    clientConnector: ClientHttpConnector? = null,
    codecs: Consumer<ClientCodecConfigurer>? = null,
    exchangeStrategies: ExchangeStrategies? = null,
    exchangeFunction: ExchangeFunction? = null,
    observationRegistry: ObservationRegistry? = null,
    observationConvention: ClientRequestObservationConvention? = null,
    maxInMemorySize: DataSize? = null,
) = WebClient.builder()
    .also {
        if (baseUrl.isNotNull()) it.baseUrl(baseUrl)
    }.defaultStatusHandler(statusHandler.first, statusHandler.second)
    .contentType(contentType)
    .accept(accept)
    .codecs {
        val mapper = Json.MAPPER.rebuild()
            .addModule(SimpleModule().apply {
                addSerializer(Json::class.java, Companion.Serializer())
                addDeserializer(Json::class.java, Companion.Deserializer())
            })
            .build()!!
        if (maxInMemorySize.isNotNull()) it.defaultCodecs().maxInMemorySize((maxInMemorySize convertTo MeasureUnit.DataSizeUnit.BYTE)().value.toInt())
        it.defaultCodecs().jacksonJsonEncoder(JacksonJsonEncoder(mapper))
        it.defaultCodecs().jacksonJsonDecoder(JacksonJsonDecoder(mapper))
        it.defaultCodecs().jacksonJsonDecoder(JsonSupportingDecoder(mapper))
    }.also { builder ->
        if (fromService.isNotNull()) builder.fromService(fromService)
        if (defaultHeaders.isNotNull()) builder.defaultHeaders { it.addAll(defaultHeaders.toSpringHttpHeaders()) }
        if (defaultApiVersion.isNotNull()) builder.defaultApiVersion(defaultApiVersion)
        if (defaultUriVariables.isNotNull()) builder.defaultUriVariables(defaultUriVariables)
        if (defaultFilters.isNotEmpty()) builder.filters { it.addAll(defaultFilters) }
        if (defaultCookies.isNotEmpty()) builder.defaultCookies { it.addAll(defaultCookies.toMultiValueMap()) }
        if (defaultRequest.isNotNull()) builder.defaultRequest(defaultRequest)
        if (clientConnector.isNotNull()) builder.clientConnector(clientConnector)
        if (codecs.isNotNull()) builder.codecs(codecs)
        if (exchangeStrategies.isNotNull()) builder.exchangeStrategies(exchangeStrategies)
        if (exchangeFunction.isNotNull()) builder.exchangeFunction(exchangeFunction)
        if (observationRegistry.isNotNull()) builder.observationRegistry(observationRegistry)
        if (observationConvention.isNotNull()) builder.observationConvention(observationConvention)
    }.build()

/**
 * Sets the base URL for the WebClient during its construction.
 *
 * @param baseUrl The base URL to be used for all requests made by the WebClient.
 * @since 2.3.6
 */
fun WebClient.Builder.baseUrl(baseUrl: Url) = baseUrl(baseUrl.toString())

/**
 * Sets the default Content-Type header for the WebClient being built.
 *
 * @param contentType the media type to be used as the value of the Content-Type header
 * @since 2.3.6
 */
fun WebClient.Builder.contentType(contentType: MediaType) = defaultHeader(HttpHeader.CONTENT_TYPE, contentType.toString())
/**
 * Sets the default "Content-Type" header for requests made by the WebClient.
 *
 * @param contentType the MIME type to set as the default value for the "Content-Type" header
 * @since 2.3.6
 */
fun WebClient.Builder.contentType(contentType: MimeType) = defaultHeader(HttpHeader.CONTENT_TYPE, contentType.toString())

/**
 * Sets the "Accept" header to the specified media type for the WebClient builder.
 *
 * @param contentType the media type to be set as the value of the "Accept" header
 * @since 2.3.6
 */
fun WebClient.Builder.accept(vararg contentType: MediaType) = defaultHeader(HttpHeader.ACCEPT, *contentType.map { it.toString() }.toTypedArray())

/**
 * Sets the "From-Service" header for the WebClient requests.
 *
 * @param service The name of the service to set in the "From-Service" header.
 * @since 2.3.6
 */
fun WebClient.Builder.fromService(service: String) = defaultHeader(HttpHeader.FROM_SERVICE, service)

/**
 * Extends the `WebClient.Builder` to set default headers for all requests.
 *
 * @param header The `HttpHeader` instance containing the name and values
 *               of the header to be added by default to every request.
 * @since 2.3.6
 */
fun WebClient.Builder.defaultHeader(header: HttpHeader) = defaultHeader(header.name, *header.values.toTypedArray())
/**
 * Configures default headers for the `WebClient.Builder` instance by adding all headers
 * from the provided `HttpHeaders` object.
 *
 * This function utilizes the `toSpringHttpHeaders` extension function to convert the given
 * `HttpHeaders` into an instance of `org.springframework.http.HttpHeaders`. The resulting
 * headers are then added to the builder's default headers configuration.
 *
 * @param headers The `HttpHeaders` instance containing the headers to be set as default.
 * @since 2.3.6
 */
fun WebClient.Builder.defaultHeaders(headers: HttpHeaders) = defaultHeaders {
    it.addAll(headers.toSpringHttpHeaders())
}
