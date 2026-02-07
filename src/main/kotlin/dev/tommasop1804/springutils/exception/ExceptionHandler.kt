package dev.tommasop1804.springutils.exception

import com.fasterxml.jackson.annotation.JsonInclude
import dev.tommasop1804.kutils.EMPTY
import dev.tommasop1804.kutils.asSingleList
import dev.tommasop1804.kutils.before
import dev.tommasop1804.kutils.exceptions.*
import dev.tommasop1804.kutils.invoke
import dev.tommasop1804.kutils.isNotNull
import dev.tommasop1804.springutils.annotations.Feature
import dev.tommasop1804.springutils.findCallerMethod
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.LockedException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import kotlin.apply

@ControllerAdvice
@JsonInclude(JsonInclude.Include.NON_NULL)
class ExceptionHandler : ResponseEntityExceptionHandler() {
    data class ErrorResponse(
        val title: String,
        val description: String,
        val internalErrorCode: String?
    )

    @ExceptionHandler(Exception::class)
    fun handleAllException(e: Exception): ResponseEntity<ErrorResponse> {
        val status = when (e) {
            is BadGatewayException, is ExternalServiceHttpException -> HttpStatus.BAD_GATEWAY
            is BadRequestException, is RequiredFieldException, is RequiredParameterException -> HttpStatus.BAD_REQUEST
            is ConflictException, is ResourceAlreadyExistsException, is ResourceConflictException -> HttpStatus.CONFLICT
            is ExpectationFailedException -> HttpStatus.EXPECTATION_FAILED
            is FailedDependencyException -> HttpStatus.FAILED_DEPENDENCY
            is ForbiddenException, is InsufficientPermissionsException -> HttpStatus.FORBIDDEN
            is GatewayTimeoutException -> HttpStatus.GATEWAY_TIMEOUT
            is GoneException -> HttpStatus.GONE
            is InsufficientStorageException -> HttpStatus.INSUFFICIENT_STORAGE
            is LengthRequiredException -> HttpStatus.LENGTH_REQUIRED
            is LockedException, is ResourceLockedException -> HttpStatus.LOCKED
            is LoopDetectedException -> HttpStatus.LOOP_DETECTED
            is MisdirectedRequestException -> HttpStatus.MISDIRECTED_REQUEST
            is NetworkAuthenticationRequiredException -> HttpStatus.NETWORK_AUTHENTICATION_REQUIRED
            is NotAcceptableException -> HttpStatus.NOT_ACCEPTABLE
            is NotExtendedException -> HttpStatus.NOT_EXTENDED
            is NotFoundException, is ResourceNotFoundException -> HttpStatus.NOT_FOUND
            is NotImplementedException -> HttpStatus.NOT_IMPLEMENTED
            is PayloadTooLargeException -> HttpStatus.PAYLOAD_TOO_LARGE
            is PaymentRequiredException -> HttpStatus.PAYMENT_REQUIRED
            is PreconditionFailedException -> HttpStatus.PRECONDITION_FAILED
            is PreconditionRequiredException -> HttpStatus.PRECONDITION_REQUIRED
            is RangeNotSatisfiableException -> HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE
            is RequestTimeoutException -> HttpStatus.REQUEST_TIMEOUT
            is TeapotException -> HttpStatus.I_AM_A_TEAPOT
            is TooEarlyException -> HttpStatus.TOO_EARLY
            is TooManyRequestsException -> HttpStatus.TOO_MANY_REQUESTS
            is UnauthorizedException -> HttpStatus.UNAUTHORIZED
            is UnavailableForLegalReasonsException -> HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS
            is UnprocessableEntityException, is ResourceNotAcceptableException -> HttpStatus.UNPROCESSABLE_ENTITY
            is UnsupportedMediaTypeException -> HttpStatus.UNSUPPORTED_MEDIA_TYPE
            else -> HttpStatus.INTERNAL_SERVER_ERROR
        }

        val message = (e.message?.substringAfter(" @@@ ")) ?: e::class.simpleName ?: e::class.qualifiedName ?: "Unknow error"

        return ResponseEntity(ErrorResponse(
            status.reasonPhrase + e.cause.isNotNull()({ ": " + (e.cause!!::class.simpleName ?: e.cause!!::class.qualifiedName) }, { String.EMPTY }),
            message,
            e.message?.before(" @@@ ")?.ifBlank { null }
        ), HttpHeaders().apply { put("Feature-Code", findFeatureAnnotation().asSingleList()) }, status)
    }

    private fun findFeatureAnnotation() =
        findCallerMethod()?.getAnnotation(Feature::class.java)?.code ?: String.EMPTY
}