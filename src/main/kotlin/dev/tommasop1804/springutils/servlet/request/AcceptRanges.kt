/*
 * Copyright © 2026 Tommaso Pastorelli (TommasoP1804) | Spring-Utils
 */

package dev.tommasop1804.springutils.servlet.request

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
 * Read the value of the HTTP header `Accept-Ranges`.
 *
 * Returns `null` if the header is not present.
 *
 * The parameter type of the annotated method parameter must be `List<String>`.
 * @since 2.2.0
 * @author Tommaso Pastorelli
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class AcceptRanges

@Component
class AcceptRangesArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter) =
        parameter.hasParameterAnnotation(AcceptRanges::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): List<String>? = webRequest.getHeader("Accept-Ranges")
        ?.let { it / Char.COMMA }
}

@AutoConfiguration
@ConditionalOnClass(AcceptRangesArgumentResolver::class)
class AcceptRangesArgumentResolverAutoConfiguration {
    @Configuration
    @ConditionalOnBean(AcceptRangesArgumentResolver::class)
    class AcceptRangesResolverRegistration(
        private val resolvers: List<AcceptRangesArgumentResolver>
    ) : WebMvcConfigurer {
        override fun addArgumentResolvers(resolvers: MList<HandlerMethodArgumentResolver>) {
            resolvers.addAll(this@AcceptRangesResolverRegistration.resolvers)
        }
    }
}