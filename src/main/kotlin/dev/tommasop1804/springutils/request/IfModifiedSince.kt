package dev.tommasop1804.springutils.request

import dev.tommasop1804.kutils.*
import dev.tommasop1804.springutils.exception.BadRequestException
import dev.tommasop1804.springutils.headerDateToInstant
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
import java.time.Instant

/**
 * Read the value of the HTTP header `If-Modified-Since`.
 *
 * Returns `null` if the header is not present.
 *
 * The parameter type of the annotated method parameter must be [Instant].
 * @since 1.0.0
 * @author Tommaso Pastorelli
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class IfModifiedSince

@Component
class IfModifiedSinceArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter) =
        parameter.hasParameterAnnotation(IfModifiedSince::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Instant? {
        val string = webRequest.getHeader("If-Modified-Since")
        return string?.then {
            tryOrThrow({ -> BadRequestException("Malformed header If-Modified-Since") }) {
                if (ISO_DATE_TIME_STANDARD_VALIDATOR_ONLY_SEPARATOR(string))
                    Instant(string)()
                else string.headerDateToInstant()
            }
        }
    }
}

@AutoConfiguration
@ConditionalOnClass(IfModifiedSinceArgumentResolver::class)
class IfModifiedSinceArgumentResolverAutoConfiguration {
    @Configuration
    @ConditionalOnBean(IfModifiedSinceArgumentResolver::class)
    class IfModifiedSinceResolverRegistration(
        private val resolvers: List<IfModifiedSinceArgumentResolver>
    ) : WebMvcConfigurer {
        override fun addArgumentResolvers(resolvers: MList<HandlerMethodArgumentResolver>) {
            resolvers.addAll(this@IfModifiedSinceResolverRegistration.resolvers)
        }
    }
}