package dev.tommasop1804.springutils.exception

import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import dev.tommasop1804.kutils.EMPTY
import dev.tommasop1804.kutils.asSingleList
import dev.tommasop1804.kutils.before
import dev.tommasop1804.kutils.invoke
import dev.tommasop1804.kutils.isNotNull
import dev.tommasop1804.kutils.isNotNullOrBlank
import dev.tommasop1804.springutils.exception.ExceptionHandler.Companion.extractErrorCode
import dev.tommasop1804.springutils.exception.ExceptionHandler.Companion.findFeatureAnnotation
import dev.tommasop1804.springutils.getStatus
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.exc.MismatchedInputException
import kotlin.apply

@ConditionalOnProperty(name = ["spring-utils.exceptions.body"], havingValue = "simple")
@ControllerAdvice
class SimpleExceptionHandler : ResponseEntityExceptionHandler() {
    @JsonSerialize(using = SimpleErrorResponse.Companion.Serializer::class)
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = SimpleErrorResponse.Companion.OldSerializer::class)
    data class SimpleErrorResponse(
        val title: String,
        val description: String,
        val internalErrorCode: String? = null
    ) {
        companion object {
            class Serializer : ValueSerializer<SimpleErrorResponse>() {
                override fun serialize(value: SimpleErrorResponse, gen: JsonGenerator, ctxt: SerializationContext) {
                    gen.writeStartObject()
                    gen.writeStringProperty("title", value.title)
                    gen.writeStringProperty("description", value.description)
                    if (value.internalErrorCode.isNotNullOrBlank()) gen.writeStringProperty("internalErrorCode", value.internalErrorCode)
                    gen.writeEndObject()
                }
            }

            class OldSerializer : JsonSerializer<SimpleErrorResponse>() {
                override fun serialize(value: SimpleErrorResponse, gen: com.fasterxml.jackson.core.JsonGenerator, serializers: SerializerProvider) {
                    gen.writeStartObject()
                    gen.writeStringField("title", value.title)
                    gen.writeStringField("description", value.description)
                    if (value.internalErrorCode.isNotNullOrBlank()) gen.writeStringField("internalErrorCode", value.internalErrorCode)
                    gen.writeEndObject()
                }
            }
        }
    }

    @ExceptionHandler(Exception::class)
    fun handleAllException(e: Exception): ResponseEntity<SimpleErrorResponse> {
        val status = getStatus(e)

        val message = (e.message?.substringAfter(" @@@ ")) ?: e::class.simpleName ?: e::class.qualifiedName ?: "Unknow error"

        return ResponseEntity(SimpleErrorResponse(
            status.reasonPhrase + e.cause.isNotNull()({ ": " + (e.cause!!::class.simpleName ?: e.cause!!::class.qualifiedName) }, { String.EMPTY }),
            message,
            e.message?.before(" @@@ ")?.ifBlank { null }
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
        val mismatch = ex.cause as? MismatchedInputException

        val isMissing = mismatch.isNotNull() && cause is MismatchedInputException
        val path = mismatch?.path?.joinToString(".") {
            val className = when (val from = it.from()) {
                is Class<*> -> from.kotlin.qualifiedName
                else -> from?.javaClass?.kotlin?.qualifiedName
            }.orEmpty()
            $$"$$className$$${it.propertyName.orEmpty()}"
        }
        val detail = if (isMissing) {
            $$"Missing required property: $$path"
        } else {
            $$"$$path: $${cause.message}"
        }

        val errorCode = extractErrorCode(ex, ex.cause as? MismatchedInputException)
        val internalCode = when {
            isMissing -> errorCode?.ifMissing?.ifBlank { null }
            else -> errorCode?.ifInvalid
                ?.find { cause::class in it.exceptions }
                ?.code
        }
        val featureCode = findFeatureAnnotation()

        return ResponseEntity(
            SimpleErrorResponse(
                title = httpStatus.reasonPhrase,
                description = detail,
                internalErrorCode = internalCode
            ),
            HttpHeaders().apply {
                if (featureCode.isNotNullOrBlank())
                    put("Feature-Code", featureCode.asSingleList())
            },
            httpStatus
        )
    }
}