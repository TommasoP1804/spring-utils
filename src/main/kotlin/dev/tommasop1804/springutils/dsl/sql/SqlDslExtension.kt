/*
 * Copyright © 2026 Tommaso Pastorelli (TommasoP1804) | Spring-Utils
 */

@file:Suppress("kutils_tuple_declaration", "unused", "SqlNoDataSourceInspection", "DSL_MARKER_APPLIED_TO_WRONG_TARGET")

package dev.tommasop1804.springutils.dsl.sql

import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.classes.constants.*
import dev.tommasop1804.kutils.dsl.sql.*
import jakarta.persistence.Column
import jakarta.persistence.Table
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation

/**
 * Resolves a [KProperty1] to its SQL column name using `@Column` annotation or camelCase → snake_case.
 *
 * @since 3.4.0
 */
@PublishedApi
internal fun KProperty1<*, *>.toColumnName(): String =
    findAnnotationAnywhere<Column>()?.name?.ifEmpty { null } ?: -name

/**
 * Resolves a [KProperty1] to its SQL column name qualified with an optional table alias.
 *
 * @since 3.4.0
 */
@PublishedApi
internal fun KProperty1<*, *>.toQualifiedColumnName(alias: String?): String =
    "${alias?.let { "$it." } ?: String.EMPTY}${toColumnName()}"

/**
 * SELECT with `KProperty1` columns and optional table alias per column.
 *
 * ```kotlin
 * sql {
 *     select("u" to User::name, "o" to Order::total)
 *     from<User>("u")
 * }
 * ```
 *
 * @since 3.4.0
 */
@SqlDslMarker
fun SqlBuilder.select(vararg columns: Pair<String?, KProperty1<*, *>>, distinct: Boolean = false) =
    select(
        *columns.map { [alias, prop] -> prop.toQualifiedColumnName(alias) }.toTypedArray(),
        distinct = distinct
    )
/**
 * SELECT with `KProperty1` columns, no alias.
 *
 * @since 3.4.0
 */
@SqlDslMarker
fun SqlBuilder.select(vararg columns: KProperty1<*, *>, distinct: Boolean = false) =
    select(
        *columns.map { it.toColumnName() }.toTypedArray(),
        distinct = distinct
    )
/**
 * SELECT with `KProperty1` columns, all under the same table alias.
 *
 * @since 3.4.0
 */
@SqlDslMarker
fun SqlBuilder.select(tableAlias: String?, vararg columns: KProperty1<*, *>, distinct: Boolean = false) {
    val alias = tableAlias ?: columns.firstOrNull()?.ownerClass?.simpleName?.first()?.lowercase().orEmpty()
    select(*columns.map { it.toQualifiedColumnName(alias) }.toTypedArray(), distinct = distinct)
}

/**
 * FROM with a reified entity class.
 *
 * ```kotlin
 * sql {
 *     select()
 *     from<User>("u")
 * }
 * ```
 *
 * @since 3.4.0
 */
@SqlDslMarker
inline fun <reified T : Any> SqlBuilder.from(alias: String? = null) {
    val clazz = T::class
    from(convertName(clazz) + (alias?.let { " $it" } ?: String.EMPTY))
}
/**
 * FROM with multiple entity classes and aliases.
 *
 * ```kotlin
 * sql {
 *     select()
 *     from(User::class to "u", Order::class to "o")
 * }
 * ```
 *
 * @since 3.4.0
 */
@SqlDslMarker
fun SqlBuilder.from(vararg tables: Pair<KClass<*>, String>) {
    validate(tables.map(Pair<*, *>::second).distinct().size == tables.size) { "All aliases must be unique" }
    from(*tables.map { [clazz, alias] -> "${convertName(clazz)} $alias" }.toTypedArray())
}

/**
 * JOIN with a reified entity class and [JoinType].
 *
 * @since 3.4.0
 */
@SqlDslMarker
inline fun <reified T : Any> SqlBuilder.join(alias: String? = null, type: JoinType, on: String? = null) {
    val clazz = T::class
    join(convertName(clazz) + (alias?.let { " $it" } ?: String.EMPTY), type, on)
}
/**
 * Extension for [JoinScope] to join a reified entity class.
 *
 * ```kotlin
 * sql {
 *     select()
 *     from<User>("u")
 *     joins {
 *         inner<Order>("o", "o.user_id = u.id")
 *         left<Payment>("p", "p.order_id = o.id")
 *     }
 * }
 * ```
 *
 * @since 3.4.0
 */
@SqlDslMarker
inline fun <reified T : Any> JoinScope.inner(alias: String? = null, on: String) {
    join(convertName(T::class) + (alias?.let { " $it" } ?: String.EMPTY), JoinType.Inner, on)
}
/**
 * LEFT JOIN on reified entity.
 *
 * @since 3.4.0
 */
@SqlDslMarker
inline fun <reified T : Any> JoinScope.left(alias: String? = null, on: String) {
    join(convertName(T::class) + (alias?.let { " $it" } ?: String.EMPTY), JoinType.LeftOuter, on)
}
/**
 * RIGHT JOIN on reified entity.
 *
 * @since 3.4.0
 */
@SqlDslMarker
inline fun <reified T : Any> JoinScope.right(alias: String? = null, on: String) {
    join(convertName(T::class) + (alias?.let { " $it" } ?: String.EMPTY), JoinType.RightOuter, on)
}
/**
 * FULL OUTER JOIN on reified entity.
 *
 * @since 3.4.0
 */
@SqlDslMarker
inline fun <reified T : Any> JoinScope.full(alias: String? = null, on: String) {
    join(convertName(T::class) + (alias?.let { " $it" } ?: String.EMPTY), JoinType.FullOuter, on)
}
/**
 * CROSS JOIN on reified entity.
 *
 * @since 3.4.0
 */
@SqlDslMarker
inline fun <reified T : Any> JoinScope.cross(alias: String? = null) {
    join(convertName(T::class) + (alias?.let { " $it" } ?: String.EMPTY), JoinType.Cross)
}
/**
 * NATURAL JOIN on reified entity.
 *
 * @since 3.4.0
 */
@SqlDslMarker
inline fun <reified T : Any> JoinScope.natural(alias: String? = null) {
    join(convertName(T::class) + (alias?.let { " $it" } ?: String.EMPTY), JoinType.Natural)
}

/**
 * Typed `=` condition.
 *
 * ```kotlin
 * where {
 *     condition(User::status eq "'ACTIVE'")
 * }
 * ```
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun KProperty1<*, *>.eq(value: Any) =
    "${toColumnName()} = $value"
/**
 * Typed `=` condition.
 *
 * ```kotlin
 * where {
 *     condition(User::status eq "'ACTIVE'")
 * }
 * ```
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun KProperty1<*, *>.eq(value: Any) =
    "${toColumnName()} = $value"
/**
 * Typed `=` condition.
 *
 * ```kotlin
 * where {
 *     condition(User::status eq "'ACTIVE'")
 * }
 * ```
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun KProperty1<*, *>.eq(value: Any) =
    "${toColumnName()} = $value"
/**
 * Typed `=` condition.
 *
 * ```kotlin
 * where {
 *     condition(User::status eq "'ACTIVE'")
 * }
 * ```
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun String.eq(value: Any) =
    "$this = $value"
/**
 * Typed `=` condition.
 *
 * ```kotlin
 * where {
 *     condition(User::status eq "'ACTIVE'")
 * }
 * ```
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun String.eq(value: Any) =
    "$this = $value"
/**
 * Typed `=` condition.
 *
 * ```kotlin
 * where {
 *     condition(User::status eq "'ACTIVE'")
 * }
 * ```
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun String.eq(value: Any) =
    "$this = $value"
/**
 * Typed `=` condition.
 *
 * ```kotlin
 * where {
 *     condition(User::status eq "'ACTIVE'")
 * }
 * ```
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun KProperty1<*, *>.eq(property: KProperty1<*, *>) =
    "${toColumnName()} = ${property.toColumnName()}"
/**
 * Typed `=` condition.
 *
 * ```kotlin
 * where {
 *     condition(User::status eq "'ACTIVE'")
 * }
 * ```
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun KProperty1<*, *>.eq(property: KProperty1<*, *>) =
    "${toColumnName()} = ${property.toColumnName()}"
/**
 * Typed `=` condition.
 *
 * ```kotlin
 * where {
 *     condition(User::status eq "'ACTIVE'")
 * }
 * ```
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun KProperty1<*, *>.eq(property: KProperty1<*, *>) =
    "${toColumnName()} = ${property.toColumnName()}"
/**
 * Typed `=` condition.
 *
 * ```kotlin
 * where {
 *     condition(User::status eq "'ACTIVE'")
 * }
 * ```
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun String.eq(property: KProperty1<*, *>) =
    "$this = ${property.toColumnName()}"
/**
 * Typed `=` condition.
 *
 * ```kotlin
 * where {
 *     condition(User::status eq "'ACTIVE'")
 * }
 * ```
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun String.eq(property: KProperty1<*, *>) =
    "$this = ${property.toColumnName()}"
/**
 * Typed `=` condition.
 *
 * ```kotlin
 * where {
 *     condition(User::status eq "'ACTIVE'")
 * }
 * ```
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun String.eq(property: KProperty1<*, *>) =
    "$this = ${property.toColumnName()}"
/**
 * Typed `=` condition.
 *
 * ```kotlin
 * where {
 *     condition(User::status eq "'ACTIVE'")
 * }
 * ```
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun KProperty1<*, *>.eq(property: Pair<String, KProperty1<*, *>>) =
    "${toColumnName()} = ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `=` condition.
 *
 * ```kotlin
 * where {
 *     condition(User::status eq "'ACTIVE'")
 * }
 * ```
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun KProperty1<*, *>.eq(property: Pair<String, KProperty1<*, *>>) =
    "${toColumnName()} = ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `=` condition.
 *
 * ```kotlin
 * where {
 *     condition(User::status eq "'ACTIVE'")
 * }
 * ```
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun KProperty1<*, *>.eq(property: Pair<String, KProperty1<*, *>>) =
    "${toColumnName()} = ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `=` condition.
 *
 * ```kotlin
 * where {
 *     condition(User::status eq "'ACTIVE'")
 * }
 * ```
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun String.eq(property: Pair<String, KProperty1<*, *>>) =
    "$this = ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `=` condition.
 *
 * ```kotlin
 * where {
 *     condition(User::status eq "'ACTIVE'")
 * }
 * ```
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun String.eq(property: Pair<String, KProperty1<*, *>>) =
    "$this = ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `=` condition.
 *
 * ```kotlin
 * where {
 *     condition(User::status eq "'ACTIVE'")
 * }
 * ```
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun String.eq(property: Pair<String, KProperty1<*, *>>) =
    "$this = ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `=` condition.
 *
 * ```kotlin
 * where {
 *     condition(User::status eq "'ACTIVE'")
 * }
 * ```
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun Pair<String, KProperty1<*, *>>.eq(value: Any) =
    "$first.${second.toColumnName()} = $value"
/**
 * Typed `=` condition.
 *
 * ```kotlin
 * where {
 *     condition(User::status eq "'ACTIVE'")
 * }
 * ```
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun Pair<String, KProperty1<*, *>>.eq(value: Any) =
    "$first.${second.toColumnName()} = $value"
/**
 * Typed `=` condition.
 *
 * ```kotlin
 * where {
 *     condition(User::status eq "'ACTIVE'")
 * }
 * ```
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun Pair<String, KProperty1<*, *>>.eq(value: Any) =
    "$first.${second.toColumnName()} = $value"
/**
 * Typed `=` condition.
 *
 * ```kotlin
 * where {
 *     condition(User::status eq "'ACTIVE'")
 * }
 * ```
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun Pair<String, KProperty1<*, *>>.eq(property: KProperty1<*, *>) =
    "$first.${second.toColumnName()} = ${property.toColumnName()}"
/**
 * Typed `=` condition.
 *
 * ```kotlin
 * where {
 *     condition(User::status eq "'ACTIVE'")
 * }
 * ```
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun Pair<String, KProperty1<*, *>>.eq(property: KProperty1<*, *>) =
    "$first.${second.toColumnName()} = ${property.toColumnName()}"
/**
 * Typed `=` condition.
 *
 * ```kotlin
 * where {
 *     condition(User::status eq "'ACTIVE'")
 * }
 * ```
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun Pair<String, KProperty1<*, *>>.eq(property: KProperty1<*, *>) =
    "$first.${second.toColumnName()} = ${property.toColumnName()}"
/**
 * Typed `=` condition.
 *
 * ```kotlin
 * where {
 *     condition(User::status eq "'ACTIVE'")
 * }
 * ```
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun Pair<String, KProperty1<*, *>>.eq(property: Pair<String, KProperty1<*, *>>) =
    "$first.${second.toColumnName()} = ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `=` condition.
 *
 * ```kotlin
 * where {
 *     condition(User::status eq "'ACTIVE'")
 * }
 * ```
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun Pair<String, KProperty1<*, *>>.eq(property: Pair<String, KProperty1<*, *>>) =
    "$first.${second.toColumnName()} = ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `=` condition.
 *
 * ```kotlin
 * where {
 *     condition(User::status eq "'ACTIVE'")
 * }
 * ```
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun Pair<String, KProperty1<*, *>>.eq(property: Pair<String, KProperty1<*, *>>) =
    "$first.${second.toColumnName()} = ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun KProperty1<*, *>.neq(value: Any) =
    "${toColumnName()} != $value"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun KProperty1<*, *>.neq(value: Any) =
    "${toColumnName()} != $value"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun KProperty1<*, *>.neq(value: Any) =
    "${toColumnName()} != $value"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun String.neq(value: Any) =
    "$this != $value"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun String.neq(value: Any) =
    "$this != $value"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun String.neq(value: Any) =
    "$this != $value"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun KProperty1<*, *>.neq(property: KProperty1<*, *>) =
    "${toColumnName()} != ${property.toColumnName()}"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun KProperty1<*, *>.neq(property: KProperty1<*, *>) =
    "${toColumnName()} != ${property.toColumnName()}"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun KProperty1<*, *>.neq(property: KProperty1<*, *>) =
    "${toColumnName()} != ${property.toColumnName()}"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun String.neq(property: KProperty1<*, *>) =
    "$this != ${property.toColumnName()}"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun String.neq(property: KProperty1<*, *>) =
    "$this != ${property.toColumnName()}"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun String.neq(property: KProperty1<*, *>) =
    "$this != ${property.toColumnName()}"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun KProperty1<*, *>.neq(property: Pair<String, KProperty1<*, *>>) =
    "${toColumnName()} != ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun KProperty1<*, *>.neq(property: Pair<String, KProperty1<*, *>>) =
    "${toColumnName()} != ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun KProperty1<*, *>.neq(property: Pair<String, KProperty1<*, *>>) =
    "${toColumnName()} != ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun String.neq(property: Pair<String, KProperty1<*, *>>) =
    "$this != ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun String.neq(property: Pair<String, KProperty1<*, *>>) =
    "$this != ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun String.neq(property: Pair<String, KProperty1<*, *>>) =
    "$this != ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun Pair<String, KProperty1<*, *>>.neq(value: Any) =
    "$first.${second.toColumnName()} != $value"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun Pair<String, KProperty1<*, *>>.neq(value: Any) =
    "$first.${second.toColumnName()} != $value"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun Pair<String, KProperty1<*, *>>.neq(value: Any) =
    "$first.${second.toColumnName()} != $value"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun Pair<String, KProperty1<*, *>>.neq(property: KProperty1<*, *>) =
    "$first.${second.toColumnName()} != ${property.toColumnName()}"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun Pair<String, KProperty1<*, *>>.neq(property: KProperty1<*, *>) =
    "$first.${second.toColumnName()} != ${property.toColumnName()}"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun Pair<String, KProperty1<*, *>>.neq(property: KProperty1<*, *>) =
    "$first.${second.toColumnName()} != ${property.toColumnName()}"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun Pair<String, KProperty1<*, *>>.neq(property: Pair<String, KProperty1<*, *>>) =
    "$first.${second.toColumnName()} != ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun Pair<String, KProperty1<*, *>>.neq(property: Pair<String, KProperty1<*, *>>) =
    "$first.${second.toColumnName()} != ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun Pair<String, KProperty1<*, *>>.neq(property: Pair<String, KProperty1<*, *>>) =
    "$first.${second.toColumnName()} != ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun KProperty1<*, Comparable<*>>.gt(value: Any) =
    "${toColumnName()} > $value"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun KProperty1<*, Comparable<*>>.gt(value: Any) =
    "${toColumnName()} > $value"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun KProperty1<*, Comparable<*>>.gt(value: Any) =
    "${toColumnName()} > $value"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun String.gt(value: Any) =
    "$this > $value"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun String.gt(value: Any) =
    "$this > $value"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun String.gt(value: Any) =
    "$this > $value"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun KProperty1<*, Comparable<*>>.gt(property: KProperty1<*, Comparable<*>>) =
    "${toColumnName()} > ${property.toColumnName()}"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun KProperty1<*, Comparable<*>>.gt(property: KProperty1<*, Comparable<*>>) =
    "${toColumnName()} > ${property.toColumnName()}"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun KProperty1<*, Comparable<*>>.gt(property: KProperty1<*, Comparable<*>>) =
    "${toColumnName()} > ${property.toColumnName()}"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun String.gt(property: KProperty1<*, Comparable<*>>) =
    "$this > ${property.toColumnName()}"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun String.gt(property: KProperty1<*, Comparable<*>>) =
    "$this > ${property.toColumnName()}"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun String.gt(property: KProperty1<*, Comparable<*>>) =
    "$this > ${property.toColumnName()}"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun KProperty1<*, Comparable<*>>.gt(property: Pair<String, KProperty1<*, Comparable<*>>>) =
    "${toColumnName()} > ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun KProperty1<*, Comparable<*>>.gt(property: Pair<String, KProperty1<*, Comparable<*>>>) =
    "${toColumnName()} > ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun KProperty1<*, Comparable<*>>.gt(property: Pair<String, KProperty1<*, Comparable<*>>>) =
    "${toColumnName()} > ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun String.gt(property: Pair<String, KProperty1<*, Comparable<*>>>) =
    "$this > ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun String.gt(property: Pair<String, KProperty1<*, Comparable<*>>>) =
    "$this > ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun String.gt(property: Pair<String, KProperty1<*, Comparable<*>>>) =
    "$this > ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.gt(value: Any) =
    "$first.${second.toColumnName()} > $value"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.gt(value: Any) =
    "$first.${second.toColumnName()} > $value"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.gt(value: Any) =
    "$first.${second.toColumnName()} > $value"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.gt(property: KProperty1<*, Comparable<*>>) =
    "$first.${second.toColumnName()} > ${property.toColumnName()}"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.gt(property: KProperty1<*, Comparable<*>>) =
    "$first.${second.toColumnName()} > ${property.toColumnName()}"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.gt(property: KProperty1<*, Comparable<*>>) =
    "$first.${second.toColumnName()} > ${property.toColumnName()}"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.gt(property: Pair<String, KProperty1<*, Comparable<*>>>) =
    "$first.${second.toColumnName()} > ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.gt(property: Pair<String, KProperty1<*, Comparable<*>>>) =
    "$first.${second.toColumnName()} > ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.gt(property: Pair<String, KProperty1<*, Comparable<*>>>) =
    "$first.${second.toColumnName()} > ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun KProperty1<*, Comparable<*>>.lt(value: Any) =
    "${toColumnName()} < $value"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun KProperty1<*, Comparable<*>>.lt(value: Any) =
    "${toColumnName()} < $value"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun KProperty1<*, Comparable<*>>.lt(value: Any) =
    "${toColumnName()} < $value"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun String.lt(value: Any) =
    "$this < $value"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun String.lt(value: Any) =
    "$this < $value"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun String.lt(value: Any) =
    "$this < $value"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun KProperty1<*, Comparable<*>>.lt(property: KProperty1<*, Comparable<*>>) =
    "${toColumnName()} < ${property.toColumnName()}"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun KProperty1<*, Comparable<*>>.lt(property: KProperty1<*, Comparable<*>>) =
    "${toColumnName()} < ${property.toColumnName()}"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun KProperty1<*, Comparable<*>>.lt(property: KProperty1<*, Comparable<*>>) =
    "${toColumnName()} < ${property.toColumnName()}"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun String.lt(property: KProperty1<*, Comparable<*>>) =
    "$this < ${property.toColumnName()}"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun String.lt(property: KProperty1<*, Comparable<*>>) =
    "$this < ${property.toColumnName()}"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun String.lt(property: KProperty1<*, Comparable<*>>) =
    "$this < ${property.toColumnName()}"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun KProperty1<*, Comparable<*>>.lt(property: Pair<String, KProperty1<*, Comparable<*>>>) =
    "${toColumnName()} < ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun KProperty1<*, Comparable<*>>.lt(property: Pair<String, KProperty1<*, Comparable<*>>>) =
    "${toColumnName()} < ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun KProperty1<*, Comparable<*>>.lt(property: Pair<String, KProperty1<*, Comparable<*>>>) =
    "${toColumnName()} < ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun String.lt(property: Pair<String, KProperty1<*, Comparable<*>>>) =
    "$this < ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun String.lt(property: Pair<String, KProperty1<*, Comparable<*>>>) =
    "$this < ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun String.lt(property: Pair<String, KProperty1<*, Comparable<*>>>) =
    "$this < ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.lt(value: Any) =
    "$first.${second.toColumnName()} < $value"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.lt(value: Any) =
    "$first.${second.toColumnName()} < $value"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.lt(value: Any) =
    "$first.${second.toColumnName()} < $value"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.lt(property: KProperty1<*, Comparable<*>>) =
    "$first.${second.toColumnName()} < ${property.toColumnName()}"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.lt(property: KProperty1<*, Comparable<*>>) =
    "$first.${second.toColumnName()} < ${property.toColumnName()}"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.lt(property: KProperty1<*, Comparable<*>>) =
    "$first.${second.toColumnName()} < ${property.toColumnName()}"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.lt(property: Pair<String, KProperty1<*, Comparable<*>>>) =
    "$first.${second.toColumnName()} < ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.lt(property: Pair<String, KProperty1<*, Comparable<*>>>) =
    "$first.${second.toColumnName()} < ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.lt(property: Pair<String, KProperty1<*, Comparable<*>>>) =
    "$first.${second.toColumnName()} < ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun KProperty1<*, Comparable<*>>.gte(value: Any) =
    "${toColumnName()} >= $value"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun KProperty1<*, Comparable<*>>.gte(value: Any) =
    "${toColumnName()} >= $value"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun KProperty1<*, Comparable<*>>.gte(value: Any) =
    "${toColumnName()} >= $value"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun String.gte(value: Any) =
    "$this >= $value"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun String.gte(value: Any) =
    "$this >= $value"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun String.gte(value: Any) =
    "$this >= $value"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun KProperty1<*, Comparable<*>>.gte(property: KProperty1<*, Comparable<*>>) =
    "${toColumnName()} >= ${property.toColumnName()}"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun KProperty1<*, Comparable<*>>.gte(property: KProperty1<*, Comparable<*>>) =
    "${toColumnName()} >= ${property.toColumnName()}"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun KProperty1<*, Comparable<*>>.gte(property: KProperty1<*, Comparable<*>>) =
    "${toColumnName()} >= ${property.toColumnName()}"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun String.gte(property: KProperty1<*, Comparable<*>>) =
    "$this >= ${property.toColumnName()}"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun String.gte(property: KProperty1<*, Comparable<*>>) =
    "$this >= ${property.toColumnName()}"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun String.gte(property: KProperty1<*, Comparable<*>>) =
    "$this >= ${property.toColumnName()}"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun KProperty1<*, Comparable<*>>.gte(property: Pair<String, KProperty1<*, *>>) =
    "${toColumnName()} >= ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun KProperty1<*, Comparable<*>>.gte(property: Pair<String, KProperty1<*, *>>) =
    "${toColumnName()} >= ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun KProperty1<*, Comparable<*>>.gte(property: Pair<String, KProperty1<*, *>>) =
    "${toColumnName()} >= ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun String.gte(property: Pair<String, KProperty1<*, *>>) =
    "$this >= ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun String.gte(property: Pair<String, KProperty1<*, *>>) =
    "$this >= ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun String.gte(property: Pair<String, KProperty1<*, *>>) =
    "$this >= ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.gte(value: Any) =
    "$first.${second.toColumnName()} >= $value"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.gte(value: Any) =
    "$first.${second.toColumnName()} >= $value"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.gte(value: Any) =
    "$first.${second.toColumnName()} >= $value"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.gte(property: KProperty1<*, Comparable<*>>) =
    "$first.${second.toColumnName()} >= ${property.toColumnName()}"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.gte(property: KProperty1<*, Comparable<*>>) =
    "$first.${second.toColumnName()} >= ${property.toColumnName()}"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.gte(property: KProperty1<*, Comparable<*>>) =
    "$first.${second.toColumnName()} >= ${property.toColumnName()}"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.gte(property: Pair<String, KProperty1<*, Comparable<*>>>) =
    "$first.${second.toColumnName()} >= ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.gte(property: Pair<String, KProperty1<*, Comparable<*>>>) =
    "$first.${second.toColumnName()} >= ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.gte(property: Pair<String, KProperty1<*, Comparable<*>>>) =
    "$first.${second.toColumnName()} >= ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun KProperty1<*, Comparable<*>>.lte(value: Any) =
    "${toColumnName()} <= $value"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun KProperty1<*, Comparable<*>>.lte(value: Any) =
    "${toColumnName()} <= $value"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun KProperty1<*, Comparable<*>>.lte(value: Any) =
    "${toColumnName()} <= $value"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun String.lte(value: Any) =
    "$this <= $value"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun String.lte(value: Any) =
    "$this <= $value"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun String.lte(value: Any) =
    "$this <= $value"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun KProperty1<*, Comparable<*>>.lte(property: KProperty1<*, Comparable<*>>) =
    "${toColumnName()} <= ${property.toColumnName()}"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun KProperty1<*, Comparable<*>>.lte(property: KProperty1<*, Comparable<*>>) =
    "${toColumnName()} <= ${property.toColumnName()}"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun KProperty1<*, Comparable<*>>.lte(property: KProperty1<*, Comparable<*>>) =
    "${toColumnName()} <= ${property.toColumnName()}"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun String.lte(property: KProperty1<*, Comparable<*>>) =
    "$this <= ${property.toColumnName()}"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun String.lte(property: KProperty1<*, Comparable<*>>) =
    "$this <= ${property.toColumnName()}"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun String.lte(property: KProperty1<*, Comparable<*>>) =
    "$this <= ${property.toColumnName()}"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun KProperty1<*, Comparable<*>>.lte(property: Pair<String, KProperty1<*, Comparable<*>>>) =
    "${toColumnName()} <= ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun KProperty1<*, Comparable<*>>.lte(property: Pair<String, KProperty1<*, Comparable<*>>>) =
    "${toColumnName()} <= ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun KProperty1<*, Comparable<*>>.lte(property: Pair<String, KProperty1<*, Comparable<*>>>) =
    "${toColumnName()} <= ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun String.lte(property: Pair<String, KProperty1<*, Comparable<*>>>) =
    "$this <= ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun String.lte(property: Pair<String, KProperty1<*, Comparable<*>>>) =
    "$this <= ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun String.lte(property: Pair<String, KProperty1<*, Comparable<*>>>) =
    "$this <= ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.lte(value: Any) =
    "$first.${second.toColumnName()} <= $value"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.lte(value: Any) =
    "$first.${second.toColumnName()} <= $value"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.lte(value: Any) =
    "$first.${second.toColumnName()} <= $value"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.lte(property: KProperty1<*, *>) =
    "$first.${second.toColumnName()} <= ${property.toColumnName()}"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.lte(property: KProperty1<*, *>) =
    "$first.${second.toColumnName()} <= ${property.toColumnName()}"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.lte(property: KProperty1<*, *>) =
    "$first.${second.toColumnName()} <= ${property.toColumnName()}"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.lte(property: Pair<String, KProperty1<*, *>>) =
    "$first.${second.toColumnName()} <= ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.lte(property: Pair<String, KProperty1<*, *>>) =
    "$first.${second.toColumnName()} <= ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun Pair<String, KProperty1<*, Comparable<*>>>.lte(property: Pair<String, KProperty1<*, *>>) =
    "$first.${second.toColumnName()} <= ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `like` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun KProperty1<*, CharSequence>.like(value: Any) =
    "${toColumnName()} LIKE '$value'"
/**
 * Typed `like` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun KProperty1<*, CharSequence>.like(value: Any) =
    "${toColumnName()} LIKE '$value'"
/**
 * Typed `like` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun KProperty1<*, CharSequence>.like(value: Any) =
    "${toColumnName()} LIKE '$value'"
/**
 * Typed `like` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun String.like(value: Any) =
    "$this LIKE '$value'"
/**
 * Typed `like` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun String.like(value: Any) =
    "$this LIKE '$value'"
/**
 * Typed `like` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun String.like(value: Any) =
    "$this LIKE '$value'"
/**
 * Typed `like` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun KProperty1<*, CharSequence>.like(property: KProperty1<*, CharSequence>) =
    "${toColumnName()} LIKE '${property.toColumnName()}'"
/**
 * Typed `like` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun KProperty1<*, CharSequence>.like(property: KProperty1<*, CharSequence>) =
    "${toColumnName()} LIKE '${property.toColumnName()}'"
/**
 * Typed `like` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun KProperty1<*, CharSequence>.like(property: KProperty1<*, CharSequence>) =
    "${toColumnName()} LIKE '${property.toColumnName()}'"
/**
 * Typed `like` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun String.like(property: KProperty1<*, CharSequence>) =
    "$this LIKE '${property.toColumnName()}'"
/**
 * Typed `like` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun String.like(property: KProperty1<*, CharSequence>) =
    "$this LIKE '${property.toColumnName()}'"
/**
 * Typed `like` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun String.like(property: KProperty1<*, CharSequence>) =
    "$this LIKE '${property.toColumnName()}'"
/**
 * Typed `like` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun KProperty1<*, CharSequence>.like(property: Pair<String, KProperty1<*, CharSequence>>) =
    "${toColumnName()} LIKE '${property.first}.${property.second.toColumnName()}'"
/**
 * Typed `like` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun KProperty1<*, CharSequence>.like(property: Pair<String, KProperty1<*, CharSequence>>) =
    "${toColumnName()} LIKE '${property.first}.${property.second.toColumnName()}'"
/**
 * Typed `like` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun KProperty1<*, CharSequence>.like(property: Pair<String, KProperty1<*, CharSequence>>) =
    "${toColumnName()} LIKE '${property.first}.${property.second.toColumnName()}'"
/**
 * Typed `like` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun String.like(property: Pair<String, KProperty1<*, CharSequence>>) =
    "$this LIKE '${property.first}.${property.second.toColumnName()}'"
/**
 * Typed `like` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun String.like(property: Pair<String, KProperty1<*, CharSequence>>) =
    "$this LIKE '${property.first}.${property.second.toColumnName()}'"
/**
 * Typed `like` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun String.like(property: Pair<String, KProperty1<*, CharSequence>>) =
    "$this LIKE '${property.first}.${property.second.toColumnName()}'"
/**
 * Typed `like` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun Pair<String, KProperty1<*, CharSequence>>.like(value: Any) =
    "$first.${second.toColumnName()} LIKE '$value'"
/**
 * Typed `like` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun Pair<String, KProperty1<*, CharSequence>>.like(value: Any) =
    "$first.${second.toColumnName()} LIKE '$value'"
/**
 * Typed `like` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun Pair<String, KProperty1<*, CharSequence>>.like(value: Any) =
    "$first.${second.toColumnName()} LIKE '$value'"
/**
 * Typed `like` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun Pair<String, KProperty1<*, CharSequence>>.like(property: KProperty1<*, CharSequence>) =
    "$first.${second.toColumnName()} LIKE '${property.toColumnName()}'"
/**
 * Typed `like` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun Pair<String, KProperty1<*, CharSequence>>.like(property: KProperty1<*, CharSequence>) =
    "$first.${second.toColumnName()} LIKE '${property.toColumnName()}'"
/**
 * Typed `like` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun Pair<String, KProperty1<*, CharSequence>>.like(property: KProperty1<*, CharSequence>) =
    "$first.${second.toColumnName()} LIKE '${property.toColumnName()}'"
/**
 * Typed `like` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun Pair<String, KProperty1<*, CharSequence>>.like(property: Pair<String, KProperty1<*, CharSequence>>) =
    "$first.${second.toColumnName()} LIKE '${property.first}.${property.second.toColumnName()}'"
/**
 * Typed `like` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun Pair<String, KProperty1<*, CharSequence>>.like(property: Pair<String, KProperty1<*, CharSequence>>) =
    "$first.${second.toColumnName()} LIKE '${property.first}.${property.second.toColumnName()}'"
/**
 * Typed `like` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun Pair<String, KProperty1<*, CharSequence>>.like(property: Pair<String, KProperty1<*, CharSequence>>) =
    "$first.${second.toColumnName()} LIKE '${property.first}.${property.second.toColumnName()}'"
/**
 * Typed `ilike` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun KProperty1<*, CharSequence>.ilike(value: Any) =
    "${toColumnName()} ILIKE '$value'"
/**
 * Typed `ilike` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun KProperty1<*, CharSequence>.ilike(value: Any) =
    "${toColumnName()} ILIKE '$value'"
/**
 * Typed `ilike` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun KProperty1<*, CharSequence>.ilike(value: Any) =
    "${toColumnName()} ILIKE '$value'"
/**
 * Typed `ilike` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun String.ilike(value: Any) =
    "$this ILIKE '$value'"
/**
 * Typed `ilike` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun String.ilike(value: Any) =
    "$this ILIKE '$value'"
/**
 * Typed `ilike` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun String.ilike(value: Any) =
    "$this ILIKE '$value'"
/**
 * Typed `ilike` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun KProperty1<*, CharSequence>.ilike(property: KProperty1<*, CharSequence>) =
    "${toColumnName()} ILIKE '${property.toColumnName()}'"
/**
 * Typed `ilike` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun KProperty1<*, CharSequence>.ilike(property: KProperty1<*, CharSequence>) =
    "${toColumnName()} ILIKE '${property.toColumnName()}'"
/**
 * Typed `ilike` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun KProperty1<*, CharSequence>.ilike(property: KProperty1<*, CharSequence>) =
    "${toColumnName()} ILIKE '${property.toColumnName()}'"
/**
 * Typed `ilike` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun String.ilike(property: KProperty1<*, CharSequence>) =
    "$this ILIKE '${property.toColumnName()}'"
/**
 * Typed `ilike` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun String.ilike(property: KProperty1<*, CharSequence>) =
    "$this ILIKE '${property.toColumnName()}'"
/**
 * Typed `ilike` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun String.ilike(property: KProperty1<*, CharSequence>) =
    "$this ILIKE '${property.toColumnName()}'"
/**
 * Typed `ilike` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun KProperty1<*, CharSequence>.ilike(property: Pair<String, KProperty1<*, CharSequence>>) =
    "${toColumnName()} ILIKE '${property.first}.${property.second.toColumnName()}'"
/**
 * Typed `ilike` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun KProperty1<*, CharSequence>.ilike(property: Pair<String, KProperty1<*, CharSequence>>) =
    "${toColumnName()} ILIKE '${property.first}.${property.second.toColumnName()}'"
/**
 * Typed `ilike` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun KProperty1<*, CharSequence>.ilike(property: Pair<String, KProperty1<*, CharSequence>>) =
    "${toColumnName()} ILIKE '${property.first}.${property.second.toColumnName()}'"
/**
 * Typed `ilike` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun String.ilike(property: Pair<String, KProperty1<*, CharSequence>>) =
    "$this ILIKE '${property.first}.${property.second.toColumnName()}'"
/**
 * Typed `ilike` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun String.ilike(property: Pair<String, KProperty1<*, CharSequence>>) =
    "$this ILIKE '${property.first}.${property.second.toColumnName()}'"
/**
 * Typed `ilike` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun String.ilike(property: Pair<String, KProperty1<*, CharSequence>>) =
    "$this ILIKE '${property.first}.${property.second.toColumnName()}'"
/**
 * Typed `ilike` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun Pair<String, KProperty1<*, CharSequence>>.ilike(value: Any) =
    "$first.${second.toColumnName()} ILIKE '$value'"
/**
 * Typed `ilike` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun Pair<String, KProperty1<*, CharSequence>>.ilike(value: Any) =
    "$first.${second.toColumnName()} ILIKE '$value'"
/**
 * Typed `ilike` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun Pair<String, KProperty1<*, CharSequence>>.ilike(value: Any) =
    "$first.${second.toColumnName()} ILIKE '$value'"
/**
 * Typed `ilike` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun Pair<String, KProperty1<*, CharSequence>>.ilike(property: KProperty1<*, CharSequence>) =
    "$first.${second.toColumnName()} ILIKE '${property.toColumnName()}'"
/**
 * Typed `ilike` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun Pair<String, KProperty1<*, CharSequence>>.ilike(property: KProperty1<*, CharSequence>) =
    "$first.${second.toColumnName()} ILIKE '${property.toColumnName()}'"
/**
 * Typed `ilike` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun Pair<String, KProperty1<*, CharSequence>>.ilike(property: KProperty1<*, CharSequence>) =
    "$first.${second.toColumnName()} ILIKE '${property.toColumnName()}'"
/**
 * Typed `ilike` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.5.2
 */
@SqlDslMarker
context(_: SqlBuilder)
infix fun Pair<String, KProperty1<*, CharSequence>>.ilike(property: Pair<String, KProperty1<*, CharSequence>>) =
    "$first.${second.toColumnName()} ILIKE '${property.first}.${property.second.toColumnName()}'"
/**
 * Typed `ilike` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: WhereScope)
infix fun Pair<String, KProperty1<*, CharSequence>>.ilike(property: Pair<String, KProperty1<*, CharSequence>>) =
    "$first.${second.toColumnName()} ILIKE '${property.first}.${property.second.toColumnName()}'"

/**
 * Typed `ilike` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 4.1.2
 */
@SqlDslMarker
context(_: JoinScope)
infix fun Pair<String, KProperty1<*, CharSequence>>.ilike(property: Pair<String, KProperty1<*, CharSequence>>) =
    "$first.${second.toColumnName()} ILIKE '${property.first}.${property.second.toColumnName()}'"

/**
 * Adds a condition using a [KProperty1] reference inside [WhereScope].
 *
 * @since 3.4.0
 */
@SqlDslMarker
fun WhereScope.isNullValue(prop: KProperty1<*, *>) = isNullValue(prop.toColumnName())
/**
 * Adds a condition using a [KProperty1] reference inside [WhereScope].
 *
 * @since 3.4.0
 */
@SqlDslMarker
fun WhereScope.isNotNullValue(prop: KProperty1<*, *>) = isNotNullValue(prop.toColumnName())

/**
 * GROUP BY with `KProperty1` columns and optional alias per column.
 *
 * @since 3.4.0
 */
@SqlDslMarker
fun SqlBuilder.groupBy(vararg columns: Pair<String?, KProperty1<*, *>>) =
    groupBy(*columns.map { [alias, prop] -> prop.toQualifiedColumnName(alias) }.toTypedArray())
/**
 * GROUP BY with `KProperty1` columns, no alias.
 *
 * @since 3.4.0
 */
@SqlDslMarker
fun SqlBuilder.groupBy(vararg columns: KProperty1<*, *>) =
    groupBy(*columns.map { it.toColumnName() }.toTypedArray())
/**
 * GROUP BY with `KProperty1` columns, all under the same table alias.
 *
 * @since 3.4.0
 */
@SqlDslMarker
fun SqlBuilder.groupBy(tableAlias: String?, vararg columns: KProperty1<*, *>) {
    val alias = tableAlias ?: columns.firstOrNull()?.ownerClass?.simpleName?.first()?.lowercase().orEmpty()
    groupBy(*columns.map { it.toQualifiedColumnName(alias) }.toTypedArray())
}

/**
 * INSERT INTO with a reified entity class.
 *
 * @since 3.4.0
 */
@SqlDslMarker
inline fun <reified T : Any> SqlBuilder.insertInto() {
    val clazz = T::class
    insertInto(convertName(clazz))
}
/**
 * COLUMNS with `KProperty1` references.
 *
 * @since 3.4.0
 */
@SqlDslMarker
fun SqlBuilder.columns(vararg columns: KProperty1<*, *>) =
    columns(*columns.map { it.toColumnName() }.toTypedArray())

/**
 * UPDATE with a reified entity class.
 *
 * @since 3.4.0
 */
@SqlDslMarker
inline fun <reified T : Any> SqlBuilder.update() {
    val clazz = T::class
    update(convertName(clazz))
}

/**
 * SET expression using [KProperty1].
 *
 * ```kotlin
 * sql {
 *     update<User>()
 *     set(User::name to "'Bob'", User::age to "26")
 *     where { condition(User::id eq "1") }
 * }
 * ```
 *
 * @since 3.4.0
 */
@SqlDslMarker
fun SqlBuilder.set(vararg expressions: Pair<KProperty1<*, *>, String>) =
    set(*expressions.map { [prop, `value`] -> "${prop.toColumnName()} = $value" }.toTypedArray())

/**
 * DELETE FROM with a reified entity class.
 *
 * @since 3.4.0
 */
@SqlDslMarker
inline fun <reified T : Any> SqlBuilder.deleteFrom() {
    val clazz = T::class
    deleteFrom(convertName(clazz))
}

/**
 * ORDER BY with `Triple<alias?, KProperty1, SortDirection>`.
 *
 * @since 3.4.0
 */
@SqlDslMarker
fun SqlBuilder.orderBy(vararg columns: Triple<String?, KProperty1<*, *>, SortDirection>) =
    orderBy(*columns.map { [alias, prop, dir] -> prop.toQualifiedColumnName(alias) to dir }.toTypedArray())
/**
 * ORDER BY with `Pair<KProperty1, SortDirection>`.
 *
 * @since 3.4.0
 */
@SqlDslMarker
fun SqlBuilder.orderBy(vararg columns: Pair<KProperty1<*, *>, SortDirection>) =
    orderBy(*columns.map { [prop, dir] -> prop.toColumnName() to dir }.toTypedArray())
/**
 * ORDER BY with `KProperty1` columns, all with a shared direction.
 *
 * @since 3.4.0
 */
@SqlDslMarker
fun SqlBuilder.orderBy(vararg columns: KProperty1<*, *>, direction: SortDirection = SortDirection.Ascending) =
    orderBy(*columns.map { it.toColumnName() to direction }.toTypedArray())
/**
 * ORDER BY with `KProperty1` columns under a shared table alias and direction.
 *
 * @since 3.4.0
 */
@SqlDslMarker
fun SqlBuilder.orderBy(tableAlias: String?, vararg columns: KProperty1<*, *>, direction: SortDirection = SortDirection.Ascending) {
    val alias = tableAlias ?: columns.firstOrNull()?.ownerClass?.simpleName?.first()?.lowercase().orEmpty()
    orderBy(*columns.map { it.toQualifiedColumnName(alias) to direction }.toTypedArray())
}
/**
 * ORDER BY scope with `KProperty1` support.
 *
 * @since 3.4.0
 */
@SqlDslMarker
fun OrderByScope.column(prop: KProperty1<*, *>, direction: SortDirection = SortDirection.Ascending) =
    column(prop.toColumnName(), direction)

/** @since 3.4.0 */
@SqlDslMarker
fun OrderByScope.column(alias: String?, prop: KProperty1<*, *>, direction: SortDirection = SortDirection.Ascending) =
    column(prop.toQualifiedColumnName(alias), direction)

/** @since 3.4.0 */
@SqlDslMarker
fun OrderByScope.asc(prop: KProperty1<*, *>) = asc(prop.toColumnName())

/** @since 3.4.0 */
@SqlDslMarker
fun OrderByScope.desc(prop: KProperty1<*, *>) = desc(prop.toColumnName())

/** @since 3.4.0 */
@SqlDslMarker
fun OrderByScope.nullsFirst(prop: KProperty1<*, *>, direction: SortDirection = SortDirection.Ascending) =
    nullsFirst(prop.toColumnName(), direction)

/** @since 3.4.0 */
@SqlDslMarker
fun OrderByScope.nullsLast(prop: KProperty1<*, *>, direction: SortDirection = SortDirection.Ascending) =
    nullsLast(prop.toColumnName(), direction)

/**
 * TRUNCATE with a reified entity class.
 *
 * @since 3.4.0
 */
@SqlDslMarker
inline fun <reified T : Any> SqlBuilder.truncate(ifExists: Boolean = false, dropType: DropType = DropType.Restrict) {
    val clazz = T::class
    truncate(convertName(clazz), ifExists, dropType)
}

/**
 * CREATE TABLE with a reified entity class.
 *
 * @since 3.4.0
 */
@SqlDslMarker
inline fun <reified T : Any> SqlBuilder.createTable(body: String) {
    val clazz = T::class
    createTable(convertName(clazz), body)
}
/**
 * ALTER TABLE with a reified entity class.
 *
 * @since 3.4.0
 */
@SqlDslMarker
inline fun <reified T : Any> SqlBuilder.alterTable(ifExists: Boolean = false, alteration: String) {
    val clazz = T::class
    alterTable(convertName(clazz), ifExists, alteration)
}

/**
 * DROP TABLE with a reified entity class.
 *
 * @since 3.4.0
 */
@SqlDslMarker
inline fun <reified T : Any> SqlBuilder.dropTable(ifExists: Boolean = false, dropType: DropType = DropType.Restrict) {
    val clazz = T::class
    dropTable(convertName(clazz), ifExists, dropType)
}

/**
 * CREATE INDEX on a reified entity class.
 *
 * @since 3.4.0
 */
@SqlDslMarker
inline fun <reified T : Any> SqlBuilder.createIndex(indexName: String, columns: String, unique: Boolean = false) {
    val clazz = T::class
    createIndex(indexName, convertName(clazz), columns, unique)
}

/**
 * SHOW TABLE for a reified entity class.
 *
 * @since 3.4.0
 */
@SqlDslMarker
inline fun <reified T : Any> SqlBuilder.showTable() {
    val clazz = T::class
    showTable(convertName(clazz))
}

/**
 * SHOW COLUMNS FROM TABLE for a reified entity class.
 *
 * @since 3.4.0
 */
@SqlDslMarker
inline fun <reified T : Any> SqlBuilder.showColumnsFromTable() {
    val clazz = T::class
    showColumnsFromTable(convertName(clazz))
}

/**
 * SHOW INDEX FROM TABLE for a reified entity class.
 *
 * @since 3.4.0
 */
@SqlDslMarker
inline fun <reified T : Any> SqlBuilder.showIndexFromTable() {
    val clazz = T::class
    showIndexFromTable(convertName(clazz))
}

/**
 * ON TABLE (for triggers) with a reified entity class.
 *
 * @since 3.4.0
 */
@SqlDslMarker
inline fun <reified T : Any> TriggerScope.onTable() {
    val clazz = T::class
    onTable(convertName(clazz))
}

/**
 * Converts the name of a given class into a formatted string based on its `@Table` annotation.
 *
 * @param clazz The class whose name is to be converted.
 * @return A string representation of the class name, including the schema if specified
 *         in the `@Table` annotation, or the simple name of the class if not annotated.
 * @since 2.7.0
 */
@PublishedApi
internal fun convertName(clazz: KClass<*>): String {
    val annotation = clazz.findAnnotation<Table>()
    return (annotation?.schema?.ifEmpty { null }?.let { "$it." } ?: String.EMPTY) + (annotation?.name?.ifEmpty { null } ?: -clazz.simpleName!!)
}