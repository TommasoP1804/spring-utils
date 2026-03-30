/*
 * Copyright © 2026 Tommaso Pastorelli | spring-utils
 */

@file:JvmName("RestAssuredExtensionKt")
@file:Since("3.2.0")
@file:Suppress("unused")

package dev.tommasop1804.springutils.restassured

import dev.tommasop1804.kutils.annotations.*
import dev.tommasop1804.kutils.classes.time.Duration.Companion.asMillisOfDuration
import dev.tommasop1804.kutils.classes.web.*
import dev.tommasop1804.kutils.classes.web.HttpStatus.Companion.toHttpStatus
import io.restassured.http.Header
import io.restassured.http.Headers
import io.restassured.mapper.ObjectMapper
import io.restassured.mapper.ObjectMapperType
import io.restassured.response.ExtractableResponse
import io.restassured.response.ResponseBody
import io.restassured.response.ResponseOptions
import io.restassured.response.ValidatableResponseOptions
import io.restassured.specification.RequestSpecification

/**
 * Specifies the content type of the request using the provided media type.
 *
 * @param mediaType The MediaType object representing the desired content type.
 * @return The updated RequestSpecification with the specified content type.
 * @since 3.2.0
 */
fun RequestSpecification.contentType(mediaType: MediaType): RequestSpecification = contentType(mediaType.toString())
/**
 * Sets the Content-Type header of the request specification to the provided MIME type.
 *
 * @param mimeType The MIME type to set as the Content-Type header.
 * @return The updated RequestSpecification instance.
 * @since 3.2.0
 */
fun RequestSpecification.contentType(mimeType: MimeType): RequestSpecification = contentType(mimeType.toString())
/**
 * Adds the specified HTTP header to the request specification.
 *
 * @param header the HTTP header to add, including its name and values.
 * @return the updated request specification with the added header.
 * @since 3.2.0
 */
fun RequestSpecification.header(header: HttpHeader): RequestSpecification = header(Header(header.name, header.values.joinToString()))
/**
 * Adds the provided HTTP headers to the request specification.
 *
 * @param headers the HTTP headers to be added to the request. Each header can have one or more values.
 * @return the updated request specification with the added headers.
 * @since 3.2.0
 */
fun RequestSpecification.headers(headers: HttpHeaders): RequestSpecification = headers(Headers().apply {
    headers.forEach { [name, values] ->
        values.forEach { value ->
            plus(Header(name, values.joinToString()))
        }
    }
})

/**
 * Sets the status code for this validatable response options instance using the provided HttpStatus.
 *
 * @param status The HttpStatus value to set as the status code.
 * @return The modified validatable response options instance.
 * @since 3.2.0
 */
fun <T : ValidatableResponseOptions<T, R>, R> ValidatableResponseOptions<T, R>.status(status: HttpStatus): T where R : ResponseBody<R>, R : ResponseOptions<R> =
    statusCode(status.value)
/**
 * Adds the provided headers to the request.
 *
 * @param headers The HTTP headers to add, represented as an instance of HttpHeaders.
 * @return The updated instance of ValidatableResponseOptions with the added headers.
 * @since 3.2.0
 */
fun <T : ValidatableResponseOptions<T, R>, R> ValidatableResponseOptions<T, R>.headers(headers: HttpHeaders): T where R : ResponseBody<R>, R : ResponseOptions<R> =
    headers(headers.toMap().mapValues { it.value.joinToString() })
/**
 * Adds a header to the validatable response options.
 *
 * @param header The HTTP header to be added, including its name and associated values.
 * @return The updated validatable response options.
 * @since 3.2.0
 */
fun <T : ValidatableResponseOptions<T, R>, R> ValidatableResponseOptions<T, R>.header(header: HttpHeader): T where R : ResponseBody<R>, R : ResponseOptions<R> =
    header(header.name, header.values.joinToString())
/**
 * Validates that the response has the specified content type.
 *
 * @param mediaType The expected media type of the response.
 * @return The current validatable response options, allowing for further validation chaining.
 * @since 3.2.0
 */
fun <T : ValidatableResponseOptions<T, R>, R> ValidatableResponseOptions<T, R>.contentType(mediaType: MediaType): T where R : ResponseBody<R>, R : ResponseOptions<R> =
    contentType(mediaType.toString())
/**
 * Asserts that the response content type matches the specified MIME type.
 *
 * @param mimeType The expected MIME type to validate against the response content type.
 * @return The same validatable response options instance, allowing for method chaining.
 * @since 3.2.0
 */
fun <T : ValidatableResponseOptions<T, R>, R> ValidatableResponseOptions<T, R>.contentType(mimeType: MimeType): T where R : ResponseBody<R>, R : ResponseOptions<R> =
    contentType(mimeType.toString())

/**
 * Extracts and deserializes the response body into the specified type.
 *
 * This function uses reified generics to infer the target type for deserialization.
 * It fetches the response body as a string and converts it into an instance of the given type.
 *
 * @return The deserialized response body of type T.
 * @since 3.2.0
 */
inline fun <reified T, R : ResponseOptions<R>> ExtractableResponse<R>.body(): T = body().`as`(T::class.java)
/**
 * Extracts and deserializes the response body to the specified type using the given ObjectMapperType.
 *
 * @param T the type to which the response body will be deserialized.
 * @param R the response options type of which this ExtractableResponse is an instance.
 * @param mapperType the ObjectMapperType to use for deserialization.
 * @return the deserialized response body of type T.
 * @since 3.2.0
 */
inline fun <reified T, R : ResponseOptions<R>> ExtractableResponse<R>.body(mapperType: ObjectMapperType): T = body().`as`(T::class.java, mapperType)
/**
 * Extracts and deserializes the body of the response into the specified type using the provided ObjectMapper.
 *
 * @param T The type into which the response body will be deserialized.
 * @param R The type of the response options.
 * @param mapper The ObjectMapper instance used for deserialization.
 * @return The deserialized response body of type T.
 * @since 3.2.0
 */
inline fun <reified T, R : ResponseOptions<R>> ExtractableResponse<R>.body(mapper: ObjectMapper): T = body().`as`(T::class.java, mapper)
/**
 * Provides the headers of the response as an instance of [HttpHeaders].
 *
 * This property parses the headers of the underlying response into a key-value structure.
 * Each header name is mapped to a list of its corresponding values, splitting the values
 * by commas if multiple values are present.
 * @since 3.2.0
 */
val <R : ResponseOptions<R>> ExtractableResponse<R>.headers get(): HttpHeaders {
    val result = HttpHeaders()
    headers().asList().forEach { result[it.name] = it.value.split(", ") }
    return result
}
/**
 * Retrieves the HTTP status of the response as an instance of `HttpStatus`.
 *
 * This property allows for a more idiomatic way to access the HTTP status code
 * of the response in a converted form, leveraging the `toHttpStatus()` function.
 * @since 3.2.0
 */
val <R : ResponseOptions<R>> ExtractableResponse<R>.status get() = statusCode().toHttpStatus()
/**
 * Retrieves the time duration in milliseconds taken for the response to be received.
 *
 * The property provides access to the time information of the corresponding response
 * and converts it to the duration in milliseconds for easier inspection or usage.
 * @since 3.2.0
 */
val <R : ResponseOptions<R>> ExtractableResponse<R>.time get() = time().asMillisOfDuration()