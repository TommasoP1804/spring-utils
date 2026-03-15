package dev.tommasop1804.springutils.config

import com.fasterxml.jackson.annotation.JsonFormat
import dev.tommasop1804.kutils.DataMapNN
import dev.tommasop1804.kutils.LogLevel
import dev.tommasop1804.kutils.isNotNull
import dev.tommasop1804.kutils.log
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
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.dataformat.yaml.YAMLFactory
import tools.jackson.dataformat.yaml.YAMLMapper
import tools.jackson.dataformat.yaml.YAMLWriteFeature
import tools.jackson.module.kotlin.KotlinModule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime

@AutoConfiguration
@ConditionalOnClass(ObjectMapper::class, YAMLFactory::class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
class YamlReactiveAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun yamlMapper(): YAMLMapper = YAMLMapper.builder()
        .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
        .disable(YAMLWriteFeature.SPLIT_LINES)
        .enable(YAMLWriteFeature.MINIMIZE_QUOTES)
        .enable(YAMLWriteFeature.LITERAL_BLOCK_STYLE)
        .addModule(KotlinModule.Builder().build())
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .withConfigOverride(OffsetDateTime::class.java) { it.format = JsonFormat.Value.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX") }
        .withConfigOverride(Instant::class.java) { it.format = JsonFormat.Value.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ") }
        .withConfigOverride(LocalDateTime::class.java) { it.format = JsonFormat.Value.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS") }
        .withConfigOverride(LocalDate::class.java) { it.format = JsonFormat.Value.forPattern("yyyy-MM-dd") }
        .build()

    @Bean
    fun yamlWebFluxConfigurer(yamlMapper: YAMLMapper): WebFluxConfigurer {
        return object : WebFluxConfigurer {
            override fun configureHttpMessageCodecs(configurer: org.springframework.http.codec.ServerCodecConfigurer) {
                configurer.customCodecs().register(YamlHttpMessageReader(yamlMapper))
                configurer.customCodecs().register(YamlHttpMessageWriter(yamlMapper))
            }

            override fun configureContentTypeResolver(builder: RequestedContentTypeResolverBuilder) {
                builder.headerResolver()
                builder.parameterResolver().mediaType("yaml", MediaType("application", "yaml"))
                builder.parameterResolver().mediaType("yml", MediaType("application", "yaml"))
            }
        }
    }

    @Bean
    fun yamlCodecCustomizer(yamlMapper: YAMLMapper) = CodecCustomizer { configurer ->
        configurer.customCodecs().register(YamlHttpMessageReader(yamlMapper))
        configurer.customCodecs().register(YamlHttpMessageWriter(yamlMapper))
    }
}

val YAML_MEDIA_TYPES = listOf(
    MediaType("application", "yaml"),
    MediaType("application", "x-yaml"),
    MediaType("text", "yaml")
)

class YamlHttpMessageReader(private val yamlMapper: YAMLMapper) : HttpMessageReader<Any> {

    override fun getReadableMediaTypes(): List<MediaType> = YAML_MEDIA_TYPES

    override fun canRead(elementType: ResolvableType, mediaType: MediaType?): Boolean =
        mediaType.isNotNull() && YAML_MEDIA_TYPES.any { it.includes(mediaType) }

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
                yamlMapper.readValue(bytes, elementType.resolve(Any::class.java))
            }
    }
}

class YamlHttpMessageWriter(private val yamlMapper: YAMLMapper) : HttpMessageWriter<Any> {

    override fun getWritableMediaTypes(): List<MediaType> = YAML_MEDIA_TYPES

    override fun canWrite(elementType: ResolvableType, mediaType: MediaType?): Boolean {
        val result = mediaType.isNotNull() && YAML_MEDIA_TYPES.any { it.includes(mediaType) }
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
                val bytes = yamlMapper.writeValueAsBytes(value)
                val buffer: DataBuffer = message.bufferFactory().wrap(bytes)
                message.headers.contentType = MediaType("application", "yaml")
                message.writeWith(Mono.just(buffer))
            }
    }
}