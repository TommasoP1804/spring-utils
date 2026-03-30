/*
 * Copyright © 2026 Tommaso Pastorelli | spring-utils
 */

package dev.tommasop1804.springutils.servlet.request

import dev.tommasop1804.kutils.MList
import dev.tommasop1804.kutils.classes.web.HttpHeader
import dev.tommasop1804.kutils.classes.web.HttpHeaders
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

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Headers

@Component
class HeadersArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter) =
        parameter.hasParameterAnnotation(Headers::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): HttpHeaders {
        val result = HttpHeaders()
        webRequest.headerNames.forEach { result += HttpHeader(it, webRequest.getHeaderValues(it).orEmpty().toList()) }
        return result
    }
}

@AutoConfiguration
@ConditionalOnClass(HeadersArgumentResolver::class)
class HeadersArgumentResolverAutoConfiguration {
    @Configuration
    @ConditionalOnBean(HeadersArgumentResolver::class)
    class HeadersResolverRegistration(
        private val resolvers: List<HeadersArgumentResolver>
    ) : WebMvcConfigurer {
        override fun addArgumentResolvers(resolvers: MList<HandlerMethodArgumentResolver>) {
            resolvers.addAll(this@HeadersResolverRegistration.resolvers)
        }
    }
}