/*
 * Copyright © 2026 Tommaso Pastorelli (TommasoP1804) | Spring-Utils
 */

package dev.tommasop1804.springutils.config

import com.fasterxml.jackson.annotation.JsonFormat
import dev.tommasop1804.kutils.*
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.AbstractHttpMessageConverter
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.dataformat.xml.XmlMapper
import tools.jackson.module.kotlin.KotlinModule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime

@AutoConfiguration
@ConditionalOnClass(ObjectMapper::class, XmlMapper::class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class XmlServletAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun xmlHttpMessageConverter() = XmlHttpMessageConverter()

    @Bean
    fun xmlWebMvcConfigurer(xmlConverter: XmlHttpMessageConverter): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun configureMessageConverters(builder: HttpMessageConverters.ServerBuilder) {
                builder.addCustomConverter(xmlConverter)
            }

            override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {
                configurer
                    .defaultContentType(MediaType.APPLICATION_JSON)
                    .mediaType("json", MediaType.APPLICATION_JSON)
                    .mediaType("xml", MediaType.APPLICATION_XML)
            }
        }
    }
}

class XmlHttpMessageConverter : AbstractHttpMessageConverter<Any>(
    MediaType.APPLICATION_XML,
    MediaType.TEXT_XML,
    MediaType("application", "problem+xml"),
    MediaType("application", "xhtml+xml"),
    MediaType("application", "rss+xml"),
    MediaType("application", "atom+xml"),
    MediaType("application", "mathml+xml"),
    MediaType("application", "svg+xml"),
    MediaType("application", "xslt+xml")
) {
    private val xmlMapper = XmlMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .defaultUseWrapper(false)
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .withConfigOverride(OffsetDateTime::class.java) { it.format = JsonFormat.Value.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX") }
        .withConfigOverride(Instant::class.java) { it.format = JsonFormat.Value.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ") }
        .withConfigOverride(LocalDateTime::class.java) { it.format = JsonFormat.Value.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS") }
        .withConfigOverride(LocalDate::class.java) { it.format = JsonFormat.Value.forPattern("yyyy-MM-dd") }
        .build()

    override fun canWrite(clazz: Class<*>, mediaType: MediaType?) = mediaType.isNotNull() && supportedMediaTypes.any { it.includes(mediaType) }

    override fun supports(clazz: Class<*>): Boolean = true

    override fun readInternal(
        clazz: Class<out Any>,
        inputMessage: HttpInputMessage
    ): Any = xmlMapper.readValue(inputMessage.body, clazz)

    override fun writeInternal(
        t: Any,
        outputMessage: HttpOutputMessage
    ) {
        xmlMapper.writeValue(outputMessage.body, t)
    }
}