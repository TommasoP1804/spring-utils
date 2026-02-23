package dev.tommasop1804.springutils.log

import dev.tommasop1804.kutils.classes.identifiers.ULID
import dev.tommasop1804.kutils.isNull
import dev.tommasop1804.springutils.annotations.Feature
import dev.tommasop1804.springutils.getStatus
import dev.tommasop1804.springutils.reactive.security.username
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping
import org.springframework.web.server.CoWebFilter
import org.springframework.web.server.CoWebFilterChain
import org.springframework.web.server.ServerWebExchange
import kotlin.coroutines.coroutineContext

@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
class LoggingWebFilter(
    private val handlerMappingProvider: ObjectProvider<RequestMappingHandlerMapping>
) : CoWebFilter() {

    override suspend fun filter(exchange: ServerWebExchange, chain: CoWebFilterChain) {
        val handlerMapping = handlerMappingProvider.ifAvailable
        val handler = handlerMapping?.getHandler(exchange)?.awaitSingleOrNull()

        if (handler !is HandlerMethod) {
            return chain.filter(exchange)
        }

        val annotation = handler.getMethodAnnotation(LogExecution::class.java)
            ?: handler.beanType.getAnnotation(LogExecution::class.java)

        if (annotation.isNull()) {
            return chain.filter(exchange)
        }

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

            val finalComponents = checkExcludeOrInclude(annotation.exclude, annotation.includeOnly)

            if (LogExecution.Behaviour.BEFORE in annotation.behaviour) {
                Logs.logStart(finalComponents, className, methodName, currentUser, serviceValue, featureCode, ulid)
            }

            try {
                chain.filter(exchange)

                if (LogExecution.Behaviour.AFTER in annotation.behaviour) {
                    Logs.logEnd(finalComponents, className, methodName, currentUser, serviceValue, featureCode, ulid)
                }
            } catch (e: Throwable) {
                if (LogExecution.Behaviour.AFTER_THROWING in annotation.behaviour) {
                    val status = getStatus(e)
                    var resolvedBasePackage: String? = null

                    if (annotation.includeHighlight) {
                        val pkg = annotation.basePackage.ifEmpty { null }
                        resolvedBasePackage = pkg ?: handler.beanType.packageName.split(".").let {
                            if (it.size >= 2) "${it[0]}.${it[1]}" else null
                        }
                    }

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

    private fun checkExcludeOrInclude(
        exclude: Array<LogExecution.Component>,
        includeOnly: Array<LogExecution.Component>
    ): Array<LogExecution.Component> {
        if (exclude.isEmpty() && includeOnly.isEmpty()) return LogExecution.Component.entries.toTypedArray()
        if (exclude.isNotEmpty() && includeOnly.isEmpty()) return LogExecution.Component.entries.filterNot { it in exclude }.toTypedArray()
        if (exclude.isEmpty()) return includeOnly
        return LogExecution.Component.entries.filterNot { it in exclude }.filter { it in includeOnly }.toTypedArray()
    }
}