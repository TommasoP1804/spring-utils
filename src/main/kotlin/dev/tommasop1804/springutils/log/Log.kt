package dev.tommasop1804.springutils.log

import dev.tommasop1804.kutils.EMPTY
import dev.tommasop1804.kutils.Instant
import dev.tommasop1804.kutils.classes.identifiers.ULID
import dev.tommasop1804.kutils.get
import dev.tommasop1804.kutils.invoke
import dev.tommasop1804.kutils.isNotNullOrBlank
import dev.tommasop1804.kutils.isNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Duration
import kotlin.text.startsWith

@Component
class Log {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(Log::class.java)!!
    }
    
    fun logStart(clazz: String?, method: String?, username: String?, service: String?, featureCode: String?, id: ULID) {
        LOGGER.info(
            "\u001B[34m⏵\u001B[0m STARTED \u001b[1m\u001b[3m {}\u001b[0m in class\u001b[3m {}\u001b[0m, called by: {}${
                if (service.isNotNullOrBlank()) " from $service" else String.EMPTY
            }, with id: {}${
                if (featureCode.isNotNullOrBlank()) ", for the feature: $featureCode" else String.EMPTY
            }",
            method,
            clazz,
            username,
            id.toString()
        )
    }

    fun logEnd(clazz: String?, method: String?, username: String?, service: String?, featureCode: String?, id: ULID?) {
        val end = Instant()
        if (id.isNull()) LOGGER.info(
            "\u001B[32m⏹\u001B[0m ENDED   \u001b[1m\u001b[3m {}\u001b[0m in class\u001b[3m {}\u001b[0m, called by: {}${
                if (service.isNotNullOrBlank()) " from $service" else String.EMPTY
            }${
                if (featureCode.isNotNullOrBlank()) ", for the feature: $featureCode" else String.EMPTY
            }",
            method,
            clazz,
            username
        )
        else LOGGER.info(
            "\u001B[32m⏹\u001B[0m ENDED   \u001b[1m\u001b[3m {}\u001b[0m in class\u001b[3m {}\u001b[0m, called by: {}${
                if (service.isNotNullOrBlank()) " from $service" else String.EMPTY
            }, with id: {}${
                if (featureCode.isNotNullOrBlank()) ", for the feature: $featureCode" else String.EMPTY
            }, elapsed time: {}",
            method,
            clazz,
            username,
            id.toString(),
            Duration.ofMillis(end.toEpochMilli() - id.instant.toEpochMilli())
        )
    }

    fun logException(clazz: String?, method: String?, username: String?, status: String, service: String?, featureCode: String?, id: ULID?, e: Throwable?) {
        val end = Instant()
        val stackTrace: String = e.getPrettyStackTrace()
        var index = -1
        for (i in stackTrace.indices) {
            if (Character.isUpperCase(stackTrace[i])) {
                index = i
                break
            }
        }

        if (id.isNull()) LOGGER.error(
            "\u001B[31m✖\u001B[0m ENDED   \u001b[1m\u001b[3m {}\u001b[0m in class\u001b[3m {}\u001b[0m, called by: {}${
                if (service.isNotNullOrBlank()) " from $service" else String.EMPTY
            }${
                if (featureCode.isNotNullOrBlank()) ", for the feature: $featureCode" else String.EMPTY
            }, status: \u001b[41;30m{}\u001b[0m with exception: \u001b[1m{}\u001b[0m\n\u001b[1m\u001b[31m{}",
            method,
            clazz,
            username,
            status,
            stackTrace[(if (index == -1) 0 else index)..<stackTrace.indexOf("\n")],
            (-if (index == -1) 0 else index)(stackTrace)
        )
        else LOGGER.error(
            "\u001B[31m✖\u001B[0m ENDED   \u001b[1m\u001b[3m {}\u001b[0m in class\u001b[3m {}\u001b[0m, called by: {}${
                if (service.isNotNullOrBlank()) " from $service" else String.EMPTY
            }, with id: {}${
                if (featureCode.isNotNullOrBlank()) ", for the feature: $featureCode" else String.EMPTY
            }, elapsed time: {}, status: \u001b[41;30m{}\u001b[0m with exception: \u001b[1m{}\u001b[0m\n\u001b[1m\u001b[31m{}",
            method,
            clazz,
            username,
            id,
            Duration.ofMillis(end.toEpochMilli() - id.instant.toEpochMilli()),
            status,
            stackTrace[(if (index == -1) 0 else index)..<stackTrace.indexOf("\n")],
            (-if (index == -1) 0 else index)(stackTrace)
        )
    }

    private fun Throwable?.getPrettyStackTrace(): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        this!!.printStackTrace(printWriter)
        val stackTrace = stringWriter.toString()

        val lines = stackTrace.split(System.lineSeparator().toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val sb = StringBuilder()
        for (line in lines) {
            if (line.trim().startsWith("at com.sigeosrl.") && !line.trim()
                    .startsWith("at com.sigeosrl.exceptionandlog.")
            ) sb.append("\u001B[7m")
            else if (line.trim().startsWith("Caused by")) sb.append("\u001B[1m")
            sb.append("\u001b[31m").append(line).append(System.lineSeparator()).append("\u001B[0m")
        }
        return sb.toString()
    }
}