package dev.tommasop1804.springutils.exception

import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import dev.tommasop1804.kutils.EMPTY
import dev.tommasop1804.kutils.asSingleList
import dev.tommasop1804.kutils.before
import dev.tommasop1804.kutils.invoke
import dev.tommasop1804.kutils.isNotNull
import dev.tommasop1804.kutils.isNotNullOrBlank
import dev.tommasop1804.kutils.isNotNullOrEmpty
import dev.tommasop1804.springutils.ProblemDetail
import dev.tommasop1804.springutils.annotations.Feature
import dev.tommasop1804.springutils.annotations.InternalErrorCode
import dev.tommasop1804.springutils.findCallerMethod
import dev.tommasop1804.springutils.getStatus
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.*
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.DatabindException
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.exc.MismatchedInputException
import kotlin.apply
import kotlin.reflect.full.declaredMembers
import kotlin.reflect.full.findAnnotation

@ConditionalOnProperty(name = ["spring-utils.exceptions.body"], havingValue = "RFC", matchIfMissing = true)
@ControllerAdvice
class ExceptionHandler : ResponseEntityExceptionHandler() {
    @JsonSerialize(using = ExtendedProblemDetail.Companion.Serializer::class)
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = ExtendedProblemDetail.Companion.OldSerializer::class)
    data class ExtendedProblemDetail(
        val internalErrorCode: String? = null,
        val exception: String? = null
    ) : ProblemDetail() {
        constructor(problemDetail: ProblemDetail, internalErrorCode: String? = null, exception: String? = null) : this(internalErrorCode, exception) {
            this.title = problemDetail.title
            this.type = problemDetail.type
            this.status = problemDetail.status
            this.detail = problemDetail.detail
            this.instance = problemDetail.instance
            this.properties = problemDetail.properties
        }

        companion object {
            class Serializer : ValueSerializer<ExtendedProblemDetail>() {
                override fun serialize(value: ExtendedProblemDetail, gen: JsonGenerator, ctxt: SerializationContext) {
                    gen.writeStartObject()
                    if (value.title.isNotNull()) gen.writeStringProperty("title", value.title)
                    if (value.type.isNotNull()) gen.writeStringProperty("type", value.type.toString())
                    if (value.status.isNotNull()) gen.writeNumberProperty("status", value.status)
                    if (value.detail.isNotNull()) gen.writeStringProperty("detail", value.detail)
                    if (value.instance.isNotNull()) gen.writeStringProperty("instance", value.instance.toString())
                    if (value.internalErrorCode.isNotNull()) gen.writeStringProperty("internalErrorCode", value.internalErrorCode)
                    if (value.exception.isNotNull()) gen.writeStringProperty("exception", value.exception)
                    if (value.properties.isNotNullOrEmpty()) gen.writePOJOProperty("properties", value.properties)
                    gen.writeEndObject()
                }
            }

            class OldSerializer : JsonSerializer<ExtendedProblemDetail>() {
                override fun serialize(value: ExtendedProblemDetail, gen: com.fasterxml.jackson.core.JsonGenerator, serializers: SerializerProvider) {
                    gen.writeStartObject()
                    if (value.title.isNotNull()) gen.writeStringField("title", value.title)
                    if (value.type.isNotNull()) gen.writeStringField("type", value.type.toString())
                    if (value.status.isNotNull()) gen.writeNumberField("status", value.status)
                    if (value.detail.isNotNull()) gen.writeStringField("detail", value.detail)
                    if (value.instance.isNotNull()) gen.writeStringField("instance", value.instance.toString())
                    if (value.internalErrorCode.isNotNull()) gen.writeStringField(
                        "internalErrorCode",
                        value.internalErrorCode
                    )
                    if (value.exception.isNotNull()) gen.writeStringField("exception", value.exception)
                    if (value.properties.isNotNullOrEmpty()) gen.writeObjectField("properties", value.properties)
                    gen.writeEndObject()
                }
            }
        }
    }

    companion object {
        internal fun findFeatureAnnotation() =
            findCallerMethod()?.getAnnotation(Feature::class.java)?.code ?: String.EMPTY

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
        val status = getStatus(e)
        val message = (e.message?.substringAfter(" @@@ ")) ?: e::class.simpleName ?: e::class.qualifiedName ?: "Unknow error"

        return ResponseEntity(
            ProblemDetail(
                title = status.reasonPhrase,
                status = status,
                detail = message,
                internalErrorCode = e.message?.before(" @@@ ")?.ifBlank { null },
                exception = e.cause.isNotNull()({ (e.cause!!::class.simpleName ?: e.cause!!::class.qualifiedName) }, { e::class.simpleName })
            ), HttpHeaders().apply { put("Feature-Code", findFeatureAnnotation().asSingleList()) }, status)
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
        val internalCode = when {
            isMissing -> errorCode?.ifMissing?.ifBlank { null }
            else -> errorCode?.ifInvalid
                ?.find { mapping ->
                    mapping.exceptions.any { exceptionClass ->
                        exceptionClass.isInstance(cause)
                    }
                }?.code
        }
        val featureCode = findFeatureAnnotation()

        return ResponseEntity(
            ProblemDetail(
                title = httpStatus.reasonPhrase,
                status = httpStatus,
                detail = detail,
                internalErrorCode = internalCode,
                exception = isMissing(onFalse = { cause::class.simpleName })
            ),
            HttpHeaders().apply {
                if (featureCode.isNotNullOrBlank())
                    put("Feature-Code", featureCode.asSingleList())
            },
            httpStatus
        )
    }
}