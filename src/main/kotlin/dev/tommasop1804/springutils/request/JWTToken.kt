package dev.tommasop1804.springutils.request

import dev.tommasop1804.kutils.MList
import dev.tommasop1804.kutils.classes.security.JWT
import dev.tommasop1804.kutils.notStartsWithIgnoreCase
import dev.tommasop1804.springutils.exception.UnauthorizedException
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
 * Read the value of the HTTP header `Authorization`.
 *
 * **DO NOT USE if the header value is expected as a non-JTW authorization.**
 *
 * Throws [UnauthorizedException] if the header is not present or if is not a valid Bearer token.
 *
 * The parameter type of the annotated method parameter must be [JWT].
 * @property header the name of the header to read
 * @since 1.0.0
 * @author Tommaso Pastorelli
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class JWTToken(
    val header: String = "Authorization"
)
@Component
class JWTTokenArgumentResolver : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter) =
        parameter.hasParameterAnnotation(JWTToken::class.java)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): JWT {
        val annotation = parameter.getParameterAnnotation(JWTToken::class.java)!!
        val authHeader = webRequest.getHeader(annotation.header)
            ?: throw UnauthorizedException("Missing ${annotation.header} header")

        if (authHeader notStartsWithIgnoreCase "Bearer ")
            throw UnauthorizedException("Invalid ${annotation.header} header format")
        return JWT(authHeader)
    }
}

@AutoConfiguration
@ConditionalOnClass(JWTTokenArgumentResolver::class)
class JWTTokenArgumentResolverAutoConfiguration {
    @Configuration
    @ConditionalOnBean(JWTTokenArgumentResolver::class)
    class JwtResolverRegistration(
        private val resolvers: List<JWTTokenArgumentResolver>
    ) : WebMvcConfigurer {
        override fun addArgumentResolvers(resolvers: MList<HandlerMethodArgumentResolver>) {
            resolvers.addAll(this@JwtResolverRegistration.resolvers)
        }
    }
}