package dev.tommasop1804.springutils.classic.request

import dev.tommasop1804.kutils.MList
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
 * Read the value of the HTTP header `From-Service`.
 *
 * Returns `null` if the header is not present.
 *
 * The parameter type of the annotated method parameter must be [String].
 * @since 1.0.0
 * @author Tommaso Pastorelli
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class FromService

@Component
class FromServiceArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter) =
        parameter.hasParameterAnnotation(FromService::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): String? = webRequest.getHeader("From-Service")
}

@AutoConfiguration
@ConditionalOnClass(FromServiceArgumentResolver::class)
class FromServiceArgumentResolverAutoConfiguration {
    @Configuration
    @ConditionalOnBean(FromServiceArgumentResolver::class)
    class FromServiceResolverRegistration(
        private val resolvers: List<FromServiceArgumentResolver>
    ) : WebMvcConfigurer {
        override fun addArgumentResolvers(resolvers: MList<HandlerMethodArgumentResolver>) {
            resolvers.addAll(this@FromServiceResolverRegistration.resolvers)
        }
    }
}