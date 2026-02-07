package dev.tommasop1804.springutils.log

import dev.tommasop1804.kutils.classes.identifiers.ULID
import dev.tommasop1804.kutils.exceptions.*
import dev.tommasop1804.kutils.isNull
import dev.tommasop1804.springutils.annotations.Feature
import dev.tommasop1804.springutils.exception.*
import dev.tommasop1804.springutils.security.username
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.AfterThrowing
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.LockedException
import org.springframework.stereotype.Component

@Aspect
@Component
@Suppress("unused")
class LoggingAspect(
    private val id: ThreadLocal<ULID> = ThreadLocal<ULID>(),
    private val isAfterThrowing: ThreadLocal<Boolean> = ThreadLocal.withInitial { false },
    private val log: Log,
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

    @Before("@annotation(LoggingBefore) || @annotation(Logging) || @within(LoggingBefore) || @within(Logging)")
    fun logBefore(joinPoint: JoinPoint) {
        val signature = joinPoint.signature as MethodSignature
        val parameterNames = signature.parameterNames
        val args = joinPoint.args
        val serviceIndex = parameterNames.indexOf("fromService")
        val serviceValue = if (serviceIndex != -1) args[serviceIndex]?.toString() else null
        val featureCode = (signature.method.annotations.find { it.annotationClass == Feature::class } as? Feature)?.code

        isAfterThrowing.set(false)

        val methodName = joinPoint.signature.name
        val className = joinPoint.target.javaClass.getSimpleName()
        if (id.get().isNull()) id.set(ULID(monotonic = true))
        log.logStart(className, methodName, username, serviceValue, featureCode, id.get()!!)
    }

    @After("@annotation(LoggingAfter) || @annotation(Logging) || @within(LoggingAfter) || @within(Logging)")
    fun logAfter(joinPoint: JoinPoint) {
        val signature = joinPoint.signature as MethodSignature
        val parameterNames = signature.parameterNames
        val args = joinPoint.args
        val serviceIndex = parameterNames.indexOf("fromService")
        val serviceValue = if (serviceIndex != -1) args[serviceIndex]?.toString() else null
        val featureCode = (signature.method.annotations.find { it.annotationClass == Feature::class } as? Feature)?.code

        if (!isAfterThrowing.get()!!) {
            val methodName = joinPoint.signature.name
            val className = joinPoint.target.javaClass.getSimpleName()
            log.logEnd(className, methodName, username, serviceValue, featureCode, id.get())
        }
        id.remove()
        isAfterThrowing.remove()
    }

    @AfterThrowing(
        pointcut = "@annotation(dev.tommasop1804.springutils.log.LoggingAfterThrowing) || @annotation(dev.tommasop1804.springutils.log.Logging) || @within(dev.tommasop1804.springutils.log.LoggingAfterThrowing) || @within(dev.tommasop1804.springutils.log.Logging)",
        throwing = "e"
    )
    fun logAfterThrowing(joinPoint: JoinPoint, e: Throwable) {
        val signature = joinPoint.signature as MethodSignature
        val parameterNames = signature.parameterNames
        val args = joinPoint.args
        val serviceIndex = parameterNames.indexOf("fromService")
        val serviceValue = if (serviceIndex != -1) args[serviceIndex]?.toString() else null
        val featureCode = (signature.method.annotations.find { it.annotationClass == Feature::class } as? Feature)?.code

        isAfterThrowing.set(true)

        val className = joinPoint.target.javaClass.getSimpleName()
        val methodName = joinPoint.signature.name
        log.logException(className, methodName, username, checkStatus(e).toString(), serviceValue, featureCode, id.get(), e)
    }

    private fun checkStatus(e: Throwable): HttpStatus {
        return when (e) {
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
    }
}