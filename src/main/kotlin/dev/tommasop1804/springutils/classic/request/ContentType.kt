package dev.tommasop1804.springutils.classic.request

import dev.tommasop1804.kutils.*
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Configuration
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.charset.Charset

/**
 * Read the value of the HTTP header `ContentType`.
 *
 * Returns `null` if the header is not present.
 *
 * The parameter type of the annotated method parameter must be [MediaType].
 * @since 1.0.0
 * @author Tommaso Pastorelli
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class ContentType

@Component
class ContentTypeArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter) =
        parameter.hasParameterAnnotation(ContentType::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): MediaType? = try {
        webRequest.getHeader("Content-Type")?.then(MediaType::parseMediaType)
    } catch (_: Exception) {
        val result = webRequest.getHeader("Content-Type")!!
        if (Char.SLASH in result) {
            val splitted = result / Char.SLASH
            if (Char.SEMICOLON in result && Char.SEMICOLON in splitted[1] && "charset=" in splitted[1])
                MediaType(splitted[0], splitted[1] before Char.SEMICOLON, Charset.forName((splitted[2] after Char.SEMICOLON) after "charset="))
            else MediaType(splitted[0], splitted[1])
        } else try {
            MediaType.valueOf(result)
        } catch (_: Exception) {
            MediaType(result)
        }
    }
}

@AutoConfiguration
@ConditionalOnClass(ContentTypeArgumentResolver::class)
class ContentTypeArgumentResolverAutoConfiguration {
    @Configuration
    @ConditionalOnBean(ContentTypeArgumentResolver::class)
    class ContentTypeResolverRegistration(
        private val resolvers: List<ContentTypeArgumentResolver>
    ) : WebMvcConfigurer {
        override fun addArgumentResolvers(resolvers: MList<HandlerMethodArgumentResolver>) {
            resolvers.addAll(this@ContentTypeResolverRegistration.resolvers)
        }
    }
}