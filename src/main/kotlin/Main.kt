import dev.tommasop1804.kutils.println
import dev.tommasop1804.springutils.exception.BadRequestException

fun main() {
    (BadRequestException() as Throwable)::class.qualifiedName.println()
}