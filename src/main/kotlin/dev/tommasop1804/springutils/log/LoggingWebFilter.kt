package dev.tommasop1804.springutils.log

import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.classes.identifiers.ULID
import dev.tommasop1804.springutils.annotations.Feature
import dev.tommasop1804.springutils.findCallerMethod
import dev.tommasop1804.springutils.getStatus
import dev.tommasop1804.springutils.reactive.security.username
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.reactive.function.server.HandlerFunction
import org.springframework.web.reactive.function.server.support.RouterFunctionMapping
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping
import org.springframework.web.server.CoWebFilter
import org.springframework.web.server.CoWebFilterChain
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.ServerWebExchangeDecorator
import java.util.concurrent.ConcurrentHashMap

@ConfigurationProperties(prefix = "spring-utils.reactive.logging")
data class LoggingProperties(
    @DefaultValue("FUNCTION_NAME,CLASS_NAME,PATH,USER,SERVICE,ID,FEATURE_CODE,ELAPSED_TIME,STATUS,EXCEPTION,STACKTRACE")
    val include: Set<LogExecution.Component> = LogExecution.Component.entries.toSet(),
    @DefaultValue
    val exclude: Set<LogExecution.Component> = emptySet(),
    @DefaultValue("true")
    val includeHighlight: Boolean = true,
    val basePackage: String? = null,
    @DefaultValue("BEFORE,AFTER,AFTER_THROWING")
    val behaviour: Set<LogExecution.Behaviour> = setOf(
        LogExecution.Behaviour.BEFORE,
        LogExecution.Behaviour.AFTER,
        LogExecution.Behaviour.AFTER_THROWING
    ),
)

@Suppress("kutils_map_declaration")
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnProperty(name = ["spring-utils.reactive.logging.enabled"], havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(LoggingProperties::class)
class LoggingWebFilter(
    private val handlerMappingProvider: ObjectProvider<RequestMappingHandlerMapping>,
    @param:Qualifier("routerFunctionMapping") private val routerFunctionMappingProvider: ObjectProvider<RouterFunctionMapping>,
    private val properties: LoggingProperties,
) : CoWebFilter() {

    private val log = LoggerFactory.getLogger(LoggingWebFilter::class.java)

    private val finalComponents: Array<LogExecution.Component> by lazy {
        val included = properties.include.ifEmpty { LogExecution.Component.entries.toSet() }
        included.filterNot { it in properties.exclude }.toTypedArray()
    }

    override suspend fun filter(exchange: ServerWebExchange, chain: CoWebFilterChain) {
        val sandboxedExchange = object : ServerWebExchangeDecorator(exchange) {
            private val localAttributes = ConcurrentHashMap(exchange.attributes)
            override fun getAttributes(): MutableMap<String, Any> = localAttributes
        }

        var resolvedHandler: Any? = null

        val handlerMapping = handlerMappingProvider.ifAvailable
        if (handlerMapping.isNotNull()) {
            resolvedHandler = try {
                handlerMapping.getHandler(sandboxedExchange).awaitSingleOrNull()
            } catch (e: Exception) {
                log.debug("LoggingWebFilter: failed to resolve handler via RequestMappingHandlerMapping: {}", e.message)
                null
            }
        }

        if (resolvedHandler.isNull()) {
            val routerFunctionMapping = routerFunctionMappingProvider.ifAvailable
            if (routerFunctionMapping.isNotNull()) {
                resolvedHandler = try {
                    routerFunctionMapping.getHandler(sandboxedExchange).awaitSingleOrNull()
                } catch (e: Exception) {
                    log.debug("LoggingWebFilter: failed to resolve handler via RouterFunctionMapping: {}", e.message)
                    null
                }
            }
        }

        return when (resolvedHandler) {
            is HandlerMethod -> filterWithHandlerMethod(exchange, chain, resolvedHandler)
            is HandlerFunction<*> -> filterWithHandlerFunction(exchange, chain)
            else -> {
                if (resolvedHandler.isNull()) {
                    log.debug("LoggingWebFilter: no handler found for {}", exchange.request.uri.path)
                } else {
                    log.debug("LoggingWebFilter: unexpected handler type {}", resolvedHandler::class.simpleName)
                }
                chain.filter(exchange)
            }
        }
    }

    private suspend fun filterWithHandlerMethod(
        exchange: ServerWebExchange,
        chain: CoWebFilterChain,
        handler: HandlerMethod
    ) {
        val ulid = ULID(monotonic = true)

        val currentReactorCtx = currentCoroutineContext()[ReactorContext]?.context ?: reactor.util.context.Context.empty()
        val newReactorCtx = currentReactorCtx.put(ULID::class.java, ulid)
        val contextToPropagate = LogIdContext(ulid) + ReactorContext(newReactorCtx)

        withContext(contextToPropagate) {
            val currentUser = username()
            val methodName = handler.method.name
            val className = handler.beanType.simpleName
            val featureCode = handler.getMethodAnnotation(Feature::class.java)?.code
            val serviceValue: String? = exchange.request.headers.getFirst("From-Service")

            if (LogExecution.Behaviour.BEFORE in properties.behaviour) {
                Logs.logStart(finalComponents, className, methodName, currentUser, serviceValue, featureCode, ulid)
            }

            try {
                chain.filter(exchange)

                if (LogExecution.Behaviour.AFTER in properties.behaviour) {
                    Logs.logEnd(finalComponents, className, methodName, currentUser, serviceValue, featureCode, ulid)
                }
            } catch (e: Throwable) {
                if (LogExecution.Behaviour.AFTER_THROWING in properties.behaviour) {
                    val status = getStatus(e)

                    val resolvedBasePackage = if (properties.includeHighlight) {
                        properties.basePackage ?: handler.beanType.packageName.splitAndTrim(Char.DOT).then {
                            if (size >= 2) "${first()}.${get(1)}" else null
                        }
                    } else null

                    Logs.logException(
                        finalComponents, className, methodName, currentUser,
                        "${status.value()} ${status.reasonPhrase}",
                        serviceValue, featureCode, ulid, e, resolvedBasePackage
                    )
                }
                throw e
            }
        }
    }

    @OptIn(ConditionNotPreventingExceptions::class)
    private suspend fun filterWithHandlerFunction(
        exchange: ServerWebExchange,
        chain: CoWebFilterChain
    ) {
        val ulid = ULID(monotonic = true)

        val currentReactorCtx = currentCoroutineContext()[ReactorContext]?.context ?: reactor.util.context.Context.empty()
        val newReactorCtx = currentReactorCtx.put(ULID::class.java, ulid)
        val contextToPropagate = LogIdContext(ulid) + ReactorContext(newReactorCtx)

        withContext(contextToPropagate) {
            val currentUser = username()
            val path = exchange.request.uri.path
            val serviceValue: String? = exchange.request.headers.getFirst("From-Service")
            val featureCode = findCallerMethod()?.getAnnotation(Feature::class.java)?.code

            if (LogExecution.Behaviour.BEFORE in properties.behaviour) {
                Logs.logStart(finalComponents, null, path whenTrue (LogExecution.Component.PATH in finalComponents), currentUser, serviceValue, featureCode, ulid)
            }

            try {
                chain.filter(exchange)

                if (LogExecution.Behaviour.AFTER in properties.behaviour) {
                    Logs.logEnd(finalComponents, null, path whenTrue (LogExecution.Component.PATH in finalComponents), currentUser, serviceValue, featureCode, ulid)
                }
            } catch (e: Throwable) {
                if (LogExecution.Behaviour.AFTER_THROWING in properties.behaviour) {
                    val status = getStatus(e)

                    Logs.logException(
                        finalComponents, null, path whenTrue (LogExecution.Component.PATH in finalComponents), currentUser,
                        "${status.value()} ${status.reasonPhrase}",
                        serviceValue, null, ulid, e, properties.basePackage
                    )
                }
                throw e
            }
        }
    }
}