@file:Suppress("FunctionName", "unused")

package dev.tommasop1804.springutils.dsl.restclient

import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.annotations.*
import dev.tommasop1804.kutils.classes.coding.*
import dev.tommasop1804.kutils.classes.web.*
import dev.tommasop1804.kutils.exceptions.*
import dev.tommasop1804.springutils.servlet.*
import dev.tommasop1804.springutils.servlet.security.*
import org.springframework.web.client.RestClient
import kotlin.reflect.KClass

@DslMarker
annotation class RestClientDslMarker

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
     * Represents an optional authorization header derived from the request route's context.
     * 
     * This property is used to prepopulate or override authorization information in the HTTP headers
     * for outgoing requests. It can be `null` if no specific authorization header is automatically provided.
     * Typically, this header may be used to include credentials such as API keys or tokens while
     * constructing requests for secured endpoints.
     * @since 3.1.0
     */
    val autoAuthorizationHeader: String?

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
    override val defaults: ReqSpec,
    override val autoAuthorizationHeader: String?
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
    override operator fun invoke(spec: ReceiverConsumer<ReqSpec>?): Response<T> {
        val execSpec = defaults.copy()
        spec?.invoke(execSpec)

        val request = when (method) {
            HttpMethod.GET -> client.get().uri { buildUri(it, pathTemplate, execSpec) }.applyHeaders(execSpec, autoAuthorizationHeader).apply {
                if (execSpec.body.isNotNull()) log(LogLevel.WARN, "Request body will be ignored")
            }
            HttpMethod.HEAD -> client.head().uri { buildUri(it, pathTemplate, execSpec) }.applyHeaders(execSpec, autoAuthorizationHeader).apply {
                if (execSpec.body.isNotNull()) log(LogLevel.WARN, "Request body will be ignored")
            }
            HttpMethod.OPTIONS -> client.options().uri { buildUri(it, pathTemplate, execSpec) }.applyHeaders(execSpec, autoAuthorizationHeader).apply {
                if (execSpec.body.isNotNull()) log(LogLevel.WARN, "Request body will be ignored")
            }
            HttpMethod.DELETE -> client.delete().uri { buildUri(it, pathTemplate, execSpec) }.applyHeaders(execSpec, autoAuthorizationHeader).apply {
                if (execSpec.body.isNotNull()) log(LogLevel.WARN, "Request body will be ignored")
            }
            HttpMethod.POST -> client.post().uri { buildUri(it, pathTemplate, execSpec) }.applyHeaders(execSpec, autoAuthorizationHeader).applyBody(execSpec)
            HttpMethod.PUT -> client.put().uri { buildUri(it, pathTemplate, execSpec) }.applyHeaders(execSpec, autoAuthorizationHeader).applyBody(execSpec)
            HttpMethod.PATCH -> client.patch().uri { buildUri(it, pathTemplate, execSpec) }.applyHeaders(execSpec, autoAuthorizationHeader).applyBody(execSpec)
            else -> throw ConfigurationException()
        }

        @Suppress("UNCHECKED_CAST")
        return request
            .retrieve()
            .toEntity(returnType.java)
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
    override val autoAuthorizationHeader: String?
) : RequestRoute<Json> {
    /**
     * Invokes the specified consumer to configure the request and returns the resulting JSON response.
     *
     * @param spec A consumer function to configure the request specifications, such as path variables, query parameters, and headers.
     *             If null, the default specifications will be used.
     * @return A `JsonResponse` containing the result of the request execution.
     * @since 3.1.0
     */
    override operator fun invoke(spec: ReceiverConsumer<ReqSpec>?): JsonResponse =
        TypedRequestRoute(client, method, pathTemplate, returnType, defaults, autoAuthorizationHeader)(spec)
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
     * @param autoAuthorizationHeader The header name for automatic authorization, defaults to "Authorization". To disable, set the parameter to `null`.
     * @param spec An optional configuration block to customize the request, such as setting headers,
     *             query parameters, path variables, or request body.
     * @since 3.1.0
     */
    fun GET(
        path: String,
        autoAuthorizationHeader: String? = HttpHeader.AUTHORIZATION,
        spec: ReceiverConsumer<ReqSpec>? = null
    ) = buildJsonRoute(HttpMethod.GET, path, autoAuthorizationHeader, spec)

    /**
     * Defines an HTTP GET request for a specific path with an optional request specification.
     * Builds a typed route for the GET operation that can handle responses of type `T`.
     *
     * @param T The expected type of the response body.
     * @param path The URL path for the GET request. This path may include placeholders for variables.
     * @param autoAuthorizationHeader The header name for automatic authorization, defaults to "Authorization". To disable, set the parameter to `null`.
     * @param spec An optional lambda to configure request specifications, including headers, query parameters, and body.
     * @since 3.1.0
     */
    inline fun <reified T : Any> GET(
        path: String,
        autoAuthorizationHeader: String? = HttpHeader.AUTHORIZATION,
        noinline spec: ReceiverConsumer<ReqSpec>? = null,
    ) = buildRoute<T>(HttpMethod.GET, path, autoAuthorizationHeader, spec)
    
    /**
     * Defines an HTTP HEAD request for the specified path with an optional request specification.
     *
     * @param path The endpoint path for the HTTP HEAD request.
     * @param autoAuthorizationHeader The header name for automatic authorization, defaults to "Authorization". To disable, set the parameter to `null`.
     * @param spec An optional configuration block for specifying additional request parameters such as headers, query parameters, or path variables.
     * @since 3.1.0
     */
    fun HEAD(
        path: String,
        autoAuthorizationHeader: String? = HttpHeader.AUTHORIZATION,
        spec: ReceiverConsumer<ReqSpec>? = null,
    ) = buildJsonRoute(HttpMethod.HEAD, path, autoAuthorizationHeader, spec)

    /**
     * Defines an HTTP OPTIONS request to the specified path with an optional request specification.
     *
     * @param path The URI path for the OPTIONS request.
     * @param autoAuthorizationHeader The header name for automatic authorization, defaults to "Authorization". To disable, set the parameter to `null`.
     * @param spec An optional lambda to configure the request, allowing customization of headers, query parameters,
     * and other request-specific settings.
     * @since 3.1.0
     */
    fun OPTIONS(
        path: String,
        autoAuthorizationHeader: String? = HttpHeader.AUTHORIZATION,
        spec: ReceiverConsumer<ReqSpec>? = null,
    ) = buildJsonRoute(HttpMethod.OPTIONS, path, autoAuthorizationHeader, spec)

    /**
     * Defines a route for an HTTP OPTIONS request with a specified path and request specification.
     * The response type is determined by the reified type parameter [T].
     *
     * @param T The expected type of the response body.
     * @param path The endpoint path for the OPTIONS request.
     * @param autoAuthorizationHeader The header name for automatic authorization, defaults to "Authorization". To disable, set the parameter to `null`.
     * @param spec An optional consumer for configuring request parameters, headers, or body.
     * @since 3.1.0
     */
    inline fun <reified T : Any> OPTIONS(
        path: String,
        autoAuthorizationHeader: String? = HttpHeader.AUTHORIZATION,
        noinline spec: ReceiverConsumer<ReqSpec>? = null,
    ) = buildRoute<T>(HttpMethod.OPTIONS, path, autoAuthorizationHeader, spec)

    /**
     * Defines a POST request route within the scope of the DSL client.
     *
     * This function facilitates the creation of a route that will execute a POST HTTP request
     * to the specified path. Additionally, a custom request specification can be provided to
     * configure query parameters, headers, and body for the request.
     *
     * @param path The relative path for the POST request.
     * @param autoAuthorizationHeader The header name for automatic authorization, defaults to "Authorization". To disable, set the parameter to `null`.
     * @param spec An optional lambda that allows customization of the request using the given [ReqSpec].
     * @since 3.1.0
     */
    fun POST(
        path: String,
        autoAuthorizationHeader: String? = HttpHeader.AUTHORIZATION,
        spec: ReceiverConsumer<ReqSpec>? = null,
    ) = buildJsonRoute(HttpMethod.POST, path, autoAuthorizationHeader, spec)

    /**
     * Creates a POST HTTP route with the specified path and an optional request specification.
     * This method is used to define the structure and behavior of a POST HTTP request within a DSL context.
     *
     * @param T The expected response type for the request.
     * @param path The URL path of the POST route. This should be a relative path combined with the client prefix.
     * @param autoAuthorizationHeader The header name for automatic authorization, defaults to "Authorization". To disable, set the parameter to `null`.
     * @param spec An optional configuration block for specifying request-specific attributes (e.g., headers,
     *             query parameters, body, etc.). The block receives an instance of [ReqSpec] for customization.
     * @since 3.1.0
     */
    inline fun <reified T : Any> POST(
        path: String,
        autoAuthorizationHeader: String? = HttpHeader.AUTHORIZATION,
        noinline spec: ReceiverConsumer<ReqSpec>? = null,
    ) = buildRoute<T>(HttpMethod.POST, path, autoAuthorizationHeader, spec)

    /**
     * Defines a PUT request route.
     *
     * @param path The endpoint path for the PUT request.
     * @param autoAuthorizationHeader The header name for automatic authorization, defaults to "Authorization". To disable, set the parameter to `null`.
     * @param spec An optional consumer for configuring the request specification, such as headers, query parameters, and body.
     * @since 3.1.0
     */
    fun PUT(
        path: String,
        autoAuthorizationHeader: String? = HttpHeader.AUTHORIZATION,
        spec: ReceiverConsumer<ReqSpec>? = null,
    ) = buildJsonRoute(HttpMethod.PUT, path, autoAuthorizationHeader, spec)

    /**
     * Constructs a PUT HTTP request route with the given path and optional request specification.
     *
     * @param T The expected response type of the request.
     * @param path The endpoint path for the PUT request. This path can include path variables.
     * @param autoAuthorizationHeader The header name for automatic authorization, defaults to "Authorization". To disable, set the parameter to `null`.
     * @param spec An optional lambda to configure the request specification, such as headers,
     *             query parameters, path variables, and request body.
     * @since 3.1.0
     */
    inline fun <reified T : Any> PUT(
        path: String,
        autoAuthorizationHeader: String? = HttpHeader.AUTHORIZATION,
        noinline spec: ReceiverConsumer<ReqSpec>? = null,
    ) = buildRoute<T>(HttpMethod.PUT, path, autoAuthorizationHeader, spec)

    /**
     * Configures a PATCH HTTP request for the specified path with an optional request specification.
     *
     * This function is used to define a PATCH route within the DSL scope of a REST client.
     * It allows for customization of the request by modifying headers, query parameters, request body, and path variables.
     *
     * @param path The endpoint path for the PATCH request. It can contain path templates that will be resolved using the request specification.
     * @param autoAuthorizationHeader The header name for automatic authorization, defaults to "Authorization". To disable, set the parameter to `null`.
     * @param spec An optional request specification used to customize various aspects of the request,
     * such as headers, query parameters, and body content.
     * @since 3.1.0
     */
    fun PATCH(
        path: String,
        autoAuthorizationHeader: String? = HttpHeader.AUTHORIZATION,
        spec: ReceiverConsumer<ReqSpec>? = null,
    ) = buildJsonRoute(HttpMethod.PATCH, path, autoAuthorizationHeader, spec)

    /**
     * Defines a PATCH request with the ability to specify a path and an optional HTTP request specification.
     * The method constructs a route for a typed response of the specified class [T].
     *
     * @param T The expected response type of the PATCH request.
     * @param path The endpoint path to which the PATCH request will be sent.
     *             This should be a relative path appended to the client's base URL.
     * @param autoAuthorizationHeader The header name for automatic authorization, defaults to "Authorization". To disable, set the parameter to `null`.
     * @param spec An optional specification to configure various parts of the HTTP request,
     *             such as headers, query parameters, path variables, and body.
     *             The specification is represented as a consumer of [ReqSpec].
     * @since 3.1.0
     */
    inline fun <reified T : Any> PATCH(
        path: String,
        autoAuthorizationHeader: String? = HttpHeader.AUTHORIZATION,
        noinline spec: ReceiverConsumer<ReqSpec>? = null,
    ) = buildRoute<T>(HttpMethod.PATCH, path, autoAuthorizationHeader, spec)

    /**
     * Creates a DELETE HTTP request route with the specified path and optional request specification.
     *
     * @param path The URI path for the DELETE request.
     * @param autoAuthorizationHeader The header name for automatic authorization, defaults to "Authorization". To disable, set the parameter to `null`.
     * @param spec An optional receiver function to configure the request specification, such as
     *             headers, query parameters, and request body.
     * @since 3.1.0
     */
    fun DELETE(
        path: String,
        autoAuthorizationHeader: String? = HttpHeader.AUTHORIZATION,
        spec: ReceiverConsumer<ReqSpec>? = null,
    ) = buildJsonRoute(HttpMethod.DELETE, path, autoAuthorizationHeader, spec)

    // ── Internals ──

    /**
     * Builds a typed request route for the specified HTTP method and path, with optional request specifications.
     *
     * @param T The type of the response that will be returned by the route.
     * @param method The HTTP method to be used for the route.
     * @param path The URI path template for the route.
     * @param autoAuthorizationHeader The header name for automatic authorization, defaults to "Authorization". To disable, set the parameter to `null`.
     * @param specInit An optional consumer to initialize or modify request specifications.
     * @return A typed request route configured for the given HTTP method, path, and request specifications.
     * @since 3.1.0
     */
    @PublishedApi
    internal inline fun <reified T : Any> buildRoute(
        method: HttpMethod,
        path: String,
        autoAuthorizationHeader: String?,
        noinline specInit: ReceiverConsumer<ReqSpec>?
    ): TypedRequestRoute<T> {
        val defaults = ReqSpec()
        specInit?.invoke(defaults)
        return TypedRequestRoute(client, method, prefix + path, T::class, defaults, autoAuthorizationHeader)
    }

    /**
     * Builds a JSON-based request route for the specified HTTP method and path.
     *
     * @param method the HTTP method to use for the route (e.g., GET, POST, etc.).
     * @param path the URL path template for the route.
     * @param autoAuthorizationHeader The header name for automatic authorization, defaults to "Authorization". To disable, set the parameter to `null`.
     * @param specInit an optional consumer for configuring the request specification.
     * @return a `JsonRequestRoute` object representing the configured JSON route.
     * @since 3.1.0
     */
    @PublishedApi
    internal fun buildJsonRoute(
        method: HttpMethod,
        path: String,
        autoAuthorizationHeader: String?,
        specInit: ReceiverConsumer<ReqSpec>?
    ): JsonRequestRoute {
        val defaults = ReqSpec()
        specInit?.invoke(defaults)
        return JsonRequestRoute(client, method, prefix + path, defaults = defaults, autoAuthorizationHeader = autoAuthorizationHeader)
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
    fun queryParam(name: String, value: Any) {
        queryParams[name] = value
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
    fun header(name: String, vararg value: String) {
        headers[name] = value.toList()
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
@Beta(expectedStableVersion = "3.1.0")
fun routes(client: RestClient, init: ReceiverConsumer<RestClientDslScope>): RestClientDslScope {
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
internal fun <S : RestClient.RequestHeadersSpec<S>> S.applyHeaders(spec: ReqSpec, autoAuthorizationHeader: String?): S = apply {
    spec.headers.forEach { [name, values] ->
        header(name, *values.toTypedArray())
    }
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