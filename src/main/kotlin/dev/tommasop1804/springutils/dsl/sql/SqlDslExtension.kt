/*
 * Copyright © 2026 Tommaso Pastorelli (TommasoP1804) | Spring-Utils
 */

@file:Suppress("kutils_tuple_declaration", "unused", "SqlNoDataSourceInspection")

package dev.tommasop1804.springutils.dsl.sql

import dev.tommasop1804.kutils.EMPTY
import dev.tommasop1804.kutils.classes.constants.SortDirection
import dev.tommasop1804.kutils.dsl.sql.DropType
import dev.tommasop1804.kutils.dsl.sql.JoinScope
import dev.tommasop1804.kutils.dsl.sql.JoinType
import dev.tommasop1804.kutils.dsl.sql.OrderByScope
import dev.tommasop1804.kutils.dsl.sql.SqlDsl
import dev.tommasop1804.kutils.dsl.sql.TriggerScope
import dev.tommasop1804.kutils.dsl.sql.WhereScope
import dev.tommasop1804.kutils.findAnnotationAnywhere
import dev.tommasop1804.kutils.ownerClass
import dev.tommasop1804.kutils.unaryMinus
import dev.tommasop1804.kutils.validate
import dev.tommasop1804.springutils.jpa.convertName
import jakarta.persistence.Column
import org.intellij.lang.annotations.Language
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

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
fun SqlDsl.select(vararg columns: Pair<String?, KProperty1<*, *>>, distinct: Boolean = false) =
    select(
        *columns.map { [alias, prop] -> prop.toQualifiedColumnName(alias) }.toTypedArray(),
        distinct = distinct
    )
/**
 * SELECT with `KProperty1` columns, no alias.
 *
 * @since 3.4.0
 */
fun SqlDsl.select(vararg columns: KProperty1<*, *>, distinct: Boolean = false) =
    select(
        *columns.map { it.toColumnName() }.toTypedArray(),
        distinct = distinct
    )
/**
 * SELECT with `KProperty1` columns, all under the same table alias.
 *
 * @since 3.4.0
 */
fun SqlDsl.select(tableAlias: String?, vararg columns: KProperty1<*, *>, distinct: Boolean = false) {
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
inline fun <reified T : Any> SqlDsl.from(alias: String? = null) {
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
fun SqlDsl.from(vararg tables: Pair<KClass<*>, String>) {
    validate(tables.map(Pair<*, *>::second).distinct().size == tables.size) { "All aliases must be unique" }
    from(*tables.map { [clazz, alias] -> "${convertName(clazz)} $alias" }.toTypedArray())
}

/**
 * JOIN with a reified entity class and raw join type string.
 *
 * @since 3.4.0
 */
inline fun <reified T : Any> SqlDsl.join(alias: String? = null, @Language("sql") type: String, @Language("sql") on: String) {
    val clazz = T::class
    join(convertName(clazz) + (alias?.let { " $it" } ?: String.EMPTY), type, on)
}
/**
 * JOIN with a reified entity class and [JoinType].
 *
 * @since 3.4.0
 */
inline fun <reified T : Any> SqlDsl.join(alias: String? = null, type: JoinType, @Language("sql") on: String) {
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
inline fun <reified T : Any> JoinScope.inner(alias: String? = null, @Language("sql") on: String) {
    join(convertName(T::class) + (alias?.let { " $it" } ?: String.EMPTY), JoinType.INNER, on)
}
/**
 * LEFT JOIN on reified entity.
 *
 * @since 3.4.0
 */
inline fun <reified T : Any> JoinScope.left(alias: String? = null, @Language("sql") on: String) {
    join(convertName(T::class) + (alias?.let { " $it" } ?: String.EMPTY), JoinType.LEFT_OUTER, on)
}
/**
 * RIGHT JOIN on reified entity.
 *
 * @since 3.4.0
 */
inline fun <reified T : Any> JoinScope.right(alias: String? = null, @Language("sql") on: String) {
    join(convertName(T::class) + (alias?.let { " $it" } ?: String.EMPTY), JoinType.RIGHT_OUTER, on)
}
/**
 * FULL OUTER JOIN on reified entity.
 *
 * @since 3.4.0
 */
inline fun <reified T : Any> JoinScope.full(alias: String? = null, @Language("sql") on: String) {
    join(convertName(T::class) + (alias?.let { " $it" } ?: String.EMPTY), JoinType.FULL_OUTER, on)
}
/**
 * CROSS JOIN on reified entity.
 *
 * @since 3.4.0
 */
inline fun <reified T : Any> JoinScope.cross(alias: String? = null) {
    val name = convertName(T::class) + (alias?.let { " $it" } ?: String.EMPTY)
    cross(" CROSS JOIN $name")
}
/**
 * NATURAL JOIN on reified entity.
 *
 * @since 3.4.0
 */
inline fun <reified T : Any> JoinScope.natural(alias: String? = null) {
    val name = convertName(T::class) + (alias?.let { " $it" } ?: String.EMPTY)
    natural(" NATURAL JOIN $name")
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
infix fun KProperty1<*, *>.eq(@Language("sql") value: Any) =
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
 * @since 3.4.0
 */
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
 * @since 3.4.0
 */
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
 * @since 3.4.0
 */
infix fun Pair<String, KProperty1<*, *>>.eq(@Language("sql") value: Any) =
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
infix fun Pair<String, KProperty1<*, *>>.eq(property: Pair<String, KProperty1<*, *>>) =
    "$first.${second.toColumnName()} = ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun KProperty1<*, *>.neq(@Language("sql") value: Any) =
    "${toColumnName()} != $value"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun KProperty1<*, *>.neq(property: KProperty1<*, *>) =
    "${toColumnName()} != ${property.toColumnName()}"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun KProperty1<*, *>.neq(property: Pair<String, KProperty1<*, *>>) =
    "${toColumnName()} != ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun Pair<String, KProperty1<*, *>>.neq(@Language("sql") value: Any) =
    "$first.${second.toColumnName()} != $value"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun Pair<String, KProperty1<*, *>>.neq(property: KProperty1<*, *>) =
    "$first.${second.toColumnName()} != ${property.toColumnName()}"
/**
 * Typed `!=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun Pair<String, KProperty1<*, *>>.neq(property: Pair<String, KProperty1<*, *>>) =
    "$first.${second.toColumnName()} != ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun KProperty1<*, *>.gt(@Language("sql") value: Any) =
    "${toColumnName()} > $value"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun KProperty1<*, *>.gt(property: KProperty1<*, *>) =
    "${toColumnName()} > ${property.toColumnName()}"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun KProperty1<*, *>.gt(property: Pair<String, KProperty1<*, *>>) =
    "${toColumnName()} > ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun Pair<String, KProperty1<*, *>>.gt(@Language("sql") value: Any) =
    "$first.${second.toColumnName()} > $value"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun Pair<String, KProperty1<*, *>>.gt(property: KProperty1<*, *>) =
    "$first.${second.toColumnName()} > ${property.toColumnName()}"
/**
 * Typed `>` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun Pair<String, KProperty1<*, *>>.gt(property: Pair<String, KProperty1<*, *>>) =
    "$first.${second.toColumnName()} > ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun KProperty1<*, *>.lt(@Language("sql") value: Any) =
    "${toColumnName()} < $value"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun KProperty1<*, *>.lt(property: KProperty1<*, *>) =
    "${toColumnName()} < ${property.toColumnName()}"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun KProperty1<*, *>.lt(property: Pair<String, KProperty1<*, *>>) =
    "${toColumnName()} < ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun Pair<String, KProperty1<*, *>>.lt(@Language("sql") value: Any) =
    "$first.${second.toColumnName()} < $value"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun Pair<String, KProperty1<*, *>>.lt(property: KProperty1<*, *>) =
    "$first.${second.toColumnName()} < ${property.toColumnName()}"
/**
 * Typed `<` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun Pair<String, KProperty1<*, *>>.lt(property: Pair<String, KProperty1<*, *>>) =
    "$first.${second.toColumnName()} < ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun KProperty1<*, *>.gte(@Language("sql") value: Any) =
    "${toColumnName()} >= $value"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun KProperty1<*, *>.gte(property: KProperty1<*, *>) =
    "${toColumnName()} >= ${property.toColumnName()}"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun KProperty1<*, *>.gte(property: Pair<String, KProperty1<*, *>>) =
    "${toColumnName()} >= ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun Pair<String, KProperty1<*, *>>.gte(@Language("sql") value: Any) =
    "$first.${second.toColumnName()} >= $value"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun Pair<String, KProperty1<*, *>>.gte(property: KProperty1<*, *>) =
    "$first.${second.toColumnName()} >= ${property.toColumnName()}"
/**
 * Typed `>=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun Pair<String, KProperty1<*, *>>.gte(property: Pair<String, KProperty1<*, *>>) =
    "$first.${second.toColumnName()} >= ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun KProperty1<*, *>.lte(@Language("sql") value: Any) =
    "${toColumnName()} <= $value"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun KProperty1<*, *>.lte(property: KProperty1<*, *>) =
    "${toColumnName()} <= ${property.toColumnName()}"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun KProperty1<*, *>.lte(property: Pair<String, KProperty1<*, *>>) =
    "${toColumnName()} <= ${property.first}.${property.second.toColumnName()}"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun Pair<String, KProperty1<*, *>>.lte(@Language("sql") value: Any) =
    "$first.${second.toColumnName()} <= $value"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun Pair<String, KProperty1<*, *>>.lte(property: KProperty1<*, *>) =
    "$first.${second.toColumnName()} <= ${property.toColumnName()}"
/**
 * Typed `<=` condition.
 *
 * Use it in a condition/and/or.
 *
 * @since 3.4.0
 */
infix fun Pair<String, KProperty1<*, *>>.lte(property: Pair<String, KProperty1<*, *>>) =
    "$first.${second.toColumnName()} <= ${property.first}.${property.second.toColumnName()}"
/**
 * Adds a condition using a [KProperty1] reference inside [WhereScope].
 *
 * @since 3.4.0
 */
fun WhereScope.isNullValue(prop: KProperty1<*, *>) = isNullValue(prop.toColumnName())
/**
 * Adds a condition using a [KProperty1] reference inside [WhereScope].
 *
 * @since 3.4.0
 */
fun WhereScope.isNotNullValue(prop: KProperty1<*, *>) = isNotNullValue(prop.toColumnName())

/**
 * GROUP BY with `KProperty1` columns and optional alias per column.
 *
 * @since 3.4.0
 */
fun SqlDsl.groupBy(vararg columns: Pair<String?, KProperty1<*, *>>) =
    groupBy(*columns.map { [alias, prop] -> prop.toQualifiedColumnName(alias) }.toTypedArray())
/**
 * GROUP BY with `KProperty1` columns, no alias.
 *
 * @since 3.4.0
 */
fun SqlDsl.groupBy(vararg columns: KProperty1<*, *>) =
    groupBy(*columns.map { it.toColumnName() }.toTypedArray())
/**
 * GROUP BY with `KProperty1` columns, all under the same table alias.
 *
 * @since 3.4.0
 */
fun SqlDsl.groupBy(tableAlias: String?, vararg columns: KProperty1<*, *>) {
    val alias = tableAlias ?: columns.firstOrNull()?.ownerClass?.simpleName?.first()?.lowercase().orEmpty()
    groupBy(*columns.map { it.toQualifiedColumnName(alias) }.toTypedArray())
}

/**
 * INSERT INTO with a reified entity class.
 *
 * @since 3.4.0
 */
inline fun <reified T : Any> SqlDsl.insertInto() {
    val clazz = T::class
    insertInto(convertName(clazz))
}
/**
 * COLUMNS with `KProperty1` references.
 *
 * @since 3.4.0
 */
fun SqlDsl.columns(vararg columns: KProperty1<*, *>) =
    columns(*columns.map { it.toColumnName() }.toTypedArray())

/**
 * UPDATE with a reified entity class.
 *
 * @since 3.4.0
 */
inline fun <reified T : Any> SqlDsl.update() {
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
fun SqlDsl.set(vararg expressions: Pair<KProperty1<*, *>, String>) =
    set(*expressions.map { [prop, `value`] -> "${prop.toColumnName()} = $value" }.toTypedArray())

/**
 * DELETE FROM with a reified entity class.
 *
 * @since 3.4.0
 */
inline fun <reified T : Any> SqlDsl.deleteFrom() {
    val clazz = T::class
    deleteFrom(convertName(clazz))
}

/**
 * ORDER BY with `Triple<alias?, KProperty1, SortDirection>`.
 *
 * @since 3.4.0
 */
fun SqlDsl.orderBy(vararg columns: Triple<String?, KProperty1<*, *>, SortDirection>) =
    orderBy(*columns.map { [alias, prop, dir] -> prop.toQualifiedColumnName(alias) to dir }.toTypedArray())
/**
 * ORDER BY with `Pair<KProperty1, SortDirection>`.
 *
 * @since 3.4.0
 */
fun SqlDsl.orderBy(vararg columns: Pair<KProperty1<*, *>, SortDirection>) =
    orderBy(*columns.map { [prop, dir] -> prop.toColumnName() to dir }.toTypedArray())
/**
 * ORDER BY with `KProperty1` columns, all with a shared direction.
 *
 * @since 3.4.0
 */
fun SqlDsl.orderBy(vararg columns: KProperty1<*, *>, direction: SortDirection = SortDirection.ASCENDING) =
    orderBy(*columns.map { it.toColumnName() to direction }.toTypedArray())
/**
 * ORDER BY with `KProperty1` columns under a shared table alias and direction.
 *
 * @since 3.4.0
 */
fun SqlDsl.orderBy(tableAlias: String?, vararg columns: KProperty1<*, *>, direction: SortDirection = SortDirection.ASCENDING) {
    val alias = tableAlias ?: columns.firstOrNull()?.ownerClass?.simpleName?.first()?.lowercase().orEmpty()
    orderBy(*columns.map { it.toQualifiedColumnName(alias) to direction }.toTypedArray())
}
/**
 * ORDER BY scope with `KProperty1` support.
 *
 * @since 3.4.0
 */
fun OrderByScope.column(prop: KProperty1<*, *>, direction: SortDirection = SortDirection.ASCENDING) =
    column(prop.toColumnName(), direction)

/** @since 3.4.0 */
fun OrderByScope.column(alias: String?, prop: KProperty1<*, *>, direction: SortDirection = SortDirection.ASCENDING) =
    column(prop.toQualifiedColumnName(alias), direction)

/** @since 3.4.0 */
fun OrderByScope.asc(prop: KProperty1<*, *>) = asc(prop.toColumnName())

/** @since 3.4.0 */
fun OrderByScope.desc(prop: KProperty1<*, *>) = desc(prop.toColumnName())

/** @since 3.4.0 */
fun OrderByScope.nullsFirst(prop: KProperty1<*, *>, direction: SortDirection = SortDirection.ASCENDING) =
    nullsFirst(prop.toColumnName(), direction)

/** @since 3.4.0 */
fun OrderByScope.nullsLast(prop: KProperty1<*, *>, direction: SortDirection = SortDirection.ASCENDING) =
    nullsLast(prop.toColumnName(), direction)

/**
 * TRUNCATE with a reified entity class.
 *
 * @since 3.4.0
 */
inline fun <reified T : Any> SqlDsl.truncate(ifExists: Boolean = false, dropType: DropType = DropType.RESTRICT) {
    val clazz = T::class
    truncate(convertName(clazz), ifExists, dropType)
}

/**
 * CREATE TABLE with a reified entity class.
 *
 * @since 3.4.0
 */
inline fun <reified T : Any> SqlDsl.createTable(@Language("sql") body: String) {
    val clazz = T::class
    createTable(convertName(clazz), body)
}
/**
 * ALTER TABLE with a reified entity class.
 *
 * @since 3.4.0
 */
inline fun <reified T : Any> SqlDsl.alterTable(ifExists: Boolean = false, @Language("sql") alteration: String) {
    val clazz = T::class
    alterTable(convertName(clazz), ifExists, alteration)
}

/**
 * DROP TABLE with a reified entity class.
 *
 * @since 3.4.0
 */
inline fun <reified T : Any> SqlDsl.dropTable(ifExists: Boolean = false, dropType: DropType = DropType.RESTRICT) {
    val clazz = T::class
    dropTable(convertName(clazz), ifExists, dropType)
}

/**
 * CREATE INDEX on a reified entity class.
 *
 * @since 3.4.0
 */
inline fun <reified T : Any> SqlDsl.createIndex(indexName: String, @Language("sql") columns: String, unique: Boolean = false) {
    val clazz = T::class
    createIndex(indexName, convertName(clazz), columns, unique)
}

/**
 * SHOW TABLE for a reified entity class.
 *
 * @since 3.4.0
 */
inline fun <reified T : Any> SqlDsl.showTable() {
    val clazz = T::class
    showTable(convertName(clazz))
}

/**
 * SHOW COLUMNS FROM TABLE for a reified entity class.
 *
 * @since 3.4.0
 */
inline fun <reified T : Any> SqlDsl.showColumnsFromTable() {
    val clazz = T::class
    showColumnsFromTable(convertName(clazz))
}

/**
 * SHOW INDEX FROM TABLE for a reified entity class.
 *
 * @since 3.4.0
 */
inline fun <reified T : Any> SqlDsl.showIndexFromTable() {
    val clazz = T::class
    showIndexFromTable(convertName(clazz))
}

/**
 * ON TABLE (for triggers) with a reified entity class.
 *
 * @since 3.4.0
 */
inline fun <reified T : Any> TriggerScope.onTable() {
    val clazz = T::class
    onTable(convertName(clazz))
}
