package dev.tommasop1804.springutils.log

import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.classes.identifiers.ULID
import dev.tommasop1804.springutils.annotations.Feature
import dev.tommasop1804.springutils.getStatus
import dev.tommasop1804.springutils.security.username
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.AfterThrowing
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component

@Aspect
@Component
@Suppress("unused")
class LoggingAspect(
    private val id: ThreadLocal<ULID> = ThreadLocal<ULID>(),
    private val isAfterThrowing: ThreadLocal<Boolean> = ThreadLocal.withInitial { false },
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
     * @return The current ULID if available, or null if no identifier is set.
     * @since 1.0.0
     */
    val currentId: ULID? get() = if (id.get().isNull()) {
        id.set(ULID(monotonic = true))
        id.get()
    } else ULID(id.get().toString())

    @Before("@annotation(LogExecution) || @within(LogExecution)")
    fun logBefore(joinPoint: JoinPoint) {
        if (id.get().isNull()) id.set(ULID(monotonic = true))
        val signature = joinPoint.signature as MethodSignature
        val annotation = signature.method.getAnnotation(LogExecution::class.java)
            ?: joinPoint.target.javaClass.getAnnotation(LogExecution::class.java)

        if (annotation?.behaviour == LogExecution.Behaviour.ALL || annotation?.behaviour == LogExecution.Behaviour.BEFORE) {
            val parameterNames = signature.parameterNames
            val args = joinPoint.args
            val serviceIndex = parameterNames.indexOf("fromService")
            val serviceValue = if (serviceIndex != -1) args[serviceIndex]?.toString() else null
            val featureCode =
                (signature.method.annotations.find { it.annotationClass == Feature::class } as? Feature)?.code

            isAfterThrowing.set(false)

            val compontents = annotation.then { exclude to includeOnly }
            val finalComponents =
                checkExcludeOrInclude(compontents.first, compontents.second)

            val methodName = joinPoint.signature.name
            val className = joinPoint.target.javaClass.getSimpleName()
            Logs.logStart(finalComponents, className, methodName, username, serviceValue, featureCode, id.get()!!)
        }
    }

    @After("@annotation(LogExecution) || @within(LogExecution)")
    fun logAfter(joinPoint: JoinPoint) {
        val signature = joinPoint.signature as MethodSignature
        val annotation = signature.method.getAnnotation(LogExecution::class.java)
            ?: joinPoint.target.javaClass.getAnnotation(LogExecution::class.java)

        if (annotation?.behaviour == LogExecution.Behaviour.ALL || annotation?.behaviour == LogExecution.Behaviour.AFTER) {
            val parameterNames = signature.parameterNames
            val args = joinPoint.args
            val serviceIndex = parameterNames.indexOf("fromService")
            val serviceValue = if (serviceIndex != -1) args[serviceIndex]?.toString() else null
            val featureCode =
                (signature.method.annotations.find { it.annotationClass == Feature::class } as? Feature)?.code

            val compontents = annotation.then { exclude to includeOnly }
            val finalComponents = checkExcludeOrInclude(compontents.first, compontents.second)

            if (!isAfterThrowing.get()!!) {
                val methodName = joinPoint.signature.name
                val className = joinPoint.target.javaClass.getSimpleName()
                Logs.logEnd(finalComponents, className, methodName, username, serviceValue, featureCode, id.get())
            }
        }
        id.remove()
        isAfterThrowing.remove()
    }

    @AfterThrowing(
        pointcut = "@annotation(LogExecution) || @within(LogExecution)",
        throwing = "e"
    )
    fun logAfterThrowing(joinPoint: JoinPoint, e: Throwable) {
        val signature = joinPoint.signature as MethodSignature
        val annotation = signature.method.getAnnotation(LogExecution::class.java)
            ?: joinPoint.target.javaClass.getAnnotation(LogExecution::class.java)

        if (annotation?.behaviour == LogExecution.Behaviour.ALL || annotation?.behaviour == LogExecution.Behaviour.AFTER_THROWING) {
            val parameterNames = signature.parameterNames
            val args = joinPoint.args
            val serviceIndex = parameterNames.indexOf("fromService")
            val serviceValue = if (serviceIndex != -1) args[serviceIndex]?.toString() else null
            val featureCode =
                (signature.method.annotations.find { it.annotationClass == Feature::class } as? Feature)?.code

            var (basePackage, includeHighlight) = annotation.then { basePackage.ifEmpty { null } to includeHighlight }
            if (!includeHighlight) basePackage = null
            else {
                if (basePackage.isNull()) basePackage = tryOrNull {
                    signature.method.declaringClass.packageName.splitAndTrim(Char.DOT).then { "${first()}.${get(1)}" }
                } ?: joinPoint.target.javaClass.packageName.splitAndTrim(Char.DOT).then { "${first()}.${get(1)}" }
            }

            val compontents = annotation.then { exclude to includeOnly }
            val finalComponents = checkExcludeOrInclude(compontents.first, compontents.second)

            isAfterThrowing.set(true)

            val className = joinPoint.target.javaClass.getSimpleName()
            val methodName = joinPoint.signature.name
            val status = getStatus(e)
            Logs.logException(
                finalComponents,
                className,
                methodName,
                username,
                "${status.value()} ${status.reasonPhrase}",
                serviceValue,
                featureCode,
                id.get(),
                e,
                basePackage
            )
        }
        id.remove()
    }

    private fun checkExcludeOrInclude(exclude: Array<LogExecution.Component>, includeOnly: Array<LogExecution.Component>): Array<LogExecution.Component> {
        if (exclude.isEmpty() && includeOnly.isEmpty()) return LogExecution.Component.entries.toTypedArray()
        if (exclude.isNotEmpty() && includeOnly.isEmpty()) return LogExecution.Component.entries.filterNot { it in exclude }.toTypedArray()
        if (exclude.isEmpty()) return includeOnly
        return LogExecution.Component.entries.filterNot { it in exclude }.filter { it in includeOnly }.toTypedArray()
    }
}