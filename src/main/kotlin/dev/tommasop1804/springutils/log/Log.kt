package dev.tommasop1804.springutils.log

import dev.tommasop1804.kutils.ConditionNotPreventingExceptions
import dev.tommasop1804.kutils.EMPTY
import dev.tommasop1804.kutils.Instant
import dev.tommasop1804.kutils.classes.identifiers.ULID
import dev.tommasop1804.kutils.get
import dev.tommasop1804.kutils.invoke
import dev.tommasop1804.kutils.isNotNull
import dev.tommasop1804.kutils.isNotNullOrBlank
import dev.tommasop1804.kutils.then
import dev.tommasop1804.kutils.whenTrue
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Duration
import kotlin.text.startsWith

@Component
@Suppress("LoggingSimilarMessage")
@OptIn(ConditionNotPreventingExceptions::class)
class Log {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(Log::class.java)!!
    }

    // method, class, called by, from service, with id, feature code
    fun logStart(components: Array<LogComponent>, clazz: String?, method: String?, username: String?, service: String?, featureCode: String?, id: ULID) {
        val clazz = clazz whenTrue (LogComponent.CLASS_NAME in components)
        val method = method whenTrue (LogComponent.FUNCTION_NAME in components)
        val username = username whenTrue (LogComponent.USER in components)
        val service = service whenTrue (LogComponent.SERVICE in components)
        val id = id whenTrue (LogComponent.ID in components)
        val featureCode = featureCode whenTrue (LogComponent.FEATURE_CODE in components)

        LOGGER.info(
            "\u001B[34m⏵\u001B[0m STARTED "
                    + (if (method.isNotNullOrBlank()) "\u001b[1m\u001b[3m $method\u001b[0m " else String.EMPTY)
                    + (if (clazz.isNotNullOrBlank()) "\u001b[3m in class $clazz\u001b[0m" else String.EMPTY)
                    + (if (username.isNotNullOrBlank()) ", called by: $username" else String.EMPTY)
                    + (if (service.isNotNullOrBlank()) ", from $service" else String.EMPTY)
                    + (if (id.isNotNull()) ", with id: $id" else String.EMPTY)
                    + (if (featureCode.isNotNullOrBlank()) ", for the feature: $featureCode" else String.EMPTY)
        )
    }

    // method, class, called by, from service, with id, feature code, elapsed time
    fun logEnd(components: Array<LogComponent>, clazz: String?, method: String?, username: String?, service: String?, featureCode: String?, id: ULID) {
        val clazz = clazz whenTrue (LogComponent.CLASS_NAME in components)
        val method = method whenTrue (LogComponent.FUNCTION_NAME in components)
        val username = username whenTrue (LogComponent.USER in components)
        val service = service whenTrue (LogComponent.SERVICE in components)
        val id = id whenTrue (LogComponent.ID in components)
        val featureCode = featureCode whenTrue (LogComponent.FEATURE_CODE in components)
        val end = Instant()
        val elapsed = id?.then { Duration.ofMillis(end.toEpochMilli() - id.instant.toEpochMilli()) } whenTrue (LogComponent.ELAPSED_TIME in components)

        LOGGER.info(
            "\u001B[31m✖\u001B[0m ENDED   "
                    + (if (method.isNotNullOrBlank()) "\u001b[1m\u001b[3m $method\u001b[0m " else String.EMPTY)
                    + (if (clazz.isNotNullOrBlank()) "\u001b[3m in class $clazz\u001b[0m" else String.EMPTY)
                    + (if (username.isNotNullOrBlank()) ", called by: $username" else String.EMPTY)
                    + (if (service.isNotNullOrBlank()) ", from $service" else String.EMPTY)
                    + (if (id.isNotNull()) ", with id: $id" else String.EMPTY)
                    + (if (featureCode.isNotNullOrBlank()) ", for the feature: $featureCode" else String.EMPTY)
                    + (if (elapsed.isNotNull()) ", elapsed time: $elapsed" else String.EMPTY)
        )
    }

    // method, class, called by, from service, with id, feature code, elapsed time, status, exception, stacktrace
    fun logException(components: Array<LogComponent>, clazz: String?, method: String?, username: String?, status: String, service: String?, featureCode: String?, id: ULID, e: Throwable?, basePackage: String?) {
        val clazz = clazz whenTrue (LogComponent.CLASS_NAME in components)
        val method = method whenTrue (LogComponent.FUNCTION_NAME in components)
        val username = username whenTrue (LogComponent.USER in components)
        val service = service whenTrue (LogComponent.SERVICE in components)
        val id = id whenTrue (LogComponent.ID in components)
        val featureCode = featureCode whenTrue (LogComponent.FEATURE_CODE in components)
        val end = Instant()
        val elapsed = id?.then { Duration.ofMillis(end.toEpochMilli() - id.instant.toEpochMilli()) } whenTrue (LogComponent.ELAPSED_TIME in components)
        val status = status whenTrue (LogComponent.STATUS in components)
        val stackTrace = e.getPrettyStackTrace(basePackage) whenTrue (LogComponent.STACKTRACE in components)
        var index = -1
        if (stackTrace.isNotNull())
            for (i in stackTrace.indices) {
                if (Character.isUpperCase(stackTrace[i])) {
                    index = i
                    break
                }
            }

        LOGGER.info(
            "\u001B[31m✖\u001B[0m ENDED   "
                    + (if (method.isNotNullOrBlank()) "\u001b[1m\u001b[3m $method\u001b[0m " else String.EMPTY)
                    + (if (clazz.isNotNullOrBlank()) "\u001b[3m in class $clazz\u001b[0m" else String.EMPTY)
                    + (if (username.isNotNullOrBlank()) ", called by: $username" else String.EMPTY)
                    + (if (service.isNotNullOrBlank()) ", from $service" else String.EMPTY)
                    + (if (id.isNotNull()) ", with id: $id" else String.EMPTY)
                    + (if (featureCode.isNotNullOrBlank()) ", for the feature: $featureCode" else String.EMPTY)
                    + (if (elapsed.isNotNull()) ", elapsed time: $elapsed" else String.EMPTY)
                    + (if (status.isNotNull()) ", status: \u001b[41;30m$status\u001b[0m" else String.EMPTY)
                    + (if (stackTrace.isNotNull()) ", with exception:  \u001b[1m${stackTrace[(if (index == -1) 0 else index)..<stackTrace.indexOf("\n")]}\u001b[0m" else String.EMPTY)
                    + (if (stackTrace.isNotNull()) "\n\u001b[1m\u001b[31m${(-if (index == -1) 0 else index)(stackTrace)}" else String.EMPTY)
        )
    }

    private fun Throwable?.getPrettyStackTrace(basePackage: String?): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        this!!.printStackTrace(printWriter)
        val stackTrace = stringWriter.toString()

        val lines = stackTrace.split(System.lineSeparator().toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val sb = StringBuilder()
        for (line in lines) {
            if (basePackage.isNotNullOrBlank() && line.trim().startsWith("at $basePackage")) sb.append("\u001B[7m")
            else if (line.trim().startsWith("Caused by")) sb.append("\u001B[1m")
            sb.append("\u001b[31m").append(line).append(System.lineSeparator()).append("\u001B[0m")
        }
        return sb.toString()
    }
}