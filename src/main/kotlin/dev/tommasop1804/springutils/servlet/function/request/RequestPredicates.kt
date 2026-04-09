/*
 * Copyright © 2026 Tommaso Pastorelli (TommasoP1804) | Spring-Utils
 */

package dev.tommasop1804.springutils.servlet.function.request

import dev.tommasop1804.kutils.classes.web.*
import dev.tommasop1804.springutils.*
import org.springframework.web.servlet.function.RequestPredicates as SpringPred

/**
 * Provides a set of predicates for matching and filtering HTTP requests. This class wraps
 * the functionality of Spring Framework's `RequestPredicates` with utility methods for
 * easier integration with custom types, like `HttpMethod` and `MediaType`.
 *
 * The predicates represented by the methods in this class can be used to define request
 * matching logic in web frameworks or HTTP request handling pipelines.
 * @since 3.3.0
 */
@Suppress("unused")
object RequestPredicates {
    /**
     * Creates a Spring predicate for the given HTTP method by converting it to the corresponding
     * Spring Framework HTTP method.
     *
     * @param method The `HttpMethod` to be converted and used for the predicate creation.
     * @return A Spring predicate based on the converted `HttpMethod`.
     * @since 3.3.0
     */
    fun method(method: HttpMethod) = SpringPred.method(method.toSpringHttpMethod())
    /**
     * Creates a Spring HTTP method-based predicate by converting the given `HttpMethod` values
     * to their corresponding Spring Framework `HttpMethod` representations.
     *
     * @param methods A vararg of `HttpMethod` instances that represent the HTTP methods to include
     *                in the resulting Spring predicate.
     * @since 3.3.0
     */
    fun methods(vararg methods: HttpMethod) = SpringPred.methods(
        *methods.map(HttpMethod::toSpringHttpMethod).toTypedArray()
    )

    /**
     * Creates a content type predicate for the provided media types.
     *
     * @param mediaTypes One or more `MediaType` instances representing the desired content types.
     * These are converted to Spring's `MediaType` objects and used to create the predicate.
     * @since 3.3.0
     */
    fun contentType(vararg mediaTypes: MediaType) = SpringPred.contentType(
        *mediaTypes.map(MediaType::toSpringMediaType).toTypedArray()
    )
    /**
     * Creates a predicate that matches requests with the specified media types.
     *
     * @param mediaTypes A variable number of custom `MediaType` objects that should be converted
     * to their Spring Framework counterparts and used to configure the predicate.
     * @since 3.3.0
     */
    fun accept(vararg mediaTypes: MediaType) = SpringPred.accept(
        *mediaTypes.map(MediaType::toSpringMediaType).toTypedArray()
    )

    /**
     * Configures a predicate that checks if the given HTTP header matches any of the specified values.
     *
     * @param header The HTTP header containing a name and a list of allowed values to check against.
     * @since 3.3.0
     */
    fun header(header: HttpHeader) = SpringPred.headers { headers ->
        headers.header(header.name).any { it in header.values }
    }
}