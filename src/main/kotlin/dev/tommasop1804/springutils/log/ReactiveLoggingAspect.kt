package dev.tommasop1804.springutils.log

import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.classes.identifiers.ULID
import dev.tommasop1804.springutils.annotations.Feature
import dev.tommasop1804.springutils.getStatus
import dev.tommasop1804.springutils.reactive.security.username
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED

@Aspect
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@Suppress("unused")
class ReactiveLoggingAspect {
    @Around("@annotation(LogExecution) || @within(LogExecution)")
    suspend fun logAround(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature

        val isSuspend = signature.method.parameterTypes.lastOrNull()?.let {
            Continuation::class.java.isAssignableFrom(it)
        } == true

        val ulid = ULID(monotonic = true)
        val annotation = getAnnotation(joinPoint, signature)

        val capturedUsername = username()

        return if (isSuspend) {
            handleSuspend(joinPoint, signature, annotation, ulid, capturedUsername)
        } else {
            handleReactiveOrBlocking(joinPoint, signature, annotation, ulid, capturedUsername)
        }
    }

    private fun handleReactiveOrBlocking(
        joinPoint: ProceedingJoinPoint,
        signature: MethodSignature,
        annotation: LogExecution?,
        ulid: ULID,
        capturedUsername: String?
    ): Any? {
        val result = try {
            joinPoint.proceed()
        } catch (e: Throwable) {
            if (annotation?.behaviour?.contains(LogExecution.Behaviour.AFTER_THROWING) == true) {
                doLogException(joinPoint, signature, annotation, ulid, capturedUsername, e)
            }
            throw e
        }

        return when (result) {
            is Mono<*> -> {
                result.doOnSubscribe {
                    if (annotation?.behaviour?.contains(LogExecution.Behaviour.BEFORE) == true) doLogStart(joinPoint, signature, annotation, ulid, capturedUsername)
                }.doOnSuccess {
                    if (annotation?.behaviour?.contains(LogExecution.Behaviour.AFTER) == true) doLogEnd(joinPoint, signature, annotation, ulid, capturedUsername)
                }.doOnError { e ->
                    if (annotation?.behaviour?.contains(LogExecution.Behaviour.AFTER_THROWING) == true) doLogException(joinPoint, signature, annotation, ulid, capturedUsername, e)
                }.contextWrite { it.put(LogId::class.java, LogId(ulid)) }
            }
            is Flux<*> -> {
                result.doOnSubscribe {
                    if (annotation?.behaviour?.contains(LogExecution.Behaviour.BEFORE) == true) doLogStart(joinPoint, signature, annotation, ulid, capturedUsername)
                }.doOnComplete {
                    if (annotation?.behaviour?.contains(LogExecution.Behaviour.AFTER) == true) doLogEnd(joinPoint, signature, annotation, ulid, capturedUsername)
                }.doOnError { e ->
                    if (annotation?.behaviour?.contains(LogExecution.Behaviour.AFTER_THROWING) == true) doLogException(joinPoint, signature, annotation, ulid, capturedUsername, e)
                }.contextWrite { it.put(LogId::class.java, LogId(ulid)) }
            }
            is Flow<*> -> {
                result.onStart {
                    if (annotation?.behaviour?.contains(LogExecution.Behaviour.BEFORE) == true) doLogStart(joinPoint, signature, annotation, ulid, capturedUsername)
                }.onCompletion { e ->
                    if (e.isNotNull()) {
                        if (annotation?.behaviour?.contains(LogExecution.Behaviour.AFTER_THROWING) == true) doLogException(joinPoint, signature, annotation, ulid, capturedUsername, e)
                    } else {
                        if (annotation?.behaviour?.contains(LogExecution.Behaviour.AFTER) == true) doLogEnd(joinPoint, signature, annotation, ulid, capturedUsername)
                    }
                }
            }
            else -> {
                if (annotation?.behaviour?.contains(LogExecution.Behaviour.BEFORE) == true) {
                    doLogStart(joinPoint, signature, annotation, ulid, capturedUsername)
                }
                if (annotation?.behaviour?.contains(LogExecution.Behaviour.AFTER) == true) {
                    doLogEnd(joinPoint, signature, annotation, ulid, capturedUsername)
                }
                result
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun handleSuspend(
        joinPoint: ProceedingJoinPoint,
        signature: MethodSignature,
        annotation: LogExecution?,
        ulid: ULID,
        capturedUsername: String?
    ): Any? {
        if (annotation?.behaviour?.contains(LogExecution.Behaviour.BEFORE) == true) {
            doLogStart(joinPoint, signature, annotation, ulid, capturedUsername)
        }

        val args = joinPoint.args.clone()
        val originalContinuation = args.last() as Continuation<Any?>

        args[args.lastIndex] = LoggingContinuation(
            originalContinuation, joinPoint, signature, annotation, ulid, capturedUsername
        )

        return try {
            val result = joinPoint.proceed(args)
            if (result !== COROUTINE_SUSPENDED) {
                if (annotation?.behaviour?.contains(LogExecution.Behaviour.AFTER) == true) {
                    doLogEnd(joinPoint, signature, annotation, ulid, capturedUsername)
                }
            }
            result
        } catch (e: Throwable) {
            if (annotation?.behaviour?.contains(LogExecution.Behaviour.AFTER_THROWING) == true) {
                doLogException(joinPoint, signature, annotation, ulid, capturedUsername, e)
            }
            throw e
        }
    }

    // --- CONTINUATION WRAPPER ---

    private inner class LoggingContinuation(
        private val original: Continuation<Any?>,
        private val joinPoint: JoinPoint,
        private val signature: MethodSignature,
        private val annotation: LogExecution?,
        private val ulid: ULID,
        private val capturedUsername: String?
    ) : Continuation<Any?> {
        override val context: CoroutineContext = original.context + LogId(ulid)

        override fun resumeWith(result: Result<Any?>) {
            result.fold(
                onSuccess = {
                    if (annotation?.behaviour?.contains(LogExecution.Behaviour.AFTER) == true) {
                        doLogEnd(joinPoint, signature, annotation, ulid, capturedUsername)
                    }
                },
                onFailure = { e ->
                    if (annotation?.behaviour?.contains(LogExecution.Behaviour.AFTER_THROWING) == true) {
                        doLogException(joinPoint, signature, annotation, ulid, capturedUsername, e)
                    }
                }
            )
            original.resumeWith(result)
        }
    }

    private fun getAnnotation(joinPoint: JoinPoint, signature: MethodSignature): LogExecution? =
        signature.method.getAnnotation(LogExecution::class.java)
            ?: joinPoint.target.javaClass.getAnnotation(LogExecution::class.java)

    private data class LogContext(
        val components: Array<LogExecution.Component>,
        val className: String?,
        val methodName: String?,
        val serviceValue: String?,
        val featureCode: String?
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as LogContext
            if (!components.contentEquals(other.components)) return false
            if (className != other.className) return false
            if (methodName != other.methodName) return false
            if (serviceValue != other.serviceValue) return false
            if (featureCode != other.featureCode) return false
            return true
        }

        override fun hashCode(): Int {
            var result = components.contentHashCode()
            result = 31 * result + (className?.hashCode() ?: 0)
            result = 31 * result + (methodName?.hashCode() ?: 0)
            result = 31 * result + (serviceValue?.hashCode() ?: 0)
            result = 31 * result + (featureCode?.hashCode() ?: 0)
            return result
        }
    }

    private fun extractContext(joinPoint: JoinPoint, signature: MethodSignature, annotation: LogExecution?): LogContext {
        if (annotation.isNull()) return LogContext(emptyArray(), null, null, null, null)

        val parameterNames = signature.parameterNames
        val args = joinPoint.args
        val serviceIndex = parameterNames.indexOf("fromService")
        val serviceValue = if (serviceIndex != -1) args[serviceIndex]?.toString() else null
        val featureCode = (signature.method.annotations.find { it.annotationClass == Feature::class } as? Feature)?.code

        val components = annotation.then { exclude to includeOnly }
        val finalComponents = checkExcludeOrInclude(components.first, components.second)

        val methodName = joinPoint.signature.name
        val className = joinPoint.target.javaClass.simpleName

        return LogContext(finalComponents, className, methodName, serviceValue, featureCode)
    }

    private fun doLogStart(joinPoint: JoinPoint, signature: MethodSignature, annotation: LogExecution?, ulid: ULID, username: String?) {
        val ctx = extractContext(joinPoint, signature, annotation)
        Logs.logStart(ctx.components, ctx.className, ctx.methodName, username, ctx.serviceValue, ctx.featureCode, ulid)
    }

    private fun doLogEnd(joinPoint: JoinPoint, signature: MethodSignature, annotation: LogExecution?, ulid: ULID, username: String?) {
        val ctx = extractContext(joinPoint, signature, annotation)
        Logs.logEnd(ctx.components, ctx.className, ctx.methodName, username, ctx.serviceValue, ctx.featureCode, ulid)
    }

    private fun doLogException(joinPoint: JoinPoint, signature: MethodSignature, annotation: LogExecution?, ulid: ULID, username: String?, e: Throwable) {
        val ctx = extractContext(joinPoint, signature, annotation)

        var resolvedBasePackage: String? = null

        if (annotation.isNotNull() && annotation.includeHighlight) {
            val pkg = annotation.basePackage.ifEmpty { null }
            resolvedBasePackage = pkg ?: tryOrNull {
                signature.method.declaringClass.packageName.splitAndTrim(Char.DOT).then { "${first()}.${get(1)}" }
            } ?: joinPoint.target.javaClass.packageName.splitAndTrim(Char.DOT).then { "${first()}.${get(1)}" }
        }

        val status = getStatus(e)
        Logs.logException(
            ctx.components,
            ctx.className,
            ctx.methodName,
            username,
            "${status.value()} ${status.reasonPhrase}",
            ctx.serviceValue,
            ctx.featureCode,
            ulid,
            e,
            resolvedBasePackage
        )
    }

    private fun checkExcludeOrInclude(exclude: Array<LogExecution.Component>, includeOnly: Array<LogExecution.Component>): Array<LogExecution.Component> {
        if (exclude.isEmpty() && includeOnly.isEmpty()) return LogExecution.Component.entries.toTypedArray()
        if (exclude.isNotEmpty() && includeOnly.isEmpty()) return LogExecution.Component.entries.filterNot { it in exclude }.toTypedArray()
        if (exclude.isEmpty()) return includeOnly
        return LogExecution.Component.entries.filterNot { it in exclude }.filter { it in includeOnly }.toTypedArray()
    }
}