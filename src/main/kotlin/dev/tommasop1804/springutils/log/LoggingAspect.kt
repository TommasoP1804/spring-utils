package dev.tommasop1804.springutils.log

import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.classes.identifiers.ULID
import dev.tommasop1804.springutils.annotations.Feature
import dev.tommasop1804.springutils.getStatus
import dev.tommasop1804.springutils.security.username
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED

@Aspect
@Component
@Suppress("unused")
class LoggingAspect(
    private val id: ThreadLocal<ULID> = ThreadLocal<ULID>(),
) {
    /**
     * A computed property that retrieves the current unique identifier (ULID) associated
     * with the ongoing logging context.
     *
     * This property checks the value of `id` and returns a ULID instance if the value
     * is not null. If `id` is null, it returns null instead.
     *
     * The primary purpose of this property is to provide a unique identifier for
     * tracking logging events and associating them with specific actions or contexts.
     *
     * For coroutine/reactive code, use `coroutineContext[LogId]?.id` instead.
     *
     * @return The current ULID if available, or null if no identifier is set.
     * @since 1.0.0
     */
    val currentId: ULID? get() = if (id.get().isNull()) {
        id.set(ULID(monotonic = true))
        id.get()
    } else ULID(id.get().toString())

    @Around("@annotation(LogExecution) || @within(LogExecution)")
    fun logAround(joinPoint: ProceedingJoinPoint): Any? {
        val signature = joinPoint.signature as MethodSignature
        val isSuspend = signature.method.parameterTypes.lastOrNull()?.let {
            Continuation::class.java.isAssignableFrom(it)
        } == true

        return if (isSuspend) handleSuspend(joinPoint, signature)
        else handleBlocking(joinPoint, signature)
    }

    private fun handleBlocking(joinPoint: ProceedingJoinPoint, signature: MethodSignature): Any? {
        if (id.get().isNull()) id.set(ULID(monotonic = true))
        val ulid = id.get()!!
        val annotation = getAnnotation(joinPoint, signature)
        val capturedUsername = username

        if (annotation?.behaviour?.contains(LogExecution.Behaviour.BEFORE) == true) {
            doLogStart(joinPoint, signature, annotation, ulid, capturedUsername)
        }

        return try {
            val result = joinPoint.proceed()
            if (annotation?.behaviour?.contains(LogExecution.Behaviour.AFTER) == true) {
                doLogEnd(joinPoint, signature, annotation, ulid, capturedUsername)
            }
            result
        } catch (e: Throwable) {
            if (annotation?.behaviour?.contains(LogExecution.Behaviour.AFTER_THROWING) == true) {
                doLogException(joinPoint, signature, annotation, ulid, capturedUsername, e)
            }
            throw e
        } finally {
            id.remove()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun handleSuspend(joinPoint: ProceedingJoinPoint, signature: MethodSignature): Any? {
        val ulid = ULID(monotonic = true)
        val annotation = getAnnotation(joinPoint, signature)
        val capturedUsername = username

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
                // Synchronous completion: the continuation wrapper won't be called
                if (annotation?.behaviour?.contains(LogExecution.Behaviour.AFTER) == true) {
                    doLogEnd(joinPoint, signature, annotation, ulid, capturedUsername)
                }
            }
            // If COROUTINE_SUSPENDED, the LoggingContinuation handles logging on async completion
            result
        } catch (e: Throwable) {
            // Synchronous exception before any suspension point
            if (annotation?.behaviour?.contains(LogExecution.Behaviour.AFTER_THROWING) == true) {
                doLogException(joinPoint, signature, annotation, ulid, capturedUsername, e)
            }
            throw e
        }
    }

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

    private fun extractContext(joinPoint: JoinPoint, signature: MethodSignature, annotation: LogExecution): LogContext {
        val parameterNames = signature.parameterNames
        val args = joinPoint.args
        val serviceIndex = parameterNames.indexOf("fromService")
        val serviceValue = if (serviceIndex != -1) args[serviceIndex]?.toString() else null
        val featureCode =
            (signature.method.annotations.find { it.annotationClass == Feature::class } as? Feature)?.code

        val compontents = annotation.then { exclude to includeOnly }
        val finalComponents = checkExcludeOrInclude(compontents.first, compontents.second)

        val methodName = joinPoint.signature.name
        val className = joinPoint.target.javaClass.getSimpleName()

        return LogContext(finalComponents, className, methodName, serviceValue, featureCode)
    }

    private fun doLogStart(joinPoint: JoinPoint, signature: MethodSignature, annotation: LogExecution, ulid: ULID, username: String?) {
        val ctx = extractContext(joinPoint, signature, annotation)
        Logs.logStart(ctx.components, ctx.className, ctx.methodName, username, ctx.serviceValue, ctx.featureCode, ulid)
    }

    private fun doLogEnd(joinPoint: JoinPoint, signature: MethodSignature, annotation: LogExecution, ulid: ULID, username: String?) {
        val ctx = extractContext(joinPoint, signature, annotation)
        Logs.logEnd(ctx.components, ctx.className, ctx.methodName, username, ctx.serviceValue, ctx.featureCode, ulid)
    }

    private fun doLogException(joinPoint: JoinPoint, signature: MethodSignature, annotation: LogExecution, ulid: ULID, username: String?, e: Throwable) {
        val ctx = extractContext(joinPoint, signature, annotation)

        var (basePackage, includeHighlight) = annotation.then { basePackage.ifEmpty { null } to includeHighlight }
        if (!includeHighlight) basePackage = null
        else {
            if (basePackage.isNull()) basePackage = tryOrNull {
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
            basePackage
        )
    }

    private fun checkExcludeOrInclude(exclude: Array<LogExecution.Component>, includeOnly: Array<LogExecution.Component>): Array<LogExecution.Component> {
        if (exclude.isEmpty() && includeOnly.isEmpty()) return LogExecution.Component.entries.toTypedArray()
        if (exclude.isNotEmpty() && includeOnly.isEmpty()) return LogExecution.Component.entries.filterNot { it in exclude }.toTypedArray()
        if (exclude.isEmpty()) return includeOnly
        return LogExecution.Component.entries.filterNot { it in exclude }.filter { it in includeOnly }.toTypedArray()
    }
}
