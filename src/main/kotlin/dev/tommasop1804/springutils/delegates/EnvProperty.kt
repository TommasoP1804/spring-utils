/*
 * Copyright © 2026 Tommaso Pastorelli (TommasoP1804) | Spring-Utils
 */

@file:Suppress("unused")

package dev.tommasop1804.springutils.delegates

import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.exceptions.*
import org.springframework.core.env.Environment
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * A delegating property wrapper for retrieving environment properties.
 *
 * @param T The type of the property value.
 * @property key The key used to look up the property in the environment.
 * @property type The expected type of the property value.
 * @property environment The environment from where the property value will be retrieved.
 * @property defaultValue An optional default value supplier to provide a fallback if the property is not found. Null to throw an exception.
 * @since 3.6.0
 * @author Tommaso Pastorelli
 */
class EnvProperty<T : Any>(
    private val key: String,
    private val type: KClass<T>,
    private val environment: Environment,
    private val defaultValue: Supplier<T>? = null
) {
    /**
     * Retrieves the value of a property from the environment, or uses a default value if defined.
     *
     * @param thisRef the reference to the object which contains the property being accessed
     * @param property metadata for the property being accessed
     * @return the value of the property, either from the environment or a default value
     * @since 3.6.0
     */
    fun getValue(thisRef: Any?, property: KProperty<*>): T =
        tryOr({ (defaultValue ?: throw NoSuchEnvPropertyException("Property $property not found"))() }) {
            environment.getRequiredProperty(key, type.java)
        }
}
/**
 * A delegate class for working with nullable environment properties.
 *
 * This class enables retrieving environment properties as nullable values, allowing default values to be
 * specified if the property is not found. It supports type-safety by requiring the expected type as a parameter.
 *
 * @param T The type of the environment property.
 * @param key The key used to look up the environment property.
 * @param type The Kotlin class type of the environment property.
 * @param environment The environment instance from which the property value is retrieved.
 * @param defaultValue An optional supplier for providing a default value if the property is not found. Null to throw an exception. `{ null }` to return null if the property is not found.
 * @since 3.6.0
 * @author Tommaso Pastorelli
 */
class NullableEnvProperty<T : Any>(
    private val key: String,
    private val type: KClass<T>,
    private val environment: Environment,
    private val defaultValue: Supplier<T?>? = null
) {
    /**
     * Retrieves the value of the delegated property. If the environment property is not found,
     * the default value is used if provided. Otherwise, an exception is thrown.
     *
     * @param thisRef The reference to the object for which the property is being accessed.
     * @param property Metadata for the property that is being accessed.
     * @return The value of the property if found, or the default value if specified.
     *         Returns null if the value cannot be retrieved and no default value is provided.
     * @since 3.6.0
     */
    fun getValue(thisRef: Any?, property: KProperty<*>): T? =
        tryOr({ (defaultValue ?: throw NoSuchEnvPropertyException("Property $property not found"))() }) {
            environment.getRequiredProperty(key, type.java)
        }
}