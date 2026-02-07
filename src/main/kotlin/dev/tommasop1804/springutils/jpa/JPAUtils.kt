@file:JvmName("JPAUtilsKt")
@file:Since("1.0.0")
@file:Suppress("unused", "FunctionName")

package dev.tommasop1804.springutils.jpa

import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.annotations.Since
import dev.tommasop1804.kutils.classes.constants.SortDirection
import dev.tommasop1804.kutils.classes.pagination.Chunked
import dev.tommasop1804.kutils.classes.pagination.FilterOption
import dev.tommasop1804.kutils.classes.pagination.SortOption
import dev.tommasop1804.kutils.exceptions.PropertyNotAccessibleException
import dev.tommasop1804.kutils.exceptions.ResourceNotFoundException
import dev.tommasop1804.kutils.exceptions.TooManyResultsException
import org.springframework.data.domain.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.CrudRepository
import kotlin.apply
import kotlin.jvm.optionals.getOrNull
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1
import kotlin.reflect.jvm.javaField

/**
 * Extension property for `JpaRepository` that checks if the repository is not empty.
 *
 * Retrieves the total count of entities in the repository and returns `true` if the count is greater than zero, otherwise `false`.
 *
 * It provides a convenient way to verify the existence of any entities in the repository without fetching the actual data.
 * @since 1.0.0
 */
val <T : Any> JpaRepository<T, *>.isNotEmpty
    get() = count() > 0
/**
 * Extension property for checking if the repository contains no entities.
 * Returns `true` if the repository is empty, `false` otherwise.
 * @since 1.0.0
 */
val <T : Any> JpaRepository<T, *>.isEmpty
    get() = count() == 0L

/**
 * Retrieves the first entity from the repository based on its natural order.
 *
 * This method fetches the first entity by requesting a single element from the repository using pagination.
 * If no elements are found, it throws a `NoSuchElementException`.
 *
 * @return The first entity of type `T` in the repository.
 * @throws NoSuchElementException if the repository does not contain any elements.
 * @since 1.0.0
 */
fun <T : Any> JpaRepository<T, *>.first(): T = findAll(PageRequest.of(0, 1)).firstOrThrow { NoSuchElementException("No elements found.") }
/**
 * Retrieves the first entity from the repository or returns null if the repository is empty.
 *
 * This method fetches the first result by requesting a single page of size one and
 * then extracting the first element if present.
 *
 * @return the first entity if available, or null if the repository is empty
 * @since 1.0.0
 */
fun <T : Any> JpaRepository<T, *>.firstOrNull(): T? = findAll(PageRequest.of(0, 1)).firstOrNull()
/**
 * Returns the first element of the repository wrapped in a PageRequest with a page size of 1.
 * If no elements are found, this method throws an exception provided by the given lazy exception supplier.
 *
 * @param lazyException A lambda function that supplies the exception to be thrown if no element is found.
 * @return The first element from the repository.
 * @throws Throwable The exception supplied by the lazyException lambda if no element is found.
 * @since 1.0.0
 */
inline fun <T : Any> JpaRepository<T, *>.firstOrThrow(lazyException: () -> Throwable): T = findAll(PageRequest.of(0, 1)).firstOrNull() ?: throw lazyException()

/**
 * Verifies whether an entity of type [T] with the specified [id] exists in the repository.
 * If the entity does not exist, the provided [lazyException] is thrown.
 *
 * @param id The identifier of the entity to check for existence.
 * @param lazyException A supplier function that provides an exception to be thrown if the entity does not exist.
 * Defaults to throwing a [ResourceNotFoundException] with the given [id] and entity type [T].
 * @throws Throwable If the entity does not exist and the [lazyException] provides the throwable to be raised.
 * @since 1.0.0
 */
inline fun <reified T : Any, ID : Any> CrudRepository<T, ID>.existsByIdOrThrow(id: ID, lazyException: ThrowableSupplier = { ResourceNotFoundException(id, T::class) }) =
    existsById(id) || throw lazyException()
/**
 * Checks whether an entity exists by its identifier in the repository. If the entity does not exist,
 * a `ResourceNotFoundException` is thrown with the provided lazy message.
 *
 * @param id the identifier of the entity to check for existence.
 * @param lazyMessage a supplier for the exception message to be used if the entity is not found.
 * @throws ResourceNotFoundException if no entity exists with the given identifier.
 * @since 1.0.0
 */
@JvmName("existsByIdOrThrowLazyMessage")
inline fun <reified T : Any, ID : Any> CrudRepository<T, ID>.existsByIdOrThrow(id: ID, lazyMessage: Supplier<Any>) =
    existsById(id) || throw ResourceNotFoundException(message = lazyMessage().toString())

/**
 * Finds an entity by its ID or throws an exception if the entity is not found.
 *
 * @param id The ID of the entity to retrieve.
 * @param lazyException A supplier for the exception to be thrown if the entity is not found. Defaults to a `ResourceNotFoundException` initialized with the provided ID and type.
 * @return The entity associated with the provided ID if found.
 * @throws Throwable The exception provided by the `lazyException` supplier if the entity is not found.
 * @since 1.0.0
 */
inline fun <reified T : Any, ID : Any> CrudRepository<T, ID>.findByIdOrThrow(id: ID, lazyException: ThrowableSupplier = { ResourceNotFoundException(id, T::class) }): T =
    findById(id).getOrNull() ?: throw lazyException()
/**
 * Finds an entity by its ID or throws a `ResourceNotFoundException` if the entity is not found.
 *
 * @param id The unique identifier of the entity to find.
 * @param internalErrorCode Optional error code to be used in the exception message.
 * @param lazyMessage A supplier providing the error message to be used if the entity is not found.
 * @return The entity of type `T` if found.
 * @since 1.0.0
 */
@JvmName("findByIdOrThrowLazyMessage")
inline fun <reified T : Any, ID : Any> CrudRepository<T, ID>.findByIdOrThrow(id: ID, internalErrorCode: String? = null, lazyMessage: Supplier<Any>): T =
    findById(id).getOrNull() ?: throw ResourceNotFoundException(message = lazyMessage().toString(), internalErrorCode = internalErrorCode)

/**
 * Finds all entities matching the given example and sorts the results based on the provided sort options.
 *
 * @param example the example entity used to filter the results
 * @param sort the sort options defining the sorting direction and property fields
 * @return a list of entities matching the specified example and sorted according to the given sort options
 * @since 1.0.0
 */
inline fun <reified T : Any> JpaRepository<T, *>.findAll(example: Example<T>, vararg sort: SortOption): List<T> = findAll(example, sort.toList().toJPASort())
/**
 * Retrieves a list of entities of type [T] from the repository, sorted according to the provided sorting options.
 *
 * @param sort An array of [SortOption] objects specifying the sorting criteria, including field names and sort direction.
 * @return A list of entities of type [T] sorted as per the provided options.
 * @since 1.0.0
 */
inline fun <reified T : Any> JpaRepository<T, *>.findAll(vararg sort: SortOption): List<T> = findAll(sort.toList().toJPASort())

/**
 * Retrieves an entity by its ID or throws an exception if the entity is not found.
 *
 * This is a shorthand operator function for `findByIdOrThrow`, allowing more concise syntax.
 *
 * @param id The ID of the entity to retrieve.
 * @return The entity associated with the provided ID if found.
 * @throws Throwable If the entity is not found, the exception supplied by `findByIdOrThrow` will be thrown.
 * @since 1.0.0
 */
inline operator fun <reified T : Any, ID : Any> CrudRepository<T, ID>.get(id: ID): T = findByIdOrThrow(id)
/**
 * Provides a convenient operator to retrieve an entity by its ID from the repository
 * or throw a `ResourceNotFoundException` if the entity is not found.
 *
 * @param id The unique identifier of the entity to retrieve.
 * @param internalErrorCode Optional error code to include in the exception if the entity is not found.
 * @param lazyMesage A supplier providing the error message to include in the exception if the entity is not found.
 * @return The entity of type `T` if found in the repository.
 * @since 1.0.0
 */
inline operator fun <reified T : Any, ID : Any> CrudRepository<T, ID>.get(id: ID, internalErrorCode: String? = null, lazyMesage: Supplier<Any>): T = findByIdOrThrow(id, internalErrorCode, lazyMesage)
/**
 * Retrieves an entity by its ID from the repository or throws an exception if the entity is not found.
 *
 * @param id The ID of the entity to retrieve.
 * @param lazyException A supplier for the exception to be thrown if the entity is not found.
 *                     This allows for deferred exception construction, ensuring exceptions
 *                     are created only when needed.
 * @return The entity associated with the provided ID if found.
 * @throws Throwable The exception provided by the `lazyException` supplier if the entity is not found.
 * @since 1.0.0
 */
inline operator fun <reified T : Any, ID : Any> CrudRepository<T, ID>.get(id: ID, lazyException: ThrowableSupplier): T = findByIdOrThrow(id, lazyException)
/**
 * Retrieves a list of entities with the specified IDs from the repository.
 *
 * @param ids The collection of IDs corresponding to the entities to be retrieved.
 * @return A list of entities matching the provided IDs.
 * @since 1.0.0
 */
inline operator fun <reified T : Any, ID : Any> CrudRepository<T, ID>.get(ids: Iterable<ID>): List<T> = findAllById(ids).toList()

/**
 * Retrieves an entity from the repository based on a set of property-value pairs. If no entity is found,
 * or more than one entity is found, a custom exception can be thrown.
 *
 * @param T The type of the entity.
 * @param search A variable number of pairs representing the properties to match and their corresponding values.
 * @param lazyException A supplier for the exception to throw if no entity is found. Defaults to a `ResourceNotFoundException`.
 * @return The entity matching the specified criteria.
 * @throws PropertyNotAccessibleException If a property provided in the search is not accessible.
 * @throws TooManyResultsException If multiple results are found when one was expected.
 * @since 1.0.0
 */
inline operator fun <reified T : Any> JpaRepository<T, *>.get(
    vararg search: Pair<KProperty1<T, *>, *>,
    noinline lazyException: ThrowableSupplier = { ResourceNotFoundException(T::class) }
): T = invoke(*search) then {
    if (isEmpty()) throw lazyException()
    onlyElementOrThrow { TooManyResultsException(size) }
}

/**
 * Retrieves an entity of type [T] from the repository based on the provided search criteria.
 *
 * @param T The type of the entity to be retrieved. Must be a non-nullable type.
 * @param search A vararg of [Pair]s where each pair consists of a property of the entity and its expected value.
 * @param internalErrorCode An optional error code to include in the exception message if the entity is not found.
 * @param lazyMesage A supplier function to lazily generate a custom error message when an exception is thrown.
 * @return The entity of type [T] that matches the specified search criteria.
 * @throws ResourceNotFoundException If no entity matching the criteria is found.
 * @throws TooManyResultsException If multiple entities match the criteria.
 * @throws IllegalStateException If one of the properties in the search criteria is not accessible.
 * @since 1.0.0
 */
inline operator fun <reified T : Any> JpaRepository<T, *>.get(
    vararg search: Pair<KProperty1<T, *>, *>,
    internalErrorCode: String? = null,
    crossinline lazyMesage: Supplier<Any>
): T = invoke(*search) then {
    requireOrThrow({ ResourceNotFoundException(lazyMesage().toString(), internalErrorCode = internalErrorCode) }) { isEmpty() }
    onlyElementOrThrow { TooManyResultsException(size) }
}

/**
 * Retrieves all entities of type T from the repository.
 *
 * @return A list containing all entities of type T found in the repository.
 * @since 1.0.0
 */
operator fun <T : Any> JpaRepository<T, *>.invoke(): List<T> = findAll()
/**
 * Invokes the repository to filter its entries based on the given predicate.
 *
 * @param filter A predicate that defines the filtering criteria to be applied
 *               on the repository's entries.
 * @return A list of entries that match the specified filtering criteria.
 * @since 1.0.0
 */
operator fun <T : Any> CrudRepository<T, *>.invoke(filter: Predicate<T>) = findAll().filter(filter)
/**
 * Retrieves a list of entities with the specified IDs from the repository.
 *
 * @param ids The collection of IDs corresponding to the entities to be retrieved.
 * @return A list of entities matching the provided IDs.
 * @since 1.0.0
 */
operator fun <T : Any, ID : Any> CrudRepository<T, ID>.invoke(ids: Iterable<ID>): List<T> = findAllById(ids).toList()
/**
 * Extension operator function for invoking a JpaRepository with a specified Pageable
 * to retrieve a paginated result of entities.
 *
 * @param pageable the pagination information specifying the page number, size, and sorting
 * @return a Page containing the paginated list of entities of type T
 * @since 1.0.0
 */
operator fun <T : Any> JpaRepository<T, *>.invoke(pageable: Pageable): Page<T> = findAll(pageable)
/**
 * Retrieves all entities of type [T] from the repository, sorted according to the provided [Sort] order.
 *
 * @param sort the sorting criteria to apply when retrieving entities.
 * @return a list of all entities of type [T], sorted as specified.
 * @since 1.0.0
 */
operator fun <T : Any> JpaRepository<T, *>.invoke(sort: Sort): List<T> = findAll(sort)
/**
 * Retrieves a list of entities of type [T] from the repository, sorted according to the provided sorting options.
 *
 * @param sort An array of [SortOption] objects specifying the sorting criteria, including field names and sort direction.
 * @return A list of entities of type [T] sorted as per the provided options.
 * @since 1.0.0
 */
operator fun <T : Any> JpaRepository<T, *>.invoke(vararg sort: SortOption): List<T> = findAll(sort.toList().toJPASort())
/**
 * Invokes the repository to find all entities matching the provided example.
 *
 * This operator function simplifies the process of querying the repository
 * by allowing direct invocation with an example instance.
 *
 * @param example an Example object used for creating the query to match entities.
 * @return a list of entities that match the criteria defined in the example.
 * @since 1.0.0
 */
operator fun <T : Any> JpaRepository<T, *>.invoke(example: Example<T>): List<T> = findAll(example)
/**
 * Invokes the `findAll` function of the `JpaRepository` with the provided example and sorting criteria.
 *
 * @param example the example object used to define the query conditions.
 * @param sort the sorting criteria to apply to the query results.
 * @return a list of entities that match the given example and sort criteria.
 * @since 1.0.0
 */
operator fun <T : Any> JpaRepository<T, *>.invoke(example: Example<T>, sort: Sort): List<T> = findAll(example, sort)
/**
 * Finds all entities matching the given example and sorts the results based on the provided sort options.
 *
 * @param example the example entity used to filter the results
 * @param sort the sort options defining the sorting direction and property fields
 * @return a list of entities matching the specified example and sorted according to the given sort options
 * @since 1.0.0
 */
operator fun <T : Any> JpaRepository<T, *>.invoke(example: Example<T>, vararg sort: SortOption): List<T> = findAll(example, sort.toList().toJPASort())
/**
 * Executes a query based on the provided example and pageable information.
 *
 * @param example an example instance used to define query conditions.
 * @param pageable pagination information for the query result.
 * @return a page of entities matching the query defined by the example.
 * @since 1.0.0
 */
operator fun <T : Any> JpaRepository<T, *>.invoke(example: Example<T>, pageable: Pageable): Page<T> = findAll(example, pageable)
/**
 * Invokes a search on the repository using specified property-value pairs to create a probe object.
 *
 * @param T The type of the entity.
 * @param R The type of the property values to search by.
 * @param search A vararg of property-value pairs that will be used to set values on a probe instance.
 * @return A list of entities matching the probe instance created from the provided properties.
 * @throws PropertyNotAccessibleException If any of the specified properties is not accessible.
 * @since 1.0.0
 */
inline operator fun <reified T : Any, R> JpaRepository<T, *>.invoke(vararg search: Pair<KProperty1<T, R>, R>): List<T> {
    val probe = T::class.java.getDeclaredConstructor().newInstance()
    search.forEach { (property, value) ->
        property.javaField?.apply { isAccessible = true; set(probe, value) }
            ?: throw PropertyNotAccessibleException("Property ${property.name} is not accessible.")
    }
    val nullValues = search.filter { it.second.isNull() }
    val list = findAll(Example.of(probe))
    if (nullValues.isEmpty()) return list
    list.removeIf { e -> nullValues.any { e.getPropertyValue<T, Any?>(it.first.name).isNotNull() } }
    return list
}

/**
 * Counts the number of entries in the repository that satisfy the given predicate.
 *
 * WARNING: This method is not efficient as `count()`. This use a `findAll` operation and
 * perform a kotlin-filtering.
 *
 * @param predicate The condition used to filter the entries in the repository.
 * @return The count of entries that match the specified predicate.
 * @since 1.0.0
 */
fun <T : Any> CrudRepository<T, *>.count(predicate: Predicate<T>): Long = invoke(predicate).size.toLong()
/**
 * Invokes a count on the repository using specified property-value pairs to create a probe object.
 *
 * @param T The type of the entity.
 * @param R The type of the property values to search by.
 * @param search A vararg of property-value pairs that will be used to set values on a probe instance.
 * @return A count of entities matching the probe instance created from the provided properties.
 * @throws PropertyNotAccessibleException If any of the specified properties is not accessible.
 * @since 1.0.0
 */
inline fun <reified T : Any, R> JpaRepository<T, *>.count(vararg search: Pair<KProperty1<T, R>, R>): Long {
    val probe = T::class.java.getDeclaredConstructor().newInstance()
    search.forEach { (property, value) ->
        property.javaField?.apply { isAccessible = true; set(probe, value) }
            ?: throw PropertyNotAccessibleException("Property ${property.name} is not accessible.")
    }
    val nullValues = search.filter { it.second.isNull() }
    return if (nullValues.isEmpty()) count(Example.of(probe))
    else invoke(*search).size.toLong()
}

/**
 * Deletes an entity by its ID or throws a provided exception if the entity is not found.
 *
 * @param id The ID of the entity to be deleted.
 * @param lazyException A supplier for the exception to be thrown if the entity is not found.
 * Defaults to a `ResourceNotFoundException` with the provided ID and the entity type.
 * @param T The type of the entity.
 * @param ID The type of the entity's ID.
 * @since 1.0.0
 */
inline fun <reified T : Any, ID : Any> CrudRepository<T, ID>.deleteByIdOrThrow(id: ID, lazyException: ThrowableSupplier = { ResourceNotFoundException(id, T::class) }) =
    (findById(id).getOrNull() ?: throw lazyException()) then (::delete)
/**
 * Deletes an entity by its ID if it exists, or throws a `ResourceNotFoundException` with the specified message if it does not.
 *
 * @param id The ID of the entity to delete.
 * @param internalErrorCode Optional internal error code for the exception.
 * @param lazyMessage A supplier function that provides the exception message if the entity does not exist.
 * @throws ResourceNotFoundException If no entity with the specified ID exists.
 * @since 1.0.0
 */
@JvmName("deleteByIdOrThrowLazyMessage")
inline fun <reified T : Any, ID : Any> CrudRepository<T, ID>.deleteByIdOrThrow(id: ID, internalErrorCode: String? = null, lazyMessage: Supplier<Any>) =
    (findById(id).getOrNull() ?: throw ResourceNotFoundException(message = lazyMessage().toString(), internalErrorCode = internalErrorCode)) then { deleteById(id) }

/**
 * Checks if an entity with the given ID exists in the repository.
 *
 * @param id The ID of the entity to check for existence in the repository.
 * @return `true` if an entity with the given ID exists, `false` otherwise.
 * @since 1.0.0
 */
operator fun <T : Any, ID : Any> CrudRepository<T, ID>.contains(id: ID) = existsById(id)
/**
 * Checks if all entities with the given IDs exist in the repository.
 *
 * This operator function allows using the `in` operator to verify whether all entities
 * identified by the provided collection of IDs are present in the repository.
 *
 * @param ids an iterable collection of entity IDs to check for existence
 * @return true if all entities with the specified IDs exist in the repository, false otherwise
 * @since 1.0.0
 */
operator fun <T : Any, ID : Any> CrudRepository<T, ID>.contains(ids: Iterable<ID>) = ids.all { existsById(it) }

/**
 * Adds the given entity to the repository by saving it.
 *
 * @param entity The entity to be saved in the repository.
 * @since 1.0.0
 */
operator fun <T : Any> CrudRepository<T, *>.plusAssign(entity: T) {
    save(entity)
}
/**
 * Adds the provided iterable of entities to the repository by invoking the saveAll function.
 *
 * @param entities The iterable collection of entities to be added to the repository.
 * @since 1.0.0
 */
operator fun <T : Any> CrudRepository<T, *>.plusAssign(entities: Iterable<T>) {
    saveAll(entities)
}
/**
 * Removes an entity identified by the provided ID from the repository.
 *
 * @param id The identifier of the entity to be removed.
 * @param T The type of the entity.
 * @param ID The type of the entity's ID.
 * @since 1.0.0
 */
inline operator fun <reified T : Any, ID : Any> CrudRepository<T, ID>.minusAssign(id: ID) {
    deleteByIdOrThrow(id)
}
/**
 * Removes multiple entities identified by the given IDs from the repository.
 *
 * @param ids An iterable collection of IDs corresponding to the entities to be deleted.
 * @since 1.0.0
 */
operator fun <ID : Any> CrudRepository<*, ID>.minusAssign(ids: Iterable<ID>) {
    deleteAllById(ids)
}

/**
 * Creates a new PageRequest object with the specified offset and page size.
 *
 * @param offset The zero-based offset of the first item to be retrieved.
 * @param pageSize The maximum number of items to be retrieved in the page.
 * @return A PageRequest object configured with the given offset and page size.
 * @since 1.0.0
 */
fun PageRequest(offset: Int, pageSize: Int): PageRequest = PageRequest.of(offset, pageSize)
/**
 * Creates a new PageRequest instance with the specified offset, page size, and sorting parameters.
 *
 * @param offset the zero-based starting position of the page.
 * @param pageSize the number of items to include in a page.
 * @param sort the sorting parameters to apply to the page request.
 * @return a new PageRequest instance with the specified parameters.
 * @since 1.0.0
 */
fun PageRequest(offset: Int, pageSize: Int, sort: Sort): PageRequest = PageRequest.of(offset, pageSize, sort)
/**
 * Creates a new PageRequest object for pagination and sorting.
 *
 * @param offset The zero-based page index, representing the starting point of the page.
 * @param pageSize The number of items to be included in the page.
 * @param sort The sorting configuration, including the direction and field details.
 * @return A PageRequest object configured with the provided pagination and sorting parameters.
 * @since 1.0.0
 */
fun PageRequest(offset: Int, pageSize: Int, sort: SortOption): PageRequest = PageRequest.of(offset, pageSize, Sort.by(
    when (sort.direction) {
        SortDirection.ASCENDING -> Sort.Direction.ASC
        SortDirection.DESCENDING -> Sort.Direction.DESC
    },
    *(sort.field.split(".").toTypedArray())
))
/**
 * Creates a new PageRequest instance with the specified offset, page size, sorting direction,
 * and sorting properties.
 *
 * @param offset the zero-based index of the first element to be retrieved.
 * @param pageSize the number of elements to be retrieved per page.
 * @param direction the direction of sorting, either ascending or descending.
 * @param properties the properties to sort by.
 * @return a PageRequest instance configured with the provided parameters.
 * @since 1.0.0
 */
fun PageRequest(offset: Int, pageSize: Int, direction: Sort.Direction, vararg properties: String): PageRequest = PageRequest.of(offset, pageSize, direction, *properties)
/**
 * Creates a PageRequest object that is used for pagination and sorting of query results.
 *
 * @param offset The offset index indicating the starting point of the page.
 * @param pageSize The number of items to be included in each page.
 * @param direction The direction of sorting (ascending or descending).
 * @param properties The properties by which the results should be sorted.
 * @return A PageRequest object configured with the specified pagination and sorting parameters.
 * @since 1.0.0
 */
fun PageRequest(offset: Int, pageSize: Int, direction: Sort.Direction, vararg properties: KProperty<*>): PageRequest = PageRequest.of(offset, pageSize, direction, *(properties.map(KProperty<*>::name).toTypedArray()))
/**
 * Constructs a new `PageRequest` with the specified offset, page size, sort direction, and properties.
 *
 * @param offset the zero-based page index that specifies the starting position of the results.
 * @param pageSize the number of items to be included in a single page.
 * @param direction the direction of sorting, either ascending or descending.
 * @param properties the properties by which results should be sorted, in priority order.
 * @return a `PageRequest` instance configured with the given parameters.
 * @since 1.0.0
 */
fun PageRequest(offset: Int, pageSize: Int, direction: SortDirection, vararg properties: String): PageRequest = PageRequest.of(offset, pageSize, direction.toJPASortDirection(), *properties)
/**
 * Creates and returns a PageRequest object configured with the given parameters for pagination and sorting.
 *
 * @param offset the zero-based page index to start retrieving data from.
 * @param pageSize the number of items to be included in each page.
 * @param direction the sorting direction, either ascending or descending.
 * @param properties the properties to sort by.
 * @return a PageRequest object configured with the provided page and sorting settings.
 * @since 1.0.0
 */
fun PageRequest(offset: Int, pageSize: Int, direction: SortDirection, vararg properties: KProperty<*>): PageRequest = PageRequest.of(offset, pageSize, direction.toJPASortDirection(), *(properties.map(KProperty<*>::name).toTypedArray()))
/**
 * Returns a new [PageRequest] with the specified sort configuration applied.
 *
 * @param sort A variable number of [SortOption] objects representing the sorting criteria.
 * @return A new [PageRequest] instance incorporating the specified sort options.
 * @since 1.0.0
 */
fun PageRequest.withSort(vararg sort: SortOption): PageRequest = withSort(sort.toList().toJPASort())
/**
 * Returns a new `PageRequest` instance with sorting applied based on the specified direction and properties.
 *
 * @param direction The direction of the sort, either ascending or descending.
 * @param properties The property or properties to sort by.
 * @return A `PageRequest` instance with the specified sorting applied.
 * @since 1.0.0
 */
fun PageRequest.withSort(direction: SortDirection, vararg properties: String): PageRequest = withSort(Sort.by(
    direction.toJPASortDirection(),
    *properties
))
/**
 * Creates a new PageRequest instance with sorting applied based on the provided direction and properties.
 *
 * @param direction The direction of sorting, either ascending or descending.
 * @param properties The properties to sort by.
 * @return A new PageRequest object with sorting applied.
 * @since 1.0.0
 */
fun PageRequest.withSort(direction: SortDirection, vararg properties: KProperty<*>): PageRequest = withSort(Sort.by(
    direction.toJPASortDirection(),
    *(properties.map(KProperty<*>::name).toTypedArray())
))

/**
 * Converts a `Page` object into a `Chunked` object, preserving pagination details
 * and optionally applying a limit and filters.
 *
 * @param limit Optional limit to override the page size. If not provided, the page's size is used.
 * @param appliedFilters A list of filters applied to the `Chunked` object. An empty list by default.
 * @return A `Chunked` object containing pagination details and the content of the page.
 * @since 1.0.0
 */
fun <T : Any> Page<T>.toChunkedObj(limit: Int? = null, appliedFilters: List<FilterOption> = emptyList()) = Chunked(
    totalPages,
    number,
    totalElements.toInt(),
    limit ?: size,
    appliedFilters,
    sort.toSortOption(),
    content
)
/**
 * Converts a `Chunked<T>` instance into a `PageImpl` object representing a paginated JPA-compatible page.
 *
 * The method takes the properties of the `Chunked<T>` object, such as `data`, `pageIndex`, `limit`,
 * `sort`, and `totalElements`, and uses them to create and return a new `PageImpl` instance. The resulting
 * `PageImpl` object encapsulates the paginated data along with details like sorting and pagination metadata.
 *
 * @receiver The `Chunked<T>` instance to be converted into a JPA-compatible page representation.
 * @return A `PageImpl` instance containing the paginated data and metadata derived from the original `Chunked<T>`.
 * @since 1.0.0
 */
fun <T : Any> Chunked<T>.toJPAPage() = PageImpl(
    data.orEmpty(),
    PageRequest(
        pageIndex,
        limit.requiredField(::limit, "chunked"),
        sort.toJPASort()
    ),
    totalElements.toLong()
)

internal fun Sort.toSortOption(): List<SortOption> = toList().map { SortOption(it.property, when (it.direction) {
    Sort.Direction.ASC -> SortDirection.ASCENDING
    Sort.Direction.DESC -> SortDirection.DESCENDING
}) }
/**
 * Converts a collection of SortOption objects into a JPA Sort instance.
 *
 * This function transforms each SortOption in the collection into a JPA Sort.Order by converting
 * the sort direction and field name, then combines all orders into a single Sort object.
 *
 * @return a JPA Sort instance containing all the sort orders from this collection
 * @since 1.0.0
 */
fun Collection<SortOption>.toJPASort(): Sort {
    val list = emptyMList<Sort.Order>()
    forEach { list.add(Sort.Order(it.direction.toJPASortDirection(), it.field)) }
    return Sort.by(list)
}
/**
 * Converts a SortDirection enum value to its corresponding JPA Sort.Direction value.
 *
 * Maps ASCENDING to Sort.Direction.ASC and DESCENDING to Sort.Direction.DESC,
 * enabling seamless conversion between the application's sort direction representation
 * and JPA's sort direction type used in Spring Data queries.
 *
 * @receiver the SortDirection value to convert.
 * @return the corresponding JPA Sort.Direction value.
 * @since 1.0.0
 */
fun SortDirection.toJPASortDirection() = when (this) {
    SortDirection.ASCENDING -> Sort.Direction.ASC
    SortDirection.DESCENDING -> Sort.Direction.DESC
}
/**
 * Converts a Spring Data Sort.Direction to a SortDirection enumeration value.
 *
 * This extension function maps Spring Data's Sort.Direction enumeration to the corresponding
 * SortDirection enumeration used in this application. It performs a direct mapping where
 * ascending direction maps to ASCENDING and descending direction maps to DESCENDING.
 *
 * @return The corresponding SortDirection value: ASCENDING for ASC, DESCENDING for DESC
 * @since 1.0.0
 */
fun Sort.Direction.toSortDirection() = when (this) {
    Sort.Direction.ASC -> SortDirection.ASCENDING
    Sort.Direction.DESC -> SortDirection.DESCENDING
}