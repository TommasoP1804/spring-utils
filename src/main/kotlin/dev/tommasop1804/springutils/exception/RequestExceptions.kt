/*
 * Copyright © 2026 Tommaso Pastorelli (TommasoP1804) | Spring-Utils
 */

@file:Suppress("unused")

package dev.tommasop1804.springutils.exception

import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.exceptions.*
import kotlin.reflect.KClass

/**
 * Exception indicating that a required path variable is missing or invalid in the server request.
 * This exception is typically thrown during the validation of path variables within reactive HTTP
 * requests when a mandatory path variable is not provided or fails to meet the expected criteria.
 *
 * @author Tommaso Pastorelli
 * @since 3.0.0
 */
open class RequiredPathVariableException : RequiredParameterException {
    /**
     * Default constructor for the RequiredPathVariableException class.
     *
     * Inherits behavior from the ValidationFailedException superclass.
     * Typically utilized when no additional details need to be specified
     * at the time of throwing the exception.
     * @since 3.0.0
     */
    constructor() : super()
    /**
     * Constructs a RequiredPathVariableException with a detailed error message
     * indicating the missing or required path variable. The error message includes
     * the name of the required path variable and its type (if provided), as well as
     * an optional internal error code for additional context.
     *
     * @param name The name of the missing or required path variable.
     *             If the name is not null or blank, it will be included in the error message.
     * @param `class` The Kotlin class of the expected type for the path variable.
     *                If provided, the type will be included in the error message.
     * @param cause The cause of the exception, if any. This can be null.
     * @param internalErrorCode An optional internal error code for contextualizing the exception.
     *                          Defaults to null. If provided, it will be prepended to the error message.
     * @since 3.0.0
     */
    constructor(name: String?, `class`: KClass<*>?, cause: Throwable?, internalErrorCode: String? = null) : super("Path variable ${if (name.isNotNullOrBlank()) "`$name` " else String.EMPTY}${if (`class`.isNotNull()) "of type `${`class`.simpleName}` " else String.EMPTY}is required", cause, internalErrorCode)
    /**
     * Constructs a `RequiredPathVariableException` with a message indicating that a specific path variable is required.
     *
     * @param name The name of the missing or required path variable. If non-null and not blank, it is included in the message.
     * @param `class` The expected type of the path variable. If non-null, the simple name of the type is included in the message.
     * @param internalErrorCode An optional internal error code that is prefixed to the exception message, separated by " @@@ ".
     * @since 3.0.0
     */
    constructor(name: String?, `class`: KClass<*>?, internalErrorCode: String? = null) : super("Path variable ${if (name.isNotNullOrBlank()) "`$name` " else String.EMPTY}${if (`class`.isNotNull()) "of type `${`class`.simpleName}` " else String.EMPTY}is required", internalErrorCode)
}

/**
 * Exception indicating that a required query parameter is missing or invalid.
 *
 * This exception is typically thrown when a required query parameter is not provided
 * in an HTTP request, or when its value does not fulfill the expected conditions.
 * @author Tommaso Pastorelli
 * @since 3.0.0
 */
open class RequiredQueryParamException : RequiredParameterException {
    /**
     * Constructs a new instance of the RequiredQueryParamException class.
     * This constructor calls the default constructor of the superclass
     * ValidationFailedException, initializing an empty exception instance.
     * @since 3.0.0
     */
    constructor() : super()
    /**
     * Constructs a RequiredQueryParamException with a detailed error message indicating
     * a missing or required query parameter and its type, if provided.
     *
     * @param name The name of the query parameter that is required. Can be null or blank.
     * @param `class` The expected type of the query parameter. Can be null.
     * @param cause The underlying cause of the exception, if any. Can be null.
     * @param internalErrorCode An optional internal error code to be included in the error message. Defaults to null.
     *                          If provided, it is prepended to the error message with a separator " @@@ ".
     * @since 3.0.0
     */
    constructor(name: String?, `class`: KClass<*>?, cause: Throwable?, internalErrorCode: String? = null) : super("Query param ${if (name.isNotNullOrBlank()) "`$name` " else String.EMPTY}${if (`class`.isNotNull()) "of type `${`class`.simpleName}` " else String.EMPTY}is required", cause, internalErrorCode)
    /**
     * Constructs an exception indicating that a required query parameter is missing.
     *
     * @param name The name of the missing query parameter. If not null or blank, it will be included
     * in the exception message.
     * @param `class` The expected type of the query parameter. If not null, it will be included
     * in the exception message as the type of the required parameter.
     * @param internalErrorCode An optional internal error code for further classification of the error.
     * If provided, it will be included as a prefix in the exception message.
     * @since 3.0.0
     */
    constructor(name: String?, `class`: KClass<*>?, internalErrorCode: String? = null) : super("Query param ${if (name.isNotNullOrBlank()) "`$name` " else String.EMPTY}${if (`class`.isNotNull()) "of type `${`class`.simpleName}` " else String.EMPTY}is required", internalErrorCode)
}

/**
 * Exception thrown when a path variable in a request is found to be malformed or invalid.
 *
 * This exception is typically used to indicate issues with path variable resolution
 * when dealing with server requests, where the variable name or type does not meet
 * the expected format or requirements.
 * @author Tommaso Pastorelli
 * @since 3.0.0
 */
open class MalformedPathVariableException : MalformedInputException {
    /**
     * Constructs an instance of `MalformedPathVariableException` with no additional details.
     * This constructor initializes the exception with default values inherited from the superclass.
     * @since 3.0.0
     */
    constructor() : super()
    /**
     * Constructs a `MalformedPathVariableException` with the specified message and
     * an optional internal error code.
     *
     * @param message The detail message explaining the reason for the exception.
     * @param internalErrorCode An optional internal error code that can provide
     * additional information about the error. Defaults to `null`. If present, the
     * error code is prepended to the message, separated by " @@@ ".
     * @since 3.0.0
     */
    constructor(message: String, internalErrorCode: String? = null) : super(message, internalErrorCode)
    /**
     * Constructs a `MalformedPathVariableException` with a detailed error message based on the provided
     * path variable name, its expected type, the cause of the exception, and an optional internal error code.
     *
     * The error message is formatted to include the following:
     * - The internal error code, if provided, followed by "@@@"
     * - The path variable name, if available and not blank
     * - The type of the path variable (from the class), if provided
     *
     * @param name The name of the path variable that caused the exception. Can be null or blank.
     * @param `class` The expected type (class) of the path variable. Can be null.
     * @param cause The underlying cause of the exception, if any. Can be null.
     * @param internalErrorCode An optional identifier to represent the specific internal error. Defaults to null.
     * @since 3.0.0
     */
    constructor(name: String?, `class`: KClass<*>?, cause: Throwable?, internalErrorCode: String? = null) : super("Path variable ${if (name.isNotNullOrBlank()) "`$name` " else String.EMPTY}is not valid${if (`class`.isNotNull()) " `${`class`.simpleName}`" else String.EMPTY}", cause, internalErrorCode)
    /**
     * Constructs a MalformedPathVariableException with a detailed message describing the invalid path variable.
     *
     * @param name The name of the path variable that is considered invalid. Can be null or blank.
     * @param `class` The expected class of the path variable, if applicable. Can be null.
     * @param internalErrorCode An optional internal error code to include in the exception message for debugging purposes.
     * Defaults to null.
     * @since 3.0.0
     */
    constructor(name: String?, `class`: KClass<*>?, internalErrorCode: String? = null) : super("Path variable ${if (name.isNotNullOrBlank()) "`$name` " else String.EMPTY}is not valid${if (`class`.isNotNull()) " `${`class`.simpleName}`" else String.EMPTY}", internalErrorCode)
}

/**
 * Exception thrown to indicate that a query parameter provided in an HTTP request is malformed or invalid.
 * This class extends the `MalformedInputException` to provide context-specific error handling.
 * @author Tommaso Pastorelli
 * @since 3.0.0
 */
open class MalformedQueryParamException : MalformedInputException {
    /**
     * A default constructor for the `MalformedQueryParamException` class.
     * This initializes the exception without any specific message or cause.
     * @since 3.0.0
     */
    constructor() : super()
    /**
     * Constructs a new instance of the exception with the provided message and an optional internal error code.
     * If the internal error code is provided, it will be prefixed to the message, separated by " @@@ ".
     *
     * @param message The message describing the error.
     * @param internalErrorCode An optional internal error code to associate with this exception.
     * @since 3.0.0
     */
    constructor(message: String, internalErrorCode: String? = null) : super(message, internalErrorCode)
    /**
     * Constructs a MalformedQueryParamException with a detailed error message based on the provided
     * query parameter name, class type, optional cause, and an optional internal error code.
     *
     * The message includes the internal error code (if provided), the invalid query parameter name
     * (if non-blank), and the simple name of the class type (if provided).
     *
     * @param name The name of the invalid query parameter. If null or blank, it is omitted from the message.
     * @param `class` The class type of the expected query parameter. If null, it is omitted from the message.
     * @param cause The optional cause of the exception, typically another Throwable.
     * @param internalErrorCode An optional internal error code to include in the message.
     * @since 3.0.0
     */
    constructor(name: String?, `class`: KClass<*>?, cause: Throwable?, internalErrorCode: String? = null) : super("Query param ${if (name.isNotNullOrBlank()) "`$name` " else String.EMPTY}is not valid${if (`class`.isNotNull()) " `${`class`.simpleName}`" else String.EMPTY}", cause, internalErrorCode)
    /**
     * Constructs an instance of MalformedQueryParamException with a detailed message indicating
     * the invalid query parameter. Optionally includes the internal error code and the type of the
     * associated class, if provided.
     *
     * @param name The name of the query parameter that is invalid. If null or blank, it will not be included in the message.
     * @param `class` The KClass of the parameter type associated with the query parameter. If null, it will be excluded from the message.
     * @param internalErrorCode An optional internal error code to include in the message. If null, no error code is added.
     * @since 3.0.0
     */
    constructor(name: String?, `class`: KClass<*>?, internalErrorCode: String? = null) : super("Query param ${if (name.isNotNullOrBlank()) "`$name` " else String.EMPTY}is not valid${if (`class`.isNotNull()) " `${`class`.simpleName}`" else String.EMPTY}", internalErrorCode)
}