/*
 * Copyright © 2026 Tommaso Pastorelli | spring-utils
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
 * Read the value of the HTTP header `Connection`.
 *
 * Returns `null` if the header is not present.
 *
 * The parameter type of the annotated method parameter must be [ConnectionBehaviour].
 * @since 2.2.0
 * @author Tommaso Pastorelli
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Connection

@Component
class ConnectionArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter) =
        parameter.hasParameterAnnotation(Connection::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): ConnectionBehaviour? = webRequest.getHeader("Connection")
        ?.let { ConnectionBehaviour.of(it) }
}

@AutoConfiguration
@ConditionalOnClass(ConnectionArgumentResolver::class)
class ConnectionArgumentResolverAutoConfiguration {
    @Configuration
    @ConditionalOnBean(ConnectionArgumentResolver::class)
    class ConnectionResolverRegistration(
        private val resolvers: List<ConnectionArgumentResolver>
    ) : WebMvcConfigurer {
        override fun addArgumentResolvers(resolvers: MList<HandlerMethodArgumentResolver>) {
            resolvers.addAll(this@ConnectionResolverRegistration.resolvers)
        }
    }
}

enum class ConnectionBehaviour {
    KEEP_ALIVE, CLOSE;

    companion object {
        infix fun of(value: String) = when {
            value equalsIgnoreCase "keep-alive" -> KEEP_ALIVE
            value equalsIgnoreCase "close" -> CLOSE
            else -> null
        }
    }
}