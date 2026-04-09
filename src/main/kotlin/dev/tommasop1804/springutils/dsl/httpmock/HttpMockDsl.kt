/*
 * Copyright © 2026 Tommaso Pastorelli (TommasoP1804) | Spring-Utils
 */

@file:Suppress("unused")
@file:Since("3.1.0")

package dev.tommasop1804.springutils.dsl.httpmock

import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.annotations.*
import dev.tommasop1804.kutils.classes.time.*
import dev.tommasop1804.kutils.classes.web.*
import dev.tommasop1804.kutils.classes.web.HttpHeader.Companion.CONTENT_TYPE

// --- MARKER ---

@DslMarker
annotation class HttpMockDslMarker

// --- MODEL ---

/**
 * Represents a stub definition for a mock HTTP API. A stub defines the behavior of the mock by
 * mapping a request to one or more potential responses, with optional configuration for priority
 * and scenario handling.
 *
 * @property request The request matcher used to determine if the incoming request should trigger this stub.
 * @property responses A list of response definitions that can be returned for the stub. The response to be
 * selected is typically determined by the implementation.
 * @property priority An integer representing the priority of this stub compared to other stubs. Higher values
 * indicate higher priority during request matching.
 * @property scenario Optional scenario configuration that allows the stubs to model state-dependent behavior.
 * This supports complex workflows where responses may depend on the current state of the mock API.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
data class StubDefinition(
    val request: RequestMatcher,
    val responses: List<ResponseDefinition>,
    val priority: Int,
    val scenario: ScenarioConfig?,
)

/**
 * Represents the criteria used to match an incoming HTTP request.
 *
 * This class is used for defining the rules to evaluate whether an HTTP request satisfies
 * the conditions specified for a stubbed response or mock in HTTP testing or mocking scenarios.
 *
 * @property method Specifies the HTTP method (e.g., GET, POST) to match. Can be null to indicate
 * that the method does not factor into the matching logic.
 * @property path Specifies the exact request path to match. Can be null if the path is not part
 * of the matching criteria.
 * @property pathPattern Specifies a regular expression pattern for matching the request path. Can
 * be null to ignore path pattern matching.
 * @property queryParams A map of query parameter names and their respective matchers, defining how
 * query parameters should be evaluated for matching.
 * @property headers A map of HTTP headers to their expected values, specifying the conditions
 * under which a request header matches.
 * @property bodyPatterns A list of patterns or conditions that the request body must satisfy to
 * be considered a match.
 * @property pathParams A map of path parameter names to their expected values, used for matching
 * dynamic URL segments.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
data class RequestMatcher(
    val method: HttpMethod?,
    val path: Uri?,
    val pathPattern: Regex?,
    val queryParams: Map<String, ParameterMatcher>,
    val headers: StringMap,
    val bodyPatterns: List<BodyPattern>,
    val pathParams: StringMap,
)

/**
 * Represents a matcher for an individual parameter in a request. This can be used
 * to define constraints or conditions to determine whether a parameter matches.
 *
 * @property equalTo Specifies that the parameter's value must be equal to the given string.
 * @property contains Specifies that the parameter's value must contain the given substring.
 * @property matches Specifies that the parameter's value must match the given regular expression.
 * @property absent Specifies whether the parameter must be absent. If true, it indicates
 * that the parameter should not be present in the request.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
data class ParameterMatcher(
    val equalTo: String? = null,
    val contains: String? = null,
    val matches: Regex? = null,
    val absent: Boolean = false,
)

/**
 * Represents a pattern used to match the body of an HTTP request.
 *
 * This sealed interface standardizes the structure of various body-matching strategies
 * such as exact JSON matches, JSONPath evaluations, substring matches, and regex evaluations.
 * Classes implementing this interface specify the logic for different matching criteria.
 *
 * Implementations of this interface include:
 * - JsonBodyEquals: Matches when the request body exactly matches a specific JSON string.
 * - JsonPathExists: Matches when a specified JSONPath expression exists in the request body.
 * - JsonPathEquals: Matches when a specified JSONPath expression evaluates to a particular value.
 * - BodyContains: Matches when the request body contains a specific substring.
 * - BodyMatches: Matches when the request body matches a given regular expression.
 *
 * Used primarily in conjunction with the `RequestMatcher` class and its builder
 * to define request expectations in a declarative DSL.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
sealed interface BodyPattern
/**
 * Represents a body pattern for request matching where the body is expected to match
 * a specific JSON structure.
 *
 * This data class is primarily used to compare the JSON payload of an incoming HTTP
 * request with a predefined JSON string representation.
 *
 * @property json The JSON string to compare against the incoming request body.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
data class JsonBodyEquals(val json: String) : BodyPattern
/**
 * Represents a pattern for checking the existence of a JSON path in a request body.
 *
 * This class is used to verify the presence of a JSON path in the request body during
 * HTTP request matching. It ensures that the JSON path expression specified in [expression]
 * exists within the request payload, regardless of its associated value.
 *
 * @property expression The JSON path expression to check for existence.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
data class JsonPathExists(val expression: String) : BodyPattern
/**
 * Represents a body pattern that asserts the existence and expected value of a JSON path expression.
 *
 * This class is used to verify that a specified JSON path `expression` exists within the request body
 * and that the value associated with this path matches the provided `value`.
 *
 * The `expression` follows the JSONPath standard syntax for querying specific parts of a JSON structure.
 * The `value` represents the expected value at the specified JSON path.
 *
 * Example use case includes enforcing assertions on JSON payloads in HTTP request matching logic.
 *
 * @property expression The JSON path expression to evaluate within the JSON payload.
 * @property value The expected value that must be present at the JSON path defined by `expression`.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
data class JsonPathEquals(val expression: String, val value: Any) : BodyPattern
/**
 * Represents a condition for matching requests based on whether the body contains a specific text.
 *
 * This class is part of the `BodyPattern` hierarchy, which is used to describe patterns
 * that must match the body of an HTTP request for it to satisfy a given request matcher.
 *
 * @property text The text that must be present in the body of the request for it to match.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
data class BodyContains(val text: String) : BodyPattern
/**
 * A body pattern that checks if the request body matches the specified regular expression.
 *
 * This is commonly used in scenarios where the exact content of the request body is
 * dynamic but can be validated against a specific pattern. The regular expression
 * allows users to specify flexible validation rules.
 *
 * @property regex The regular expression used to validate the request body.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
data class BodyMatches(val regex: Regex) : BodyPattern

/**
 * Represents the definition of an HTTP response, including its status, headers, body content,
 * potential delay, and optional fault injection. This data class is primarily used to configure
 * mock responses within the HTTP mocking framework.
 *
 * @property status The HTTP status code to be returned in the response.
 * @property headers A map containing the headers to be included in the response.
 * @property body The body content of the response, which can be null if no body is required.
 * @property delay An optional delay before the response is returned, to simulate network latency.
 * @property fault An optional fault type that can simulate specific failure scenarios, such as
 * connection resets or malformed responses.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
data class ResponseDefinition(
    val status: HttpStatus,
    val headers: StringMap,
    val body: String?,
    val delay: Duration?,
    val fault: FaultType?,
)

/**
 * Represents different types of faults that can occur during the processing of a response.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
enum class FaultType {
    /**
     * Indicates that the connection was unexpectedly closed by the remote host,
     * resulting in a failure to complete the intended operation.
     * @since 3.1.0
     */
    CONNECTION_RESET,
    /**
     * Represents a fault scenario where a response is missing or entirely empty.
     * @since 3.1.0
     */
    EMPTY_RESPONSE,
    /**
     * Represents a fault type indicating that the response received does not conform to the
     * expected format or structure. This type of fault typically occurs when the data returned
     * from an external source is corrupted, incomplete, or organized in an invalid manner.
     * @since 3.1.0
     */
    MALFORMED_RESPONSE,
    /**
     * Represents a fault type indicating that random or nonsensical data
     * was encountered in a response where meaningful data was expected.
     * @since 3.1.0
     */
    RANDOM_DATA,
}

/**
 * Represents the configuration for a scenario which is used to define state transitions
 * within a request/response interaction system.
 *
 * @property name The name of the scenario.
 * @property requiredState The state that must be active for the scenario to be considered applicable.
 * If null, the scenario will apply irrespective of the current state.
 * @property newState The new state to transition to upon execution of the scenario.
 * If null, the state will remain unchanged.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
data class ScenarioConfig(
    val name: String,
    val requiredState: String?,
    val newState: String?,
)

/**
 * Represents criteria for verifying HTTP interactions in a mock API context.
 *
 * @property method The HTTP method being verified, such as GET, POST, PUT, etc.
 * @property path The URI path being verified. It can be null if the path is not specified in the criteria.
 * @property count A matcher that specifies the expected number of interactions.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
data class VerificationCriteria(
    val method: HttpMethod?,
    val path: Uri?,
    val count: CountMatcher,
)

/**
 * Represents a mechanism to define matching criteria for a count constraint.
 * This sealed interface is used to describe various ways to match the occurrence
 * count of an event or an operation.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
sealed interface CountMatcher {
    /**
     * Represents a matcher for verifying that a method is called exactly a specified number of times.
     *
     * @property n The exact number of times the method is expected to be called.
     * @since 3.1.0
     * @author Tommaso Pastorelli
     */
    data class Exactly(val n: Int) : CountMatcher
    /**
     * Represents a condition that the count of occurrences must be at least a specified number.
     *
     * @property n The minimum number of occurrences required.
     * @since 3.1.0
     * @author Tommaso Pastorelli
     */
    data class AtLeast(val n: Int) : CountMatcher
    /**
     * Represents a condition where a count should not exceed a specified maximum value.
     *
     * @property n The maximum allowed value for the count.
     * @since 3.1.0
     * @author Tommaso Pastorelli
     */
    data class AtMost(val n: Int) : CountMatcher
    /**
     * Represents a count matcher that ensures the number of occurrences falls within a specific range.
     *
     * This class is used to define a range with a minimum and maximum bound for verifying how many times
     * an operation or event occurred. The `min` value specifies the inclusive lower limit,
     * and the `max` value specifies the inclusive upper limit of the range.
     *
     * @property min The inclusive minimum number of occurrences allowed.
     * @property max The inclusive maximum number of occurrences allowed.
     * @since 3.1.0
     * @author Tommaso Pastorelli
     */
    data class Between(val min: Int, val max: Int) : CountMatcher
    /**
     * A singleton implementation of the [CountMatcher] interface that represents a condition where a specific event
     * or action is expected to happen zero times.
     *
     * This is typically used in scenarios where an operation or method invocation must never occur.
     *
     * Example usage includes constructing verification criteria in contexts where assertions are being made
     * about the absence of certain calls or interactions.
     * @since 3.1.0
     * @author Tommaso Pastorelli
     */
    data object Never : CountMatcher
}

// --- MOCKAPI RESULT (HOLDS STUBS + VERIFICATIONS) ---

/**
 * Represents the definition of a mock API, including its stubs and verification criteria.
 *
 * This class is a central configuration unit for defining the behavior and verification expectations
 * of a mock HTTP API. It allows users to specify the stubs that describe how the API responds to
 * specific requests, as well as the criteria used to verify the interactions with the API.
 *
 * @property stubs A list of stub definitions that specify how the mock API responds to different requests.
 * Each stub maps incoming requests to corresponding responses, potentially based on various matching
 * and priority rules.
 * @property verifications A list of verification criteria that describe the expected interactions with
 * the mock API, such as the HTTP methods, paths, and the number of occurrences.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
data class MockApiDefinition(
    val stubs: List<StubDefinition>,
    val verifications: List<VerificationCriteria>,
)

// --- JSON BUILDER (LIGHTWEIGHT, NO JACKSON NEEDED) ---

/**
 * A DSL builder class for constructing JSON objects programmatically.
 *
 * This class is used to define JSON structures as key-value pairs where
 * the value can be another JSON object, an array, or a primitive value.
 * The JSON object can be represented as a string using the `toJsonString` method.
 *
 * The builder is designed to work with custom DSLs for creating mock HTTP requests
 * and responses, enabling a fluent API for JSON construction.
 *
 * This class is annotated with `@HttpMockDslMarker` to ensure proper DSL scoping.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
@HttpMockDslMarker
class JsonBuilder {
    /**
     * Holds a mutable list of key-value pairs where the key is a string and the value can be nullable.
     *
     * This property is used internally for constructing JSON-like structures. Entries are dynamically
     * added using functions such as `obj`, `array`, and processed to generate the JSON string representation.
     * The data structure supports nesting via `JsonBuilder` and `JsonArrayBuilder`.
     * @since 3.1.0
     */
    private val entries = emptyMList<Pair<String, Any?>>()

    /**
     * Adds a key-value pair to the JSON structure where the value is defined by the DSL block.
     *
     * @param key The key associated with the JSON object to be created.
     * @param block A DSL block to define the structure and content of the JSON object.
     * @since 3.1.0
     */
    fun obj(key: String, block: JsonBuilder.() -> Unit) {
        entries += key to JsonBuilder().apply(block)
    }

    /**
     * Adds a key-value pair to the JSON structure, where the value is represented as an array.
     *
     * @param key The key associated with the array.
     * @param items A variable number of items to be included in the array.
     * @since 3.1.0
     */
    fun array(key: String, vararg items: Any?) {
        entries += key to items.toList()
    }

    /**
     * Adds a JSON array associated with the given key to the builder and applies the specified block to configure the array.
     *
     * @param key The key to associate with the JSON array.
     * @param block A lambda function applied to a [JsonArrayBuilder] to configure the contents of the array.
     * @since 3.1.0
     */
    fun array(key: String, block: JsonArrayBuilder.() -> Unit) {
        entries += key to JsonArrayBuilder().apply(block).items
    }

    /**
     * Converts the internal structure of the `JsonBuilder` instance into a JSON-formatted string.
     *
     * The resulting string is a serialized JSON representation of the key-value pairs stored
     * within the builder. Nested objects and arrays are serialized recursively to ensure
     * a complete JSON output.
     *
     * @return A JSON-formatted string representation of the `JsonBuilder` content.
     * @since 3.1.0
     */
    fun toJsonString(): String = buildString {
        append("{")
        entries.forEachIndexed { i, [key, `value`] ->
            if (i > 0) append(", ")
            append("\"$key\": ${renderValue(value)}")
        }
        append("}")
    }

    /**
     * Renders the given value as a JSON-compatible string representation.
     *
     * @param value The value to be rendered. It can be of type `String`, `Number`, `Boolean`, `JsonBuilder`,
     *              `List`, `Map`, or `null`. Any other type will be rendered as a string wrapped in quotes.
     * @return A JSON-compatible string representation of the given value.
     * @since 3.1.0
     */
    private fun renderValue(value: Any?): String = when (value) {
        null -> "null"
        is String -> "\"${value.replace("\"", "\\\"")}\""
        is Number, is Boolean -> value.toString()
        is JsonBuilder -> value.toJsonString()
        is List<*> -> "[${value.joinToString(", ") { renderValue(it) }}]"
        is Map<*, *> -> {
            val b = JsonBuilder()
            value.forEach { [k, v] -> b.entries += (k.toString() to v) }
            b.toJsonString()
        }
        else -> "\"$value\""
    }
}

/**
 * A builder class for constructing JSON arrays. This class allows adding individual items or
 * JSON objects to the array.
 *
 * The built JSON array can contain elements of different types, including primitive values,
 * nulls, and complex objects.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
@HttpMockDslMarker
class JsonArrayBuilder {
    /**
     * A mutable list of items representing the elements of a JSON array
     * being constructed within a DSL context. Each item can be of any type,
     * including null, making it suitable for constructing diverse JSON structures.
     *
     * This property is primarily used internally by the `JsonArrayBuilder` class
     * to aggregate items added through various DSL functions such as `item()` and `obj()`.
     *
     * The `items` list is constructed using a custom implementation of `MList`
     * to allow efficient operations and flexibility in handling JSON elements.
     * @since 3.1.0
     */
    val items: MList<Any?> = emptyMList()

    /**
     * Adds the specified value to the collection of items in the JSON array.
     *
     * @param value The value to be added to the JSON array. This can be any type or null.
     * @since 3.1.0
     */
    fun item(value: Any?) { items += value }
    /**
     * Adds a new `JsonBuilder` object to the current context by applying the provided DSL block.
     *
     * @param block A lambda expression with a receiver of type `JsonBuilder` that defines the structure
     * of the JSON object to be added.
     * @since 3.1.0
     */
    fun obj(block: ReceiverConsumer<JsonBuilder>) { items += JsonBuilder().apply(block) }
}

// --- REQUEST MATCHER BUILDER ---

/**
 * A builder class for constructing instances of `RequestMatcher` used to define
 * constraints or conditions for HTTP request matching in a declarative way.
 *
 * This class is annotated with `@HttpMockDslMarker` to support a DSL-style syntax
 * for building request matchers. It provides properties and methods to specify
 * various aspects of an HTTP request, such as method, path, query parameters,
 * headers, and body matching patterns.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
@HttpMockDslMarker
class RequestMatcherBuilder {
    /**
     * Represents the HTTP method associated with a request.
     *
     * This variable holds the HTTP method (e.g., GET, POST, PUT) to be used or processed.
     * It is nullable, meaning it can hold a null value if no method is explicitly assigned.
     * @since 3.1.0
     */
    var method: HttpMethod? = null
    /**
     * Defines the specific path of an HTTP request to be matched.
     *
     * This variable is used to represent the target endpoint or resource path
     * within the request matching logic. The value can be null, indicating that
     * no explicit path matching is required.
     * @since 3.1.0
     */
    var path: Uri? = null
    /**
     * A regular expression pattern used to match the request path.
     *
     * This variable allows the specification of a flexible matching rule for the path
     * of an incoming HTTP request. By assigning a regex to this property, requests can
     * be matched against paths that conform to the defined pattern.
     *
     * If set to `null`, no regex matching is applied to the request path.
     * @since 3.1.0
     */
    var pathPattern: Regex? = null
    /**
     * Represents a collection of query parameter matchers used to define rules for matching
     * query parameters in HTTP requests.
     *
     * This mutable map stores the conditions associated with each query parameter name,
     * allowing fine-grained control over the validation of query parameters in an
     * HTTP request.
     *
     * Key: The name of the query parameter as a string.
     * Value: A `ParameterMatcher` instance specifying the matching criteria (e.g., exact match,
     * substring presence, regex match, or absence).
     *
     * Query parameter matchers can be added or modified using functions like `queryParam`,
     * `queryParamContains`, `queryParamMathes`, and `queryParamAbsent`.
     * @since 3.1.0
     */
    val queryParams = emptyMMap<String, ParameterMatcher>()
    /**
     * Represents the headers used to match an HTTP request.
     *
     * This property is used to define constraints or conditions for the headers
     * of an incoming HTTP request. Each header can have one or more values,
     * and these values are matched based on the specified criteria during
     * the request matching process.
     *
     * Headers can be added, modified, or inspected as part of building
     * a request matcher using the DSL.
     * @since 3.1.0
     */
    val headers: HttpHeaders = HttpHeaders()
    /**
     * A mutable list of `BodyPattern` objects used to define conditions for matching the
     * body of an HTTP request in the context of a request matcher.
     *
     * This property accumulates various types of body-matching patterns, including:
     * - Exact JSON matches, using `JsonBodyEquals`.
     * - JSONPath conditions, using `JsonPathExists` and `JsonPathEquals`.
     * - Substring matches, using `BodyContains`.
     * - Regular expression matches, using `BodyMatches`.
     *
     * These patterns are applied as part of the request matching logic for HTTP mock definitions.
     * @since 3.1.0
     */
    val bodyPatterns = emptyMList<BodyPattern>()
    /**
     * Represents a collection of path parameters used to match or configure an HTTP request.
     *
     * This property holds a mutable map-like structure where keys are the names of path parameters
     * and values are their corresponding values. Path parameters are often utilized to define
     * dynamic segments in an HTTP request path, enabling flexible matching or routing.
     * @since 3.1.0
     */
    val pathParams: StringMMap = emptyMMap()

    /**
     * Configures the HTTP method of the request matcher to `GET`.
     *
     * This function is typically used in the context of building a request matcher
     * through a DSL. When invoked, it sets the HTTP method of the matcher to `GET`,
     * allowing it to match HTTP `GET` requests.
     * @since 3.1.0
     */
    fun get() { method = HttpMethod.GET }
    /**
     * Sets the HTTP method of the request matcher to `POST`.
     *
     * This function is part of the `RequestMatcherBuilder` DSL and updates the HTTP method
     * to specifically match requests using the POST method. It can be used to define
     * behavior or expectations for HTTP POST requests in the context of the request matching logic.
     * @since 3.1.0
     */
    fun post() { method = HttpMethod.POST }
    /**
     * Sets the HTTP method of the request matcher to `PUT`.
     *
     * This function is part of the `RequestMatcherBuilder` class and is used to define the HTTP
     * method for the incoming requests that the matcher should target. By invoking this function,
     * you specify that the matcher should filter and respond to requests using the `PUT` HTTP method.
     * @since 3.1.0
     */
    fun put() { method = HttpMethod.PUT }
    /**
     * Configures the request matcher to use the HTTP DELETE method.
     *
     * This method is used to specify that the HTTP request being matched
     * must utilize the DELETE method. It sets the request's `method`
     * property to `HttpMethod.DELETE` within the request matcher.
     * @since 3.1.0
     */
    fun delete() { method = HttpMethod.DELETE }
    /**
     * Sets the HTTP method to PATCH for the request being built.
     *
     * This function modifies the internal state of the `RequestMatcherBuilder` instance
     * by assigning the `HttpMethod.PATCH` value to the `method` field. It is typically
     * used in DSL-style request matchers to specify that the HTTP request being defined
     * should use the PATCH method.
     * @since 3.1.0
     */
    fun patch() { method = HttpMethod.PATCH }

    /**
     * Adds or updates a path parameter in the request matcher configuration.
     *
     * This method allows you to specify a path parameter by providing its name
     * and value. The path parameter is added to the internal map of parameters
     * used for request matching.
     *
     * @param name The name of the path parameter to be added or updated.
     * @param value The value of the path parameter corresponding to the specified name.
     * @since 3.1.0
     */
    fun pathParam(name: String, value: String) { pathParams[name] = value }

    /**
     * Adds a query parameter to the request matcher with an exact value match.
     *
     * This method allows specifying that a specific query parameter must exist in the
     * request and its value must exactly match the provided value.
     *
     * @param name The name of the query parameter to match.
     * @param value The expected value of the query parameter.
     * @since 3.1.0
     */
    fun queryParam(name: String, value: String) {
        queryParams[name] = ParameterMatcher(equalTo = value)
    }
    /**
     * Adds a condition to the request matcher that verifies if the specified query parameter contains
     * the given substring.
     *
     * @param name The name of the query parameter to be checked.
     * @param value The substring that should be contained in the query parameter's value.
     * @since 3.1.0
     */
    fun queryParamContains(name: String, value: String) {
        queryParams[name] = ParameterMatcher(contains = value)
    }
    /**
     * Associates a query parameter with a regular expression to validate its value.
     *
     * This method allows defining a matcher for a query parameter where the parameter's value
     * must conform to the specified regular expression. If the query parameter matches the regex,
     * it is considered valid for the request.
     *
     * @param name The name of the query parameter to be matched.
     * @param regex The regular expression that the parameter's value must match.
     * @since 3.1.0
     */
    fun queryParamMatches(name: String, regex: String) {
        queryParams[name] = ParameterMatcher(matches = Regex(regex))
    }
    /**
     * Marks a query parameter as absent in the request matcher configuration. When this method is
     * called, the specified query parameter must not be present in the request for it to match.
     *
     * @param name The name of the query parameter to mark as absent.
     * @since 3.1.0
     */
    fun queryParamAbsent(name: String) {
        queryParams[name] = ParameterMatcher(absent = true)
    }

    /**
     * Adds a condition to match requests whose body contains the specified text.
     *
     * The condition checks if the text provided as a parameter is present in the body
     * of an HTTP request.
     *
     * @param text The substring that must be present in the request body to satisfy the condition.
     * @since 3.1.0
     */
    fun bodyContains(text: String) { bodyPatterns += BodyContains(text) }
    /**
     * Adds a body pattern to match requests where the body content matches
     * the specified regular expression.
     *
     * @param regex The regular expression to match the body content against.
     * @since 3.1.0
     */
    fun bodyMatches(regex: String) { bodyPatterns += BodyMatches(Regex(regex)) }
    /**
     * Specifies the JSON structure of the request body using a DSL block.
     * The JSON structure is built using the [JsonBuilder] class and added
     * to the list of expected body patterns for request matching.
     *
     * @param block A lambda extension function with [JsonBuilder] as the receiver,
     * allowing the construction of a JSON object or array structure.
     * @since 3.1.0
     */
    fun jsonBody(block: JsonBuilder.() -> Unit) {
        bodyPatterns += JsonBodyEquals(JsonBuilder().apply(block).toJsonString())
    }
    /**
     * Adds a condition to verify the existence of a specific JSON path in the request body.
     *
     * This method appends a `JsonPathExists` pattern to the `bodyPatterns` list, ensuring that
     * the specified JSON path expression is present in the incoming request body.
     *
     * @param expression The JSON path expression to check for existence in the request body.
     * @since 3.1.0
     */
    fun jsonPath(expression: String) { bodyPatterns += JsonPathExists(expression) }
    /**
     * Adds a JSONPath expression assertion to match requests based on a specific JSON path
     * and its expected value within the request body.
     *
     * This method evaluates whether the request body contains a JSON path specified by [expression]
     * and checks if the value associated with that path matches the given [value].
     *
     * @param expression The JSONPath expression used to locate a specific node or value within the JSON payload.
     * @param value The expected value that must be present at the specified [expression] within the JSON payload.
     * @since 3.1.0
     */
    fun jsonPath(expression: String, value: Any) { bodyPatterns += JsonPathEquals(expression, value) }

    /**
     * Constructs and returns an instance of `RequestMatcher` using the current method properties.
     * This function gathers all properties such as HTTP method, path, path pattern, query parameters,
     * headers, body patterns, and path parameters to initialize the `RequestMatcher` object.
     *
     * @return A new `RequestMatcher` instance configured with the specified parameters.
     * @since 3.1.0
     */
    fun build() = RequestMatcher(
        method, path, pathPattern, queryParams.toMap(),
        headers.toMap().mapValues { it.value.joinToString() }, bodyPatterns.toList(), pathParams.toMap(),
    )
}

// --- RESPONSE BUILDER ---

/**
 * A builder class for constructing HTTP response definitions in a DSL-like manner.
 *
 * This class provides functionality to configure various aspects of an HTTP response such as
 * status code, headers, body, delay, and fault types. It supports constructing JSON bodies
 * and JSON arrays via dedicated helper functions.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
@HttpMockDslMarker
class ResponseBuilder {
    /**
     * Specifies the HTTP status code for the response being configured.
     *
     * This property determines the HTTP status that will be used when the
     * response is generated. By default, it is set to `HttpStatus.OK`.
     * Users can modify this value to simulate different HTTP response scenarios
     * in mock setups, such as `HttpStatus.NOT_FOUND` or `HttpStatus.INTERNAL_SERVER_ERROR`.
     * @since 3.1.0
     */
    var status: HttpStatus = HttpStatus.OK
    /**
     * Represents the HTTP headers for a request or response.
     * This variable holds a collection of key-value pairs that define protocol
     * headers used for metadata or control information in HTTP communication.
     * @since 3.1.0
     */
    val headers: HttpHeaders = HttpHeaders()
    /**
     * Represents the main content or textual information.
     * This variable can hold a nullable string and is typically used
     * to store the body of a document, message, or similar content.
     * @since 3.1.0
     */
    var body: String? = null
    /**
     * Specifies an optional delay to simulate network latency before the HTTP response is returned.
     * This can be used to test scenarios where delayed responses impact client behavior, such as
     * timeouts or retries.
     *
     * The delay is expressed as a `Duration` object. If null, no delay will be applied, and the
     * response will be sent immediately.
     * @since 3.1.0
     */
    var delay: Duration? = null
    /**
     * Specifies the type of fault to simulate during the processing of a response. This property is optional
     * and can be used to inject specific failure scenarios, such as connection resets, empty responses, or
     * malformed data, into the mocked HTTP response.
     *
     * When set to a non-null value, the `fault` property influences how the response will behave
     * depending on the specified fault type:
     * - `CONNECTION_RESET`: Simulates a sudden connection termination by the remote host.
     * - `EMPTY_RESPONSE`: Represents a scenario where no response is returned.
     * - `MALFORMED_RESPONSE`: Indicates that the response content does not meet the expected structure.
     * - `RANDOM_DATA`: Simulates the return of arbitrary or nonsensical data in place of a valid response.
     * @since 3.1.0
     */
    var fault: FaultType? = null

    /**
     * Configures the HTTP body as a JSON payload using the provided block to build the JSON structure.
     *
     * @param block A lambda with a receiver of type JsonBuilder to define the JSON structure.
     * @since 3.1.0
     */
    fun json(block: ReceiverConsumer<JsonBuilder>) {
        if (CONTENT_TYPE !in headers) headers += "Content-Type" to MediaType.APPLICATION_JSON.toString().asSingleList()
        body = JsonBuilder().apply(block).toJsonString()
    }

    /**
     * Configures the HTTP response body as a JSON array by utilizing a DSL-style builder block.
     * Automatically sets the "Content-Type" header to "application/json" if not already present.
     *
     * @param block A lambda receiver of type [JsonArrayBuilder] that allows for the construction
     * of the JSON array content. Items can be added to the array, including other objects or primitives.
     * @since 3.1.0
     */
    fun jsonArray(block: ReceiverConsumer<JsonArrayBuilder>) {
        if (CONTENT_TYPE !in headers) headers += "Content-Type" to MediaType.APPLICATION_JSON.toString().asSingleList()
        val items = JsonArrayBuilder().apply(block).items
        body = "[${items.joinToString(", ") { renderArrayItem(it) }}]"
    }

    /**
     * Renders an array item as a String representation based on its type.
     *
     * @param item The item to be rendered. It can be of type JsonBuilder, String, null, or any other object.
     * @return A String representation of the input item. If the item is of type JsonBuilder, its JSON string is returned.
     *         If the item is a String, it is returned wrapped in double quotes. If the item is null, the string "null"
     *         is returned. For all other types, their `toString` representation is returned.
     * @since 3.1.0
     */
    private fun renderArrayItem(item: Any?): String = when (item) {
        is JsonBuilder -> item.toJsonString()
        is String -> "\"$item\""
        null -> "null"
        else -> item.toString()
    }

    /**
     * Constructs a `ResponseDefinition` object using the provided parameters.
     *
     * @return A `ResponseDefinition` instance, populated with the current values of status, headers, body, delay, and fault.
     *
     * The `headers` field is transformed into a map where the values are concatenated into a single string.
     * @since 3.1.0
     */
    fun build() = ResponseDefinition(status, headers.toMap().mapValues { it.value.joinToString() }, body, delay, fault)
}

// --- STUB BUILDER ---

/**
 * Builder class used for constructing a stub definition in an HTTP mocking framework.
 * A stub definition consists of a request matcher, one or more response definitions,
 * an optional scenario configuration, and an optional priority.
 *
 * This class is part of a DSL for defining HTTP mocks and is annotated with `HttpMockDslMarker`
 * to enforce DSL scope rules.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
@HttpMockDslMarker
class StubBuilder {
    /**
     * Holds a reference to the criteria used for matching incoming HTTP requests.
     *
     * This property is configured via the `request` function in the `StubBuilder` class
     * and is a mandatory component for defining stubbed interactions.
     * It encapsulates the logic for evaluating whether an HTTP request satisfies
     * specific matching conditions, such as the HTTP method, path, headers, and body.
     *
     * If not set, the `build` function in `StubBuilder` will throw an error, as
     * the request matcher is required for creating a valid `StubDefinition`.
     * @since 3.1.0
     */
    private var requestMatcher: RequestMatcher? = null
    /**
     * Holds a mutable list of response definitions that define the behavior of the stubbed responses.
     *
     * This property is used to store the responses that will be returned by the stub during HTTP
     * mocking or testing. The responses can be configured through the `response` or `responses`
     * builder functions within the `StubBuilder` class. In the absence of explicit responses,
     * a default response with status `HttpStatus.OK` will be used during the stub construction.
     * @since 3.1.0
     */
    private val responses = emptyMList<ResponseDefinition>()
    /**
     * Defines the priority of the stub.
     *
     * A stub with a higher priority takes precedence over stubs with lower priorities when matching requests.
     * This property is utilized during the build process to determine the order of evaluation among stubs.
     * The value should be an integer, where higher values indicate higher priorities.
     *
     * Defaults to `0` if not explicitly set.
     * @since 3.1.0
     */
    var priority: Int = 0
    /**
     * Holds the configuration for the scenario associated with the current stub being built.
     *
     * This property defines the state transition information, such as the name of the scenario,
     * the required state condition for applicability, and the new state to transition to upon execution.
     * If no scenario is defined, this property remains null.
     *
     * It is initialized or modified within the `scenario` method of the `StubBuilder` class.
     *
     * @since 3.1.0
     */
    private var scenario: ScenarioConfig? = null

    /**
     * Configures and builds a request matcher using the provided block.
     *
     * @param block A lambda with a receiver that allows configuration of the RequestMatcherBuilder.
     * @since 3.1.0
     */
    fun request(block: ReceiverConsumer<RequestMatcherBuilder>) {
        requestMatcher = RequestMatcherBuilder().apply(block).build()
    }

    /**
     * Adds a response specification to the current stub configuration.
     *
     * This method allows the user to define the details of an HTTP response, including
     * its status, headers, body, delay, and potential faults, by building it through
     * the provided `ResponseBuilder` block.
     *
     * @param block A lambda with a receiver of type `ResponseBuilder` used to configure the desired
     * HTTP response. The builder allows setting various response parameters in a DSL-like manner.
     * @since 3.1.0
     */
    fun response(block: ReceiverConsumer<ResponseBuilder>) {
        responses += ResponseBuilder().apply(block).build()
    }

    /**
     * Configures a sequence of responses for the current stub.
     *
     * The method allows defining multiple responses that will be returned sequentially in the order
     * they are configured. Each response in the sequence is built using the provided `block`.
     * This is useful for simulating scenarios where multiple requests to the same endpoint yield
     * different responses.
     *
     * @param block A lambda block used to configure the sequence of responses. Each response
     * is added to the internal list of responses contained in the `ResponseSequenceBuilder`.
     * @since 3.1.0
     */
    fun responses(block: ReceiverConsumer<ResponseSequenceBuilder>) {
        responses += ResponseSequenceBuilder().apply(block).list
    }

    /**
     * Configures the scenario to be associated with the current stub. A scenario allows for modeling
     * stateful behavior in the request/response interaction by defining state transitions.
     *
     * @param name The name of the scenario. This is used to identify the scenario that the stub belongs to.
     * @param requiredState The state that must be active for the stub to be applied. If null, the stub
     * can match requests regardless of the current state.
     * @param newState The new state to transition to after the stub is executed. If null, no state
     * transition will occur.
     * @since 3.1.0
     */
    fun scenario(name: String, requiredState: String? = null, newState: String? = null) {
        scenario = ScenarioConfig(name, requiredState, newState)
    }

    /**
     * Builds a `StubDefinition` object based on the current state of the `StubBuilder`.
     *
     * This method finalizes the configuration of the stub, including the required request matcher,
     * responses, priority, and optional scenario configuration. If the request matcher is not specified,
     * an error is thrown. If no responses are defined, a default response with a status of `HttpStatus.OK`
     * is used.
     *
     * @return A fully constructed `StubDefinition` instance, encapsulating the request matcher, responses,
     * priority, and scenario configuration.
     *
     * @throws IllegalStateException If no request matcher is configured via the `request {}` block.
     * @since 3.1.0
     */
    fun build() = StubDefinition(
        requestMatcher ?: error("request {} block is required"),
        responses.ifEmpty { listOf(ResponseBuilder().apply { status = HttpStatus.OK }.build()) },
        priority,
        scenario,
    )
}

/**
 * A builder class used to define a sequence of HTTP responses in a DSL-like fashion.
 *
 * This class provides functionality to configure multiple HTTP response definitions that can be executed
 * sequentially. Each response in the sequence is constructed using a block of code that configures
 * a `ResponseBuilder`. The responses are stored internally and can be iterated in the order they are defined.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
@HttpMockDslMarker
class ResponseSequenceBuilder {
    /**
     * A mutable list of ResponseDefinition objects representing the sequence of HTTP responses
     * to be returned in a mocked HTTP scenario. The list is used to configure the order and
     * properties of responses within a response sequence builder.
     *
     * This list is populated by invoking the `then` function of the ResponseSequenceBuilder class,
     * where each call adds a new ResponseDefinition built using a ResponseBuilder.
     *
     * The list is utilized in the `responses` function of the StubBuilder class to add a sequence
     * of responses to a stub definition.
     * @since 3.1.0
     */
    val list = emptyMList<ResponseDefinition>()

    /**
     * Adds a new response to the response sequence by applying the provided configuration block.
     *
     * This method enables the addition of a `ResponseDefinition` to the `ResponseSequenceBuilder`'s list
     * after configuring a `ResponseBuilder` instance using the given DSL-style block.
     *
     * @param block A lambda with a receiver of type [ResponseBuilder], used to define the properties of the response.
     * @since 3.1.0
     */
    fun then(block: ReceiverConsumer<ResponseBuilder>) {
        list += ResponseBuilder().apply(block).build()
    }
}

// --- VERIFY BUILDER ---

/**
 * Builder class for constructing verification criteria for HTTP interactions within a mock API context.
 *
 * This class allows you to specify an HTTP method, a path, and the expected number of interactions
 * using a count matcher. It provides a DSL-style API for defining the verification logic.
 *
 * The resulting criteria can be used to assert that specific interactions with the mock API have
 * occurred as expected, based on the defined method, path, and count constraints.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
@HttpMockDslMarker
class VerifyBuilder {
    /**
     * Represents an HTTP method to be used in a request.
     *
     * This variable holds an optional [HttpMethod] instance, which specifies
     * the HTTP method (e.g., GET, POST, PUT, DELETE) for the request. It is
     * initially set to `null` and can be assigned a proper HTTP method as needed.
     *
     * Assigning a value to this variable determines the type of operation
     * that should be performed by the HTTP client.
     * @since 3.1.0
     */
    var method: HttpMethod? = null
    /**
     * Specifies the URI path to be verified in the HTTP interaction criteria.
     *
     * This variable is used to define the endpoint path associated with the HTTP method
     * during the construction of verification criteria. It can hold a nullable string value
     * representing the path, or be `null` if the path is not specified.
     * @since 3.1.0
     */
    var path: Uri? = null
    /**
     * Specifies the count constraint for verifying the number of occurrences of an operation.
     *
     * The `count` property is used to enforce constraints on the number of times an operation
     * or event must occur. By default, it is initialized to [CountMatcher.AtLeast] with a
     * minimum value of 1 occurrence. This value can be updated using the various
     * methods provided in the containing class.
     *
     * It is used in criteria construction to ensure expected behavior during HTTP interaction
     * verifications, allowing conditions such as exact matches, ranges, or a lack of occurrences.
     * @since 3.1.0
     */
    private var count: CountMatcher = CountMatcher.AtLeast(1)

    /**
     * Sets the expected number of times a method should be invoked to exactly the specified value.
     *
     * @param exactly The exact number of times the method is expected to be called.
     * @since 3.1.0
     */
    fun called(exactly: Int) { count = CountMatcher.Exactly(exactly) }
    /**
     * Configures the verification to ensure that the specified method or interaction
     * was called at least the given number of times.
     *
     * @param n The minimum number of times the method is expected to be called.
     * @since 3.1.0
     */
    fun calledAtLeast(n: Int) { count = CountMatcher.AtLeast(n) }
    /**
     * Specifies that the method being verified should be called at most the given number of times.
     *
     * @param n The maximum number of times the method is allowed to be called.
     * @since 3.1.0
     */
    fun calledAtMost(n: Int) { count = CountMatcher.AtMost(n) }
    /**
     * Specifies that the number of method invocations must fall within the given range, inclusive.
     *
     * @param range The inclusive range of times the method must be called.
     * @since 3.1.0
     */
    fun calledBetween(range: IntRange) { count = CountMatcher.Between(range.first, range.last) }
    /**
     * Specifies that the method or action should never be called during the verification process.
     *
     * This function updates the count matcher to [CountMatcher.Never], indicating a requirement
     * that the specified event, method, or operation must not occur even once.
     * @since 3.1.0
     */
    fun neverCalled() { count = CountMatcher.Never }

    /**
     * Constructs and returns an instance of `VerificationCriteria` with the configured method, path, and count.
     *
     * @return An initialized `VerificationCriteria` object based on the provided parameters.
     * @since 3.1.0
     */
    fun build() = VerificationCriteria(method, path, count)
}

// --- TOP-LEVEL MOCKAPI BUILDER ---

/**
 * Defines a builder for creating mock API definitions.
 * Provides methods to define stubbed responses and verification criteria
 * for mocking and testing API interactions.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
@HttpMockDslMarker
class MockApiBuilder {
    /**
     * Represents a mutable list of `StubDefinition` objects.
     * This list is initially empty and can be used to store and manipulate
     * instances of `StubDefinition` as needed within the application.
     * @since 3.1.0
     */
    private val stubs = emptyMList<StubDefinition>()
    /**
     * A mutable list containing verification criteria to be applied.
     *
     * This list is initialized as empty and is used to store instances of `VerificationCriteria`.
     * It may be populated with specific criteria during the program execution to validate or verify
     * certain conditions or requirements.
     * @since 3.1.0
     */
    private val verifications = emptyMList<VerificationCriteria>()

    /**
     * Adds a new stub to the mock API configuration. A stub defines the behavior of the mock
     * API for specific requests and responses, allowing users to define request matching rules,
     * response behavior, and other configurations such as priority or scenarios.
     *
     * @param block A configuration block that allows users to customize the stub using a
     * [StubBuilder]. The block provides a DSL for defining the request matching rules,
     * responses, priority, and scenarios for the stub.
     * @since 3.1.0
     */
    fun stub(block: ReceiverConsumer<StubBuilder>) {
        stubs += StubBuilder().apply(block).build()
    }

    /**
     * Adds a verification criterion to the mock API definition. This method allows the definition
     * of expectations regarding the HTTP interactions with the mock API, such as methods, paths,
     * and the expected number of invocations.
     *
     * @param block A receiver function that configures a [VerifyBuilder], enabling the specification
     *              of the verification criteria, including HTTP methods, paths, and call counts.
     * @since 3.1.0
     */
    fun verify(block: ReceiverConsumer<VerifyBuilder>) {
        verifications += VerifyBuilder().apply(block).build()
    }

    /**
     * Builds an instance of `MockApiDefinition` containing the stubs and verification criteria
     * defined in the current context.
     *
     * The method collects all stub definitions and verification criteria that were added to the
     * current `MockApiBuilder` instance and consolidates them into a `MockApiDefinition` object.
     *
     * @return A `MockApiDefinition` instance containing the configured stubs and verifications.
     * @since 3.1.0
     */
    fun build() = MockApiDefinition(stubs.toList(), verifications.toList())
}

/**
 * Creates and configures a mock API definition by using a DSL-style builder.
 *
 * The `mockApi` function allows you to define the behavior and verification criteria
 * for a mock HTTP API. This includes setting up request-response stubs and specifying
 * the expected interactions with the API.
 *
 * @param block A lambda with receiver that configures the `MockApiBuilder` to define
 *              stubs and verification criteria for the mock API.
 * @return A `MockApiDefinition` containing all the configured stubs and verification criteria.
 * @since 3.1.0
 */
@Beta
fun buildMockApi(block: MockApiBuilder.() -> Unit): MockApiDefinition =
    MockApiBuilder().apply(block).build()

// --- WIREMOCK ADAPTER (GENERATES WIREMOCK STUBS) ---

/**
 * Provides an adapter for generating WireMock-compatible JSON from a mock API definition.
 *
 * This object acts as a utility to transform structured definitions of mock APIs into JSON
 * format that WireMock can use to simulate HTTP request/response behavior. It handles converting
 * stub definitions and their attributes (e.g., requests, responses, scenarios) into a JSON
 * representation suitable for creating mapping files in WireMock.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
object WireMockAdapter {
    /**
     * Converts the given `MockApiDefinition` into a JSON representation compatible with WireMock.
     *
     * This method serializes the stubs defined in the provided `MockApiDefinition` into a JSON string,
     * which can be used to configure WireMock mappings for mocking HTTP APIs.
     *
     * @param definition The `MockApiDefinition` containing the stub definitions and other mock API configurations.
     * @return A JSON string representing the WireMock mappings derived from the provided definition.
     * @since 3.1.0
     */
    fun toWireMockJson(definition: MockApiDefinition): String = buildString {
        appendLine("{")
        appendLine("  \"mappings\": [")
        definition.stubs.forEachIndexed { i, stub ->
            if (i > 0) appendLine(",")
            renderStub(stub)
        }
        appendLine()
        appendLine("  ]")
        append("}")
    }

    /**
     * Renders a stub definition into a JSON representation and appends it to the provided StringBuilder.
     * The method processes both the request and response sections of the stub, including optional elements
     * such as headers, query parameters, scenarios, and delays, to generate a mock-compatible stub format.
     *
     * @param stub The stub definition to be rendered into JSON format. It contains details about the request
     * and response configuration, including optional priority and scenario support.
     * @since 3.1.0
     */
    @OptIn(RiskyApproximationOfTemporal::class)
    private fun StringBuilder.renderStub(stub: StubDefinition) {
        append("    {")
        appendLine()
        if (stub.priority > 0) appendLine("      \"priority\": ${stub.priority},")

        stub.scenario?.let { sc ->
            appendLine("      \"scenarioName\": \"${sc.name}\",")
            sc.requiredState?.let { appendLine("      \"requiredScenarioState\": \"$it\",") }
            sc.newState?.let { appendLine("      \"newScenarioState\": \"$it\",") }
        }

        // Request
        appendLine("      \"request\": {")
        val req = stub.request
        val parts: StringMList = emptyMList()
        req.method?.let { parts += "        \"method\": \"${it.name}\"" }

        val resolvedPath = req.path?.let { path ->
            var resolved = path
            req.pathParams.forEach { [name, `value`] -> resolved = Uri(resolved.toString().replace("{$name}", value)) }
            resolved
        }
        resolvedPath?.let { parts += "        \"url\": \"$it\"" }
        req.pathPattern?.let { parts += "        \"urlPattern\": \"${it.pattern}\"" }

        if (req.headers.isNotEmpty()) {
            val hdr = req.headers.entries.joinToString(",\n") { [k, v] ->
                "          \"$k\": { \"equalTo\": \"$v\" }"
            }
            parts += "        \"headers\": {\n$hdr\n        }"
        }
        if (req.queryParams.isNotEmpty()) {
            val qp = req.queryParams.entries.joinToString(",\n") { [k, v] ->
                val matcher = when {
                    v.equalTo.isNotNull() -> "\"equalTo\": \"${v.equalTo}\""
                    v.contains.isNotNull() -> "\"contains\": \"${v.contains}\""
                    v.matches.isNotNull() -> "\"matches\": \"${v.matches.pattern}\""
                    v.absent -> "\"absent\": true"
                    else -> "\"equalTo\": \"\""
                }
                "          \"$k\": { $matcher }"
            }
            parts += "        \"queryParameters\": {\n$qp\n        }"
        }

        appendLine(parts.joinToString(",\n"))
        appendLine("      },")

        // Response (first only for WireMock basic)
        val resp = stub.responses.first()
        appendLine("      \"response\": {")
        val rParts: StringMList = emptyMList()
        rParts += "        \"status\": ${resp.status.value}"
        if (resp.headers.isNotEmpty()) {
            val hdr = resp.headers.entries.joinToString(",\n") { [k, v] ->
                "          \"$k\": \"$v\""
            }
            rParts += "        \"headers\": {\n$hdr\n        }"
        }
        resp.body?.let { rParts += "        \"body\": ${escapeJson(it)}" }
        resp.delay?.let { rParts += "        \"fixedDelayMilliseconds\": ${it.toMillis().toLong()}" }
        resp.fault?.let { rParts += "        \"fault\": \"${it.name}\"" }
        appendLine(rParts.joinToString(",\n"))
        appendLine("      }")
        append("    }")
    }

    /**
     * Escapes special characters in a given string to make it safe for use in JSON.
     * Converts backslashes, double quotes, and newline characters into their corresponding
     * JSON escape sequences.
     *
     * @param s The input string to be escaped.
     * @return The escaped string, enclosed in double quotes, suitable for embedding in a JSON document.
     * @since 3.1.0
     */
    private fun escapeJson(s: String): String =
        "\"${s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}\""
}

// --- MOCKRESTSERVICESERVER ADAPTER (SPRING TEST) ---

/**
 * Provides utilities for generating Spring MockRestServiceServer test configurations from
 * a given HTTP Mock DSL definition.
 *
 * This object simplifies the process of translating a `MockApiDefinition` into a corresponding
 * Spring Mock test setup. Using the generated code, developers can emulate server responses
 * and validate HTTP requests using the Spring testing framework.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
object SpringMockAdapter {

    /**
     * Generates test code for mocking API behavior based on the provided mock API definition.
     *
     * The generated code creates expectations and responses that can be used in tests to simulate
     * HTTP interactions. This method leverages the `MockRestServiceServer` utility from Spring to configure
     * HTTP request matchers and corresponding responses as defined in the API stubs.
     *
     * @param definition The definition of the mock API, which includes stubs (request/response pairs) and
     * potential verification criteria for tests.
     * @param baseUrl The base URL to be used for constructing request expectations. Defaults to "http://localhost:8080".
     * @return A string containing the auto-generated test code that simulates the behavior of the mock API.
     * @since 3.1.0
     */
    fun generateTestCode(definition: MockApiDefinition, baseUrl: String = "http://localhost:8080"): String = buildString {
        appendLine("// Auto-generated from HTTP Mock DSL")
        appendLine("import org.springframework.test.web.client.MockRestServiceServer")
        appendLine("import org.springframework.test.web.client.match.MockRestRequestMatchers.*")
        appendLine("import org.springframework.test.web.client.response.MockRestResponseCreators.*")
        appendLine("import org.springframework.http.HttpMethod")
        appendLine("import org.springframework.http.MediaType")
        appendLine()

        definition.stubs.forEachIndexed { i, stub ->
            appendLine("// Stub #${i + 1}")
            appendLine("server.expect(")
            stub.request.method?.let { append("    requestTo(\"$baseUrl${stub.request.path ?: "/"}\")") }
            appendLine(")")
            stub.request.method?.let { appendLine("    .andExpect(method(HttpMethod.${it.name}))") }
            stub.request.headers.forEach { [k, v] ->
                appendLine("    .andExpect(header(\"$k\", \"$v\"))")
            }
            val resp = stub.responses.first()
            appendLine("    .andRespond(")
            appendLine("        withStatus(HttpStatus.${resp.status.name})")
            resp.headers["Content-Type"]?.let { appendLine("            .contentType(MediaType.parseMediaType(\"$it\"))") }
            resp.body?.let { appendLine("            .body(\"\"\"$it\"\"\")") }
            appendLine("    )")
            appendLine()
        }
    }
}