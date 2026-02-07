package dev.tommasop1804.springutils.config

import com.fasterxml.jackson.annotation.JsonFormat
import dev.tommasop1804.kutils.isNotNull
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.AbstractHttpMessageConverter
import org.springframework.http.converter.HttpMessageConverters
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
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
class YAMLAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun yamlHttpMessageConverter() = YAMLHttpMessageConverter()

    @Bean
    fun yamlWebMvcConfigurer(yamlConverter: YAMLHttpMessageConverter): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun configureMessageConverters(builder: HttpMessageConverters.ServerBuilder) {
                builder.addCustomConverter(yamlConverter)
            }

            override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {
                configurer
                    .defaultContentType(MediaType.APPLICATION_JSON)
                    .mediaType("json", MediaType.APPLICATION_JSON)
                    .mediaType("yaml", MediaType("application", "yaml"))
                    .mediaType("yml", MediaType("application", "yaml"))
            }
        }
    }
}

class YAMLHttpMessageConverter : AbstractHttpMessageConverter<Any>(
    MediaType("application", "yaml"),
    MediaType("application", "x-yaml"),
    MediaType("text", "yaml")
) {
    private val yamlMapper = YAMLMapper.builder()
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

    override fun canWrite(clazz: Class<*>, mediaType: MediaType?) = mediaType.isNotNull() && supportedMediaTypes.any { it.includes(mediaType) }

    override fun supports(clazz: Class<*>): Boolean = true

    override fun readInternal(
        clazz: Class<out Any>,
        inputMessage: HttpInputMessage
    ): Any = yamlMapper.readValue(inputMessage.body, clazz)

    override fun writeInternal(
        t: Any,
        outputMessage: HttpOutputMessage
    ) {
        yamlMapper.writeValue(outputMessage.body, t)
    }
}