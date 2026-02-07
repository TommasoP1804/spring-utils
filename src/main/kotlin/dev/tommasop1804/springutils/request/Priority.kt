package dev.tommasop1804.springutils.request

import dev.tommasop1804.kutils.*
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

/**
 * Read the value of the HTTP header `Priority`.
 *
 * Returns `null` if the header is not present.
 *
 * The parameter type of the annotated method parameter must be `Pair<UInt, UInt>`.
 * The first element is the incremental directive, the second is the urgency parameter.
 * @since 1.0.0
 * @author Tommaso Pastorelli
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Priority

@Component
class PriorityArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter) =
        parameter.hasParameterAnnotation(Priority::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): MonoPair<UInt>? = webRequest.getHeader("Priority")
        ?.then {
            try {
                val newValue = if ("u=" in this) this / Char.COMMA else asSingleList()
                if (newValue.isSingleElement) newValue[0].toUInt() to 3u
                else newValue[{ it notStartsWith "u=" }]!!.toUInt() to (newValue[{ it startsWith "u=" }]!! after "u=").toUInt().validate { this in 0u..7u }
            } catch (e: Exception) {
                throw BadRequestException("Malformed header Priority")
            }
        }
}

@AutoConfiguration
@ConditionalOnClass(PriorityArgumentResolver::class)
class PriorityArgumentResolverAutoConfiguration {
    @Configuration
    @ConditionalOnBean(PriorityArgumentResolver::class)
    class PriorityResolverRegistration(
        private val resolvers: List<PriorityArgumentResolver>
    ) : WebMvcConfigurer {
        override fun addArgumentResolvers(resolvers: MList<HandlerMethodArgumentResolver>) {
            resolvers.addAll(this@PriorityResolverRegistration.resolvers)
        }
    }
}