@file:JvmName("ClientUtilsKt")
@file:Since("2.3.0")
@file:Suppress("unused")

package dev.tommasop1804.springutils.servlet.client

import dev.tommasop1804.kutils.annotations.Since
import dev.tommasop1804.kutils.classes.web.HttpHeader
import dev.tommasop1804.kutils.classes.web.MediaType
import dev.tommasop1804.kutils.classes.web.MimeType
import dev.tommasop1804.springutils.FROM_SERVICE
import org.springframework.web.reactive.function.client.WebClient
import java.net.URL

/**
 * Sets the base URL for the WebClient during its construction.
 *
 * @param baseUrl The base URL to be used for all requests made by the WebClient.
 * @since 2.3.0
 */
fun WebClient.Builder.baseUrl(baseUrl: URL) = baseUrl(baseUrl.toString())

/**
 * Sets the default Content-Type header for the WebClient being built.
 *
 * @param contentType the media type to be used as the value of the Content-Type header
 * @since 2.3.0
 */
fun WebClient.Builder.contentType(contentType: MediaType) = defaultHeader(HttpHeader.CONTENT_TYPE, contentType.toString())
/**
 * Sets the default "Content-Type" header for requests made by the WebClient.
 *
 * @param contentType the MIME type to set as the default value for the "Content-Type" header
 * @since 2.3.0
 */
fun WebClient.Builder.contentType(contentType: MimeType) = defaultHeader(HttpHeader.CONTENT_TYPE, contentType.toString())

/**
 * Sets the "Accept" header to the specified media type for the WebClient builder.
 *
 * @param contentType the media type to be set as the value of the "Accept" header
 * @since 2.3.0
 */
fun WebClient.Builder.accept(contentType: MediaType) = defaultHeader(HttpHeader.ACCEPT, contentType.toString())
/**
 * Sets the "Accept" header with the specified MIME type for the WebClient requests.
 *
 * @param contentType the MIME type to set in the "Accept" header.
 * @since 2.3.0
 */
fun WebClient.Builder.accept(contentType: MimeType) = defaultHeader(HttpHeader.ACCEPT, contentType.toString())

/**
 * Sets the "From-Service" header for the WebClient requests.
 *
 * @param service The name of the service to set in the "From-Service" header.
 * @since 2.3.0
 */
fun WebClient.Builder.fromService(service: String) = defaultHeader(HttpHeader.FROM_SERVICE, service)
