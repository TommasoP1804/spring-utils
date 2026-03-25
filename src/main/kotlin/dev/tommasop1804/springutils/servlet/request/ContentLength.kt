package dev.tommasop1804.springutils.servlet.request

import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.classes.measure.*
import dev.tommasop1804.kutils.classes.measure.RMeasurement.Companion.ofUnit
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Configuration
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Read the value of the HTTP header `Content-Length`.
 *
 * Returns `null` if the header is not present.
 *
 * The parameter type of the annotated method parameter must be [Int].
 * @since 2.2.0
 * @author Tommaso Pastorelli
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class ContentLength

@Component
class ContentLengthArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter) =
        parameter.hasParameterAnnotation(ContentLength::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): DataSize? = webRequest.getHeader("Content-Length")?.let { it.toInt() ofUnit MeasureUnit.DataSizeUnit.BYTE }
}

@AutoConfiguration
@ConditionalOnClass(ContentLengthArgumentResolver::class)
class ContentLengthArgumentResolverAutoConfiguration {
    @Configuration
    @ConditionalOnBean(ContentLengthArgumentResolver::class)
    class ContentLengthResolverRegistration(
        private val resolvers: List<ContentLengthArgumentResolver>
    ) : WebMvcConfigurer {
        override fun addArgumentResolvers(resolvers: MList<HandlerMethodArgumentResolver>) {
            resolvers.addAll(this@ContentLengthResolverRegistration.resolvers)
        }
    }
}