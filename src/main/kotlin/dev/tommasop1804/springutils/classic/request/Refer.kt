package dev.tommasop1804.springutils.classic.request

import dev.tommasop1804.kutils.MList
import dev.tommasop1804.kutils.invoke
import dev.tommasop1804.kutils.toURL
import dev.tommasop1804.springutils.exception.BadRequestException
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
import java.net.URL

/**
 * Read the value of the HTTP header `Refer`.
 *
 * Returns `null` if the header is not present.
 *
 * The parameter type of the annotated method parameter must be [URL].
 * @since 1.0.0
 * @author Tommaso Pastorelli
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Refer

@Component
class ReferArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter) =
        parameter.hasParameterAnnotation(Refer::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): URL? = webRequest.getHeader("Refer")?.toURL()() { BadRequestException("Invalid Refer header") }
}

@AutoConfiguration
@ConditionalOnClass(ReferArgumentResolver::class)
class ReferArgumentResolverAutoConfiguration {
    @Configuration
    @ConditionalOnBean(ReferArgumentResolver::class)
    class ReferResolverRegistration(
        private val resolvers: List<ReferArgumentResolver>
    ) : WebMvcConfigurer {
        override fun addArgumentResolvers(resolvers: MList<HandlerMethodArgumentResolver>) {
            resolvers.addAll(this@ReferResolverRegistration.resolvers)
        }
    }
}