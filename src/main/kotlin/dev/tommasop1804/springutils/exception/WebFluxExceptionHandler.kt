package dev.tommasop1804.springutils.exception

import dev.tommasop1804.kutils.asSingleList
import dev.tommasop1804.kutils.before
import dev.tommasop1804.kutils.invoke
import dev.tommasop1804.kutils.isNotNull
import dev.tommasop1804.kutils.isNull
import dev.tommasop1804.springutils.ProblemDetail
import dev.tommasop1804.springutils.exception.ExceptionHandler.Companion.findFeatureAnnotation
import dev.tommasop1804.springutils.getStatus
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.annotation.Order
import org.springframework.core.codec.DecodingException
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import org.springframework.web.reactive.resource.NoResourceFoundException
import org.springframework.web.server.*
import reactor.core.publisher.Mono
import tools.jackson.databind.DatabindException
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.exc.MismatchedInputException
import kotlin.text.isNullOrBlank

@ConditionalOnProperty(name = ["spring-utils.exceptions.body"], havingValue = "RFC", matchIfMissing = true)
@Component
@Order(-2)
@Suppress("unused")
class WebFluxExceptionHandler(
    private val environment: Environment,
    private val objectMapper: ObjectMapper
) : WebExceptionHandler {

    override fun handle(exchange: ServerWebExchange, e: Throwable): Mono<Void> {
        val status = getStatus(e)
        val message = (e.message?.substringAfter(" @@@ ")) ?: e::class.simpleName ?: e::class.qualifiedName ?: "Unknow error"

        val internalCode = environment
            .getProperty("spring-utils.exceptions.internal-error-code.${e::class.simpleName}")
            ?: environment.getProperty("spring-utils.exceptions.internal-error-code.default")

        val response = exchange.response
        response.statusCode = status
        response.headers.put("Feature-Code", findFeatureAnnotation().asSingleList())
        response.headers.contentType = MediaType.APPLICATION_JSON
        val body = ProblemDetail(
            title = status.reasonPhrase,
            status = status,
            detail = message,
            internalErrorCode = e.message?.before(" @@@ ")?.ifBlank { null } ?: internalCode,
            exception = e.cause.isNotNull()({ (e.cause!!::class.simpleName ?: e.cause!!::class.qualifiedName) }, { e::class.simpleName })
        )
        return response.writeWith(Mono.just(response.bufferFactory().wrap(objectMapper.writeValueAsString(body).toByteArray(Charsets.UTF_8))))
    }

    suspend fun filter(
        request: ServerRequest,
        next: suspend (ServerRequest) -> ServerResponse
    ): ServerResponse {
        return try {
            next(request)
        } catch (ex: Throwable) {
            handleException(ex)
        }
    }

    suspend fun handleException(ex: Throwable): ServerResponse = when (ex) {
        is DecodingException -> handleMessageNotReadable(ex)
        is ServerWebInputException -> handleServerWebInputException(ex)
        is MethodNotAllowedException -> handleMethodNotAllowed(ex)
        is UnsupportedMediaTypeStatusException -> handleMediaTypeNotSupported(ex)
        is NotAcceptableStatusException -> handleMediaTypeNotAcceptable(ex)
        is NoResourceFoundException -> handleNoResourceFound(ex)
        is ResponseStatusException -> handleResponseStatusException(ex)
        else -> handleGenericException(ex)
    }

    private suspend fun handleMessageNotReadable(ex: DecodingException): ServerResponse {
        val httpStatus = HttpStatus.BAD_REQUEST
        val cause = ex.mostSpecificCause

        val databindEx = findCause<DatabindException>(ex)
        val mismatchEx = findCause<MismatchedInputException>(ex)

        val isMissing = mismatchEx.isNotNull()
        val path = databindEx?.path?.joinToString(".") { ref ->
            val className = when (val from = ref.from()) {
                is Class<*> -> from.kotlin.simpleName
                else -> from?.javaClass?.kotlin?.simpleName
            }.orEmpty()
            buildString {
                append(className)
                if (ref.propertyName.isNotNull()) {
                    append(".")
                    append(ref.propertyName)
                }
            }
        }

        val detail = if (isMissing) {
            "Missing required property: $path"
        } else {
            "$path: ${cause.message}"
        }

        val errorCode = resolveInternalErrorCode(
            when {
                isMissing -> "missing-property"
                else -> cause::class.simpleName
            }
        )

        return buildErrorResponse(
            status = httpStatus,
            detail = detail,
            internalErrorCode = errorCode,
            exception = if (isMissing) null else cause::class.simpleName,
        )
    }

    private suspend fun handleServerWebInputException(ex: ServerWebInputException): ServerResponse {
        val httpStatus = HttpStatus.valueOf(ex.statusCode.value())

        val detail = buildString {
            append("Invalid input")
            val reason = ex.reason
            if (!reason.isNullOrBlank()) {
                append(": ")
                append(reason)
            }
            val methodParam = ex.methodParameter
            if (methodParam.isNotNull()) {
                append(" (`${methodParam.containingClass.simpleName}")
                append(".${methodParam.method?.name}")
                append(".${methodParam.parameterName}`")
                append(" of type `${methodParam.parameterType.simpleName}`)")
            }
        }

        val errorCode = resolveInternalErrorCode("invalid-input")

        return buildErrorResponse(
            status = httpStatus,
            detail = detail,
            internalErrorCode = errorCode,
            exception = ex.cause?.let { it::class.simpleName } ?: ex::class.simpleName,
        )
    }

    private suspend fun handleMethodNotAllowed(ex: MethodNotAllowedException): ServerResponse {
        val httpStatus = HttpStatus.METHOD_NOT_ALLOWED

        val supported = ex.supportedMethods
        val detail = buildString {
            append("HTTP method not supported: ${ex.httpMethod}")
            if (supported.isNotEmpty()) {
                append(". Choose one of [${supported.joinToString(", ")}]")
            }
        }

        val errorCode = resolveInternalErrorCode("method-not-supported")

        return buildErrorResponse(
            status = httpStatus,
            detail = detail,
            internalErrorCode = errorCode,
            exception = ex.cause?.let { it::class.simpleName } ?: ex::class.simpleName,
        )
    }

    private suspend fun handleMediaTypeNotSupported(ex: UnsupportedMediaTypeStatusException): ServerResponse {
        val httpStatus = HttpStatus.UNSUPPORTED_MEDIA_TYPE

        val detail = buildString {
            append("HTTP media type not supported: ${ex.contentType}")
            val supported = ex.supportedMediaTypes
            if (supported.isNotEmpty()) {
                append(". Choose one of [${supported.joinToString(", ")}]")
            }
        }

        val errorCode = resolveInternalErrorCode("media-type-not-supported")

        return buildErrorResponse(
            status = httpStatus,
            detail = detail,
            internalErrorCode = errorCode,
            exception = ex.cause?.let { it::class.simpleName } ?: ex::class.simpleName,
        )
    }

    private suspend fun handleMediaTypeNotAcceptable(ex: NotAcceptableStatusException): ServerResponse {
        val httpStatus = HttpStatus.NOT_ACCEPTABLE

        val detail = buildString {
            append("HTTP media type not acceptable")
            val supported = ex.supportedMediaTypes
            if (supported.isNotEmpty()) {
                append(". Choose one of [${supported.joinToString(", ")}]")
            }
        }

        val errorCode = resolveInternalErrorCode("media-type-not-acceptable")

        return buildErrorResponse(
            status = httpStatus,
            detail = detail,
            internalErrorCode = errorCode,
            exception = ex.cause?.let { it::class.simpleName } ?: ex::class.simpleName,
        )
    }

    private suspend fun handleNoResourceFound(ex: NoResourceFoundException): ServerResponse {
        val httpStatus = HttpStatus.NOT_FOUND

        val errorCode = resolveInternalErrorCode("resource-not-found")

        return buildErrorResponse(
            status = httpStatus,
            detail = "Resource with this path not found",
            internalErrorCode = errorCode,
            exception = ex.cause?.let { it::class.simpleName } ?: ex::class.simpleName,
        )
    }

    private suspend fun handleResponseStatusException(ex: ResponseStatusException): ServerResponse {
        val httpStatus = HttpStatus.valueOf(ex.statusCode.value())

        val errorCode = resolveInternalErrorCode(ex::class.simpleName)

        return buildErrorResponse(
            status = httpStatus,
            detail = ex.reason ?: httpStatus.reasonPhrase,
            internalErrorCode = errorCode,
            exception = ex.cause?.let { it::class.simpleName } ?: ex::class.simpleName,
        )
    }

    private suspend fun handleGenericException(ex: Throwable): ServerResponse {
        val httpStatus = HttpStatus.INTERNAL_SERVER_ERROR

        val errorCode = resolveInternalErrorCode("default")

        return buildErrorResponse(
            status = httpStatus,
            detail = "Internal server error",
            internalErrorCode = errorCode,
            exception = ex::class.simpleName,
        )
    }

    private suspend fun buildErrorResponse(
        status: HttpStatus,
        detail: String,
        internalErrorCode: String?,
        exception: String?,
    ): ServerResponse {
        val problemDetail = ProblemDetail(
            title = status.reasonPhrase,
            status = status.value(),
            detail = detail,
            internalErrorCode = internalErrorCode,
            exception = exception,
        )

        val builder = ServerResponse.status(status)
            .contentType(MediaType.APPLICATION_JSON)

        return builder.bodyValueAndAwait(problemDetail)
    }

    private fun resolveInternalErrorCode(key: String?): String? {
        if (key.isNull()) return resolveInternalErrorCode("default")
        return environment.getProperty("spring-utils.exceptions.internal-error-code.$key")
            ?: environment.getProperty("spring-utils.exceptions.internal-error-code.default")
    }

    private inline fun <reified T> findCause(ex: Throwable): T? {
        var current: Throwable? = ex
        while (current.isNotNull()) {
            if (current is T) return current
            current = current.cause
        }
        return null
    }
}