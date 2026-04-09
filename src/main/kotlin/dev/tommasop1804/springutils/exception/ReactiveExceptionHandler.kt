/*
 * Copyright © 2026 Tommaso Pastorelli (TommasoP1804) | Spring-Utils
 */

package dev.tommasop1804.springutils.exception

import dev.tommasop1804.kutils.before
import dev.tommasop1804.kutils.isNotNull
import dev.tommasop1804.kutils.isNull
import dev.tommasop1804.springutils.ProblemDetail
import dev.tommasop1804.springutils.findCorrectException
import dev.tommasop1804.springutils.getStatus
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.codec.DecodingException
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.accept.InvalidApiVersionException
import org.springframework.web.accept.MissingApiVersionException
import org.springframework.web.accept.NotAcceptableApiVersionException
import org.springframework.web.reactive.resource.NoResourceFoundException
import org.springframework.web.server.*
import reactor.core.publisher.Mono
import tools.jackson.databind.DatabindException
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.exc.MismatchedInputException

@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnProperty(name = ["spring-utils.exceptions.body"], havingValue = "RFC", matchIfMissing = true)
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Suppress("unused")
class ReactiveExceptionHandler(
    private val environment: Environment,
    private val objectMapper: ObjectMapper
) : WebExceptionHandler {

    override fun handle(exchange: ServerWebExchange, e: Throwable): Mono<Void> {
        val status = if (e is ResponseStatusException) HttpStatus.valueOf(e.statusCode.value()) else getStatus(e)
        val message = (e.message?.substringAfter(" @@@ ")) ?: e::class.simpleName ?: e::class.qualifiedName ?: "Unknow error"

        val internalCode = environment
            .getProperty("spring-utils.exceptions.internal-error-code.${e::class.simpleName}")
            ?: environment.getProperty("spring-utils.exceptions.internal-error-code.default")

        val response = exchange.response
        response.statusCode = status
        response.headers.contentType = MediaType.APPLICATION_JSON
        
        val body = when (e) {
            is DecodingException -> handleMessageNotReadable(e)
            is ServerWebInputException -> handleServerWebInputException(e)
            is MethodNotAllowedException -> handleMethodNotAllowed(e)
            is UnsupportedMediaTypeStatusException -> handleMediaTypeNotSupported(e)
            is NotAcceptableStatusException -> handleMediaTypeNotAcceptable(e)
            is NoResourceFoundException -> handleNoResourceFound(e)
            is NotAcceptableApiVersionException -> handleNotAcceptableApiVersion(e)
            is InvalidApiVersionException -> handleInvalidApiVersion(e)
            is MissingApiVersionException -> handleMissingApiVersion(e)
            is ResponseStatusException -> handleResponseStatusException(e)
            else -> {
                ProblemDetail(
                    title = status.reasonPhrase,
                    status = status,
                    detail = message,
                    internalErrorCode = e.message?.before(" @@@ ")?.ifBlank { null } ?: internalCode,
                    exception = findCorrectException(e).let { it::class.simpleName ?: it::class.qualifiedName }
                )
            }
        }
        
        return response.writeWith(Mono.just(response.bufferFactory().wrap(objectMapper.writeValueAsString(body).toByteArray(Charsets.UTF_8))))
    }

    private fun handleMessageNotReadable(ex: DecodingException): ExtendedProblemDetail {
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

    private fun handleServerWebInputException(ex: ServerWebInputException): ExtendedProblemDetail {
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

    private fun handleMethodNotAllowed(ex: MethodNotAllowedException): ExtendedProblemDetail {
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

    private fun handleMediaTypeNotSupported(ex: UnsupportedMediaTypeStatusException): ExtendedProblemDetail {
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

    private fun handleMediaTypeNotAcceptable(ex: NotAcceptableStatusException): ExtendedProblemDetail {
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

    private fun handleNoResourceFound(ex: NoResourceFoundException): ExtendedProblemDetail {
        val httpStatus = HttpStatus.NOT_FOUND

        val errorCode = resolveInternalErrorCode("resource-not-found")

        return buildErrorResponse(
            status = httpStatus,
            detail = "Resource with this path not found",
            internalErrorCode = errorCode,
            exception = ex.cause?.let { it::class.simpleName } ?: ex::class.simpleName,
        )
    }

    private fun handleResponseStatusException(ex: ResponseStatusException): ExtendedProblemDetail {
        val httpStatus = HttpStatus.valueOf(ex.statusCode.value())

        val errorCode = resolveInternalErrorCode(ex::class.simpleName)

        return buildErrorResponse(
            status = httpStatus,
            detail = ex.reason ?: httpStatus.reasonPhrase,
            internalErrorCode = errorCode,
            exception = ex.cause?.let { it::class.simpleName } ?: ex::class.simpleName,
        )
    }

    private fun handleInvalidApiVersion(ex: InvalidApiVersionException): ExtendedProblemDetail {
        val httpStatus = HttpStatus.BAD_REQUEST

        val errorCode = resolveInternalErrorCode("invalid-api-version")

        return buildErrorResponse(
            status = httpStatus,
            detail = "Invalid API version `${ex.version}`",
            internalErrorCode = errorCode,
            exception = ex.cause?.let { it::class.simpleName } ?: ex::class.simpleName,
        )
    }

    private fun handleMissingApiVersion(ex: MissingApiVersionException): ExtendedProblemDetail {
        val httpStatus = HttpStatus.BAD_REQUEST
        return buildErrorResponse(
            status = httpStatus,
            detail = "Missing API version",
            internalErrorCode = resolveInternalErrorCode("missing-api-version"),
            exception = ex.cause?.let { it::class.simpleName } ?: ex::class.simpleName,
        )
    }

    private fun handleNotAcceptableApiVersion(ex: NotAcceptableApiVersionException): ExtendedProblemDetail {
        val httpStatus = HttpStatus.NOT_ACCEPTABLE
        return buildErrorResponse(
            status = httpStatus,
            detail = "API version `${ex.version}` not acceptable",
            internalErrorCode = resolveInternalErrorCode("not-acceptable-api-version"),
            exception = ex.cause?.let { it::class.simpleName } ?: ex::class.simpleName,
        )
    }

    private fun buildErrorResponse(
        status: HttpStatus,
        detail: String,
        internalErrorCode: String?,
        exception: String?,
    ): ExtendedProblemDetail {
        return ProblemDetail(
            title = status.reasonPhrase,
            status = status.value(),
            detail = detail,
            internalErrorCode = internalErrorCode,
            exception = exception,
        )
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