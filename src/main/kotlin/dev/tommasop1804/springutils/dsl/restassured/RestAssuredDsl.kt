/*
 * Copyright © 2026 Tommaso Pastorelli (TommasoP1804) | Spring-Utils
 */

@file:Suppress("FunctionName", "unused")

package dev.tommasop1804.springutils.dsl.restassured

import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.classes.coding.*
import dev.tommasop1804.kutils.classes.web.*
import dev.tommasop1804.kutils.exceptions.*
import io.restassured.RestAssured
import io.restassured.response.ExtractableResponse
import io.restassured.response.Response
import io.restassured.response.ValidatableResponse
import io.restassured.specification.RequestSpecification
import java.io.File
import java.io.InputStream
import kotlin.reflect.KClass

@DslMarker
annotation class RestAssuredDslMarker

// --- EXTENDED RESPONSE ---

/**
 * A specialized response wrapper that adds metadata about the HTTP request,
 * including the request URI and HTTP method. Mirrors `ExtendedResponse` from the RestClient DSL.
 *
 * @param T the type of the response body
 * @param response the original RestAssured extractable response
 * @param path the URI of the request
 * @param method the HTTP method of the request
 * @since 3.2.2
 * @author Tommaso Pastorelli
 */
class ExtendedTestResponse<T : Any>(
    val response: ExtractableResponse<Response>,
    val path: String,
    val method: HttpMethod,
    val returnType: KClass<T>,
) {
    /**
     * The HTTP status of the response as a Spring [HttpStatus].
     * @since 3.2.2
     */
    val status: HttpStatus get() = HttpStatus.of(response.statusCode())!!

    /**
     * Extracts the response body as the specified type.
     * @since 3.2.4
     */
    val body: T get() = response.`as`(returnType.java)

    /**
     * Extracts a value from the response body at the given JSON path.
     * @since 3.2.2
     */
    fun <V> path(path: String): V = response.path(path)

    /**
     * Returns the underlying [ValidatableResponse] for assertion chaining.
     * @since 3.2.2
     */
    fun then(): ValidatableResponse = response.response().then()
}

/**
 * A type alias for `ExtendedTestResponse` with a specific type of `Json` for the response body.
 * @since 3.2.2
 */
typealias ExtendedJsonTestResponse = ExtendedTestResponse<Json>

// --- ROUTE INTERFACE ---

/**
 * Represents an abstraction for defining and executing REST Assured test requests
 * with a specific HTTP method, path template, and expected return type.
 * Mirrors `RequestRoute` from the RestClient DSL.
 *
 * @param T The type of response expected from the route.
 * @since 3.2.2
 * @author Tommaso Pastorelli
 */
interface TestRequestRoute<T : Any> {
    /**
     * The HTTP method to be used in the request.
     * @since 3.2.2
     */
    val method: HttpMethod

    /**
     * The template for the HTTP request path, may include placeholders (e.g., `/resource/{id}`).
     * @since 3.2.2
     */
    val pathTemplate: String

    /**
     * The return type of the request, represented as a Kotlin class.
     * @since 3.2.2
     */
    val returnType: KClass<T>

    /**
     * Default configuration for the request specification.
     * @since 3.2.2
     */
    val defaults: TestReqSpec

    /**
     * Executes the test request with an optional configuration block for the request specification.
     *
     * @param spec An optional lambda for configuring the request specification.
     * @return An [ExtendedTestResponse] containing the result and request metadata.
     * @since 3.2.2
     */
    operator fun invoke(spec: ReceiverConsumer<TestReqSpec>? = null): ExtendedTestResponse<T>
}

// --- TYPED ROUTE ---

/**
 * A typed test route for making HTTP requests with a specific return type via RestAssured.
 * Mirrors `TypedRequestRoute` from the RestClient DSL.
 *
 * @param T The type of the response expected from the request.
 * @property method The HTTP method used for the request.
 * @property pathTemplate The URI template for the request path.
 * @property returnType The return type of the response, represented as a Kotlin class.
 * @property defaults Default request specifications that can be merged with additional customizations.
 * @since 3.2.2
 * @author Tommaso Pastorelli
 */
class TypedTestRoute<T : Any> @PublishedApi internal constructor(
    override val method: HttpMethod,
    override val pathTemplate: String,
    override val returnType: KClass<T>,
    override val defaults: TestReqSpec,
) : TestRequestRoute<T> {

    /**
     * Executes the HTTP request based on the provided specification.
     *
     * @param spec A lambda function to customize the request specification. Can be `null`.
     * @return An [ExtendedTestResponse] containing the result and request metadata.
     * @since 3.2.2
     */
    override operator fun invoke(spec: ReceiverConsumer<TestReqSpec>?): ExtendedTestResponse<T> {
        val execSpec = defaults.copy()
        spec?.invoke(execSpec)

        val uri = buildUri(pathTemplate, execSpec)
        val request = RestAssured.given().applySpec(execSpec).`when`()

        if (method in listOf(HttpMethod.Get, HttpMethod.Head, HttpMethod.Options, HttpMethod.Delete)
            && execSpec.body.isNotNull()
        ) log(LogLevel.Warn, "Request body will be ignored for $method")

        val response = when (method) {
            HttpMethod.Get -> request.get(uri)
            HttpMethod.Head -> request.head(uri)
            HttpMethod.Options -> request.options(uri)
            HttpMethod.Post -> request.post(uri)
            HttpMethod.Put -> request.put(uri)
            HttpMethod.Patch -> request.patch(uri)
            HttpMethod.Delete -> request.delete(uri)
            else -> throw ConfigurationException()
        }

        val result = ExtendedTestResponse(
            response.then().extract(),
            uri,
            method,
            returnType
        )

        if (execSpec.autoStatusValidation.isNotNull() && result.status.isError) {
            throw when (val exception = execSpec.autoStatusValidation!!()) {
                is PlaceholderException -> {
                    val effectiveException = ExternalServiceHttpException(
                        serviceName = null,
                        statusCode = result.status,
                        uri = java.net.URI.create(uri),
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

// --- JSON ROUTE ---

/**
 * A specialized test route for handling requests that expect a JSON response.
 * Mirrors `JsonRequestRoute` from the RestClient DSL.
 *
 * @property method The HTTP method to be used for the request.
 * @property pathTemplate The URI template for the request path.
 * @property returnType The return type, fixed to `Json`.
 * @property defaults Default request specifications.
 * @since 3.2.2
 * @author Tommaso Pastorelli
 */
class JsonTestRoute @PublishedApi internal constructor(
    override val method: HttpMethod,
    override val pathTemplate: String,
    override val returnType: KClass<Json> = Json::class,
    override val defaults: TestReqSpec,
) : TestRequestRoute<Json> {

    /**
     * Invokes the request and returns the resulting JSON test response.
     *
     * @param spec A consumer function to configure the request specifications. If null, defaults are used.
     * @return An [ExtendedJsonTestResponse] containing the result and request metadata.
     * @since 3.2.2
     */
    override operator fun invoke(spec: ReceiverConsumer<TestReqSpec>?): ExtendedJsonTestResponse =
        TypedTestRoute(method, pathTemplate, returnType, defaults)(spec)
}

internal data class MultiPart(
    val controlName: String? = null,
    val content: Any,
    val mimeType: MediaType,
    val fileName: String? = null
)

// --- REQUEST SPEC ---

/**
 * Represents a DSL configuration for specifying details of a RestAssured test request.
 * Mirrors `ReqSpec` from the RestClient DSL.
 *
 * @since 3.2.2
 * @author Tommaso Pastorelli
 */
@RestAssuredDslMarker
class TestReqSpec {
    /**
     * Path variables for template expansion.
     * @since 3.2.2
     */
    internal val variables: DataMMap = emptyMMap()

    /**
     * Query parameters to be appended to the request URI.
     * @since 3.2.2
     */
    internal val queryParams: DataMMap = emptyMMap()

    /**
     * A mutable map used to store form parameters for an HTTP request.
     * These parameters are typically sent as a part of the request body
     * in `application/x-www-form-urlencoded` format.
     * @since 4.2.0
     */
    internal val formParams: DataMMap = emptyMMap()

    /**
     * A mutable map representing the cookies associated with the request specification.
     *
     * This map holds the key-value pairs where keys are the cookie names
     * and values are the corresponding cookie values.
     * It is initially an empty map but can be populated with cookies as needed
     * for request customization.
     * @since 4.2.0
     */
    internal val cookies: DataMMapNN = emptyMMap()

    /**
     * HTTP headers for the request.
     * @since 3.2.2
     */
    internal val headers: HttpHeaders = HttpHeaders()

    /**
     * Represents a collection of multipart entities that can be used
     * to define file uploads or data submissions within a request.
     *
     * Each multipart entity is defined by its control name, associated file,
     * and MIME type, as encapsulated in the [MultiPart] data class.
     *
     * This list is initially empty and can be populated as needed for
     * constructing multipart/form-data requests.
     * @since 4.2.0
     */
    internal val multiParts: MList<MultiPart> = emptyMList()

    /**
     * Optional request body.
     * @since 3.2.2
     */
    var body: Any? = null

    /**
     * Optional content type override.
     * @since 3.2.2
     */
    var contentType: MediaType? = null

    /**
     * Optional port override for this specific request.
     * @since 3.2.2
     */
    var port: Int? = null

    /**
     * Custom validation logic for HTTP status codes.
     * Mirrors `ReqSpec.autoStatusValidation` from the RestClient DSL.
     * @since 3.2.2
     */
    internal var autoStatusValidation: ThrowableSupplier? = null

    /**
     * Sets a path variable with the specified name and value.
     * @since 3.2.2
     */
    fun pathVariable(name: String, value: Any) {
        variables[name] = value
    }
    /**
     * Sets a path variable using a pair of name and value.
     *
     * @param pair a pair where the first element represents the name of the variable
     * and the second element represents the value to associate with the variable
     * @since 3.2.4
     */
    fun pathVariable(pair: Pair<String, Any>) {
        variables[pair.first] = pair.second
    }

    /**
     * Configures path variables using a DSL block.
     * @since 3.2.2
     */
    fun pathVariables(init: ReceiverConsumer<ParamSpec>) {
        val spec = ParamSpec()
        spec.init()
        variables += spec.params
    }

    /**
     * Adds a single query parameter.
     * @since 3.2.2
     */
    fun queryParam(name: String, vararg value: Any?) {
        queryParams[name] = value.joinToString(String.COMMA)
    }
    /**
     * Adds a query parameter using the given name-value pair.
     *
     * If the value in the pair is an iterable, each element in the iterable is added as a separate query parameter
     * using the same name. Otherwise, the value is directly added as a query parameter.
     *
     * @param pair a pair where the first element represents the name of the query parameter
     * and the second element represents the value to associate with the parameter. If the second element
     * is an iterable, each item in the iterable is added as a separate query parameter.
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
     * Configures query parameters using a DSL block.
     * @since 3.2.2
     */
    fun queryParams(init: ReceiverConsumer<ParamSpec>) {
        val spec = ParamSpec()
        spec.init()
        spec.params.forEach { queryParam(it.key to it.value) }
    }

    /**
     * Adds or updates a cookie with the specified name and value.
     *
     * @param name the name of the cookie to be added or updated
     * @param value the value to associate with the specified cookie name
     * @since 4.2.0
     */
    fun cookie(name: String, value: Any) {
        cookies[name] = value.toString()
    }

    /**
     * Adds a cookie using the given name-value pair.
     *
     * @param pair a pair where the first element represents the name of the cookie
     * and the second element represents the value to associate with the cookie
     * @since 4.2.0
     */
    fun cookie(pair: Pair<String, Any>) {
        cookie(pair.first, pair.second)
    }

    /**
     * Configures cookies using a DSL block. The provided block can be used to define
     * key-value pairs representing cookie names and their associated values. Only non-null
     * values are included as cookies.
     *
     * @param init a DSL block used to populate cookie key-value pairs. Use `String to Any`
     * to associate cookie names and values within the block.
     * @since 4.2.0
     */
    fun cookies(init: ReceiverConsumer<ParamSpec>) {
        val spec = ParamSpec()
        spec.init()
        spec.params.filterValues(Any?::isNotNull).forEach { cookie(it.key, it.value!!) }
    }

    /**
     * Adds a single form parameter with the specified name and value.
     *
     * @param name the name of the form parameter
     * @param value the value to associate with the form parameter. It will be converted to a string.
     * @since 4.2.0
     */
    fun formParam(name: String, value: Any) {
        formParams[name] = value.toString()
    }

    /**
     * Adds a form parameter using the given name-value pair.
     *
     * @param pair a pair where the first element represents the name of the form parameter
     * and the second element represents the value to associate with the parameter.
     * @since 4.2.0
     */
    fun formParam(pair: Pair<String, Any>) {
        formParam(pair.first, pair.second)
    }

    /**
     * Configures form parameters using a DSL block.
     *
     * Allows specifying multiple form parameters through the provided initialization block
     * that operates on a [ParamSpec] instance. Any non-null key-value pairs defined within the block
     * will be added as form parameters.
     *
     * @param init a DSL block that accepts a [ReceiverConsumer] of [ParamSpec] for defining form parameters
     * @since 4.2.0
     */
    fun formParam(init: ReceiverConsumer<ParamSpec>) {
        val spec = ParamSpec()
        spec.init()
        spec.params.filterValues(Any?::isNotNull).forEach { formParam(it.key, it.value!!) }
    }

    /**
     * Sets a header with the specified name and value(s).
     * @since 3.2.2
     */
    fun header(name: String, vararg value: Any) {
        headers[name] = value.toList().map { it.toString() }
    }
    /**
     * Adds a header using the given name-value pair.
     *
     * If the value in the pair is an iterable, each element in the iterable is added as a separate header
     * entry using the same name. Otherwise, the value is directly added as a header.
     *
     * @param pair a pair where the first element represents the name of the header and the second
     * element represents the value to associate with the header. If the second element is an iterable,
     * each item in the iterable is added as a separate header entry.
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
     * Configures headers using a DSL block.
     * @since 3.2.2
     */
    fun headers(init: ReceiverConsumer<HttpHeaders>) {
        headers.init()
    }

    /**
     * Adds multiple headers to the existing set of headers. Each header from the provided [HttpHeaders]
     * instance is appended to the current headers collection.
     *
     * @param headers a collection of HTTP headers to be added
     * @since 3.2.4
     */
    fun headers(headers: HttpHeaders) = headers.forEach { this.headers += it }

    /**
     * Adds a multipart entity with the specified parameters to the request.
     *
     * @param controlName the name of the control or field in the form that the server expects.
     * @param file the file to be uploaded as part of the multipart request.
     * @param mimeType the MIME type of the file. Defaults to `MediaType.APPLICATION_OCTET_STREAM`.
     * @param fileName the name of the file as it should appear in the server-side form data.
     * @since 4.2.0
     */
    fun multiPart(
        controlName: String,
        file: File,
        mimeType: MediaType = MediaType.APPLICATION_OCTET_STREAM,
        fileName: String? = null
    ) { multiParts += MultiPart(controlName, file, mimeType, fileName) }
    /**
     * Adds a multipart file to the request with the specified control name, file, MIME type, and file name.
     *
     * @param controlName The name of the form control associated with the file.
     * @param file The file to be included in the multipart request.
     * @param mimeType The MIME type of the file. Defaults to `MimeType.APPLICATION_OCTET_STREAM`.
     * @param fileName The name of the file to be sent in the request.
     * @since 4.2.0
     */
    fun multiPart(
        controlName: String,
        file: File,
        mimeType: MimeType = MimeType.APPLICATION_OCTET_STREAM,
        fileName: String? = null
    ) { multiPart(controlName, file, mimeType.toMediaType(), fileName) }
    /**
     * Adds a multi-part component to the request.
     * Commonly used for file uploads or sending binary data as part of a request payload.
     *
     * @param controlName the name of the control parameter associated with the multi-part data
     * @param obj the content of the multi-part data, which can be any object
     * @param mimeType the MIME type of the multi-part data; defaults to `MediaType.APPLICATION_OCTET_STREAM`
     * @param fileName the file name to associate with the multi-part data
     * @since 4.2.0
     */
    fun multiPart(
        controlName: String,
        obj: Any,
        mimeType: MediaType = MediaType.APPLICATION_OCTET_STREAM,
        fileName: String? = null
    ) { multiParts += MultiPart(controlName, obj, mimeType, fileName) }
    /**
     * Adds a multipart section to the request with the specified details.
     *
     * @param controlName the name of the form field that this part corresponds to
     * @param obj the content to include in the multipart section
     * @param mimeType the MIME type of the content; defaults to application/json
     * @since 4.2.0
     */
    fun multiPart(
        controlName: String,
        obj: Any,
        mimeType: MimeType = MimeType.APPLICATION_JSON,
    ) { multiPart(controlName, obj, mimeType.toMediaType()) }
    /**
     * Adds a multi-part component to the request.
     *
     * This method allows you to include a multi-part section with a specified control name,
     * content body, MIME type, and file name. The added multi-part will be stored in the
     * internal `multiParts` collection of the request.
     *
     * @param controlName the name of the control associated with the multi-part section
     * @param contentBody the content to include as part of the multi-part section
     * @param mimeType the MIME type of the content, defaults to `MediaType.TEXT_PLAIN`
     * @since 4.2.0
     */
    fun multiPart(
        controlName: String,
        contentBody: String,
        mimeType: MediaType = MediaType.TEXT_PLAIN,
    ) { multiParts += MultiPart(controlName, contentBody, mimeType) }
    /**
     * Adds a multi-part form data entry to the request body.
     *
     * @param controlName The name of the form field for the multipart entry.
     * @param contentBody The content to be associated with the form field.
     * @param mimeType The MIME type of the content, with a default of `MimeType.TEXT_PLAIN`.
     * @since 4.2.0
     */
    fun multiPart(
        controlName: String,
        contentBody: String,
        mimeType: MimeType = MimeType.TEXT_PLAIN,
    ) { multiPart(controlName, controlName, mimeType.toMediaType()) }
    /**
     * Adds a multipart section to the request with the specified parameters.
     *
     * @param controlName The name of the form control being submitted.
     * @param contentBody The content of the multipart section as a byte array.
     * @param mimeType The MIME type of the content. Defaults to `MediaType.APPLICATION_OCTET_STREAM`.
     * @param fileName The name of the file to associate with the multipart section.
     * @since 4.2.0
     */
    fun multiPart(
        controlName: String,
        contentBody: ByteArray,
        mimeType: MediaType = MediaType.APPLICATION_OCTET_STREAM,
        fileName: String
    ) { multiParts += MultiPart(controlName, contentBody, mimeType, fileName) }
    /**
     * Prepares a multipart form-data component with the specified parameters.
     *
     * @param controlName The name of the control or field in the multipart form-data.
     * @param contentBody The byte array representing the content of the file or data being sent.
     * @param mimeType The MIME type of the content. Defaults to application/octet-stream.
     * @param fileName The name of the file to be associated with the content.
     * @since 4.2.0
     */
    fun multiPart(
        controlName: String,
        contentBody: ByteArray,
        mimeType: MimeType = MimeType.APPLICATION_OCTET_STREAM,
        fileName: String
    ) { multiPart(controlName, controlName, mimeType.toMediaType(), fileName) }
    /**
     * Adds a multipart part to the collection of multiParts.
     *
     * @param controlName The name of the control associated with the multipart part.
     * @param contentBody The content of the multipart as an InputStream.
     * @param mimeType The MIME type of the content. Defaults to MediaType.APPLICATION_OCTET_STREAM.
     * @param fileName The name of the file associated with the multipart part.
     * @since 4.2.0
     */
    fun multiPart(
        controlName: String,
        contentBody: InputStream,
        mimeType: MediaType = MediaType.APPLICATION_OCTET_STREAM,
        fileName: String
    ) { multiParts += MultiPart(controlName, contentBody, mimeType, fileName) }
    /**
     * Prepares a multipart request with the specified parameters.
     *
     * @param controlName The name of the control or field in the multipart request.
     * @param contentBody The input stream representing the data to be included in the multipart request.
     * @param mimeType The MIME type of the content, defaults to application/octet-stream.
     * @param fileName The name of the file being included in the multipart request.
     * @since 4.2.0
     */
    fun multiPart(
        controlName: String,
        contentBody: InputStream,
        mimeType: MimeType = MimeType.APPLICATION_OCTET_STREAM,
        fileName: String
    ) { multiPart(controlName, controlName, mimeType.toMediaType(), fileName) }

    /**
     * Configures default status validation with an [ExternalServiceHttpException].
     * @since 3.2.2
     */
    fun validateStatus(
        message: String? = null,
        internalErrorCode: String? = null,
        causeOf: Throwable? = null,
        causedBy: Throwable? = null,
    ) {
        autoStatusValidation = { PlaceholderException(message, internalErrorCode, causeOf, causedBy) }
    }

    /**
     * Configures a custom validation logic for the status of a request.
     * @since 3.2.2
     */
    fun validateStatus(validation: ThrowableSupplier) {
        autoStatusValidation = validation
    }

    /**
     * Creates a copy of the current `TestReqSpec` instance.
     * @since 3.2.2
     */
    internal fun copy(): TestReqSpec = TestReqSpec().also { copy ->
        copy.variables += variables
        copy.queryParams += queryParams
        headers.forEach { [name, values] -> copy.headers[name] = values.toMutableList() }
        copy.body = body
        copy.contentType = contentType
        copy.port = port
        copy.autoStatusValidation = autoStatusValidation
    }
}

internal class PlaceholderException(
    val mes: String?,
    val internalErrorCode: String?,
    val causeOf: Throwable?,
    val causedBy: Throwable?,
) : RuntimeException()

// --- PARAM SPEC ---

/**
 * A DSL class used to define key-value pairs for path variables or query parameters.
 * Mirrors [ParamSpec] from the RestClient DSL.
 *
 * @since 3.2.2
 * @author Tommaso Pastorelli
 */
@RestAssuredDslMarker
class ParamSpec {
    internal val params: DataMMap = emptyMMap()

    /**
     * Associates the given value with the string key.
     * @since 3.2.2
     */
    infix fun String.to(value: Any) {
        params[this] = value
    }
}

// --- DSL SCOPE ---

/**
 * A DSL scope for building and configuring RestAssured test routes with chained nesting.
 * Mirrors `RestClientDslScope` from the RestClient DSL.
 *
 * @property prefix The path prefix used to build hierarchical routes.
 * @since 3.2.2
 * @author Tommaso Pastorelli
 */
@RestAssuredDslMarker
class RestAssuredDslScope(
    val prefix: String = String.EMPTY,
) {
    /**
     * Creates a new nested scope using the current string as part of the prefix.
     * @since 3.2.2
     */
    fun String.nest(init: ReceiverConsumer<RestAssuredDslScope>) {
        val nested = RestAssuredDslScope(prefix + this)
        nested.init()
    }

    // -- HTTP verbs (Json response) --

    /**
     * Builds a JSON `GET` test route.
     * @since 3.2.2
     */
    fun GET(
        path: String,
        spec: ReceiverConsumer<TestReqSpec>? = null,
    ) = buildJsonRoute(HttpMethod.Get, path, spec)

    /**
     * Builds a typed `GET` test route.
     * @since 3.2.2
     */
    inline fun <reified T : Any> GET(
        path: String,
        noinline spec: ReceiverConsumer<TestReqSpec>? = null,
    ) = buildRoute<T>(HttpMethod.Get, path, spec)

    /**
     * Builds a JSON `HEAD` test route.
     * @since 3.2.2
     */
    fun HEAD(
        path: String,
        spec: ReceiverConsumer<TestReqSpec>? = null,
    ) = buildJsonRoute(HttpMethod.Head, path, spec)

    /**
     * Builds a JSON `OPTIONS` test route.
     * @since 3.2.2
     */
    fun OPTIONS(
        path: String,
        spec: ReceiverConsumer<TestReqSpec>? = null,
    ) = buildJsonRoute(HttpMethod.Options, path, spec)

    /**
     * Builds a typed `OPTIONS` test route.
     * @since 3.2.2
     */
    inline fun <reified T : Any> OPTIONS(
        path: String,
        noinline spec: ReceiverConsumer<TestReqSpec>? = null,
    ) = buildRoute<T>(HttpMethod.Options, path, spec)

    /**
     * Builds a JSON `POST` test route.
     * @since 3.2.2
     */
    fun POST(
        path: String,
        spec: ReceiverConsumer<TestReqSpec>? = null,
    ) = buildJsonRoute(HttpMethod.Post, path, spec)

    /**
     * Builds a typed `POST` test route.
     * @since 3.2.2
     */
    inline fun <reified T : Any> POST(
        path: String,
        noinline spec: ReceiverConsumer<TestReqSpec>? = null,
    ) = buildRoute<T>(HttpMethod.Post, path, spec)

    /**
     * Builds a JSON `PUT` test route.
     * @since 3.2.2
     */
    fun PUT(
        path: String,
        spec: ReceiverConsumer<TestReqSpec>? = null,
    ) = buildJsonRoute(HttpMethod.Put, path, spec)

    /**
     * Builds a typed `PUT` test route.
     * @since 3.2.2
     */
    inline fun <reified T : Any> PUT(
        path: String,
        noinline spec: ReceiverConsumer<TestReqSpec>? = null,
    ) = buildRoute<T>(HttpMethod.Put, path, spec)

    /**
     * Builds a JSON `PATCH` test route.
     * @since 3.2.2
     */
    fun PATCH(
        path: String,
        spec: ReceiverConsumer<TestReqSpec>? = null,
    ) = buildJsonRoute(HttpMethod.Patch, path, spec)

    /**
     * Builds a typed `PATCH` test route.
     * @since 3.2.2
     */
    inline fun <reified T : Any> PATCH(
        path: String,
        noinline spec: ReceiverConsumer<TestReqSpec>? = null,
    ) = buildRoute<T>(HttpMethod.Patch, path, spec)

    /**
     * Builds a JSON `DELETE` test route.
     * @since 3.2.2
     */
    fun DELETE(
        path: String,
        spec: ReceiverConsumer<TestReqSpec>? = null,
    ) = buildJsonRoute(HttpMethod.Delete, path, spec)

    // ── Internals ──

    /**
     * Builds a typed test route for the specified HTTP method and path.
     * @since 3.2.2
     */
    @PublishedApi
    internal inline fun <reified T : Any> buildRoute(
        method: HttpMethod,
        path: String,
        noinline specInit: ReceiverConsumer<TestReqSpec>?,
    ): TypedTestRoute<T> {
        val defaults = TestReqSpec()
        specInit?.invoke(defaults)
        return TypedTestRoute(method, prefix + path, T::class, defaults)
    }

    /**
     * Builds a JSON test route for the specified HTTP method and path.
     * @since 3.2.2
     */
    @PublishedApi
    internal fun buildJsonRoute(
        method: HttpMethod,
        path: String,
        specInit: ReceiverConsumer<TestReqSpec>?,
    ): JsonTestRoute {
        val defaults = TestReqSpec()
        specInit?.invoke(defaults)
        return JsonTestRoute(method, prefix + path, defaults = defaults)
    }
}

// --- INFIX THEN ---

/**
 * Infix extension for assertion chaining on [ExtendedTestResponse].
 *
 * ```kotlin
 * getUser { pathVariables { "id" to 42 } } then {
 *     statusCode(200)
 *     body("name", equalTo("Tom"))
 * }
 * ```
 * @since 3.2.2
 */
infix fun <T : Any> ExtendedTestResponse<T>.then(
    block: ReceiverConsumer<ValidatableResponse>,
): ValidatableResponse = then().apply(block)

// --- SHARED INTERNALS ---

/**
 * Constructs the URI string by expanding path variables and appending query parameters.
 * @since 3.2.2
 */
private fun buildUri(pathTemplate: String, spec: TestReqSpec): String {
    val expandedPath = spec.variables.entries.fold(pathTemplate) { acc, (key, value) ->
        acc.replace("{$key}", value.toString())
    }

    if (spec.queryParams.isEmpty()) return expandedPath

    val builder = org.springframework.web.util.UriComponentsBuilder.fromUriString(expandedPath)
    spec.queryParams.forEach { (key, value) ->
        when (value) {
            is Collection<*> -> value.forEach { if (it.isNotNull()) builder.queryParam(key, it) }
            is Array<*> -> value.forEach { if (it.isNotNull()) builder.queryParam(key, it) }
            else -> if (value.isNotNull()) builder.queryParam(key, value)
        }
    }

    return builder.build().toUriString()
}

/**
 * Applies the [TestReqSpec] configuration to the RestAssured [RequestSpecification].
 * @since 3.2.2
 */
private fun RequestSpecification.applySpec(spec: TestReqSpec): RequestSpecification = apply {
    spec.contentType?.let { contentType(it.toString()) }
    spec.port?.let { port(it) }

    spec.headers.forEach { [name, values] ->
        header(name, values.first(), *(-1)(values).toTypedArray())
    }

    spec.cookies.forEach { (key, value) ->
        cookie(key, value)
    }

    spec.formParams.forEach { (key, value) ->
        when (value) {
            is Collection<*> -> formParam(key, *value.toTypedArray())
            is Array<*> -> formParam(key, *value)
            else -> if (value.isNotNull()) formParam(key, value)
        }
    }

    spec.multiParts.forEach {
        when (it.content) {
            is File -> {
                if (it.controlName.isNotNull()) multiPart(it.controlName, it.content, it.mimeType.toString())
                else multiPart(it.content)
            }
            is String -> multiPart(it.controlName, it.content, it.mimeType.toString())
            is ByteArray -> multiPart(it.controlName, it.fileName, it.content, it.mimeType.toString())
            is InputStream -> multiPart(it.controlName, it.fileName, it.content, it.mimeType.toString())
            else -> {
                if (it.fileName.isNotNull()) multiPart(it.controlName, it.fileName, it.content, it.mimeType.toString())
                else multiPart(it.controlName, it.content, it.mimeType.toString())
            }
        }
    }

    spec.body?.let {
        if (spec.contentType.isNull()) contentType(MediaType.APPLICATION_JSON.toString())
        when (it) {
            is String -> body(it)
            is ByteArray -> body(it)
            is File -> body(it)
            is InputStream -> body(it)
            else -> body(it)
        }
        body(it)
    }
}

// --- ENTRY POINT ---

/**
 * Configures and returns a DSL scope for defining RestAssured test routes.
 * Mirrors `restClientRouter` from the RestClient DSL.
 *
 * @param init A receiver function used to configure the [RestAssuredDslScope].
 * @return A configured instance of [RestAssuredDslScope].
 * @since 3.2.2
 */
fun restAssuredRouter(init: ReceiverConsumer<RestAssuredDslScope>): RestAssuredDslScope {
    val scope = RestAssuredDslScope()
    scope.init()
    return scope
}

/**
 * Configures and returns a DSL scope for defining RestAssured test routes with a base prefix.
 *
 * @param prefix The base path prefix for all routes.
 * @param init A receiver function used to configure the [RestAssuredDslScope].
 * @return A configured instance of [RestAssuredDslScope].
 * @since 3.2.2
 */
fun restAssuredRouter(prefix: String, init: ReceiverConsumer<RestAssuredDslScope>): RestAssuredDslScope {
    val scope = RestAssuredDslScope(prefix)
    scope.init()
    return scope
}