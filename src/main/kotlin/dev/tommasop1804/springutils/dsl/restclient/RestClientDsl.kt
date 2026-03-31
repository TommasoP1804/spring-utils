/*
 * Copyright © 2026 Tommaso Pastorelli | spring-utils
 */

@file:Suppress("FunctionName", "unused")

package dev.tommasop1804.springutils.dsl.restclient

import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.classes.coding.*
import dev.tommasop1804.kutils.classes.web.*
import dev.tommasop1804.kutils.exceptions.*
import dev.tommasop1804.springutils.servlet.*
import dev.tommasop1804.springutils.servlet.response.*
import dev.tommasop1804.springutils.servlet.security.*
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestClient
import kotlin.reflect.KClass

@DslMarker
annotation class RestClientDslMarker

/**
 * ExtendedRestClient is a wrapper for the RestClient interface, providing additional
 * functionality or contextual information to enhance the behavior of the underlying
 * RestClient. This class uses delegation to forward all calls to an existing
 * RestClient instance.
 *
 * @property restClient The delegate instance of RestClient to which all calls are forwarded.
 * @property name An additional property that can hold contextual information
 *                or a descriptive name for the client instance.
 * @since 3.1.1
 * @author Tommaso Pastorelli
 */
class ExtendedRestClient(
    val restClient: RestClient,
    val name: String
) : RestClient by restClient

/**
 * Represents an HTTP route, defining an HTTP method, a path, and an associated handler.
 *
 * @property method The HTTP method to be used for this route (e.g., GET, POST, PUT, DELETE).
 * @property path The URL path associated with the route.
 * @property handler A function or transformer that processes requests for the route.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
data class Route(
    val method: String,
    val path: String,
    val handler: ReceiverTransformer<RestClient, Any?>
)

/**
 * A specialized extension of the ResponseEntity class that adds additional metadata
 * about the HTTP request, including the request URI and HTTP method.
 *
 * @param T the type of the response body
 * @constructor Creates an instance of ExtendedResponseEntity that wraps an existing ResponseEntity
 * while also including the URI path and HTTP method of the request.
 * @param responseEntity the original ResponseEntity to be extended
 * @param path the URI of the incoming request
 * @param method the HTTP method of the incoming request
 * @since 3.1.1
 * @author Tommaso Pastorelli
 */
class ExtendedResponse<T : Any>(
    responseEntity: ResponseEntity<T>,
    val path: Uri,
    val method: HttpMethod
) : ResponseEntity<T>(responseEntity.body, responseEntity.headers, responseEntity.statusCode)
/**
 * A type alias for `ExtendedResponse` with a specific type of `Json` for the response body.
 *
 * This alias simplifies the usage of `ExtendedResponse` when the response body is of type `Json`.
 * It retains all functionality of the `ExtendedResponse` class, including metadata about the HTTP
 * request such as the request URI and the HTTP method.
 * @since 3.1.1
 */
typealias ExtendedJsonResponse = ExtendedResponse<Json>

/**
 * Represents an abstraction for defining and executing REST API requests with a specific HTTP method, path template,
 * and expected return type.
 *
 * @param T The type of response expected from the route.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
interface RequestRoute<T : Any> {
    /**
     * The `RestClient` instance used for executing HTTP requests.
     *
     * This property provides access to the main client responsible for sending
     * requests and receiving responses. It is utilized internally by the
     * `RequestRoute` interface to manage request execution based on the defined
     * route configuration, such as HTTP methods, path templates, query parameters,
     * headers, and request bodies.
     *
     * The `client` is pivotal in facilitating interaction with remote endpoints
     * while adhering to the specifications defined in the `RequestRoute`.
     * @since 3.1.0
     */
    val client: RestClient
    /**
     * Represents the HTTP method to be used in an HTTP request.
     *
     * This property defines the type of action to be performed for a given request,
     * such as GET, POST, PUT, DELETE, etc. It is typically utilized to specify the
     * desired operation when constructing or handling HTTP requests.
     * @since 3.1.0
     */
    val method: HttpMethod
    /**
     * Represents the template for the HTTP request path.
     *
     * The `pathTemplate` is a string containing placeholders for path variables
     * that can be replaced with actual values when invoking a specific request.
     * It defines the structure of the resource URL and may include parameterized
     * segments surrounded by curly braces (e.g., `/resource/{id}`).
     * @since 3.1.0
     */
    val pathTemplate: String
    /**
     * Represents the return type of the request associated with this route.
     *
     * This is a reference to the Kotlin class of the type parameter `T`, which allows runtime access
     * to the type information of the response expected from executing this route.
     *
     * For example, when invoking the route, the response will be deserialized into an object of this type.
     * @since 3.1.0
     */
    val returnType: KClass<T>
    /**
     * Represents the default configuration for a request specification in the context of a route.
     *
     * This property defines the baseline settings such as variables, query parameters, headers,
     * and optional body content to be used when invoking the route. It can be overridden or extended
     * by providing a custom specification during the route invocation.
     * @since 3.1.0
     */
    val defaults: ReqSpec

    /**
     * Executes the request route with an optional configuration block for the request specification.
     *
     * @param spec An optional lambda for configuring the request specification, such as path variables,
     *             query parameters, headers, or the request body. If null, the defaults will be used.
     * @return A response of type [Response] containing the result of the request execution.
     * @since 3.1.0
     */
    operator fun invoke(spec: ReceiverConsumer<ReqSpec>? = null): Response<T>
}
/**
 * Represents a typed route for making HTTP requests with a specific return type.
 *
 * This class provides a mechanism to construct and execute HTTP requests using a
 * predefined HTTP method, path template, and response type. It also supports customization
 * through request specifications, such as headers, query parameters, and body.
 *
 * @param T The type of the response expected from the request.
 * @property client The REST client used to initiate the request.
 * @property method The HTTP method (e.g., GET, POST) used for the request.
 * @property pathTemplate The URI template for the request path, supporting parameterized substitution.
 * @property returnType The return type of the response, represented as a Kotlin class.
 * @property defaults Default request specifications that can be merged with additional customizations.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
class TypedRequestRoute<T : Any> @PublishedApi internal constructor(
    override val client: RestClient,
    override val method: HttpMethod,
    override val pathTemplate: String,
    override val returnType: KClass<T>,
    override val defaults: ReqSpec
) : RequestRoute<T> {
    /**
     * Executes the HTTP request based on the provided [ReceiverConsumer] specification.
     *
     * This method allows customization of the request by applying the given consumer to a copy
     * of the default request specification. It builds the request, applies headers, and body
     * if applicable, and retrieves the typed response entity.
     *
     * @param spec A lambda function to customize the request specification. This includes
     * modifying path variables, query parameters, headers, or the request body. Can be `null`.
     * @return The response of the request encapsulated in a [Response].
     * @since 3.1.0
     */
    override operator fun invoke(spec: ReceiverConsumer<ReqSpec>?): ExtendedResponse<T> {
        val execSpec = defaults.copy()
        spec?.invoke(execSpec)
        var uri: Uri? = null
        var method: HttpMethod? = null

        val request = when (this.method) {
            HttpMethod.GET -> client.get().uri { uri = buildUri(it, pathTemplate, execSpec); uri }.applyHeaders(execSpec).apply {
                if (execSpec.body.isNotNull()) log(LogLevel.WARN, "Request body will be ignored")
            }.apply { method = HttpMethod.GET }
            HttpMethod.HEAD -> client.head().uri { uri = buildUri(it, pathTemplate, execSpec); uri }.applyHeaders(execSpec).apply {
                if (execSpec.body.isNotNull()) log(LogLevel.WARN, "Request body will be ignored")
            }.apply { method = HttpMethod.HEAD }
            HttpMethod.OPTIONS -> client.options().uri { uri = buildUri(it, pathTemplate, execSpec); uri }.applyHeaders(execSpec).apply {
                if (execSpec.body.isNotNull()) log(LogLevel.WARN, "Request body will be ignored")
            }.apply { method = HttpMethod.OPTIONS }
            HttpMethod.DELETE -> client.delete().uri { uri = buildUri(it, pathTemplate, execSpec); uri }.applyHeaders(execSpec).apply {
                if (execSpec.body.isNotNull()) log(LogLevel.WARN, "Request body will be ignored")
            }
            HttpMethod.POST -> client.post().uri { uri = buildUri(it, pathTemplate, execSpec); uri }.applyHeaders(execSpec).applyBody(execSpec).apply { method = HttpMethod.POST }
            HttpMethod.PUT -> client.put().uri { uri = buildUri(it, pathTemplate, execSpec); uri }.applyHeaders(execSpec).applyBody(execSpec).apply { method = HttpMethod.PUT }
            HttpMethod.PATCH -> client.patch().uri { uri = buildUri(it, pathTemplate, execSpec); uri }.applyHeaders(execSpec).applyBody(execSpec).apply { method = HttpMethod.PATCH }
            else -> throw ConfigurationException()
        }

        val result = ExtendedResponse(
            request
                .retrieve()
                .toEntity(returnType.java),
            uri!!,
            method!!
        )
        if (execSpec.autoStatusValidation.isNotNull() && result.status.isError) {
            throw when (val exception = execSpec.autoStatusValidation!!()) {
                is PlaceholderException -> {
                    val effectiveException = ExternalServiceHttpException(
                        if (client is ExtendedRestClient) client.name else null,
                        statusCode = result.status,
                        uri = uri,
                        method = method,
                        errorMessage = exception.mes,
                        internalErrorCode = exception.internalErrorCode,
                    ).let { if (exception.causedBy.isNotNull()) it.initCause(exception.causedBy) else it }
                    if (exception.causeOf.isNotNull()) exception.causeOf.initCause(effectiveException) else effectiveException
                }
                else -> exception
            }
        }

        return result
    }
}
/**
 * Represents a specialized implementation of the `RequestRoute` interface for handling requests
 * that expect a JSON response. This class uses a predefined return type of `Json` and a default
 * [ReqSpec] configuration for creating the request route.
 *
 * @param client The HTTP client used to execute requests.
 * @param method The HTTP method to be used for the request (e.g., GET, POST, etc.).
 * @param pathTemplate The URI template for the request path, optionally containing placeholders for variables.
 * @param returnType The return type of the request, fixed to `Json` for this implementation.
 * @param defaults A default [ReqSpec] instance providing initial configurations such as headers or query parameters.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
class JsonRequestRoute @PublishedApi internal constructor(
    override val client: RestClient,
    override val method: HttpMethod,
    override val pathTemplate: String,
    override val returnType: KClass<Json> = Json::class,
    override val defaults: ReqSpec,
) : RequestRoute<Json> {
    /**
     * Invokes the specified consumer to configure the request and returns the resulting JSON response.
     *
     * @param spec A consumer function to configure the request specifications, such as path variables, query parameters, and headers.
     *             If null, the default specifications will be used.
     * @return A `JsonResponse` containing the result of the request execution.
     * @since 3.1.0
     */
    override operator fun invoke(spec: ReceiverConsumer<ReqSpec>?): ExtendedJsonResponse =
        TypedRequestRoute(client, method, pathTemplate, returnType, defaults)(spec)
}

/**
 * A DSL scope for building and configuring REST client requests with chained routes.
 * This class is marked with a DSL marker to allow intuitive structuring of API routes
 * and reduce context interference in nested lambdas.
 *
 * @constructor Creates a new instance of RestClientDslScope.
 * @property client The underlying `RestClient` used for executing HTTP requests.
 * @property prefix The path prefix used to build hierarchical routes.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
@RestClientDslMarker
class RestClientDslScope(
    val client: RestClient,
    val prefix: String = String.EMPTY,
) {
    /**
     * Creates a new nested `RestClientDslScope` using the current string as part of the prefix.
     *
     * @param init A lambda to configure the nested `RestClientDslScope`.
     * @since 3.1.0
     */
    fun String.nest(init: ReceiverConsumer<RestClientDslScope>) {
        val nested = RestClientDslScope(client, prefix + this)
        nested.init()
    }

    // -- HTTP verbs --

    /**
     * Builds a `GET` request route for the specified path and optional request specification.
     * This function is part of the DSL for defining HTTP client routes.
     *
     * @param path The endpoint path template for the `GET` request.
     * @param spec An optional configuration block to customize the request, such as setting headers,
     *             query parameters, path variables, or request body.
     * @since 3.1.0
     */
    fun GET(
        path: String,
        spec: ReceiverConsumer<ReqSpec>? = null
    ) = buildJsonRoute(HttpMethod.GET, path, spec)

    /**
     * Defines an HTTP GET request for a specific path with an optional request specification.
     * Builds a typed route for the GET operation that can handle responses of type `T`.
     *
     * @param T The expected type of the response body.
     * @param path The URL path for the GET request. This path may include placeholders for variables.
     * @param spec An optional lambda to configure request specifications, including headers, query parameters, and body.
     * @since 3.1.0
     */
    inline fun <reified T : Any> GET(
        path: String,
        noinline spec: ReceiverConsumer<ReqSpec>? = null,
    ) = buildRoute<T>(HttpMethod.GET, path, spec)
    
    /**
     * Defines an HTTP HEAD request for the specified path with an optional request specification.
     *
     * @param path The endpoint path for the HTTP HEAD request.
     * @param spec An optional configuration block for specifying additional request parameters such as headers, query parameters, or path variables.
     * @since 3.1.0
     */
    fun HEAD(
        path: String,
        spec: ReceiverConsumer<ReqSpec>? = null,
    ) = buildJsonRoute(HttpMethod.HEAD, path, spec)

    /**
     * Defines an HTTP OPTIONS request to the specified path with an optional request specification.
     *
     * @param path The URI path for the OPTIONS request.
     * @param spec An optional lambda to configure the request, allowing customization of headers, query parameters,
     * and other request-specific settings.
     * @since 3.1.0
     */
    fun OPTIONS(
        path: String,
        spec: ReceiverConsumer<ReqSpec>? = null,
    ) = buildJsonRoute(HttpMethod.OPTIONS, path, spec)

    /**
     * Defines a route for an HTTP OPTIONS request with a specified path and request specification.
     * The response type is determined by the reified type parameter [T].
     *
     * @param T The expected type of the response body.
     * @param path The endpoint path for the OPTIONS request.
     * @param spec An optional consumer for configuring request parameters, headers, or body.
     * @since 3.1.0
     */
    inline fun <reified T : Any> OPTIONS(
        path: String,
        noinline spec: ReceiverConsumer<ReqSpec>? = null,
    ) = buildRoute<T>(HttpMethod.OPTIONS, path, spec)

    /**
     * Defines a POST request route within the scope of the DSL client.
     *
     * This function facilitates the creation of a route that will execute a POST HTTP request
     * to the specified path. Additionally, a custom request specification can be provided to
     * configure query parameters, headers, and body for the request.
     *
     * @param path The relative path for the POST request.
     * @param spec An optional lambda that allows customization of the request using the given [ReqSpec].
     * @since 3.1.0
     */
    fun POST(
        path: String,
        spec: ReceiverConsumer<ReqSpec>? = null,
    ) = buildJsonRoute(HttpMethod.POST, path, spec)

    /**
     * Creates a POST HTTP route with the specified path and an optional request specification.
     * This method is used to define the structure and behavior of a POST HTTP request within a DSL context.
     *
     * @param T The expected response type for the request.
     * @param path The URL path of the POST route. This should be a relative path combined with the client prefix.
     * @param spec An optional configuration block for specifying request-specific attributes (e.g., headers,
     *             query parameters, body, etc.). The block receives an instance of [ReqSpec] for customization.
     * @since 3.1.0
     */
    inline fun <reified T : Any> POST(
        path: String,
        noinline spec: ReceiverConsumer<ReqSpec>? = null,
    ) = buildRoute<T>(HttpMethod.POST, path, spec)

    /**
     * Defines a PUT request route.
     *
     * @param path The endpoint path for the PUT request.
     * @param spec An optional consumer for configuring the request specification, such as headers, query parameters, and body.
     * @since 3.1.0
     */
    fun PUT(
        path: String,
        spec: ReceiverConsumer<ReqSpec>? = null,
    ) = buildJsonRoute(HttpMethod.PUT, path, spec)

    /**
     * Constructs a PUT HTTP request route with the given path and optional request specification.
     *
     * @param T The expected response type of the request.
     * @param path The endpoint path for the PUT request. This path can include path variables.
     * @param spec An optional lambda to configure the request specification, such as headers,
     *             query parameters, path variables, and request body.
     * @since 3.1.0
     */
    inline fun <reified T : Any> PUT(
        path: String,
        noinline spec: ReceiverConsumer<ReqSpec>? = null,
    ) = buildRoute<T>(HttpMethod.PUT, path, spec)

    /**
     * Configures a PATCH HTTP request for the specified path with an optional request specification.
     *
     * This function is used to define a PATCH route within the DSL scope of a REST client.
     * It allows for customization of the request by modifying headers, query parameters, request body, and path variables.
     *
     * @param path The endpoint path for the PATCH request. It can contain path templates that will be resolved using the request specification.
     * @param spec An optional request specification used to customize various aspects of the request,
     * such as headers, query parameters, and body content.
     * @since 3.1.0
     */
    fun PATCH(
        path: String,
        spec: ReceiverConsumer<ReqSpec>? = null,
    ) = buildJsonRoute(HttpMethod.PATCH, path, spec)

    /**
     * Defines a PATCH request with the ability to specify a path and an optional HTTP request specification.
     * The method constructs a route for a typed response of the specified class [T].
     *
     * @param T The expected response type of the PATCH request.
     * @param path The endpoint path to which the PATCH request will be sent.
     *             This should be a relative path appended to the client's base URL.
     * @param spec An optional specification to configure various parts of the HTTP request,
     *             such as headers, query parameters, path variables, and body.
     *             The specification is represented as a consumer of [ReqSpec].
     * @since 3.1.0
     */
    inline fun <reified T : Any> PATCH(
        path: String,
        noinline spec: ReceiverConsumer<ReqSpec>? = null,
    ) = buildRoute<T>(HttpMethod.PATCH, path, spec)

    /**
     * Creates a DELETE HTTP request route with the specified path and optional request specification.
     *
     * @param path The URI path for the DELETE request.
     * @param spec An optional receiver function to configure the request specification, such as
     *             headers, query parameters, and request body.
     * @since 3.1.0
     */
    fun DELETE(
        path: String,
        spec: ReceiverConsumer<ReqSpec>? = null,
    ) = buildJsonRoute(HttpMethod.DELETE, path, spec)

    // ── Internals ──

    /**
     * Builds a typed request route for the specified HTTP method and path, with optional request specifications.
     *
     * @param T The type of the response that will be returned by the route.
     * @param method The HTTP method to be used for the route.
     * @param path The URI path template for the route.
     * @param specInit An optional consumer to initialize or modify request specifications.
     * @return A typed request route configured for the given HTTP method, path, and request specifications.
     * @since 3.1.0
     */
    @PublishedApi
    internal inline fun <reified T : Any> buildRoute(
        method: HttpMethod,
        path: String,
        noinline specInit: ReceiverConsumer<ReqSpec>?
    ): TypedRequestRoute<T> {
        val defaults = ReqSpec()
        specInit?.invoke(defaults)
        return TypedRequestRoute(client, method, prefix + path, T::class, defaults)
    }

    /**
     * Builds a JSON-based request route for the specified HTTP method and path.
     *
     * @param method the HTTP method to use for the route (e.g., GET, POST, etc.).
     * @param path the URL path template for the route.
     * @param specInit an optional consumer for configuring the request specification.
     * @return a `JsonRequestRoute` object representing the configured JSON route.
     * @since 3.1.0
     */
    @PublishedApi
    internal fun buildJsonRoute(
        method: HttpMethod,
        path: String,
        specInit: ReceiverConsumer<ReqSpec>?
    ): JsonRequestRoute {
        val defaults = ReqSpec()
        specInit?.invoke(defaults)
        return JsonRequestRoute(client, method, prefix + path, defaults = defaults)
    }
}

// --- URI VARIABLES BUILDER ---

/**
 * Represents a DSL configuration for specifying details of an HTTP request.
 * This class allows defining headers, path variables, query parameters, and the request body.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
@RestClientDslMarker
class ReqSpec {
    /**
     * Represents a map of variable key-value pairs used for template expansion
     * in path manipulation within a request specification. This property stores
     * variables that replace placeholders in the request path during request
     * building. It is initialized as an empty map and can be modified through
     * methods such as `pathVariable` and `pathVariables`.
     * @since 3.1.0
     */
    internal val variables: DataMMap = emptyMMap()
    /**
     * Represents a collection of query parameters to be included in the request.
     *
     * This property is used to store key-value pairs where the key is the query
     * parameter name, and the value can be a single value, a collection, or an
     * array of values. These parameters are appended to the request URI during
     * the request-building process.
     *
     * The query parameters can be populated using methods such as `queryParam`
     * and `queryParams`, which allow specifying individual parameters or a set
     * of parameters using a configuration block. Additionally, this property is
     * included when copying an existing `ReqSpec` instance or while building the
     * complete request URI.
     * @since 3.1.0
     */
    internal val queryParams: DataMMap = emptyMMap()
    /**
     * Represents the HTTP headers associated with a request or response.
     * These headers are key-value pairs where the key is the header name
     * and the value(s) represent the associated header data.
     *
     * This property is intended for internal use only and encapsulates the
     * state or metadata related to HTTP communication.
     * @since 3.1.0
     */
    internal val headers: HttpHeaders = HttpHeaders()
    /**
     * Represents a generic body content that can hold any type of data or be null.
     * This variable is commonly used to store payloads, responses, or other dynamic content
     * where the type may not be determined at compile time.
     * @since 3.1.0
     */
    var body: Any? = null
    /**
     * A mutable property used to store the `Authorization` header value for HTTP requests.
     * This property is part of the `ReqSpec` class and represents the default value of the
     * `Authorization` header that will be included in the request by default, if set.
     *
     * By default, it is initialized to the constant `HttpHeader.AUTHORIZATION`, but it can be overridden
     * with a custom value or set to `null` to disable it. This allows for flexible configuration of
     * the authorization mechanism when building a request.
     *
     * When defined, the `autoAuthorizationHeader` value is automatically appended to the request headers
     * during request execution, unless explicitly overridden by the configuration of headers
     * using other available functions in the DSL.
     * @since 3.1.1
     */
    var autoAuthorizationHeader: String? = HttpHeader.AUTHORIZATION
    /**
     * A nullable supplier function used to provide custom validation logic for automatically
     * handling HTTP status codes in a request specification.
     *
     * When set, this supplier can be invoked to throw a specific exception,
     * enabling fine-grained control over status code validation or error handling.
     * This allows for scenarios where default status code handling may need to be overridden
     * with custom logic defined by the user.
     * @since 3.1.1
     */
    internal var autoStatusValidation: ThrowableSupplier? = null

    /**
     * Sets a path variable with the specified name and value.
     *
     * @param name the name of the path variable
     * @param value the value of the path variable
     * @since 3.1.0
     */
    fun pathVariable(name: String, value: Any) {
        variables[name] = value
    }
    /**
     * Sets a path variable using a key-value pair.
     * If the value in the pair is an iterable, its elements are joined into a string
     * separated by commas and set as the path variable value. Otherwise, the value
     * is directly set as the path variable value.
     *
     * @param pair A pair where the first element is the name of the path variable
     * and the second element is the value of the path variable. The value can be
     * a single object or an iterable.
     * @since 3.3.1
     */
    fun pathVariable(pair: Pair<String, Any>) {
        when (val value = pair.second) {
            is Iterable<*> -> pathVariable(pair.first, value.joinToString(Char.COMMA))
            else -> pathVariable(pair.first, value)
        }
    }

    /**
     * Configures and adds path variables to the current context.
     *
     * @param init A lambda with receiver that initializes the ParamSpec object to define path variables.
     * @since 3.1.0
     */
    fun pathVariables(init: ReceiverConsumer<ParamSpec>) {
        val spec = ParamSpec()
        spec.init()
        variables += spec.params
    }

    /**
     * Adds a query parameter to the request specification.
     *
     * @param name The name of the query parameter.
     * @param value The value of the query parameter.
     * @since 3.1.0
     */
    fun queryParam(name: String, vararg value: Any?) {
        queryParams[name] = value.joinToString(String.COMMA)
    }
    /**
     * Adds a query parameter to the request specification using a key-value pair.
     * The key is the query parameter name, and the value can be a single value, an iterable,
     * or `null`. The method processes the value and calls the appropriate `queryParam` method.
     *
     * @param pair A pair where the first element is the name of the query parameter
     * and the second element is its value. If the value is `null`, the query parameter is added with `null`.
     * If the value is an `Iterable`, its elements are added as multiple values for the parameter.
     * Otherwise, the provided value is added directly as the query parameter value.
     * @since 3.2.4
     */
    fun queryParam(pair: Pair<String, Any?>) {
        when (val value = pair.second) {
            null -> queryParam(pair.first, null)
            is Iterable<*> -> queryParam(pair.first, *value.toList().toTypedArray())
            else -> queryParam(pair.first, value)
        }
    }

    /**
     * Configures multiple query parameters for the HTTP request by using the provided initialization block.
     * The function allows for defining multiple key-value pairs in a DSL-like manner.
     *
     * @param init A lambda with receiver of type ParamSpec that is used to define
     * key-value pairs for query parameters. Each parameter can be defined using the infix function `String.to(Any)`.
     * @since 3.1.0
     */
    fun queryParams(init: ReceiverConsumer<ParamSpec>) {
        val spec = ParamSpec()
        spec.init()
        queryParams += spec.params
    }

    /**
     * Sets a header with the specified name and value, replacing any existing headers with the same name.
     *
     * @param name the name of the header
     * @param value the value of the header
     * @since 3.1.0
     */
    fun header(name: String, vararg value: Any) {
        headers[name] = value.toList().map { it.toString() }
    }
    /**
     * Adds a header to the request specification from the provided pair. The pair's first
     * component represents the header name, and the second component represents the header value.
     * - If the value is `null`, the header will be added with the string "null".
     * - If the value is an iterable, the elements will be converted to strings and added as multiple header values.
     * - Otherwise, the value will be converted to a string and added as a single header value.
     *
     * @param pair A pair where the first element is the header name and the second element is the header value.
     * @since 3.2.4
     */
    fun header(pair: Pair<String, Any?>) {
        when (val value = pair.second) {
            null -> header(pair.first, "null")
            is Iterable<*> -> header(pair.first, *value.toList().map { it.toString() }.toTypedArray())
            else -> header(pair.first, value.toString())
        }
    }

    /**
     * Configures and initializes HTTP headers using the provided receiver consumer.
     *
     * @param init A lambda function to set up HTTP headers within the context of a `HttpHeaders` receiver.
     * @since 3.1.0
     */
    fun headers(init: ReceiverConsumer<HttpHeaders>) {
        headers.init()
    }


    /**
     * Adds the supplied HTTP headers to the current request specification.
     * Existing headers will be retained, while the new headers will be appended.
     *
     * @param headers The HTTP headers to be added to the request.
     * @since 3.2.4
     */
    fun headers(headers: HttpHeaders) = headers.forEach { this.headers += it }

    /**
     * Configures the default behavior for HTTP status code validation in the request specification.
     * The method sets up a [ExternalServiceHttpException] exception that will be used as part of the status validation logic.
     *
     * This function is typically used to define how the client should handle response status codes when
     * no explicit validation logic has been provided.
     *
     * Calling this method will overwrite any previously defined status validation logic.
     * @param message Optional error message to include in the exception. If null, a default message will be used.
     * @param internalErrorCode Optional error code to include in the exception. If null, no error code will be included.
     * @param causeOf Optional exception that caused the current exception. If null, no cause will be included.
     * @param causedBy Optional exception that caused the current exception. If null, no cause will be included.
     * @since 3.1.1
     */
    fun validateStatus(message: String? = null, internalErrorCode: String? = null, causeOf: Throwable? = null, causedBy: Throwable? = null) {
        autoStatusValidation = { PlaceholderException(message, internalErrorCode, causeOf, causedBy) }
    }
    /**
     * Configures a custom validation logic for the status of a request.
     * The provided supplier is used to define the validation to be applied.
     *
     * @param validation A supplier that provides a custom implementation for status validation.
     * @since 3.1.1
     */
    fun validateStatus(validation: ThrowableSupplier) {
        autoStatusValidation = validation
    }

    /**
     * Creates a copy of the current `ReqSpec` instance, including its variables, query parameters,
     * headers, and body.
     *
     * @return A new `ReqSpec` instance containing the same data as the original.
     * @since 3.1.0
     */
    internal fun copy(): ReqSpec = ReqSpec().also { copy ->
        copy.variables += variables
        copy.queryParams += queryParams
        headers.forEach { [name, values] -> copy.headers[name] = values.toMutableList() }
        copy.body = body
    }
}

internal class PlaceholderException(
    val mes: String?,
    val internalErrorCode: String?,
    val causeOf: Throwable?,
    val causedBy: Throwable?,
) : RuntimeException()

/**
 * A DSL class used to define key-value pairs for path variables or query parameters
 * in request specifications.
 * The defined parameters are stored in an internal map and utilized by the enclosing request specification.
 *
 * This class is annotated with [RestClientDslMarker], ensuring its usage aligns with DSL conventions.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
@RestClientDslMarker
class ParamSpec {
    /**
     * A mutable map used to store parameter key-value pairs.
     * This map is part of the internal state of the [ParamSpec] class and is used to accumulate
     * parameters specified through the DSL configuration.
     *
     * These parameters can be added to requests as query parameters or path variables, depending on the context
     * in which the [ParamSpec] is utilized (e.g., in `ReqSpec.queryParams` or `ReqSpec.pathVariables`).
     *
     * Parameters can be set using the infix function `String.to(value)` within the `ParamSpec` block.
     * @since 3.1.0
     */
    internal val params: DataMMap = emptyMMap()

    /**
     * Associates the given value with the string key in the `params` map.
     *
     * @param value The value to be associated with the string key.
     * @since 3.1.0
     */
    infix fun String.to(value: Any) {
        params[this] = value
    }
}

// --- ENTRY POINT ---

/**
 * Configures and returns a DSL scope for defining HTTP routes using the specified `RestClient`.
 *
 * @param client The `RestClient` instance used to execute HTTP requests.
 * @param init A receiver function used to configure the `RestClientDslScope`.
 * @return A configured instance of `RestClientDslScope` for building and managing HTTP routes.
 * @since 3.1.0
 */
fun restClientRouter(client: RestClient, init: ReceiverConsumer<RestClientDslScope>): RestClientDslScope {
    val scope = RestClientDslScope(client)
    scope.init()
    return scope
}

/**
 * Constructs a complete URI by expanding the provided path template with variables
 * and appending query parameters from the given specification.
 *
 * @param builder The `UriBuilder` instance used to construct the URI.
 * @param pathTemplate The URI path template containing placeholders to be replaced
 *                     with the corresponding variables from the specification.
 * @param spec The request specification containing path variables and query parameters
 *             to be applied to the URI.
 * @return A `URI` object representing the constructed URI with applied variables and query parameters.
 * @since 3.1.0
 */
internal fun buildUri(builder: org.springframework.web.util.UriBuilder, pathTemplate: String, spec: ReqSpec): java.net.URI {
    val expandedPath = spec.variables.entries.fold(pathTemplate) { acc, (key, value) ->
        acc.replace("{$key}", value.toString())
    }

    builder.path(expandedPath)

    spec.queryParams.forEach { (key, value) ->
        when (value) {
            is Collection<*> -> value.forEach { if (it.isNotNull()) builder.queryParam(key, it) }
            is Array<*> -> value.forEach { if (it.isNotNull()) builder.queryParam(key, it) }
            else -> if (value.isNotNull()) builder.queryParam(key, value)
        }
    }

    return builder.build()
}

/**
 * Applies the headers from the provided [ReqSpec] to the current request specification.
 *
 * @param spec The [ReqSpec] containing the headers to be applied to the current request.
 * @return The updated request specification with the applied headers.
 * @since 3.1.0
 */
internal fun <S : RestClient.RequestHeadersSpec<S>> S.applyHeaders(spec: ReqSpec): S = apply {
    spec.headers.forEach { [name, values] ->
        header(name, *values.toTypedArray())
    }
    val autoAuthorizationHeader = spec.autoAuthorizationHeader
    if (autoAuthorizationHeader.isNotNull() && autoAuthorizationHeader !in spec.headers) {
        header(autoAuthorizationHeader, token(autoAuthorizationHeader).toString(true))
    }
}

/**
 * Applies the request body from the provided `ReqSpec` to the `RestClient.RequestBodySpec` instance.
 * If the body in `ReqSpec` is null, no modifications are made.
 *
 * @param spec The `ReqSpec` containing the request body to be applied. If `spec.body` is null,
 *             the body is not modified.
 * @return The modified `RestClient.RequestBodySpec` instance with the request body applied,
 *         or the same instance if the body is null.
 * @since 3.1.0
 */
internal fun RestClient.RequestBodySpec.applyBody(spec: ReqSpec): RestClient.RequestBodySpec = apply {
    if (spec.body.isNull()) return@apply
    body(spec.body!!)
}