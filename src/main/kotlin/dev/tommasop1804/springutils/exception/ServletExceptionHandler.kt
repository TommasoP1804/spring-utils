package dev.tommasop1804.springutils.exception

import dev.tommasop1804.kutils.*
import dev.tommasop1804.springutils.ProblemDetail
import dev.tommasop1804.springutils.annotations.Feature
import dev.tommasop1804.springutils.annotations.InternalErrorCode
import dev.tommasop1804.springutils.findCallerMethod
import dev.tommasop1804.springutils.findCorrectException
import dev.tommasop1804.springutils.getStatus
import dev.tommasop1804.springutils.servlet.request.RequestIdProvider
import org.springframework.beans.ConversionNotSupportedException
import org.springframework.beans.TypeMismatchException
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.http.converter.HttpMessageNotWritableException
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.accept.InvalidApiVersionException
import org.springframework.web.accept.MissingApiVersionException
import org.springframework.web.accept.NotAcceptableApiVersionException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingPathVariableException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import org.springframework.web.servlet.resource.NoResourceFoundException
import tools.jackson.databind.DatabindException
import tools.jackson.databind.exc.MismatchedInputException
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.full.findAnnotation

@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(name = ["spring-utils.exceptions.body"], havingValue = "RFC", matchIfMissing = true)
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
class ServletExceptionHandler(private val environment: Environment) : ResponseEntityExceptionHandler() {
    companion object {
        internal fun extractErrorCode(ex: HttpMessageNotReadableException, backup: DatabindException?): InternalErrorCode? {
            val cause = ex.cause as? DatabindException ?: backup ?: return null
            val path = cause.path.takeIf { it.isNotEmpty() } ?: return null
            val fieldName = path.last().propertyName ?: return null

            val containingClass = if (path.size > 1) {
                path[path.size - 2].from()?.javaClass
            } else {
                when (val from = path.last().from()) {
                    is Class<*> -> from
                    else -> from?.javaClass
                }
            } ?: return null

            return containingClass.kotlin.declaredMembers
                .find { it.name == fieldName }
                ?.findAnnotation<InternalErrorCode>()
                ?: containingClass.kotlin.findAnnotation<InternalErrorCode>()
        }
    }

    @ExceptionHandler(Exception::class)
    fun handleAllException(e: Exception): ResponseEntity<ExtendedProblemDetail> {
        if (e is InvalidApiVersionException) return handleInvalidApiVersion(e)
        if (e is MissingApiVersionException) return handleMissingApiVersion(e)
        if (e is NotAcceptableApiVersionException) return handleNotAcceptableApiVersion(e)

        val status = if (e is ResponseStatusException) HttpStatus.valueOf(e.statusCode.value()) else getStatus(e)
        val message = (e.message?.substringAfter(" @@@ ")) ?: e::class.simpleName ?: e::class.qualifiedName ?: "Unknow error"

        val internalCode = environment
            .getProperty("spring-utils.exceptions.internal-error-code.${e::class.simpleName}")
            ?: environment.getProperty("spring-utils.exceptions.internal-error-code.default")
        
        val requestId = RequestIdProvider.requestId
        RequestIdProvider.requestIdThreadLocal.remove()
        return ResponseEntity(
            ProblemDetail(
                title = status.reasonPhrase,
                status = status,
                detail = message,
                internalErrorCode = e.message?.before(" @@@ ")?.ifBlank { null } ?: internalCode,
                exception = findCorrectException(e).let { it::class.simpleName ?: it::class.qualifiedName }
            ), HttpHeaders().apply {
                val featureCode = findFeatureCode()
                if (featureCode.isNotNullOrBlank())
                    put("Feature-Code", featureCode.asSingleList())
                requestId.ifNotNull {
                    if (environment.getProperty("spring-utils.exceptions.enable-request-id", "true").toBoolean())
                        put("Request-Id", toString().asSingleList())
                }
            }, status)
    }

    override fun handleHttpMessageNotReadable(
        ex: HttpMessageNotReadableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val httpStatus = HttpStatus.BAD_REQUEST
        val cause = ex.mostSpecificCause
        val mismatch = ex.cause as? DatabindException

        val isMissing = mismatch.isNotNull() && cause is MismatchedInputException
        val path = mismatch?.path?.joinToString(".") {
            val className = when (val from = it.from()) {
                is Class<*> -> from.kotlin.simpleName
                else -> from?.javaClass?.kotlin?.simpleName
            }.orEmpty()
            $$"$$className$${if (it.propertyName.isNotNull()) "$" else ""}$${it.propertyName.orEmpty()}"
        }
        val detail = if (isMissing) {
            $$"Missing required property: $$path"
        } else {
            $$"$$path: $${cause.message}"
        }

        val errorCode = extractErrorCode(ex, ex.cause as? DatabindException)
        var internalCode = when {
            isMissing -> errorCode?.ifMissing?.ifBlank { null }
            else -> errorCode?.ifInvalid
                ?.find { mapping ->
                    mapping.exceptions.any { exceptionClass ->
                        exceptionClass.isInstance(cause)
                    }
                }?.code
        }
        if (internalCode.isNull()) internalCode = environment
            .getProperty("spring-utils.exceptions.internal-error-code.${cause::class.simpleName}")
            ?: (if (isMissing) environment.getProperty("spring-utils.exceptions.internal-error-code.missing-property") else null)
            ?: environment.getProperty("spring-utils.exceptions.internal-error-code.default")

        val requestId = RequestIdProvider.requestId
        RequestIdProvider.requestIdThreadLocal.remove()
        return ResponseEntity(
            ProblemDetail(
                title = httpStatus.reasonPhrase,
                status = httpStatus,
                detail = detail,
                internalErrorCode = internalCode,
                exception = isMissing(onFalse = { cause::class.simpleName })
            ),
            HttpHeaders().apply {
                val featureCode = findFeatureCode()
                if (featureCode.isNotNullOrBlank())
                    put("Feature-Code", featureCode.asSingleList())
                requestId.ifNotNull {
                    if (environment.getProperty("spring-utils.exceptions.enable-request-id", "true").toBoolean())
                        put("Request-Id", toString().asSingleList())
                }
            },
            httpStatus
        )
    }

    override fun handleMissingPathVariable(
        ex: MissingPathVariableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val internalCode = environment
            .getProperty("spring-utils.exceptions.internal-error-code.missing-path-variable")
            ?: environment.getProperty("spring-utils.exceptions.internal-error-code.default")
        val status = HttpStatus.valueOf(ex.statusCode.value())

        val requestId = RequestIdProvider.requestId
        RequestIdProvider.requestIdThreadLocal.remove()
        return ResponseEntity(
            ProblemDetail(
                title = status.reasonPhrase,
                status = status,
                detail = $$"Missing path variable: $${ex.variableName} (`$${ex.parameter.containingClass.simpleName}$$${ex.parameter.method?.name}$$${ex.parameter.parameterName}` of type `$${ex.parameter.parameterType.simpleName}`)",
                internalErrorCode = internalCode,
                exception = ex.cause.isNotNull()({ ex.cause!!::class.simpleName ?: ex.cause!!::class.qualifiedName }, { ex::class.simpleName ?: ex::class.qualifiedName })
            ),
            HttpHeaders().apply {
                val featureCode = findFeatureCode()
                if (featureCode.isNotNullOrBlank())
                    put("Feature-Code", featureCode.asSingleList())
                requestId.ifNotNull {
                    if (environment.getProperty("spring-utils.exceptions.enable-request-id", "true").toBoolean())
                        put("Request-Id", toString().asSingleList())
                }
            },
            status
        )
    }

    override fun handleMissingServletRequestParameter(
        ex: MissingServletRequestParameterException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val internalCode = environment
            .getProperty("spring-utils.exceptions.internal-error-code.missing-request-param")
            ?: environment.getProperty("spring-utils.exceptions.internal-error-code.default")
        val status = HttpStatus.valueOf(ex.statusCode.value())

        val methodParameter = ex.methodParameter
        val methodParameterPresent = ex.methodParameter.isNotNull()
        val requestId = RequestIdProvider.requestId
        RequestIdProvider.requestIdThreadLocal.remove()
        return ResponseEntity(
            ProblemDetail(
                title = status.reasonPhrase,
                status = status,
                detail = $$"Missing request param: $${ex.parameterName}$${if (methodParameterPresent) " (`${methodParameter!!.containingClass.simpleName}.${methodParameter.method?.name}$${methodParameter.parameterName}` of type `${methodParameter.parameterType.simpleName}`)" else String.EMPTY}",
                internalErrorCode = internalCode,
                exception = ex.cause.isNotNull()({ ex.cause!!::class.simpleName ?: ex.cause!!::class.qualifiedName }, { ex::class.simpleName ?: ex::class.qualifiedName })
            ),
            HttpHeaders().apply {
                val featureCode = findFeatureCode()
                if (featureCode.isNotNullOrBlank())
                    put("Feature-Code", featureCode.asSingleList())
                requestId.ifNotNull {
                    if (environment.getProperty("spring-utils.exceptions.enable-request-id", "true").toBoolean())
                        put("Request-Id", toString().asSingleList())
                }
            },
            status
        )
    }

    override fun handleMissingServletRequestPart(
        ex: MissingServletRequestPartException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val internalCode = environment
            .getProperty("spring-utils.exceptions.internal-error-code.missing-request-part")
            ?: environment.getProperty("spring-utils.exceptions.internal-error-code.default")
        val status = HttpStatus.valueOf(ex.statusCode.value())

        val requestId = RequestIdProvider.requestId
        RequestIdProvider.requestIdThreadLocal.remove()
        return ResponseEntity(
            ProblemDetail(
                title = status.reasonPhrase,
                status = status,
                detail = $$"Missing request part: $${ex.requestPartName}",
                internalErrorCode = internalCode,
                exception = ex.cause.isNotNull()({ ex.cause!!::class.simpleName ?: ex.cause!!::class.qualifiedName }, { ex::class.simpleName ?: ex::class.qualifiedName })
            ),
            HttpHeaders().apply {
                val featureCode = findFeatureCode()
                if (featureCode.isNotNullOrBlank())
                    put("Feature-Code", featureCode.asSingleList())
                requestId.ifNotNull {
                    if (environment.getProperty("spring-utils.exceptions.enable-request-id", "true").toBoolean())
                        put("Request-Id", toString().asSingleList())
                }
            },
            status
        )
    }

    override fun handleHttpRequestMethodNotSupported(
        ex: HttpRequestMethodNotSupportedException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val internalCode = environment
            .getProperty("spring-utils.exceptions.internal-error-code.method-not-supported")
            ?: environment.getProperty("spring-utils.exceptions.internal-error-code.default")
        val status = HttpStatus.valueOf(ex.statusCode.value())

        val requestId = RequestIdProvider.requestId
        RequestIdProvider.requestIdThreadLocal.remove()
        return ResponseEntity(
            ProblemDetail(
                title = status.reasonPhrase,
                status = status,
                detail = "HTTP method not supported: ${ex.method}${if (ex.supportedMethods.isNotNull() && ex.supportedMethods!!.isNotEmpty()) ". Choose one of [${ex.supportedMethods!!.joinToString(", ")}]" else String.EMPTY}",
                internalErrorCode = internalCode,
                exception = ex.cause.isNotNull()({ ex.cause!!::class.simpleName ?: ex.cause!!::class.qualifiedName }, { ex::class.simpleName ?: ex::class.qualifiedName })
            ),
            HttpHeaders().apply {
                val featureCode = findFeatureCode()
                if (featureCode.isNotNullOrBlank())
                    put("Feature-Code", featureCode.asSingleList())
                requestId.ifNotNull {
                    if (environment.getProperty("spring-utils.exceptions.enable-request-id", "true").toBoolean())
                        put("Request-Id", toString().asSingleList())
                }
            },
            status
        )
    }

    override fun handleHttpMediaTypeNotSupported(
        ex: HttpMediaTypeNotSupportedException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val internalCode = environment
            .getProperty("spring-utils.exceptions.internal-error-code.media-type-not-supported")
            ?: environment.getProperty("spring-utils.exceptions.internal-error-code.default")
        val status = HttpStatus.valueOf(ex.statusCode.value())

        val requestId = RequestIdProvider.requestId
        RequestIdProvider.requestIdThreadLocal.remove()
        return ResponseEntity(
            ProblemDetail(
                title = status.reasonPhrase,
                status = status,
                detail = "HTTP media type not supported: ${ex.contentType}. Choose one of [${ex.supportedMediaTypes.joinToString(", ")}]",
                internalErrorCode = internalCode,
                exception = ex.cause.isNotNull()({ ex.cause!!::class.simpleName ?: ex.cause!!::class.qualifiedName }, { ex::class.simpleName ?: ex::class.qualifiedName })
            ),
            HttpHeaders().apply {
                val featureCode = findFeatureCode()
                if (featureCode.isNotNullOrBlank())
                    put("Feature-Code", featureCode.asSingleList())
                requestId.ifNotNull {
                    if (environment.getProperty("spring-utils.exceptions.enable-request-id", "true").toBoolean())
                        put("Request-Id", toString().asSingleList())
                }
            },
            status
        )
    }

    override fun handleHttpMediaTypeNotAcceptable(
        ex: HttpMediaTypeNotAcceptableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val internalCode = environment
            .getProperty("spring-utils.exceptions.internal-error-code.media-type-not-acceptable")
            ?: environment.getProperty("spring-utils.exceptions.internal-error-code.default")
        val status = HttpStatus.valueOf(ex.statusCode.value())
        
        val requestId = RequestIdProvider.requestId
        RequestIdProvider.requestIdThreadLocal.remove()
        return ResponseEntity(
            ProblemDetail(
                title = status.reasonPhrase,
                status = status,
                detail = "HTTP media type not acceptable. Choose one of [${ex.supportedMediaTypes.joinToString(", ")}]",
                internalErrorCode = internalCode,
                exception = ex.cause.isNotNull()({ ex.cause!!::class.simpleName ?: ex.cause!!::class.qualifiedName }, { ex::class.simpleName ?: ex::class.qualifiedName })
            ),
            HttpHeaders().apply {
                val featureCode = findFeatureCode()
                if (featureCode.isNotNullOrBlank())
                    put("Feature-Code", featureCode.asSingleList())
                requestId.ifNotNull {
                    if (environment.getProperty("spring-utils.exceptions.enable-request-id", "true").toBoolean())
                        put("Request-Id", toString().asSingleList())
                }
            },
            status
        )
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val internalErrorCode = environment
            .getProperty("spring-utils.exceptions.internal-error-code.invalid-method-argument")
            ?: environment.getProperty("spring-utils.exceptions.internal-error-code.default")
        val status = HttpStatus.valueOf(ex.statusCode.value())

        val requestId = RequestIdProvider.requestId
        RequestIdProvider.requestIdThreadLocal.remove()
        return ResponseEntity(
            ProblemDetail(
                title = status.reasonPhrase,
                status = status,
                detail = "Invalid parameter: ${ex.parameter} ${"(`${ex.parameter.containingClass.simpleName}.${ex.parameter.method?.name}$${ex.parameter.parameterName}` of type `${ex.parameter.parameterType.simpleName}`)"}" + ex.bindingResult.fieldErrors.joinToString(", ") { "; Invalid value for field '${it.field}': ${it.defaultMessage}" },
                internalErrorCode = internalErrorCode,
                exception = ex.cause.isNotNull()({ ex.cause!!::class.simpleName ?: ex.cause!!::class.qualifiedName }, { ex::class.simpleName ?: ex::class.qualifiedName })
            ),
            HttpHeaders().apply {
                val featureCode = findFeatureCode()
                if (featureCode.isNotNullOrBlank())
                    put("Feature-Code", featureCode.asSingleList())
                requestId.ifNotNull {
                    if (environment.getProperty("spring-utils.exceptions.enable-request-id", "true").toBoolean())
                        put("Request-Id", toString().asSingleList())
                }
            },
            status
        )
    }

    override fun handleNoResourceFoundException(
        ex: NoResourceFoundException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val internalErrorCode = environment
            .getProperty("spring-utils.exceptions.internal-error-code.resource-not-found")
            ?: environment.getProperty("spring-utils.exceptions.internal-error-code.default")
        val status = HttpStatus.valueOf(ex.statusCode.value())

        val requestId = RequestIdProvider.requestId
        RequestIdProvider.requestIdThreadLocal.remove()
        return ResponseEntity(
            ProblemDetail(
                title = status.reasonPhrase,
                status = status,
                detail = "Resource with this path not found",
                internalErrorCode = internalErrorCode,
                exception = ex.cause.isNotNull()({ ex.cause!!::class.simpleName ?: ex.cause!!::class.qualifiedName }, { ex::class.simpleName ?: ex::class.qualifiedName })
            ),
            HttpHeaders().apply {
                val featureCode = findFeatureCode()
                if (featureCode.isNotNullOrBlank())
                    put("Feature-Code", featureCode.asSingleList())
                requestId.ifNotNull {
                    if (environment.getProperty("spring-utils.exceptions.enable-request-id", "true").toBoolean())
                        put("Request-Id", toString().asSingleList())
                }
            },
            status
        )
    }

    override fun handleConversionNotSupported(
        ex: ConversionNotSupportedException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val internalErrorCode = environment
            .getProperty("spring-utils.exceptions.internal-error-code.conversion-not-supported")
            ?: environment.getProperty("spring-utils.exceptions.internal-error-code.default")
        val status = HttpStatus.INTERNAL_SERVER_ERROR

        val requestId = RequestIdProvider.requestId
        RequestIdProvider.requestIdThreadLocal.remove()
        return ResponseEntity(
            ProblemDetail(
                title = status.reasonPhrase,
                status = status,
                detail = "Conversion not supported: ${ex.message}",
                internalErrorCode = internalErrorCode,
                exception = ex.cause.isNotNull()({ ex.cause!!::class.simpleName ?: ex.cause!!::class.qualifiedName }, { ex::class.simpleName ?: ex::class.qualifiedName })
            ),
            HttpHeaders().apply {
                val featureCode = findFeatureCode()
                if (featureCode.isNotNullOrBlank())
                    put("Feature-Code", featureCode.asSingleList())
                requestId.ifNotNull {
                    if (environment.getProperty("spring-utils.exceptions.enable-request-id", "true").toBoolean())
                        put("Request-Id", toString().asSingleList())
                }
            },
            status
        )
    }

    override fun handleTypeMismatch(
        ex: TypeMismatchException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val internalErrorCode = environment
            .getProperty("spring-utils.exceptions.internal-error-code.type-mismatch")
            ?: environment.getProperty("spring-utils.exceptions.internal-error-code.default")
        val status = HttpStatus.INTERNAL_SERVER_ERROR

        val requestId = RequestIdProvider.requestId
        RequestIdProvider.requestIdThreadLocal.remove()
        return ResponseEntity(
            ProblemDetail(
                title = status.reasonPhrase,
                status = status,
                detail = "Type mismatch. Required `${ex.requiredType?.simpleName}`",
                internalErrorCode = internalErrorCode,
                exception = ex.cause.isNotNull()({ ex.cause!!::class.simpleName ?: ex.cause!!::class.qualifiedName }, { ex::class.simpleName ?: ex::class.qualifiedName })
            ),
            HttpHeaders().apply {
                val featureCode = findFeatureCode()
                if (featureCode.isNotNullOrBlank())
                    put("Feature-Code", featureCode.asSingleList())
                requestId.ifNotNull {
                    if (environment.getProperty("spring-utils.exceptions.enable-request-id", "true").toBoolean())
                        put("Request-Id", toString().asSingleList())
                }
            },
            status
        )
    }

    override fun handleMaxUploadSizeExceededException(
        ex: MaxUploadSizeExceededException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val internalCode = environment
            .getProperty("spring-utils.exceptions.internal-error-code.max-upload-size-exceeded")
            ?: environment.getProperty("spring-utils.exceptions.internal-error-code.default")
        val status = HttpStatus.valueOf(ex.statusCode.value())

        val requestId = RequestIdProvider.requestId
        RequestIdProvider.requestIdThreadLocal.remove()
        return ResponseEntity(
            ProblemDetail(
                title = status.reasonPhrase,
                status = status,
                detail = "Maximum upload size exceeded. Allowed: ${(if (ex.maxUploadSize == -1L) "unknown number of" else ex.maxUploadSize)} bytes",
                internalErrorCode = internalCode,
                exception = ex.cause.isNotNull()({ ex.cause!!::class.simpleName ?: ex.cause!!::class.qualifiedName }, { ex::class.simpleName ?: ex::class.qualifiedName })
            ),
            HttpHeaders().apply {
                val featureCode = findFeatureCode()
                if (featureCode.isNotNullOrBlank())
                    put("Feature-Code", featureCode.asSingleList())
                requestId.ifNotNull {
                    if (environment.getProperty("spring-utils.exceptions.enable-request-id", "true").toBoolean())
                        put("Request-Id", toString().asSingleList())
                }
            },
            status
        )
    }

    override fun handleHttpMessageNotWritable(
        ex: HttpMessageNotWritableException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest
    ): ResponseEntity<Any>? {
        val internalCode = environment
            .getProperty("spring-utils.exceptions.internal-error-code.http-message-not-writable")
            ?: environment.getProperty("spring-utils.exceptions.internal-error-code.default")
        val status = HttpStatus.INTERNAL_SERVER_ERROR

        val requestId = RequestIdProvider.requestId
        RequestIdProvider.requestIdThreadLocal.remove()
        return ResponseEntity(
            ProblemDetail(
                title = status.reasonPhrase,
                status = status,
                detail = "Failed to write HTTP message. ${ex.message}",
                internalErrorCode = internalCode,
                exception = ex.cause.isNotNull()({ ex.cause!!::class.simpleName ?: ex.cause!!::class.qualifiedName }, { ex::class.simpleName ?: ex::class.qualifiedName })
            ),
            HttpHeaders().apply {
                val featureCode = findFeatureCode()
                if (featureCode.isNotNullOrBlank())
                    put("Feature-Code", featureCode.asSingleList())
                requestId.ifNotNull {
                    if (environment.getProperty("spring-utils.exceptions.enable-request-id", "true").toBoolean())
                        put("Request-Id", toString().asSingleList())
                }
            },
            status
        )
    }

    private fun handleMissingApiVersion(
        ex: MissingApiVersionException,
    ): ResponseEntity<ExtendedProblemDetail> {
        val internalErrorCode = environment
            .getProperty("spring-utils.exceptions.internal-error-code.missing-api-version")
            ?: environment.getProperty("spring-utils.exceptions.internal-error-code.default")
        val status = HttpStatus.valueOf(ex.statusCode.value())

        val requestId = RequestIdProvider.requestId
        RequestIdProvider.requestIdThreadLocal.remove()
        return ResponseEntity(
            ProblemDetail(
                title = status.reasonPhrase,
                status = status,
                detail = "Missing API version",
                internalErrorCode = internalErrorCode,
                exception = ex::class.simpleName ?: ex::class.qualifiedName
            ),
            HttpHeaders().apply {
                val featureCode = findFeatureCode()
                if (featureCode.isNotNullOrBlank())
                    put("Feature-Code", featureCode.asSingleList())
                requestId.ifNotNull {
                    if (environment.getProperty("spring-utils.exceptions.enable-request-id", "true").toBoolean())
                        put("Request-Id", toString().asSingleList())
                }
            },
            status
        )
    }

    private fun handleInvalidApiVersion(
        ex: InvalidApiVersionException
    ): ResponseEntity<ExtendedProblemDetail> {
        val internalErrorCode = environment
            .getProperty("spring-utils.exceptions.internal-error-code.invalid-api-version")
            ?: environment.getProperty("spring-utils.exceptions.internal-error-code.default")
        val status = HttpStatus.valueOf(ex.statusCode.value())

        val requestId = RequestIdProvider.requestId
        RequestIdProvider.requestIdThreadLocal.remove()
        return ResponseEntity(
            ProblemDetail(
                title = status.reasonPhrase,
                status = status,
                detail = "Invalid API version `${ex.version}`",
                internalErrorCode = internalErrorCode,
                exception = ex::class.simpleName ?: ex::class.qualifiedName
            ),
            HttpHeaders().apply {
                val featureCode = findFeatureCode()
                if (featureCode.isNotNullOrBlank())
                    put("Feature-Code", featureCode.asSingleList())
                requestId.ifNotNull {
                    if (environment.getProperty("spring-utils.exceptions.enable-request-id", "true").toBoolean())
                        put("Request-Id", toString().asSingleList())
                }
            },
            status
        )
    }

    private fun handleNotAcceptableApiVersion(
        ex: NotAcceptableApiVersionException
    ): ResponseEntity<ExtendedProblemDetail> {
        val internalErrorCode = environment
            .getProperty("spring-utils.exceptions.internal-error-code.not-acceptable-api-version")
            ?: environment.getProperty("spring-utils.exceptions.internal-error-code.default")
        val status = HttpStatus.valueOf(ex.statusCode.value())

        val requestId = RequestIdProvider.requestId
        RequestIdProvider.requestIdThreadLocal.remove()
        return ResponseEntity(
            ProblemDetail(
                title = status.reasonPhrase,
                status = status,
                detail = "API version `${ex.version}` is not acceptable",
                internalErrorCode = internalErrorCode,
                exception = ex::class.simpleName ?: ex::class.qualifiedName
            ),
            HttpHeaders().apply {
                val featureCode = findFeatureCode()
                if (featureCode.isNotNullOrBlank())
                    put("Feature-Code", featureCode.asSingleList())
                requestId.ifNotNull {
                    if (environment.getProperty("spring-utils.exceptions.enable-request-id", "true").toBoolean())
                        put("Request-Id", toString().asSingleList())
                }
            },
            status
        )
    }

    internal fun findFeatureCode() =
        if (environment.getProperty("spring-utils.exceptions.enable-feature-code", "true").toBoolean())
            RequestIdProvider.featureCode.get() ?: findCallerMethod()?.getAnnotation(Feature::class.java)?.code ?: String.EMPTY
        else null
}