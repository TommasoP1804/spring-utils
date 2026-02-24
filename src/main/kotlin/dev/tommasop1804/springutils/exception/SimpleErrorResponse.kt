package dev.tommasop1804.springutils.exception

import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import dev.tommasop1804.kutils.isNotNullOrBlank
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.annotation.JsonSerialize

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