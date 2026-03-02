package dev.tommasop1804.springutils.servlet.log

import dev.tommasop1804.kutils.*
import dev.tommasop1804.springutils.annotations.Feature
import dev.tommasop1804.springutils.getStatus
import dev.tommasop1804.springutils.servlet.request.RequestIdProvider
import dev.tommasop1804.springutils.servlet.security.username
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.AfterThrowing
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.stereotype.Component

@Aspect
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Suppress("unused")
internal class LoggingAspect(
    private val isAfterThrowing: ThreadLocal<Boolean> = ThreadLocal.withInitial { false },
    private val requestIdProvider: RequestIdProvider
) {
    @Before("@annotation(LogExecution) || @within(LogExecution)")
    fun logBefore(joinPoint: JoinPoint) {
        if (RequestIdProvider.requestIdThreadLocal.get().isNull()) {
            val requestId = requestIdProvider.generate()
            RequestIdProvider.requestIdThreadLocal.set(requestId)
        }
        val signature = joinPoint.signature as MethodSignature
        val annotation = signature.method.getAnnotation(LogExecution::class.java)
            ?: joinPoint.target.javaClass.getAnnotation(LogExecution::class.java)

        if (annotation?.behaviour?.contains(LogExecution.Behaviour.BEFORE) == true) {
            val parameterNames = signature.parameterNames
            val args = joinPoint.args
            val serviceIndex = parameterNames.indexOf("fromService")
            val serviceValue = if (serviceIndex != -1) args[serviceIndex]?.toString() else null
            val featureCode =
                (signature.method.annotations.find { it.annotationClass == Feature::class } as? Feature)?.code

            isAfterThrowing.set(false)

            val compontents = annotation.run { exclude to includeOnly }
            val finalComponents =
                checkExcludeOrInclude(compontents.first, compontents.second)

            val methodName = joinPoint.signature.name
            val className = joinPoint.target.javaClass.getSimpleName()
            Logs.logStart(finalComponents, className, methodName, username, serviceValue, featureCode, RequestIdProvider.requestIdThreadLocal.get()!!)
        }
    }

    @After("@annotation(LogExecution) || @within(LogExecution)")
    fun logAfter(joinPoint: JoinPoint) {
        val signature = joinPoint.signature as MethodSignature
        val annotation = signature.method.getAnnotation(LogExecution::class.java)
            ?: joinPoint.target.javaClass.getAnnotation(LogExecution::class.java)

        if (annotation?.behaviour?.contains(LogExecution.Behaviour.AFTER) == true) {
            val parameterNames = signature.parameterNames
            val args = joinPoint.args
            val serviceIndex = parameterNames.indexOf("fromService")
            val serviceValue = if (serviceIndex != -1) args[serviceIndex]?.toString() else null
            val featureCode =
                (signature.method.annotations.find { it.annotationClass == Feature::class } as? Feature)?.code

            val compontents = annotation.run { exclude to includeOnly }
            val finalComponents = checkExcludeOrInclude(compontents.first, compontents.second)

            if (!isAfterThrowing.get()!!) {
                val methodName = joinPoint.signature.name
                val className = joinPoint.target.javaClass.getSimpleName()
                Logs.logEnd(finalComponents, className, methodName, username, serviceValue, featureCode, RequestIdProvider.requestIdThreadLocal.get())
            }
        }
    }

    @AfterThrowing(
        pointcut = "@annotation(LogExecution) || @within(LogExecution)",
        throwing = "e"
    )
    fun logAfterThrowing(joinPoint: JoinPoint, e: Throwable) {
        val signature = joinPoint.signature as MethodSignature
        val annotation = signature.method.getAnnotation(LogExecution::class.java)
            ?: joinPoint.target.javaClass.getAnnotation(LogExecution::class.java)

        if (annotation?.behaviour?.contains(LogExecution.Behaviour.AFTER_THROWING) == true) {
            val parameterNames = signature.parameterNames
            val args = joinPoint.args
            val serviceIndex = parameterNames.indexOf("fromService")
            val serviceValue = if (serviceIndex != -1) args[serviceIndex]?.toString() else null
            val featureCode =
                (signature.method.annotations.find { it.annotationClass == Feature::class } as? Feature)?.code

            var (basePackage, includeHighlight) = annotation.run { basePackage.ifEmpty { null } to includeHighlight }
            if (!includeHighlight) basePackage = null
            else {
                if (basePackage.isNull()) basePackage = tryOrNull {
                    signature.method.declaringClass.packageName.splitAndTrim(Char.DOT).run { "${first()}.${get(1)}" }
                } ?: tryOr({
                    joinPoint.target.javaClass.packageName.splitAndTrim(Char.DOT).run { "${first()}.${get(0)}" }
                }) { joinPoint.target.javaClass.packageName.splitAndTrim(Char.DOT).run { "${first()}.${get(1)}" } }
            }

            val compontents = annotation.run { exclude to includeOnly }
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
                RequestIdProvider.requestIdThreadLocal.get(),
                e,
                basePackage
            )
        }
    }

    private fun checkExcludeOrInclude(exclude: Array<LogExecution.Component>, includeOnly: Array<LogExecution.Component>): Array<LogExecution.Component> {
        if (exclude.isEmpty() && includeOnly.isEmpty()) return LogExecution.Component.entries.toTypedArray()
        if (exclude.isNotEmpty() && includeOnly.isEmpty()) return LogExecution.Component.entries.filterNot { it in exclude }.toTypedArray()
        if (exclude.isEmpty()) return includeOnly
        return LogExecution.Component.entries.filterNot { it in exclude }.filter { it in includeOnly }.toTypedArray()
    }
}