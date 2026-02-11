package dev.tommasop1804.springutils.exception

import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import dev.tommasop1804.kutils.EMPTY
import dev.tommasop1804.kutils.asSingleList
import dev.tommasop1804.kutils.before
import dev.tommasop1804.kutils.invoke
import dev.tommasop1804.kutils.isNotNull
import dev.tommasop1804.kutils.isNotNullOrBlank
import dev.tommasop1804.springutils.annotations.Feature
import dev.tommasop1804.springutils.findCallerMethod
import dev.tommasop1804.springutils.getStatus
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.annotation.JsonSerialize
import kotlin.apply

@ConditionalOnProperty(prefix = "spring-utils.exceptions.body", havingValue = "simple")
@ControllerAdvice
class SimpleExceptionHandler : ResponseEntityExceptionHandler() {
    @JsonSerialize(using = ErrorResponse.Companion.Serializer::class)
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = ErrorResponse.Companion.OldSerializer::class)
    data class ErrorResponse(
        val title: String,
        val description: String,
        val internalErrorCode: String? = null
    ) {
        companion object {
            class Serializer : ValueSerializer<ErrorResponse>() {
                override fun serialize(value: ErrorResponse, gen: JsonGenerator, ctxt: SerializationContext) {
                    gen.writeStartObject()
                    gen.writeStringProperty("title", value.title)
                    gen.writeStringProperty("description", value.description)
                    if (value.internalErrorCode.isNotNullOrBlank()) gen.writeStringProperty("internalErrorCode", value.internalErrorCode)
                    gen.writeEndObject()
                }
            }

            class OldSerializer : JsonSerializer<ErrorResponse>() {
                override fun serialize(value: ErrorResponse, gen: com.fasterxml.jackson.core.JsonGenerator, serializers: SerializerProvider) {
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
    fun handleAllException(e: Exception): ResponseEntity<ErrorResponse> {
        val status = getStatus(e)

        val message = (e.message?.substringAfter(" @@@ ")) ?: e::class.simpleName ?: e::class.qualifiedName ?: "Unknow error"

        return ResponseEntity(ErrorResponse(
            status.reasonPhrase + e.cause.isNotNull()({ ": " + (e.cause!!::class.simpleName ?: e.cause!!::class.qualifiedName) }, { String.EMPTY }),
            message,
            e.message?.before(" @@@ ")?.ifBlank { null }
        ), HttpHeaders().apply { put("Feature-Code", findFeatureAnnotation().asSingleList()) }, status)
    }

    private fun findFeatureAnnotation() =
        findCallerMethod()?.getAnnotation(Feature::class.java)?.code ?: String.EMPTY
}