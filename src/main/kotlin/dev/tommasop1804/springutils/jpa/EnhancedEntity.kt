/*
 * Copyright © 2026 Tommaso Pastorelli (TommasoP1804) | Spring-Utils
 */

@file:Suppress("unused")

package dev.tommasop1804.springutils.jpa

import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.exceptions.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.findByIdOrNull
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.KClass

/**
 * Represents an enhanced entity abstraction designed to simplify operations
 * with repositories (such as saving, deleting, and refreshing) while providing
 * utility methods for equality and hash code implementation.
 *
 * @property _entityId The unique identifier associated with the entity. Nullable to indicate the absence of an assigned ID.
 * @param T The type of the entity.
 * @param ID The type of the primary key for the entity.
 *
 * @author Tommaso Pasatorelli
 * @since 3.11.0
 */
@Suppress("unchecked_cast")
@MustUseReturnValues
abstract class EnhancedEntity<T : EnhancedEntity<T, ID>, ID : Any>(private var _entityId: ID?) {
    /**
     * Persists the current entity into the repository. Saves the entity either with or without flushing
     * changes immediately to the database, based on the provided parameter.
     *
     * @param flush Indicates whether to immediately flush changes to the database. If `true`, the
     *              entity will be saved and flushed. If `false`, the entity will be saved without
     *              immediate flushing. Defaults to `false`.
     * @since 3.11.0
     */
    @IgnorableReturnValue
    context(repository: JpaRepository<T, ID>)
    fun save(flush: Boolean = false) = if (flush) repository.saveAndFlush(this as T) else repository.save(this as T)
    /**
     * Saves the current entity to the repository if the given predicate returns true.
     *
     * @param predicate A condition that determines whether the entity should be saved.
     *                  The predicate receives the current entity as its input.
     * @return A pair where the first element is the current entity and the second element is a boolean.
     *         The boolean is `true` if the entity was saved, `false` otherwise.
     * @since 3.11.0
     */
    @IgnorableReturnValue
    context(repository: JpaRepository<T, ID>)
    fun saveIf(predicate: Predicate<T>): Pair<T, Boolean> {
        return if (predicate(this as T)) save() to true
        else this to false
    }

    /**
     * Deletes the current entity from the repository.
     *
     * @param flush A flag indicating whether the repository should be flushed after the deletion.
     *              If `true`, the repository's changes will be immediately flushed to the database.
     * @since 3.11.0
     */
    @IgnorableReturnValue
    context(repository: JpaRepository<T, ID>)
    fun delete(flush: Boolean = false) = repository.delete(this as T).apply { if (flush) repository.flush() }
    /**
     * Deletes the current entity from the repository if the specified predicate evaluates to true.
     *
     * @param predicate A predicate used to determine whether the current entity should be deleted.
     *                   The predicate is evaluated using the current entity as its argument.
     * @return `true` if the entity was deleted, `false` otherwise.
     * @since 3.11.0
     */
    @IgnorableReturnValue
    context(repository: JpaRepository<T, ID>)
    fun deleteIf(predicate: Predicate<T>) = if (predicate(this as T)) {
        delete()
        true
    } else false

    /**
     * Reloads the current entity from the repository to ensure it is up-to-date with the latest state in the database.
     *
     * @throws RequiredPropertyException if the `id` property is null.
     * @throws ResourceNotFoundException if the entity with the specified `id` cannot be found in the repository.
     * @since 3.11.0
     */
    context(repository: CrudRepository<T, ID>)
    fun refresh() = (repository.findById(_entityId ?: throw RequiredPropertyException(::_entityId)).getOrNull()
        ?: throw ResourceNotFoundException(_entityId!!, this::class))

    /**
     * Compares this object with the specified object for equality.
     *
     * @param other The object to compare with this instance.
     * @return `true` if the specified object is equal to this instance, `false` otherwise.
     * @since 3.11.0
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EnhancedEntity<T, ID>) return false
        return _entityId.isNotNull() && _entityId == other._entityId
    }

    /**
     * Returns a hash code value for this object. The hash code is based on the class type
     * to ensure consistent behavior across instances of the same class.
     *
     * @return The hash code of the runtime class of this object.
     * @since 3.11.0
     */
    override fun hashCode() = javaClass.hashCode()

    /**
     * Defines a companion interface for managing and interacting with entities in a repository.
     * Provides various utility functions and properties to simplify data access and manipulation tasks
     * for entities of type [T] with identifiers of type [ID].
     *
     * This interface requires implementations to operate within a repository context, which can be either
     * a `CrudRepository` or a `JpaRepository`, depending on the specific functionality.
     *
     * @param T The type of the entity.
     * @param ID The type of the entity's identifier.
     * @since 3.11.0
     */
    interface EnhancedEntityCompanion<T : Any, ID : Any> {
        /**
         * Retrieves all entities from the repository as a list.
         *
         * This property accesses the provided repository within the context block
         * and converts the result of `findAll()` to a list.
         * @since 3.11.0
         */
        context(repository: CrudRepository<T, ID>)
        val all get() = repository.findAll().toList()
        /**
         * Retrieves the total number of entities existing in the repository.
         *
         * Requires a context of `CrudRepository<T, ID>` to access the count of entities.
         * @since 3.11.0
         */
        context(repository: CrudRepository<T, ID>)
        val count get() = repository.count()
        /**
         * Represents the first element retrieved from the associated `JpaRepository` or `null`
         * if the repository is empty.
         *
         * This property is evaluated lazily and returns the first element as determined
         * by the repository's internal ordering, if available.
         * @since 3.11.0
         */
        context(repository: JpaRepository<T, ID>)
        val first get() = repository.firstOrNull()

        /**
         * A computed property that checks if there are any records present in the repository.
         * This property evaluates to `true` if the count of entities in the repository is greater than 0,
         * otherwise `false`.
         *
         * The presence of any entities is determined using the `count()` function of the associated `CrudRepository`.
         * @since 3.11.0
         */
        context(repository: CrudRepository<T, ID>)
        val anyPresent get() = repository.count() > 0L
        /**
         * Indicates whether there are no entities present in the repository.
         *
         * This property evaluates to `true` if the count of entities in the repository is zero.
         * Otherwise, it evaluates to `false`.
         *
         * Requires the context of a `CrudRepository` to perform the count operation.
         * @since 3.11.0
         */
        context(repository: CrudRepository<T, ID>)
        val nonePresent get() = repository.count() == 0L

        /**
         * Retrieves the first entity from the repository. If no entity is found, an exception is thrown
         * as supplied by the given lazy exception supplier.
         *
         * @param lazyException A supplier for the exception to be thrown if no entity is found.
         * The default exception is a [NoSuchElementException] with the message "Table is empty".
         * @throws Throwable The exception supplied by the lazyException if no entity is found in the repository.
         * @since 3.11.0
         */
        context(repository: JpaRepository<T, ID>)
        fun firstOrThrow(lazyException: ThrowableSupplier = { NoSuchElementException("Table is empty") }) = repository.firstOrThrow(lazyException)

        /**
         * Returns all entities from the repository that match the given predicate.
         *
         * @param predicate A predicate used to filter the entities in the repository.
         * @return A list of entities that satisfy the given predicate.
         * @since 3.11.0
         */
        context(repository: JpaRepository<T, ID>)
        fun all(predicate: Predicate<T>) = repository.invoke(predicate).toList()

        /**
         * Checks whether an entity with the given ID exists in the repository.
         *
         * @param id The ID of the entity to check for existence.
         * @return `true` if an entity with the given ID exists, otherwise `false`.
         * @since 3.11.0
         */
        context(repository: CrudRepository<T, ID>)
        infix fun has(id: ID) = repository.existsById(id)
        /**
         * Checks if an entity with the given [id] exists in the repository.
         * If the entity does not exist, an exception is thrown.
         *
         * @param id The identifier of the entity to check for existence.
         * @throws Throwable If the entity does not exist, an exception defined by the underlying `existsByIdOrThrow` implementation is raised.
         * @since 3.11.0
         */
        context(repository: JpaRepository<T, ID>)
        infix fun hasOrThrow(id: ID) = repository.existsByIdOrThrow(id, this::class as KClass<T>)
        /**
         * Checks if an entity with the specified [id] exists in the repository and throws an exception if it does not.
         *
         * @param id The identifier of the entity to check for existence.
         * @param lazyException A function supplying the exception to be thrown if the entity does not exist.
         * @throws Throwable If the entity does not exist and the supplied [lazyException] provides the throwable to be raised.
         * @since 3.11.0
         */
        context(repository: JpaRepository<T, ID>)
        fun hasOrThrow(id: ID, lazyException: ThrowableSupplier) = repository.existsByIdOrThrow(id, this::class as KClass<T>, lazyException)

        /**
         * Retrieves an entity by its ID or throws an exception if not found.
         *
         * @param id The ID of the entity to retrieve.
         * @return The entity associated with the specified ID.
         * @throws NoSuchElementException if the entity is not found.
         * @since 3.11.0
         */
        context(repository: JpaRepository<T, ID>)
        operator fun get(id: ID) = repository.findByIdOrThrow(id, this::class as KClass<T>)
        /**
         * Retrieves an entity by its ID or throws an exception if not found.
         *
         * @param id The identifier of the entity to be retrieved.
         * @param lazyException A supplier for the exception to be thrown if the entity is not found.
         * @return The entity associated with the given ID.
         * @since 3.11.0
         */
        context(repository: JpaRepository<T, ID>)
        operator fun get(id: ID, lazyException: ThrowableSupplier) = repository.findByIdOrThrow(id, this::class as KClass<T>, lazyException)
        /**
         * Retrieves an entity from the repository that matches the specified predicate.
         *
         * @param predicate The condition used to locate the desired entity.
         * @return The entity that satisfies the provided predicate, or null if no entity matches.
         * @since 3.11.0
         */
        context(repository: JpaRepository<T, ID>)
        operator fun get(predicate: Predicate<T>) = repository[predicate]
        /**
         * Retrieves a collection of entities corresponding to the provided iterable of IDs from the repository.
         *
         * @param ids An iterable containing the IDs of the entities to be retrieved.
         * @since 3.11.0
         */
        context(repository: JpaRepository<T, ID>)
        operator fun get(ids: Iterable<ID>) = repository[ids, this::class as KClass<T>]
        /**
         * Retrieves an entity by its unique identifier or returns null if the entity is not found.
         *
         * @param id The unique identifier of the entity to be retrieved.
         * @return The entity associated with the given identifier, or null if no such entity exists.
         * @since 3.11.0
         */
        context(repository: JpaRepository<T, ID>)
        infix fun getOrNull(id: ID) = repository.findByIdOrNull(id)
        /**
         * Retrieves an entity by its ID or provides a default value if the entity is not found.
         *
         * @param id The ID of the entity to be retrieved.
         * @param defaultValue A supplier that provides a default value in case the entity with the specified ID is not found.
         * @since 3.11.1
         */
        context(repository: JpaRepository<T, ID>)
        fun getOr(id: ID, defaultValue: Supplier<T>) = repository.findByIdOr(id, defaultValue)

        /**
         * Counts the number of entities that match the given predicate.
         *
         * @param predicate the condition to filter the entities to be counted
         * @return the number of entities matching the predicate
         * @since 3.11.0
         */
        context(repository: JpaRepository<T, ID>)
        fun count(predicate: Predicate<T>): Long = repository.count(predicate)

        /**
         * Adds the given entity to the repository by saving it.
         *
         * @param entity The entity to be added to the repository.
         * @since 3.11.0
         */
        context(repository: CrudRepository<T, *>)
        operator fun plusAssign(entity: T) { repository.save(entity) }
        /**
         * Operator function that deletes an entity from the repository based on its ID.
         *
         * @param id The identifier of the entity to be deleted.
         * @since 3.11.0
         */
        context(repository: JpaRepository<T, ID>)
        operator fun minusAssign(id: ID) { repository.deleteByIdOrThrow(id, this::class as KClass<T>) }
        /**
         * Deletes an entity with the given identifier from the repository.
         *
         * @param id The identifier of the entity to be deleted.
         * @since 3.11.0
         */
        context(repository: CrudRepository<*, ID>)
        infix fun delete(id: ID) { repository.deleteById(id) }

        /**
         * Flushes all pending changes to the database.
         *
         * This method ensures that any modifications made to the persistence context are synchronized
         * with the underlying database immediately. It forces the persistence context to perform
         * a commit of all changes that have been queued up but not yet written to the database.
         *
         * This is typically used in scenarios where an immediate update to the database is required
         * and waiting for the transaction to naturally commit is not desired or practical.
         *
         * Delegates the flush operation to the underlying `JpaRepository`.
         * @since 3.11.0
         */
        context(repository: JpaRepository<T, ID>)
        fun flush() = repository.flush()
    }
}

/**
 * Saves all entities in the iterable to the repository. Optionally supports flushing changes
 * and executing the save operation in batches.
 *
 * @param flush Indicates whether the repository should flush changes immediately after saving. Defaults to false.
 * @since 3.11.0
 */
context(repository: JpaRepository<T, *>)
fun <T : EnhancedEntity<T, *>> Iterable<T>.saveAll(flush: Boolean = false) =
    (if (flush) repository.saveAllAndFlush(this) else repository.saveAll(this)).toList()
/**
 * Deletes all entities in the current collection from the repository.
 * Optionally allows flushing changes and performing the operation in batches.
 *
 * @param flush Specifies whether the repository should flush changes after deleting the entities. Defaults to false.
 * @param inBatch Specifies whether the deletion should be performed in batches. Defaults to false.
 * @since 3.11.0
 */
context(repository: JpaRepository<T, *>)
fun <T : EnhancedEntity<T, *>> Iterable<T>.deleteAll(flush: Boolean = false, inBatch: Boolean = false) =
    (if (inBatch) repository.deleteAllInBatch(this) else repository.deleteAll(this)).apply {
        if (flush) repository.flush()
    }