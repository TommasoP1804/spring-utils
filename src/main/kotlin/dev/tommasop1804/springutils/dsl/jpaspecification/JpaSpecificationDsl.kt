/*
 * Copyright © 2026 Tommaso Pastorelli (TommasoP1804) | Spring-Utils
 */

@file:Suppress("unused", "kutils_null_check")
@file:Since("3.1.0")

package dev.tommasop1804.springutils.dsl.jpaspecification

import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.annotations.*
import jakarta.persistence.criteria.*
import jakarta.persistence.criteria.Predicate
import org.springframework.data.jpa.domain.Specification

@DslMarker
annotation class SpecDslMarker

// --- CORE: PREDICATE NODE TREE ---

/**
 * Represents a node in a predicate tree structure that transforms into a JPA `Predicate`.
 * This interface is designed to be used in constructing dynamic query conditions.
 *
 * @param T the type of the entity being queried.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
sealed interface PredicateNode<T> {
    /**
     * Converts the current node into a JPA `Predicate` using the provided root, query, and criteria builder.
     * This method is used to dynamically build conditions for a JPA Criteria query.
     *
     * @param root the root type in the from clause, used to construct type-safe queries.
     * @param query the criteria query instance to which the predicate will be applied.
     * @param cb the criteria builder used to create the predicate.
     * @return the constructed `Predicate` representing the condition for this node.
     * @since 3.1.0
     */
    fun toPredicate(root: Root<T>, query: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate
}

// -- Leaf predicates --

/**
 * Represents a node that implements an equality predicate for a criteria query.
 * This class is used to construct a predicate that checks if a given path
 * equals a specified value or is null.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
class EqNode<T>(val path: String, val value: Any?) : PredicateNode<T> {
    /**
     * Constructs a JPA `Predicate` that represents an equality condition or a null-check
     * based on the specified path and value.
     *
     * @param root the root type in the from clause, used as the base for resolving the path.
     * @param query the criteria query instance to which the predicate will be applied.
     * @param cb the criteria builder used to create the predicate.
     * @return a `Predicate` that checks if the attribute at the resolved path equals the specified value
     *         or is null if the value itself is null.
     * @since 3.1.0
     */
    override fun toPredicate(root: Root<T>, query: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate =
        if (value.isNull()) cb.isNull(root.resolvePath<Any>(path))
        else cb.equal(root.resolvePath<Any>(path), value)
}

/**
 * Represents a node that implements a "not equal" (`<>`) predicate for a criteria query.
 * This class is used to construct a predicate that checks if a given path
 * is not equal to a specified value, or if the value is null, ensures the path is not null.
 *
 * @param T the type of the entity being queried.
 * @param path the dot-separated string path to the attribute being compared.
 * @param value the value to compare against the attribute at the specified path.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
class NeqNode<T>(val path: String, val value: Any?) : PredicateNode<T> {
    /**
     * Constructs a `Predicate` based on the specified path and value.
     * If the provided value is null, the predicate will check for non-null values at the specified path.
     * Otherwise, it will create a predicate checking for inequality with the provided value.
     *
     * @param root the root type in the from clause, used to resolve the entity path for the predicate.
     * @param query the criteria query instance to which the predicate will be applied.
     * @param cb the criteria builder used to construct the predicate.
     * @return the constructed `Predicate` representing the inequality condition or non-null check.
     * @since 3.1.0
     */
    override fun toPredicate(root: Root<T>, query: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate =
        if (value.isNull()) cb.isNotNull(root.resolvePath<Any>(path))
        else cb.notEqual(root.resolvePath<Any>(path), value)
}

/**
 * Represents a predicate node that constructs a "greater than" condition for a JPA criteria query.
 * This class is used to create a predicate that filters results where the value of a given path
 * is greater than the specified value.
 *
 * @param T the type of the entity being queried.
 * @param path the string representation of the path to the attribute being compared.
 * @param value the value that the attribute must be greater than.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
class GtNode<T>(val path: String, val value: Comparable<*>) : PredicateNode<T> {
    /**
     * Converts the current `GtNode` to a JPA `Predicate` that applies a "greater than" condition
     * on a specified path, using the provided root, query, and criteria builder.
     *
     * @param root the root type in the from clause, used to construct type-safe queries.
     * @param query the criteria query instance to which the predicate will be applied.
     * @param cb the criteria builder used to create the predicate.
     * @return a `Predicate` representing the "greater than" condition for the specified path and value.
     * @since 3.1.0
     */
    @Suppress("UNCHECKED_CAST")
    override fun toPredicate(root: Root<T>, query: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate =
        cb.greaterThan(root.resolvePath<Any>(path) as Expression<Comparable<Any>>, value as Comparable<Any>)
}

/**
 * Represents a node that implements a "greater than or equal to" (>=) predicate for a criteria query.
 * This class is used to construct a predicate that checks if the value at a specified path
 * is greater than or equal to a given comparable value.
 *
 * @param T the type of the entity being queried.
 * @property path the path within the entity to which the predicate is applied.
 * @property value the value to be compared against the attribute at the specified path.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
class GteNode<T>(val path: String, val value: Comparable<*>) : PredicateNode<T> {
    /**
     * Constructs a JPA `Predicate` representing a "greater than or equal to" condition
     * for the specified entity attribute and value.
     *
     * @param root the root type in the from clause, used to resolve the attribute path.
     * @param query the criteria query instance to which the predicate will be applied.
     * @param cb the criteria builder used to create the predicate.
     * @return a `Predicate` representing the "greater than or equal to" condition for the entity attribute.
     * @since 3.1.0
     */
    @Suppress("UNCHECKED_CAST")
    override fun toPredicate(root: Root<T>, query: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate =
        cb.greaterThanOrEqualTo(root.resolvePath<Any>(path) as Expression<Comparable<Any>>, value as Comparable<Any>)
}

/**
 * Represents a node in the predicate structure that corresponds to a "less than" comparison in a JPA criteria query.
 * This class is used to construct a condition that checks if the value of a specified attribute is less than
 * a given comparable value.
 *
 * @param T the type of the entity being queried.
 * @param path the dot-separated path of the attribute to evaluate in the "less than" comparison.
 * @param value the value to compare against the attribute's value.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
class LtNode<T>(val path: String, val value: Comparable<*>) : PredicateNode<T> {
    /**
     * Constructs a JPA `Predicate` representing a "less than" comparison between a resolved path
     * in the entity root and a specified value.
     *
     * @param root the root type in the from clause, used to resolve the path for comparison.
     * @param query the criteria query instance to which the predicate will be applied.
     * @param cb the criteria builder used to construct the predicate.
     * @return the constructed `Predicate` representing the "less than" condition.
     * @since 3.1.0
     */
    @Suppress("UNCHECKED_CAST")
    override fun toPredicate(root: Root<T>, query: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate =
        cb.lessThan(root.resolvePath<Any>(path) as Expression<Comparable<Any>>, value as Comparable<Any>)
}

/**
 * Represents a predicate node that implements a "less than or equal to" condition
 * for a JPA criteria query. This condition compares a specified path against
 * a given comparable value.
 *
 * @param T the type of the entity being queried.
 * @property path the path to the entity attribute to be compared.
 * @property value the comparable value used in the "less than or equal to" condition.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
class LteNode<T>(val path: String, val value: Comparable<*>) : PredicateNode<T> {
    /**
     * Converts the current `LteNode` instance into a JPA `Predicate` for the less-than-or-equal-to condition.
     * This condition is based on the specified path and value within the criteria query.
     *
     * @param root the root type in the from clause, used to resolve the path of the attribute to compare.
     * @param query the criteria query instance to which the constructed predicate will belong.
     * @param cb the criteria builder used to create the less-than-or-equal-to condition predicate.
     * @return the constructed `Predicate` representing the less-than-or-equal-to condition for the given path and value.
     * @since 3.1.0
     */
    @Suppress("UNCHECKED_CAST")
    override fun toPredicate(root: Root<T>, query: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate =
        cb.lessThanOrEqualTo(root.resolvePath<Any>(path) as Expression<Comparable<Any>>, value as Comparable<Any>)
}

/**
 * Represents a node that constructs a "like" predicate for a criteria query.
 * This class is used to create a condition that checks if a given path matches
 * a specified pattern, with an option for case sensitivity.
 *
 * @param T the type of the entity being queried.
 * @param path the attribute path to apply the "like" condition.
 * @param pattern the pattern to match against the attribute.
 * @param caseSensitive whether the comparison should be case-sensitive.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
class LikeNode<T>(val path: String, val pattern: String, val caseSensitive: Boolean) : PredicateNode<T> {
    /**
     * Builds a JPA `Predicate` based on the given parameters for evaluating the condition
     * defined by this node. It supports case-sensitive and case-insensitive matching depending
     * on the `caseSensitive` property.
     *
     * @param root the root entity type used for building the predicate, allowing navigation
     *             through entity attributes.
     * @param query the criteria query instance associated with the current predicate.
     * @param cb the criteria builder used to create query expressions and predicates.
     * @return the resulting `Predicate` that enforces the condition represented by this node.
     * @since 3.1.0
     */
    override fun toPredicate(root: Root<T>, query: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate {
        val expr = root.resolvePath<String>(path)
        return if (caseSensitive) cb.like(expr, pattern)
        else cb.like(cb.lower(expr), -pattern)
    }
}

/**
 * Represents a node that implements an "IN" predicate for a criteria query.
 * This class is used to construct a predicate that checks if the value
 * at the specified path is contained within a given collection of values.
 *
 * @param T the type of the entity being queried.
 * @param path the property path within the entity to be evaluated.
 * @param values the collection of values to be used in the "IN" clause.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
class InNode<T>(val path: String, val values: Collection<*>) : PredicateNode<T> {
    /**
     * Constructs a JPA `Predicate` representing an `IN` condition for the given path and values.
     *
     * @param root the root type in the from clause, used to navigate entity attributes.
     * @param query the criteria query instance to which the predicate will be applied.
     * @param cb the criteria builder used to create the `Predicate`.
     * @return the constructed `Predicate` representing the `IN` condition for the specified path and values.
     * @since 3.1.0
     */
    override fun toPredicate(root: Root<T>, query: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate =
        root.resolvePath<Any>(path).`in`(values)
}

/**
 * Represents a predicate node that negates an "IN" clause for a JPA criteria query.
 * It evaluates to true if the value of the specified path is not contained within the provided collection of values.
 *
 * @param T the type of the entity being queried.
 * @property path the path of the attribute to be evaluated.
 * @property values the collection of values against which the attribute is checked for exclusion.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
class NotInNode<T>(val path: String, val values: Collection<*>) : PredicateNode<T> {
    /**
     * Creates a `Predicate` that evaluates to true when the specified path does not match any value
     * in the provided collection.
     *
     * @param root the root type in the from clause, used to construct type-safe queries.
     * @param query the criteria query instance to which the predicate will be applied.
     * @param cb the criteria builder used to create the predicate.
     * @return the constructed `Predicate` representing the "NOT IN" condition.
     * @since 3.1.0
     */
    override fun toPredicate(root: Root<T>, query: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate =
        cb.not(root.resolvePath<Any>(path).`in`(values))
}

/**
 * Represents a node implementing a "between" predicate for a criteria query.
 * This class is used to construct a predicate that verifies whether the value
 * of a specified path falls within a given range (inclusive of the boundaries).
 *
 * @param T the type of the entity being queried.
 * @param path the attribute path to evaluate the "between" condition on.
 * @param lower the lower boundary of the range.
 * @param upper the upper boundary of the range.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
class BetweenNode<T>(val path: String, val lower: Comparable<*>, val upper: Comparable<*>) : PredicateNode<T> {
    /**
     * Creates a `Predicate` representing a "between" condition for the given entity attribute.
     *
     * @param root the root type in the from clause, used to access the entity attribute.
     * @param query the criteria query instance to which the predicate will be applied.
     * @param cb the criteria builder used to construct the "between" predicate.
     * @return the created `Predicate` representing the "between" condition.
     * @since 3.1.0
     */
    @Suppress("UNCHECKED_CAST")
    override fun toPredicate(root: Root<T>, query: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate =
        cb.between(
            root.resolvePath<Any>(path) as Expression<Comparable<Any>>,
            lower as Comparable<Any>,
            upper as Comparable<Any>,
        )
}

/**
 * Represents a node that generates a predicate for checking if a given path is null in a JPA criteria query.
 *
 * This class is used to construct a predicate that evaluates whether the attribute at a specified
 * path in the query is null.
 *
 * @param T the type of the entity being queried.
 * @param path the path within the entity that should be checked for null values.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
class IsNullNode<T>(val path: String) : PredicateNode<T> {
    /**
     * Constructs a `Predicate` that checks if the value at the specified path is `NULL`.
     *
     * @param root the root type in the from clause, used to construct type-safe queries.
     * @param query the criteria query instance to which the predicate will be applied.
     * @param cb the criteria builder used to create the predicate.
     * @return the constructed `Predicate` representing the "is null" condition.
     * @since 3.1.0
     */
    override fun toPredicate(root: Root<T>, query: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate =
        cb.isNull(root.resolvePath<Any>(path))
}

/**
 * Represents a node that implements an "is not null" predicate for a criteria query.
 * This class is used to construct a predicate that checks if the value of a given path
 * is not null at runtime.
 *
 * @param T the type of the root entity being queried.
 * @property path the attribute path of the entity to evaluate against the "is not null" condition.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
class IsNotNullNode<T>(val path: String) : PredicateNode<T> {
    /**
     * Creates a JPA `Predicate` that checks whether the value at the specified path is not null.
     *
     * @param root the root type in the from clause, used to construct type-safe queries.
     * @param query the criteria query instance to which the predicate will be applied.
     * @param cb the criteria builder used to create the predicate.
     * @return the constructed `Predicate` that evaluates to true if the value at the specified path is not null.
     * @since 3.1.0
     */
    override fun toPredicate(root: Root<T>, query: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate =
        cb.isNotNull(root.resolvePath<Any>(path))
}

/**
 * Represents a node that constructs a predicate to check if the value at a specified path is true.
 *
 * This class is part of a dynamic query-building mechanism and implements the `PredicateNode` interface.
 * It generates a JPA `Predicate` that verifies whether a boolean attribute resolved from the given path
 * evaluates to true.
 *
 * @param T the type of the entity being queried.
 * @param path the string representation of the path to the attribute to be checked.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
class IsTrueNode<T>(val path: String) : PredicateNode<T> {
    /**
     * Constructs a JPA `Predicate` that evaluates whether the resolved path specified by the `path` property
     * is `true`. This is primarily used to build dynamic query conditions for boolean attributes.
     *
     * @param root the root type in the from clause, used to construct type-safe queries.
     * @param query the criteria query instance to which the predicate will be applied.
     * @param cb the criteria builder used to create the predicate.
     * @return the constructed `Predicate` representing the condition where the resolved path evaluates to `true`.
     * @since 3.1.0
     */
    override fun toPredicate(root: Root<T>, query: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate =
        cb.isTrue(root.resolvePath(path))
}

/**
 * Represents a predicate node that checks if a specified attribute is false.
 * This node is used to construct a JPA `Predicate` that asserts the `false` condition
 * for the given path in a criteria query.
 *
 * @param T the type of the entity being queried.
 * @property path the attribute path used to resolve the property whose value is to be checked.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
class IsFalseNode<T>(val path: String) : PredicateNode<T> {
    /**
     * Constructs a JPA `Predicate` that verifies if the value resolved by the given `path`
     * is `false`. This method is part of the dynamic Criteria API query construction process.
     *
     * @param root the root type in the from clause, representing the entity being queried.
     * @param query the criteria query instance to which the constructed predicate will be applied.
     * @param cb the criteria builder to help construct the predicate.
     * @return a predicate representing a `false` condition for the specified path.
     * @since 3.1.0
     */
    override fun toPredicate(root: Root<T>, query: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate =
        cb.isFalse(root.resolvePath(path))
}

/**
 * Represents a predicate node for constructing a JPA criteria query that checks the existence
 * of a specific subquery. This node encapsulates an "EXISTS" operation for dynamically building
 * queries where the existence of related data is a condition for filtering results.
 *
 * @param T the type of the root entity in the main query.
 * @param S the type of the entity in the subquery.
 * @property subqueryType the class type of the subquery entity.
 * @property correlationPath the path used to establish a correlation between the root entity
 *                           and the subquery entity.
 * @property subqueryBlock a lambda with a receiver for configuring the subquery. It provides
 *                         access to the `SubqueryScope` for defining conditions and relationships
 *                         within the subquery.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
// Subquery exists
class ExistsSubqueryNode<T, S>(
    val subqueryType: Class<S>,
    val correlationPath: String,
    val subqueryBlock: ReceiverConsumer<SubqueryScope<T, S>>,
) : PredicateNode<T> {
    /**
     * Creates a JPA `Predicate` based on the current subquery configuration.
     * This function sets up a subquery, applies the provided subquery logic,
     * and checks for the existence of the subquery result.
     *
     * @param root the root type in the from clause, used to construct type-safe queries.
     * @param query the criteria query instance to which the predicate will be applied.
     * @param cb the criteria builder used to create the predicate.
     * @return the constructed `Predicate` representing the condition that checks if the subquery exists.
     * @since 3.1.0
     */
    override fun toPredicate(root: Root<T>, query: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate {
        val subquery = query.subquery(subqueryType)
        val subRoot = subquery.from(subqueryType)
        val scope = SubqueryScope<T, S>(root, subRoot, subquery, cb)
        scope.subqueryBlock()
        subquery.select(subRoot)
        return cb.exists(subquery)
    }
}

/**
 * A utility class for working within the scope of a JPA subquery. It provides methods for
 * correlating entities, applying predicates, and building dynamic query conditions for the subquery.
 *
 * @param T the type of the parent entity being queried.
 * @param S the type of the entity being used in the subquery.
 * @property parentRoot the root instance representing the parent query.
 * @property subRoot the root instance representing the entity in the subquery.
 * @property subquery the JPA subquery instance.
 * @property cb the criteria builder used to construct query expressions and predicates.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
class SubqueryScope<T, S>(
    val parentRoot: Root<T>,
    val subRoot: Root<S>,
    val subquery: Subquery<S>,
    val cb: CriteriaBuilder,
) {
    /**
     * Adds a correlation condition between a path in the parent query and a path in the subquery.
     * This method applies an equality predicate that ensures the specified `parentPath` and `subPath`
     * are matched in the query.
     *
     * @param parentPath The path in the parent query to be correlated.
     * @param subPath The path in the subquery to be correlated with the parent path.
     * @since 3.1.0
     */
    fun correlate(parentPath: String, subPath: String) {
        subquery.where(cb.equal(subRoot.resolvePath<Any>(subPath), parentRoot.resolvePath<Any>(parentPath)))
    }

    /**
     * Adds the given predicates to the subquery's where clause. If the subquery
     * already has an existing restriction, the new predicates will be combined
     * with the existing restriction.
     *
     * @param predicates One or more predicates to be applied in the where clause of the subquery.
     * @since 3.1.0
     */
    fun where(vararg predicates: Predicate) {
        val existing = subquery.restriction
        val all = if (existing != null) arrayOf(existing, *predicates) else predicates
        subquery.where(*all)
    }

    /**
     * Adds an equality condition to the subquery criteria.
     * The condition compares the value at the specified attribute path in the subquery's root entity with the provided value.
     *
     * @param path The attribute path within the subquery entity to be compared.
     * @param value The value to be compared with the attribute at the specified path.
     * @since 3.1.0
     */
    fun eq(path: String, value: Any) {
        where(cb.equal(subRoot.resolvePath<Any>(path), value))
    }
}

// ── Composite predicates ──

/**
 * Represents a logical AND node in a predicate tree structure.
 * This class combines multiple child `PredicateNode` instances
 * into a single `Predicate` using a logical AND operation.
 *
 * @param T the type of the entity being queried.
 * @param children a list of child `PredicateNode` instances to be combined.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
class AndNode<T>(val children: List<PredicateNode<T>>) : PredicateNode<T> {
    /**
     * Combines the predicates generated by child nodes using a logical AND operation.
     * This method aggregates the conditions from all child nodes into a single predicate.
     *
     * @param root the root type in the from clause, used to construct type-safe queries.
     * @param query the criteria query instance to which the predicate will be applied.
     * @param cb the criteria builder used to create the resulting predicate.
     * @return the combined `Predicate` representing the logical AND of all child node predicates.
     * @since 3.1.0
     */
    override fun toPredicate(root: Root<T>, query: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate =
        cb.and(*children.map { it.toPredicate(root, query, cb) }.toTypedArray())
}

/**
 * Represents a logical OR node in a predicate tree structure for constructing JPA criteria queries.
 * This node combines multiple child predicates using the logical OR operator.
 *
 * @param T the type of the entity being queried.
 * @param children the list of child `PredicateNode` instances to combine with a logical OR.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
class OrNode<T>(val children: List<PredicateNode<T>>) : PredicateNode<T> {
    /**
     * Constructs a disjunction (logical OR) `Predicate` by combining the predicates generated by the child nodes.
     * Each child node's `toPredicate` method is invoked to create its respective predicate, which are then combined.
     *
     * @param root the root type in the from clause, used to construct type-safe queries.
     * @param query the criteria query instance to which the predicate will be applied.
     * @param cb the criteria builder used to create the predicate.
     * @return a `Predicate` representing the logical OR of the predicates generated by the child nodes.
     * @since 3.1.0
     */
    override fun toPredicate(root: Root<T>, query: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate =
        cb.or(*children.map { it.toPredicate(root, query, cb) }.toTypedArray())
}

/**
 * Represents a negation node in a predicate tree structure for JPA Criteria queries.
 * This class is used to wrap another predicate and negate its result.
 *
 * @param T the type of the entity being queried.
 * @param child the predicate node whose result will be negated.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
class NotNode<T>(val child: PredicateNode<T>) : PredicateNode<T> {
    /**
     * Constructs a JPA `Predicate` that negates the condition represented by the child node's predicate.
     *
     * @param root the root type in the from clause, used to construct type-safe queries.
     * @param query the criteria query instance to which the predicate will be applied.
     * @param cb the criteria builder used to create the predicate.
     * @return the constructed `Predicate` representing the negated condition for this node.
     * @since 3.1.0
     */
    override fun toPredicate(root: Root<T>, query: CriteriaQuery<*>, cb: CriteriaBuilder): Predicate =
        cb.not(child.toPredicate(root, query, cb))
}

// --- PATH RESOLUTION (SUPPORTS NESTED: "customer.address.city") ---

/**
 * Resolves a dot-separated string path into a JPA `Path` object, starting from the current `Root`.
 * The path is traversed step by step, resolving each segment as an entity attribute.
 *
 * @param path the dot-separated string representing the attribute path to resolve relative to the root.
 * @return the resolved `Path` corresponding to the specified attribute path.
 * @since 3.1.0
 */
@Suppress("UNCHECKED_CAST")
fun <Y> Root<*>.resolvePath(path: String): Path<Y> {
    val parts = path.split(".")
    var current: Path<*> = this
    for (part in parts) {
        current = current.get<Any>(part)
    }
    return current as Path<Y>
}

// --- WHERE SCOPE (COLLECTS PREDICATE NODES) ---

/**
 * A scope for constructing `WHERE` clauses when building criteria queries.
 * Provides a fluent DSL for defining predicates and combining them using logical operations.
 *
 * @param T the type of the entity being queried.
 * @since 3.1.0
 * @author Tommaso Pastorelli
 */
@SpecDslMarker
class WhereScope<T> {
    /**
     * A mutable list that holds instances of `PredicateNode` with a generic type parameter `T`.
     * This list is initialized as empty and can be used to manage or store multiple `PredicateNode` objects.
     * @since 3.1.0
     */
    val nodes = emptyMList<PredicateNode<T>>()

    /**
     * Adds an equality condition to the current query context, comparing the receiver string
     * (representing a path) to a specified value.
     *
     * @param value the value to compare against the receiver string's corresponding attribute.
     * @since 3.1.0
     */
    infix fun String.eq(value: Any?) { nodes += EqNode(this, value) }
    /**
     * Adds a "not equal" condition to the query using the specified attribute path and value.
     * This function appends a `NeqNode` to the `nodes` collection, representing a condition
     * that the value of the given attribute path should not be equal to the provided value.
     *
     * @param value the value to compare against the attribute at the specified path.
     * @since 3.1.0
     */
    infix fun String.neq(value: Any?) { nodes += NeqNode(this, value) }
    /**
     * Adds a "greater than" condition to the query for the specified attribute.
     *
     * This method allows the construction of a dynamic query condition where the specified
     * attribute is filtered to return values greater than the given value.
     *
     * @param value the value that the attribute must be greater than
     * @since 3.1.0
     */
    infix fun String.gt(value: Comparable<*>) { nodes += GtNode(this, value) }
    /**
     * Adds a "greater than or equal to" (>=) predicate to the query criteria.
     *
     * This method is used to specify a condition where the value of the given attribute, represented as a string path,
     * must be greater than or equal to the provided comparable value.
     *
     * @param value the value to be compared against the attribute at the specified path.
     * @since 3.1.0
     */
    infix fun String.gte(value: Comparable<*>) { nodes += GteNode(this, value) }
    /**
     * Adds a "less than" (`<`) condition to the predicate structure for the specified attribute path
     * and value. This condition evaluates whether the value of the given attribute is less than the
     * provided comparable value.
     *
     * @param value the value to compare against the attribute. Must implement the `Comparable` interface.
     * @since 3.1.0
     */
    infix fun String.lt(value: Comparable<*>) { nodes += LtNode(this, value) }
    /**
     * Adds a "less than or equal to" condition to the current scope for the specified attribute.
     * This function is used to compare the value of an entity attribute with a given comparable value.
     *
     * @param value the comparable value to compare against the attribute specified by the string receiver.
     * @since 3.1.0
     */
    infix fun String.lte(value: Comparable<*>) { nodes += LteNode(this, value) }
    /**
     * Adds a "like" condition to the current query scope, checking if the string matches
     * a specified pattern. The comparison is case-sensitive.
     *
     * @param pattern the string pattern to match against the current string.
     * @since 3.1.0
     */
    infix fun String.like(pattern: String) { nodes += LikeNode(this, pattern, caseSensitive = true) }
    /**
     * Adds a case-insensitive "like" condition to the query for the calling string.
     * This condition checks if the string value matches the specified pattern, ignoring case.
     *
     * @param pattern the pattern to be checked against, using wildcard characters (%) for matching.
     * @since 3.1.0
     */
    infix fun String.ilike(pattern: String) { nodes += LikeNode(this, pattern, caseSensitive = false) }
    /**
     * Adds an "IN" predicate to the query, asserting that the string property is contained
     * within the specified collection of values.
     *
     * @param values the collection of values to check against. The predicate will evaluate to true
     *               if the current string property is present in this collection.
     * @since 3.1.0
     */
    infix fun String.isIn(values: Collection<*>) { nodes += InNode(this, values) }
    /**
     * Adds a "NOT IN" predicate to the query, ensuring that the value of the current string path
     * is not among the specified collection of values.
     *
     * @param values the collection of values against which the string path will be excluded.
     * @since 3.1.0
     */
    infix fun String.notIn(values: Collection<*>) { nodes += NotInNode(this, values) }

    /**
     * Adds a "between" condition to the query, checking if the value of the specified attribute
     * falls within the inclusive range defined by the lower and upper boundaries.
     *
     * @param range the range defining the lower and upper boundaries of the condition.
     * @since 3.1.0
     */
    infix fun String.between(range: ClosedRange<*>) {
        nodes += BetweenNode(this, range.start, range.endInclusive)
    }

    /**
     * Checks whether the given string is null or has a null-like condition.
     * Adds an `IsNullNode` to the `nodes` collection with the context of the current string.
     * @since 3.1.0
     */
    fun String.isNull() { nodes += IsNullNode(this) }
    /**
     * Extension function for the String class that appends an `IsNotNullNode`
     * to the `nodes` collection, using the current string instance as its parameter.
     *
     * This function is typically used to express that a given string value
     * is asserted or validated to be non-null in the context of constructing
     * or modifying a `nodes` collection.
     * @since 3.1.0
     */
    fun String.isNotNull() { nodes += IsNotNullNode(this) }
    /**
     * Appends an `IsTrueNode` to the query-building structure, representing a condition
     * that checks if the value at the specified path evaluates to `true`.
     *
     * This method is intended to be used in dynamic query construction where boolean
     * attributes need to be validated for a `true` value. The path to the attribute
     * being checked is defined by the string receiver of the function.
     *
     * The resulting condition will be incorporated into the aggregated criteria
     * using the `nodes` collection in the enclosing class.
     * @since 3.1.0
     */
    fun String.isTrue() { nodes += IsTrueNode(this) }
    /**
     * Adds a condition that evaluates whether the current string attribute is false.
     * This function creates an `IsFalseNode` and appends it to the predicate tree structure.
     *
     * The `isFalse` function is part of a DSL for building complex query predicates dynamically.
     * It serves as a declarative way to state that a particular attribute should evaluate to false
     * in the constructed query.
     * @since 3.1.0
     */
    fun String.isFalse() { nodes += IsFalseNode(this) }

    /**
     * Adds a logical OR condition to the current scope by applying the provided block.
     *
     * @param block A lambda function defining the conditions to be grouped under the OR operation.
     * @since 3.1.0
     */
    fun or(block: ReceiverConsumer<WhereScope<T>>) {
        val scope = WhereScope<T>().apply(block)
        nodes += OrNode(scope.nodes)
    }

    /**
     * Adds a logical "AND" condition to the query using the specified block.
     *
     * The block defines a scope in which conditions can be added to form
     * part of the overall "AND" logical condition. These conditions are then
     * grouped together and appended to the query.
     *
     * @param block A lambda with receiver that allows specifying the conditions
     *              to include in the "AND" group.
     * @since 3.1.0
     */
    fun and(block: ReceiverConsumer<WhereScope<T>>) {
        val scope = WhereScope<T>().apply(block)
        nodes += AndNode(scope.nodes)
    }

    /**
     * Applies a negation to the conditions defined within the provided block.
     *
     * @param block A lambda with a receiver of type `WhereScope<T>` defining the conditions to negate.
     * @since 3.1.0
     */
    fun not(block: ReceiverConsumer<WhereScope<T>>) {
        val scope = WhereScope<T>().apply(block)
        scope.nodes.forEach { nodes += NotNode(it) }
    }

    /**
     * Adds an "EXISTS" subquery condition to the current query. This method is used to check
     * the existence of related data by dynamically building a subquery. The subquery is
     * defined using the provided block of operations.
     *
     * @param S The type of the entity being used in the subquery.
     * @param correlationPath The path used to correlate the parent query with the subquery entity.
     *                         Defaults to "id".
     * @param block A lambda with a receiver for defining the subquery logic. The `SubqueryScope`
     *              provides methods for setting up conditions and relationships within the subquery.
     * @since 3.1.0
     */
// Subquery exists
    inline fun <reified S> exists(
        correlationPath: String = "id",
        noinline block: ReceiverConsumer<SubqueryScope<T, S>>,
    ) {
        nodes += ExistsSubqueryNode(S::class.java, correlationPath, block)
    }
}

// --- SPECIFICATION BUILDER ---

/**
 * A DSL-based builder for constructing JPA `Specification` objects. This class provides
 * methods to define filter conditions, enable distinct queries, and specify fetch strategies.
 *
 * @param T The entity type for which the specification is built.
 * @since 3.1.0
 */
@SpecDslMarker
class SpecificationBuilder<T : Any> {
    /**
     * Holds the current configuration for the `where` clause within the specification.
     * This property stores an instance of the `WhereScope` class, which defines the
     * conditions to apply for filtering data in the corresponding query. When set,
     * it enables scoped query logic to be constructed dynamically based on the provided
     * lambda block.
     *
     * If null, no conditions are applied to the query.
     * @since 3.1.0
     */
    private var whereScope: WhereScope<T>? = null
    /**
     * Holds a mutable list of fetch paths and their associated join types for use in a query specification.
     *
     * Each item in the list is a pair consisting of:
     * - A string representing the attribute path to be fetched.
     * - A [JoinType] defining the type of join (e.g., LEFT, INNER) to be used for fetching.
     *
     * Fetch paths are typically populated through the `fetch` function and are applied during query construction
     * in the `build` method, where they are added to the root entity of the query.
     *
     * This list is only utilized for non-count queries; it remains unused when querying for a count result.
     * @since 3.1.0
     */
    private val fetchPaths = emptyMList<Pair<String, JoinType>>()
    /**
     * Indicates whether the query should enforce distinct results or not.
     *
     * When set to `true`, the generated query will include the `DISTINCT` modifier,
     * ensuring that only unique results are returned. It is primarily used to avoid
     * duplicate entries in the query result when performing joins or fetching related entities.
     * @since 3.1.0
     */
    private var distinct: Boolean = false

    /**
     * Defines the conditions for the specification by providing a block in which
     * a `WhereScope` can be configured.
     *
     * @param block A lambda with receiver to configure the `WhereScope` for building
     * logical conditions in the specification.
     * @since 3.1.0
     */
    fun where(block: ReceiverConsumer<WhereScope<T>>) {
        whereScope = WhereScope<T>().apply(block)
    }

    /**
     * Adds a fetch operation for the specified attribute with the provided join type.
     * This fetch operation will be applied to the criteria query during the build process.
     *
     * @param attribute The name of the attribute to fetch.
     * @param joinType The type of join operation to apply while fetching the attribute. Defaults to [JoinType.LEFT].
     * @since 3.1.0
     */
    fun fetch(attribute: String, joinType: JoinType = JoinType.LEFT) {
        fetchPaths += attribute to joinType
    }

    /**
     * Marks the query as distinct, ensuring that duplicate results are excluded
     * when executing the query. This sets the `distinct` flag to `true` within
     * the query specification builder.
     * @since 3.1.0
     */
    fun distinct() { distinct = true }

    /**
     * Builds and returns a JPA `Specification` based on the configured parameters such as
     * fetch paths, distinct query flag, and where scope criteria.
     *
     * The `Specification` defines a query predicate for JPA Criteria queries and can be
     * used to dynamically construct queries in a type-safe manner.
     *
     * Fetch paths are applied only for non-count queries, allowing associations to be fetched
     * eagerly with the specified join type. If the `distinct` flag is enabled, the query will
     * ensure distinct results.
     *
     * The where scope constructs the query predicates dynamically, supporting multiple conditions
     * combined either as a single predicate or as a conjunction of predicates.
     *
     * @return a `Specification` instance representing the query conditions.
     * @since 3.1.0
     */
    fun build(): Specification<T> = Specification { root, query, cb ->
        // Apply fetches only for non-count queries
        if (query.resultType != Long::class.java && query.resultType != Long::class.javaObjectType) {
            fetchPaths.forEach { [path, joinType] ->
                root.fetch<Any, Any>(path, joinType)
            }
        }

        if (distinct) query.distinct(true)

        whereScope?.let { scope ->
            if (scope.nodes.isEmpty()) null
            else if (scope.nodes.size == 1) scope.nodes.first().toPredicate(root, query, cb)
            else cb.and(*scope.nodes.map { it.toPredicate(root, query, cb) }.toTypedArray())
        }
    }
}

// --- TOP-LEVEL ENTRY POINT ---

/**
 * Creates a `Specification` for the generic type [T] based on the provided configurations defined in the [block].
 * The `SpecificationBuilder` DSL allows defining conditions, fetch paths, and distinct constraints
 * which are used to build and return the resulting `Specification`.
 *
 * @param T the entity type for which the specification is created.
 * @param block the lambda with `SpecificationBuilder` as a receiver, used to configure
 * the conditions, fetch paths, and distinct settings for the specification.
 * @return an instance of `Specification` representing the defined filter and query settings.
 * @since 3.1.0
 */
@Beta
inline fun <reified T : Any> buildSpecification(block: ReceiverConsumer<SpecificationBuilder<T>>): Specification<T> =
    SpecificationBuilder<T>().apply(block).build()

/**
 * Initializes a new instance of [SpecificationBuilder] for the specified entity type [T]
 * and applies the given configuration block to it.
 *
 * @param T The entity type for which the specification is being built.
 * @param block A lambda with receiver used to configure the [SpecificationBuilder].
 * This provides a DSL for defining query specifications, including conditions, fetch strategies,
 * and distinct options.
 * @return A configured instance of [SpecificationBuilder] ready to build JPA specifications.
 * @since 3.4.3
 */
@Beta
inline fun <reified T : Any> initSpecification(block: SpecificationBuilder<T>.() -> Unit): SpecificationBuilder<T> =
    SpecificationBuilder<T>().apply(block)

// --- OPERATOR OVERLOADS FOR COMBINING SPECS ---

/**
 * Combines the current specification with another specification using a logical AND operation.
 *
 * @param other The specification to be combined with the current specification.
 * @return A new specification representing the logical AND of the two specifications.
 * @since 3.1.0
 */
operator fun <T : Any> Specification<T>.plus(other: Specification<T>): Specification<T> =
    this.and(other)

/**
 * Combines the current specification with another specification using a logical OR operation.
 *
 * @param other The specification to be combined with the current specification.
 * @return A new specification representing the logical OR of the current specification and the provided specification.
 * @since 3.1.0
 */
infix fun <T : Any> Specification<T>.or(other: Specification<T>): Specification<T> =
    Specification.where(this).or(other)

/**
 * Combines the current specification with another specification using a logical "AND" operation.
 *
 * @param other The other specification to combine with the current specification.
 * @return A new specification representing the logical "AND" of the current and other specifications.
 * @since 3.1.0
 */
infix fun <T : Any> Specification<T>.and(other: Specification<T>): Specification<T> =
    Specification.where(this).and(other)

/**
 * Creates a new specification that negates the current specification.
 *
 * Combines the negation logic with the existing specification to produce
 * a query that matches elements not matching the original specification.
 *
 * @return A new specification that represents the negation of the current specification.
 * @since 3.1.0
 */
operator fun <T : Any> Specification<T>.not(): Specification<T> = Specification.not(this)

// --- OPTIONAL / CONDITIONAL COMPOSITION ---

/**
 * Combines multiple specifications into a single specification using logical AND.
 *
 * @param specs Vararg parameter representing individual specifications to be combined.
 *              Null specifications will be ignored.
 * @return A new specification that represents the logical AND of all provided non-null specifications.
 * @throws IllegalArgumentException If no non-null specifications are provided.
 * @since 3.1.0
 */
@Beta
inline fun <reified T : Any> specificationOf(vararg specs: Specification<T>?): Specification<T> =
    specs.filterNotNull().reduceOrNull { acc, spec -> acc and spec }
        ?: throw IllegalArgumentException("At least one specification must be provided")

/**
 * Conditionally add a spec only if value is non-null.
 * Usage:
 * ```
 * specificationOf(
 *     statusFilter?.let { specifications<Order> { where { "status" eq it } } },
 *     minTotal?.let  { specifications<Order> { where { "total" gte it } } },
 * )
 * ```
 * @since 3.1.0
 */
@Beta
inline fun <reified T : Any, V> ifPresent(
    value: V?,
    crossinline block: ReceiverBiConsumer<SpecificationBuilder<T>, V>,
): Specification<T>? = value?.let {
    SpecificationBuilder<T>().apply { block(it) }.build()
}