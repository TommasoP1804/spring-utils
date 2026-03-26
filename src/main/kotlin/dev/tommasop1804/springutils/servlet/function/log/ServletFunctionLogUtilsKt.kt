@file:JvmName("ServletFunctionLogUtilsKt")
@file:Since("3.0.0")
@file:Suppress("unused", "FunctionName", "FunctionName", "UNCHECKED_CAST")

package dev.tommasop1804.springutils.servlet.function.log

import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.annotations.*
import dev.tommasop1804.kutils.classes.web.*
import dev.tommasop1804.kutils.exceptions.*
import dev.tommasop1804.springutils.*
import dev.tommasop1804.springutils.servlet.function.request.*
import dev.tommasop1804.springutils.servlet.log.*
import dev.tommasop1804.springutils.servlet.log.LoggingAspect.Companion.checkExcludeOrInclude
import dev.tommasop1804.springutils.servlet.request.*
import dev.tommasop1804.springutils.servlet.security.*
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.function.ServerRequest
import org.springframework.web.servlet.function.paramOrNull

data class LogSettings(
    var request: ServerRequest? = null,
    var handler: String? = null,
    var exclude: Set<LogExecution.Component> = emptySet(),
    var includeOnly: Set<LogExecution.Component> = emptySet(),
    var customMessages: Iterable<LogExecution.CustomMessage> = emptyList(),
)

@Component
class LogHandler(
    private val requestIdProvider: RequestIdProvider
) {
    var settings: LogSettings = LogSettings()
        set(value) = field.run {
            request = value.request
            exclude = value.exclude
            includeOnly = value.includeOnly
            customMessages = value.customMessages
            handler = value.handler
        }

    fun logBefore(function: String? = null, featureCode: String? = null, logSettings: LogSettings? = null,) {
        (logSettings ?: settings).run {
            request.isNotNull() || throw RequiredParameterException(::logBefore, "logSettings")
            Logs.logStart(
                checkExcludeOrInclude(exclude.toTypedArray(), includeOnly.toTypedArray()),
                handler,
                function,
                "${request!!.method()} ${request!!.path()}${if (request!!.params.isEmpty()) String.EMPTY else "?${request!!.params.toList().joinToString("&") { "${it.first}=${it.second.joinToString()}" }}"}",
                username,
                request!!.header(HttpHeader.FROM_SERVICE).firstOrNull(),
                featureCode,
                compute {
                    RequestIdProvider.requestIdThreadLocal.set(requestIdProvider.generate())
                    RequestIdProvider.requestId!!
                },
                compute {
                    val customs = emptyMList<String2>()
                    customMessages.forEach { cm ->
                        when (cm.type) {
                            LogExecution.CustomMessage.Type.HEADER -> request!!.header(cm.reference).firstOrNull()?.let { customs += cm.key to it }
                            LogExecution.CustomMessage.Type.QUERY_PARAM -> request!!.paramOrNull(cm.reference)?.let { customs += cm.key to it }
                            LogExecution.CustomMessage.Type.PATH_VARIABLE -> request!!.pathVariables()[cm.reference]?.let { customs += cm.key to it }
                            LogExecution.CustomMessage.Type.PATH_INDEX -> tryOr({}) {
                                customs += cm.key to request!!.path()
                                    .let { if (it startsWith Char.SLASH) (-1)(it) else it }
                                    .splitAndTrim(Char.SLASH)[cm.reference.toIntOrNull()
                                    ?: throw ConfigurationException("Path index must be a number (got ${cm.reference}")]
                            }
                            LogExecution.CustomMessage.Type.STATIC -> cm.reference.let { if (it.isNotBlank()) customs += cm.key to it }
                        }
                    }
                    customs
                }
            )
        }
    }

    fun logAfter(function: String? = null, featureCode: String? = null, logSettings: LogSettings? = null) {
        (logSettings ?: settings).run {
            request.isNotNull() || throw RequiredParameterException(::logBefore, "logSettings")
            Logs.logEnd(
                checkExcludeOrInclude(exclude.toTypedArray(), includeOnly.toTypedArray()),
                handler,
                function,
                "${request!!.method()} ${request!!.path()}${if (request!!.params.isEmpty()) String.EMPTY else "?${request!!.params.toList().joinToString("&") { "${it.first}=${it.second.joinToString()}" }}"}",
                username,
                request!!.header(HttpHeader.FROM_SERVICE).firstOrNull(),
                featureCode,
                RequestIdProvider.requestId!!,
                compute {
                    val customs = emptyMList<String2>()
                    customMessages.forEach { cm ->
                        when (cm.type) {
                            LogExecution.CustomMessage.Type.HEADER -> request!!.header(cm.reference).firstOrNull()?.let { customs += cm.key to it }
                            LogExecution.CustomMessage.Type.QUERY_PARAM -> request!!.paramOrNull(cm.reference)?.let { customs += cm.key to it }
                            LogExecution.CustomMessage.Type.PATH_VARIABLE -> request!!.pathVariables()[cm.reference]?.let { customs += cm.key to it }
                            LogExecution.CustomMessage.Type.PATH_INDEX -> tryOr({}) {
                                customs += cm.key to request!!.path()
                                    .let { if (it startsWith Char.SLASH) (-1)(it) else it }
                                    .splitAndTrim(Char.SLASH)[cm.reference.toIntOrNull()
                                    ?: throw ConfigurationException("Path index must be a number (got ${cm.reference}")]
                            }
                            LogExecution.CustomMessage.Type.STATIC -> cm.reference.let { if (it.isNotBlank()) customs += cm.key to it }
                        }
                    }
                    customs
                }
            )
        }
    }

    fun logAfterThrowing(
        exception: Throwable,
        basePackage: String? = null,
        function: String? = null, featureCode: String? = null,
        logSettings: LogSettings? = null
    ) {
        (logSettings ?: settings).run {
            request.isNotNull() || throw RequiredParameterException(::logBefore, "logSettings")
            Logs.logException(
                checkExcludeOrInclude(exclude.toTypedArray(), includeOnly.toTypedArray()),
                handler,
                function,
                "${request!!.method()} ${request!!.path()}${if (request!!.params.isEmpty()) String.EMPTY else "?${request!!.params.toList().joinToString("&") { "${it.first}=${it.second.joinToString()}" }}"}",
                username,
                (if (exception is ResponseStatusException) HttpStatus.valueOf(exception.statusCode.value()) else getStatus(exception)).let { "${it.value()} ${it.reasonPhrase}" },
                request!!.header(HttpHeader.FROM_SERVICE).firstOrNull(),
                featureCode,
                RequestIdProvider.requestId!!,
                exception,
                basePackage,
                compute {
                    val customs = emptyMList<String2>()
                    customMessages.forEach { cm ->
                        when (cm.type) {
                            LogExecution.CustomMessage.Type.HEADER -> request!!.header(cm.reference).firstOrNull()?.let { customs += cm.key to it }
                            LogExecution.CustomMessage.Type.QUERY_PARAM -> request!!.paramOrNull(cm.reference)?.let { customs += cm.key to it }
                            LogExecution.CustomMessage.Type.PATH_VARIABLE -> request!!.pathVariables()[cm.reference]?.let { customs += cm.key to it }
                            LogExecution.CustomMessage.Type.PATH_INDEX -> tryOr({}) {
                                customs += cm.key to request!!.path()
                                    .let { if (it startsWith Char.SLASH) (-1)(it) else it }
                                    .splitAndTrim(Char.SLASH)[cm.reference.toIntOrNull()
                                    ?: throw ConfigurationException("Path index must be a number (got ${cm.reference}")]
                            }
                            LogExecution.CustomMessage.Type.STATIC -> cm.reference.let { if (it.isNotBlank()) customs += cm.key to it }
                        }
                    }
                    customs
                }
            )
        }
    }
}