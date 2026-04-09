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
 * Read the value of the HTTP header `User-Agent`.
 *
 * Returns `null` if the header is not present.
 *
 * The parameter type of the annotated method parameter must be [String].
 * @since 2.2.0
 * @author Tommaso Pastorelli
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class UserAgent

@Component
class UserAgentArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter) =
        parameter.hasParameterAnnotation(UserAgent::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): String? = webRequest.getHeader("User-Agent")
}

@AutoConfiguration
@ConditionalOnClass(UserAgentArgumentResolver::class)
class UserAgentArgumentResolverAutoConfiguration {
    @Configuration
    @ConditionalOnBean(UserAgentArgumentResolver::class)
    class UserAgentResolverRegistration(
        private val resolvers: List<UserAgentArgumentResolver>
    ) : WebMvcConfigurer {
        override fun addArgumentResolvers(resolvers: MList<HandlerMethodArgumentResolver>) {
            resolvers.addAll(this@UserAgentResolverRegistration.resolvers)
        }
    }
}