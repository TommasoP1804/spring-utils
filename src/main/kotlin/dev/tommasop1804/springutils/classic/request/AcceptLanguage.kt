package dev.tommasop1804.springutils.classic.request

import dev.tommasop1804.kutils.*
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
 * Read the value of the HTTP header `Accept`.
 *
 * Returns `null` if the header is not present.
 *
 * The parameter type of the annotated method parameter must be `List<String>`.
 * @since 1.0.0
 * @author Tommaso Pastorelli
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class AcceptLanguage

@Component
class AcceptLanguageArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter) =
        parameter.hasParameterAnnotation(AcceptLanguage::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): StringList? = webRequest.getHeader("Accept-Language")
        ?.then { this / Char.COMMA }
}

@AutoConfiguration
@ConditionalOnClass(AcceptLanguageArgumentResolver::class)
class AcceptLanguageArgumentResolverAutoConfiguration {
    @Configuration
    @ConditionalOnBean(AcceptLanguageArgumentResolver::class)
    class AcceptLanguageResolverRegistration(
        private val resolvers: List<AcceptLanguageArgumentResolver>
    ) : WebMvcConfigurer {
        override fun addArgumentResolvers(resolvers: MList<HandlerMethodArgumentResolver>) {
            resolvers.addAll(this@AcceptLanguageResolverRegistration.resolvers)
        }
    }
}