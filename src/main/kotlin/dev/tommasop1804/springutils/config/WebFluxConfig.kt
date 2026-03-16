package dev.tommasop1804.springutils.config

import dev.tommasop1804.kutils.classes.coding.Json
import org.springframework.context.annotation.Configuration
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.codec.json.JacksonJsonDecoder
import org.springframework.http.codec.json.JacksonJsonEncoder
import org.springframework.web.reactive.config.WebFluxConfigurer
import tools.jackson.databind.module.SimpleModule

@Configuration
class WebFluxConfig : WebFluxConfigurer {
    override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
        val objectMapper = Json.MAPPER.rebuild()
            .addModule(SimpleModule().apply {
                addSerializer(Json::class.java, Json.Companion.Serializer())
                addDeserializer(Json::class.java, Json.Companion.Deserializer())
            })
            .build()!!
        configurer.defaultCodecs().jacksonJsonEncoder(JacksonJsonEncoder(objectMapper))
        configurer.defaultCodecs().jacksonJsonDecoder(JacksonJsonDecoder(objectMapper))
    }
}