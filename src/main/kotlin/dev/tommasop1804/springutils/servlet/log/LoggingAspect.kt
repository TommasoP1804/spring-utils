package dev.tommasop1804.springutils.servlet.log

import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.classes.web.*
import dev.tommasop1804.springutils.*
import dev.tommasop1804.springutils.annotations.*
import dev.tommasop1804.springutils.servlet.request.*
import dev.tommasop1804.springutils.servlet.security.*
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.AfterThrowing
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.server.ResponseStatusException

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
        val requestId = requestIdProvider.generate()
        RequestIdProvider.requestIdThreadLocal.set(requestId)

        val signature = joinPoint.signature as MethodSignature
        val annotation = signature.method.getAnnotation(LogExecution::class.java)
            ?: joinPoint.target.javaClass.getAnnotation(LogExecution::class.java)
        val request = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request

        if (annotation?.behaviour?.contains(LogExecution.Behaviour.BEFORE) == true) {
            val serviceValue: String? = request.getHeader(HttpHeader.FROM_SERVICE)
            val featureCode =
                (signature.method.annotations.find { it.annotationClass == Feature::class } as? Feature)?.code
            featureCode.ifNotNull { RequestIdProvider.featureCode.set(this) }

            isAfterThrowing.set(false)

            val compontents = annotation.run { exclude to includeOnly }
            val finalComponents =
                checkExcludeOrInclude(compontents.first, compontents.second)

            val methodName = joinPoint.signature.name
            val className = joinPoint.target.javaClass.getSimpleName()
            val path = request.let {
                val query = it.queryString?.let { q -> "?$q" } ?: String.EMPTY
                "${it.method} ${it.requestURI}$query"
            }
            val customs = emptyMList<String2>()
            annotation.customMessages.forEach { cm ->
                when (cm.type) {
                    LogExecution.CustomMessage.Type.HEADER -> request.getHeader(cm.reference)?.let { customs += cm.key to it }
                    LogExecution.CustomMessage.Type.QUERY_PARAM -> request.getParameter(cm.reference)?.let { customs += cm.key to it }
                    LogExecution.CustomMessage.Type.PATH_PARAM -> request.queryString.splitAndTrim(Char.AND).find { it.splitAndTrim(Char.EQUALS_SIGN).first() equalsIgnoreCase cm.reference }?.let { customs += cm.key to it }
                    LogExecution.CustomMessage.Type.STATIC -> cm.reference.let { if (it.isNotBlank()) customs += cm.key to it }
                }
            }
            Logs.logStart(finalComponents, className, methodName, path, username, serviceValue, featureCode, RequestIdProvider.requestIdThreadLocal.get()!!, customs)
        }
    }

    @After("@annotation(LogExecution) || @within(LogExecution)")
    fun logAfter(joinPoint: JoinPoint) {
        val signature = joinPoint.signature as MethodSignature
        val annotation = signature.method.getAnnotation(LogExecution::class.java)
            ?: joinPoint.target.javaClass.getAnnotation(LogExecution::class.java)
        val request = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request

        if (annotation?.behaviour?.contains(LogExecution.Behaviour.AFTER) == true) {
            val serviceValue: String? = request.getHeader(HttpHeader.FROM_SERVICE)
            val featureCode =
                (signature.method.annotations.find { it.annotationClass == Feature::class } as? Feature)?.code
            featureCode.ifNotNull { RequestIdProvider.featureCode.set(this) }

            val compontents = annotation.run { exclude to includeOnly }
            val finalComponents = checkExcludeOrInclude(compontents.first, compontents.second)

            if (!isAfterThrowing.get()!!) {
                val methodName = joinPoint.signature.name
                val className = joinPoint.target.javaClass.getSimpleName()
                val customs = emptyMList<String2>()
                annotation.customMessages.forEach { cm ->
                    when (cm.type) {
                        LogExecution.CustomMessage.Type.HEADER -> request.getHeader(cm.reference)?.let { customs += cm.key to it }
                        LogExecution.CustomMessage.Type.QUERY_PARAM -> request.getParameter(cm.reference)?.let { customs += cm.key to it }
                        LogExecution.CustomMessage.Type.PATH_PARAM -> request.queryString.splitAndTrim(Char.AND).find { it.splitAndTrim(Char.EQUALS_SIGN).first() equalsIgnoreCase cm.reference }?.let { customs += cm.key to it }
                        LogExecution.CustomMessage.Type.STATIC -> cm.reference.let { if (it.isNotBlank()) customs += cm.key to it }
                    }
                }
                Logs.logEnd(finalComponents, className, methodName, null, username, serviceValue, featureCode, RequestIdProvider.requestIdThreadLocal.get(), customs)
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
        val request = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request

        if (annotation?.behaviour?.contains(LogExecution.Behaviour.AFTER_THROWING) == true) {
            val serviceValue: String? = request.getHeader(HttpHeader.FROM_SERVICE)
            val featureCode =
                (signature.method.annotations.find { it.annotationClass == Feature::class } as? Feature)?.code
            featureCode.ifNotNull { RequestIdProvider.featureCode.set(this) }

            var [basePackage, includeHighlight] = annotation.run { basePackage.ifEmpty { null } to includeHighlight }
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
            val status = if (e is ResponseStatusException) HttpStatus.valueOf(e.statusCode.value()) else getStatus(e)
            val customs = emptyMList<String2>()
            annotation.customMessages.forEach { cm ->
                when (cm.type) {
                    LogExecution.CustomMessage.Type.HEADER -> request.getHeader(cm.reference)?.let { customs += cm.key to it }
                    LogExecution.CustomMessage.Type.QUERY_PARAM -> request.getParameter(cm.reference)?.let { customs += cm.key to it }
                    LogExecution.CustomMessage.Type.PATH_PARAM -> request.queryString.splitAndTrim(Char.AND).find { it.splitAndTrim(Char.EQUALS_SIGN).first() equalsIgnoreCase cm.reference }?.let { customs += cm.key to it }
                    LogExecution.CustomMessage.Type.STATIC -> cm.reference.let { if (it.isNotBlank()) customs += cm.key to it }
                }
            }
            Logs.logException(
                finalComponents,
                className,
                methodName,
                null,
                username,
                "${status.value()} ${status.reasonPhrase}",
                serviceValue,
                featureCode,
                RequestIdProvider.requestIdThreadLocal.get(),
                e,
                basePackage,
                customs
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