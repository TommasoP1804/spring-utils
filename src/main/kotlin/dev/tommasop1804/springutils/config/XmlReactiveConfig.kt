/*
 * Copyright © 2026 Tommaso Pastorelli (TommasoP1804) | Spring-Utils
 */

package dev.tommasop1804.springutils.config

import com.fasterxml.jackson.annotation.JsonFormat
import dev.tommasop1804.kutils.*
import org.reactivestreams.Publisher
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.http.codec.CodecCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.core.ResolvableType
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.MediaType
import org.springframework.http.ReactiveHttpOutputMessage
import org.springframework.http.codec.HttpMessageReader
import org.springframework.http.codec.HttpMessageWriter
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder
import org.springframework.web.reactive.config.WebFluxConfigurer
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
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
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
class XmlReactiveAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun xmlMapper(): XmlMapper = XmlMapper.builder()
        .addModule(KotlinModule.Builder().build())
        .defaultUseWrapper(false)
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .enable(SerializationFeature.INDENT_OUTPUT)
        .withConfigOverride(OffsetDateTime::class.java) { it.format = JsonFormat.Value.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX") }
        .withConfigOverride(Instant::class.java) { it.format = JsonFormat.Value.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ") }
        .withConfigOverride(LocalDateTime::class.java) { it.format = JsonFormat.Value.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS") }
        .withConfigOverride(LocalDate::class.java) { it.format = JsonFormat.Value.forPattern("yyyy-MM-dd") }
        .build()

    @Bean
    fun xmlWebFluxConfigurer(xmlMapper: XmlMapper): WebFluxConfigurer {
        return object : WebFluxConfigurer {
            override fun configureHttpMessageCodecs(configurer: org.springframework.http.codec.ServerCodecConfigurer) {
                configurer.customCodecs().register(XmlHttpMessageReader(xmlMapper))
                configurer.customCodecs().register(XmlHttpMessageWriter(xmlMapper))
            }

            override fun configureContentTypeResolver(builder: RequestedContentTypeResolverBuilder) {
                builder.headerResolver()
                builder.parameterResolver().mediaType("xml", MediaType.APPLICATION_XML)
            }
        }
    }

    @Bean
    fun xmlCodecCustomizer(xmlMapper: XmlMapper) = CodecCustomizer { configurer ->
        configurer.customCodecs().register(XmlHttpMessageReader(xmlMapper))
        configurer.customCodecs().register(XmlHttpMessageWriter(xmlMapper))
    }
}

val XML_MEDIA_TYPES = listOf(
    MediaType.APPLICATION_XML,
    MediaType.TEXT_XML,
    MediaType("application", "problem+xml"),
    MediaType("application", "xhtml+xml"),
    MediaType("application", "rss+xml"),
    MediaType("application", "atom+xml"),
    MediaType("application", "mathml+xml"),
    MediaType("application", "svg+xml"),
    MediaType("application", "xslt+xml"),
)

class XmlHttpMessageReader(private val xmlMapper: XmlMapper) : HttpMessageReader<Any> {

    override fun getReadableMediaTypes(): List<MediaType> = XML_MEDIA_TYPES

    override fun canRead(elementType: ResolvableType, mediaType: MediaType?): Boolean =
        mediaType.isNotNull() && XML_MEDIA_TYPES.any { it.includes(mediaType) }

    override fun read(
        elementType: ResolvableType,
        message: org.springframework.http.ReactiveHttpInputMessage,
        hints: DataMapNN
    ): Flux<Any> = readMono(elementType, message, hints).flux()

    override fun readMono(
        elementType: ResolvableType,
        message: org.springframework.http.ReactiveHttpInputMessage,
        hints: DataMapNN
    ): Mono<Any> {
        return DataBufferUtils.join(message.body)
            .map { dataBuffer ->
                val bytes = ByteArray(dataBuffer.readableByteCount())
                dataBuffer.read(bytes)
                DataBufferUtils.release(dataBuffer)
                xmlMapper.readValue(bytes, elementType.resolve(Any::class.java))
            }
    }
}

class XmlHttpMessageWriter(private val xmlMapper: XmlMapper) : HttpMessageWriter<Any> {

    override fun getWritableMediaTypes(): List<MediaType> = XML_MEDIA_TYPES

    override fun canWrite(elementType: ResolvableType, mediaType: MediaType?): Boolean {
        val result = mediaType.isNotNull() && XML_MEDIA_TYPES.any { it.includes(mediaType) }
        log(LogLevel.DEBUG, "`canWrite` mediaType=`$mediaType` result=`$result`")
        return result
    }

    override fun write(
        inputStream: Publisher<out Any>,
        elementType: ResolvableType,
        mediaType: MediaType?,
        message: ReactiveHttpOutputMessage,
        hints: DataMapNN
    ): Mono<Void> {
        return Mono.from(inputStream)
            .flatMap { value ->
                val bytes = xmlMapper.writeValueAsBytes(value)
                val buffer: DataBuffer = message.bufferFactory().wrap(bytes)
                message.headers.contentType = MediaType.APPLICATION_XML
                message.writeWith(Mono.just(buffer))
            }
    }
}