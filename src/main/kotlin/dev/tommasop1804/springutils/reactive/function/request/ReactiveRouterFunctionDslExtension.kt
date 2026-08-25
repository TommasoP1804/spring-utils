/*
 * Copyright © 2026 Tommaso Pastorelli (TommasoP1804) | Spring-Utils
 */

@file:JvmName("ReactiveRouterFunctionDslExtensionsKt")
@file:Suppress("unused", "FunctionName")
@file:Since("4.5.0")

package dev.tommasop1804.springutils.reactive.function.request

import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.annotations.*
import dev.tommasop1804.kutils.classes.web.HttpMethod.*
import dev.tommasop1804.springutils.reactive.function.*
import dev.tommasop1804.springutils.reactive.function.request.RequestPredicates.method
import dev.tommasop1804.springutils.reactive.function.request.RequestPredicates.methods
import org.springframework.web.reactive.function.server.RequestPredicate
import org.springframework.web.reactive.function.server.RouterFunctionDsl
import reactor.core.publisher.Mono

/**
 * Registers a route handler for both HTTP GET and POST methods and applies the given transformer function
 * to the incoming requests and outgoing responses.
 *
 * @param f A transformer function that takes a `Request` object and returns a `Response` object.
 * @since 4.5.0
 */
fun RouterFunctionDsl.GETOrPOST(f: Transformer<Request, Mono<out Response>>) = methods(Get, Post)(f)
/**
 * Adds a route to the handler with a URI pattern and allows handling requests of both GET and POST methods.
 *
 * @param pattern A string defining the URI pattern that this route should match. The pattern
 *                can include placeholders or wildcards as supported by the routing configuration.
 * @param f A transformer function that takes a `Request` and returns a `Response`, which processes
 *          the incoming request and produces a response.
 * @since 4.5.0
 */
fun RouterFunctionDsl.GETOrPOST(pattern: String, f: Transformer<Request, Mono<out Response>>) =
    (methods(Get, Post) and path(pattern))(f)
/**
 * Adds a route to the current routing function that handles both GET and POST requests
 * based on the given request predicate and handler function.
 *
 * @param predicate The predicate used to filter the incoming requests. Only requests that
 * match this predicate will be handled by the provided handler function.
 * @param f The handler function that processes the matched incoming requests and produces
 * a response.
 * @since 4.5.0
 */
fun RouterFunctionDsl.GETOrPOST(predicate: RequestPredicate, f: Transformer<Request, Mono<out Response>>) =
    (methods(Get, Post) and predicate)(f)
/**
 * Defines a custom DSL function that combines `GET` and `POST` HTTP methods with a given
 * path pattern and additional request predicates, then applies a transformation to the request and
 * response objects.
 *
 * @param pattern The URL path pattern to match incoming requests against.
 * @param predicate An additional `RequestPredicate` for filtering requests, beyond matching the path
 * pattern and HTTP methods.
 * @param f A `Transformer` that applies a transformation from `Request` to `Response`.
 * @since 4.5.0
 */
fun RouterFunctionDsl.GETOrPOST(pattern: String, predicate: RequestPredicate, f: Transformer<Request, Mono<out Response>>) =
    (methods(Get, Post) and path(pattern) and predicate)(f)
/**
 * Combines HTTP GET and POST methods with a specified URL path into a single routing predicate.
 *
 * This function creates a composite request predicate that matches requests using either
 * the GET or POST HTTP methods while also matching the specified path pattern.
 *
 * @param pattern The URL path pattern to match for the request. It defines the URI structure
 *                that this predicate will accept for both GET and POST methods.
 * @since 4.5.0
 */
fun RouterFunctionDsl.GETOrPOST(pattern: String) = (methods(Get, Post) and path(pattern))

/**
 * Combines the HTTP GET and QUERY methods into a single Spring predicate for request handling.
 *
 * @param f A transformer function that accepts a `Request` object and produces a `Response` object. This function
 *          defines the handling logic for requests matching the GET or QUERY methods.
 * @since 4.5.0
 */
fun RouterFunctionDsl.GETOrQUERY(f: Transformer<Request, Mono<out Response>>) = methods(Get, Query)(f)
/**
 * Registers a route with a combined HTTP method predicate for GET and QUERY methods,
 * and a path predicate matching the specified pattern.
 *
 * @param pattern The URI path pattern to match against incoming requests.
 * @param f A transformer function that processes a request of type `Request`
 *          and produces a response of type `Response`.
 * @since 4.5.0
 */
fun RouterFunctionDsl.GETOrQUERY(pattern: String, f: Transformer<Request, Mono<out Response>>) =
    (methods(Get, Query) and path(pattern))(f)
/**
 * Combines the GET and QUERY HTTP methods with a specified request predicate to define a route handler.
 * This method allows matching requests with either GET or QUERY methods, applying the given predicate for additional filtering.
 *
 * @param predicate A `RequestPredicate` used to filter the requests based on custom conditions.
 * @param f A transformer function that processes a `Request` and produces a `Response` for matching requests.
 * @since 4.5.0
 */
fun RouterFunctionDsl.GETOrQUERY(predicate: RequestPredicate, f: Transformer<Request, Mono<out Response>>) =
    (methods(Get, Query) and predicate)(f)
/**
 * Adds a route to the `RouterFunctionDsl` that matches HTTP GET or QUERY methods for a specific path
 * and satisfies a given request predicate before invoking the provided handler function.
 *
 * @param pattern The path pattern to match against incoming requests.
 * @param predicate A `RequestPredicate` used to refine the request matching logic.
 * @param f The handler function that processes the matched request and generates a response.
 * @since 4.5.0
 */
fun RouterFunctionDsl.GETOrQUERY(pattern: String, predicate: RequestPredicate, f: Transformer<Request, Mono<out Response>>) =
    (methods(Get, Query) and path(pattern) and predicate)(f)
/**
 * Combines HTTP GET and QUERY request methods with a specified path pattern into a request predicate.
 *
 * @param pattern The URI path pattern to match against incoming requests.
 * @since 4.5.0
 */
fun RouterFunctionDsl.GETOrQUERY(pattern: String) = (methods(Get, Query) and path(pattern))

/**
 * Adds a predicate for HTTP requests specifically matching the QUERY method.
 *
 * This function allows customizing the request handling logic for QUERY requests by applying
 * the specified transformation from a `Request` to a `Response`.
 *
 * @param f A transformer function that takes an instance of `Request` as input and
 *          produces a corresponding `Response`.
 * @since 4.5.0
 */
fun RouterFunctionDsl.QUERY(f: Transformer<Request, Mono<out Response>>) = method(Query)(f)
/**
 * Configures a query-based request predicate in a routing DSL.
 * This function matches HTTP requests with the specified query string pattern
 * and applies the provided transformer function to handle the request and generate a response.
 *
 * @param pattern The query string pattern to match against incoming requests.
 * @param f A transformer function that processes the request and generates a corresponding response.
 * @since 4.5.0
 */
fun RouterFunctionDsl.QUERY(pattern: String, f: Transformer<Request, Mono<out Response>>) =
    (method(Query) and path(pattern))(f)
/**
 * Adds a query-based HTTP request predicate to the current routing DSL configuration.
 * This function combines an HTTP method predicate fixed to `Query` and an additional
 * custom predicate to define a request matching constraint.
 *
 * @param predicate The custom `RequestPredicate` that further specifies the conditions
 *                  under which the request will match. Typically used to add filters
 *                  like query parameters or specific conditions.
 * @param f A `Transformer` function that transforms an incoming `Request` object
 *          into a `Response` object upon successful matching of the predicate.
 * @since 4.5.0
 */
fun RouterFunctionDsl.QUERY(predicate: RequestPredicate, f: Transformer<Request, Mono<out Response>>) =
    (method(Query) and predicate)(f)
/**
 * Defines a route that matches HTTP queries using the specified pattern, predicate, and transformation logic.
 *
 * @param pattern The URL pattern to match against. This is typically a string representation
 *                of the desired path in the query.
 * @param predicate A condition that must be satisfied for the query to match. It allows for
 *                  additional filtering and logic to be applied to the request.
 * @param f A transformation function that takes a `Request` and produces a `Response`.
 *          This function defines the behavior of the route when a request matches.
 * @since 4.5.0
 */
fun RouterFunctionDsl.QUERY(pattern: String, predicate: RequestPredicate, f: Transformer<Request, Mono<out Response>>) =
    (method(Query) and path(pattern) and predicate)(f)
/**
 * Combines an HTTP `QUERY` method predicate with a path-matching predicate.
 * Allows requests matching the `QUERY` HTTP method and the provided path pattern
 * to pass through this combined predicate.
 *
 * @param pattern The path pattern to be matched. This can include various
 *                path wildcards or static paths to define matching logic.
 * @since 4.5.0
 */
fun RouterFunctionDsl.QUERY(pattern: String) = (method(Query) and path(pattern))