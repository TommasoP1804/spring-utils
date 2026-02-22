package dev.tommasop1804.springutils.log

import dev.tommasop1804.kutils.ConditionNotPreventingExceptions
import dev.tommasop1804.kutils.EMPTY
import dev.tommasop1804.kutils.Instant
import dev.tommasop1804.kutils.classes.identifiers.ULID
import dev.tommasop1804.kutils.get
import dev.tommasop1804.kutils.invoke
import dev.tommasop1804.kutils.isNotNull
import dev.tommasop1804.kutils.isNotNullOrBlank
import dev.tommasop1804.kutils.whenTrue
import dev.tommasop1804.springutils.log.LogExecution.Component
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Duration
import kotlin.text.startsWith
import org.springframework.stereotype.Component as SpringComponent

@SpringComponent
@Suppress("LoggingSimilarMessage")
@OptIn(ConditionNotPreventingExceptions::class)
object Logs {
    private val LOGGER = LoggerFactory.getLogger(Logs::class.java)!!

    fun logStart(components: Array<Component>, clazz: String?, method: String?, username: String?, service: String?, featureCode: String?, id: ULID) {
        val clazz = clazz whenTrue (Component.CLASS_NAME in components)
        val method = method whenTrue (Component.FUNCTION_NAME in components)
        val username = username whenTrue (Component.USER in components)
        val service = service whenTrue (Component.SERVICE in components)
        val id = id whenTrue (Component.ID in components)
        val featureCode = featureCode whenTrue (Component.FEATURE_CODE in components)

        LOGGER.info(
            (if (id.isNotNull()) "$id | " else String.EMPTY)
                    + "\u001B[34m▶︎\u001B[0m STARTED "
                    + (if (method.isNotNullOrBlank()) "\u001b[1m\u001b[3m $method\u001b[0m" else String.EMPTY)
                    + (if (clazz.isNotNullOrBlank()) " in class \u001b[3m$clazz\u001b[0m" else String.EMPTY)
                    + (if (username.isNotNullOrBlank()) ", called by: $username" else String.EMPTY)
                    + (if (service.isNotNullOrBlank()) ", from \u001b[3m$service\u001b[0m" else String.EMPTY)
                    + (if (featureCode.isNotNullOrBlank()) ", for the feature: $featureCode" else String.EMPTY)
        )
    }

    fun logEnd(components: Array<Component>, clazz: String?, method: String?, username: String?, service: String?, featureCode: String?, id: ULID) {
        val clazz = clazz whenTrue (Component.CLASS_NAME in components)
        val method = method whenTrue (Component.FUNCTION_NAME in components)
        val username = username whenTrue (Component.USER in components)
        val service = service whenTrue (Component.SERVICE in components)
        val featureCode = featureCode whenTrue (Component.FEATURE_CODE in components)
        val end = Instant()
        val elapsed = (Duration.ofMillis(end.toEpochMilli() - id.instant.toEpochMilli())) whenTrue (Component.ELAPSED_TIME in components)

        LOGGER.info(
            (if (Component.ID in components) "$id | " else String.EMPTY)
                    + "\u001B[32m✓\u001B[0m ENDED   "
                    + (if (method.isNotNullOrBlank()) "\u001b[1m\u001b[3m $method\u001b[0m" else String.EMPTY)
                    + (if (clazz.isNotNullOrBlank()) " in class \u001b[3m$clazz\u001b[0m" else String.EMPTY)
                    + (if (username.isNotNullOrBlank()) ", called by: $username" else String.EMPTY)
                    + (if (service.isNotNullOrBlank()) ", from \u001b[3m$service\u001b[0m" else String.EMPTY)
                    + (if (featureCode.isNotNullOrBlank()) ", for the feature: $featureCode" else String.EMPTY)
                    + (if (elapsed.isNotNull()) ", elapsed time: $elapsed" else String.EMPTY)
        )
    }

    fun logException(components: Array<Component>, clazz: String?, method: String?, username: String?, status: String, service: String?, featureCode: String?, id: ULID, e: Throwable?, basePackage: String?) {
        val clazz = clazz whenTrue (Component.CLASS_NAME in components)
        val method = method whenTrue (Component.FUNCTION_NAME in components)
        val username = username whenTrue (Component.USER in components)
        val service = service whenTrue (Component.SERVICE in components)
        val featureCode = featureCode whenTrue (Component.FEATURE_CODE in components)
        val end = Instant()
        val elapsed = (Duration.ofMillis(end.toEpochMilli() - id.instant.toEpochMilli())) whenTrue (Component.ELAPSED_TIME in components)
        val status = status whenTrue (Component.STATUS in components)
        val stackTrace = e.getPrettyStackTrace(basePackage)
        var index = -1
        for (i in stackTrace.indices) {
            if (Character.isUpperCase(stackTrace[i])) {
                index = i
                break
            }
        }

        LOGGER.error(
            (if (Component.ID in components) "$id | " else String.EMPTY)
                    + "\u001B[31m✖\u001B[0m ENDED   "
                    + (if (method.isNotNullOrBlank()) "\u001b[1m\u001b[3m $method\u001b[0m" else String.EMPTY)
                    + (if (clazz.isNotNullOrBlank()) " in class \u001b[3m$clazz\u001b[0m" else String.EMPTY)
                    + (if (username.isNotNullOrBlank()) ", called by: $username" else String.EMPTY)
                    + (if (service.isNotNullOrBlank()) ", from \u001b[3m$service\u001b[0m" else String.EMPTY)
                    + (if (featureCode.isNotNullOrBlank()) ", for the feature: $featureCode" else String.EMPTY)
                    + (if (elapsed.isNotNull()) ", elapsed time: $elapsed" else String.EMPTY)
                    + (if (status.isNotNull()) ", status: \u001b[41;30m$status\u001b[0m" else String.EMPTY)
                    + (if (Component.EXCEPTION in components) ", with exception: \u001b[1m${stackTrace[(if (index == -1) 0 else index)..<stackTrace.indexOf("\n")]}\u001b[0m" else String.EMPTY)
                    + (if (Component.STACKTRACE in components) "\n\u001b[1m\u001b[31m${(-if (index == -1) 0 else index)(stackTrace)}" else String.EMPTY)
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
