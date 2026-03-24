@file:Suppress("unused", "kutils_tuple_declaration", "SqlNoDataSourceInspection")
@file:OptIn(UnsafeUsage::class)

package dev.tommasop1804.springutils.jpa

import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.annotations.*
import dev.tommasop1804.kutils.classes.builder.*
import dev.tommasop1804.kutils.classes.constants.*
import jakarta.persistence.Column
import jakarta.persistence.Table
import org.intellij.lang.annotations.Language
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation

/**
 * Builds a SELECT query with the specified columns, optionally including the DISTINCT clause.
 *
 * @param columns A vararg of pairs where each pair consists of a table alias (String)
 * and a column reference (KProperty1) indicating which columns to select.
 * @param distinct A Boolean indicating whether to include the DISTINCT clause in the query.
 * Defaults to false.
 * @since 2.7.0
 */
fun SqlQueryBuilder.select(vararg columns: Pair<String?, KProperty1<*, *>>, distinct: Boolean = false) =
    select(
        *columns.map { col -> "${col.first?.let { "$it." }}${col.second.findAnnotationAnywhere<Column>()?.name?.ifEmpty { null } ?: -col.second.name}" }.toTypedArray(),
        distinct = distinct
    )
/**
 * Adds a SELECT statement to the SQL query with the specified columns.
 *
 * @param columns The properties of the columns to be selected. These are KProperty1 references,
 *                which are used to identify the columns to include in the SELECT statement.
 * @param distinct Specifies whether the SELECT statement should include the DISTINCT keyword
 *                 to prevent duplicate rows. Defaults to `false`.
 * @since 2.7.0
 */
fun SqlQueryBuilder.select(vararg columns: KProperty1<*, *>, distinct: Boolean = false) =
    select(
        *columns.map { it.findAnnotationAnywhere<Column>()?.name?.ifEmpty { null } ?: -it.name }.toTypedArray(),
        distinct = distinct
    )
/**
 * Builds a SELECT query for the specified columns, optionally scoped to a table alias
 * and including the DISTINCT clause.
 *
 * @param tableAlias An optional String representing the alias for the table. If not provided,
 * defaults to the first letter of the owner class of the first column in lowercase.
 * @param columns A vararg of column references (KProperty1) to select in the query.
 * @param distinct A Boolean indicating whether to include the DISTINCT clause in the query.
 * Defaults to false.
 * @since 2.7.0
 */
fun SqlQueryBuilder.select(tableAlias: String?, vararg columns: KProperty1<*, *>, distinct: Boolean = false) =
    (tableAlias ?: columns.firstOrNull()?.ownerClass?.simpleName?.first()?.lowercase().orEmpty()).let { alias ->
        select(*columns.map { alias to it }.toTypedArray(), distinct = distinct)
    }
/**
 * Specifies the source table for the query using a class annotated with @Table.
 * The method retrieves the table name from the @Table annotation on the specified class
 * and associates it with a given alias.
 *
 * @param T The class type representing the table to query. Must be annotated with @Table.
 * @param alias The alias to be used for the table in the SQL query.
 * @return The updated SqlQueryBuilder instance with the source table and alias set.
 * @since 2.7.0
 */
inline fun <reified T> SqlQueryBuilder.from(alias: String? = null): SqlQueryBuilder {
    val clazz = T::class
    return from(convertName(clazz) + (alias?.let { " $it" } ?: String.EMPTY))
}
/**
 * Specifies the source tables and their aliases for the SQL query.
 *
 * @param tables A vararg parameter of pairs, where each pair consists of a KClass representing
 * the table class annotated with @Table, and a String representing the alias for the table.
 * All provided aliases must be unique.
 * @return The updated instance of the SqlQueryBuilder with the specified tables and aliases set.
 * @throws dev.tommasop1804.kutils.exceptions.ValidationFailedException if the aliases are not unique.
 * @since 2.7.0
 */
fun SqlQueryBuilder.from(vararg tables: Pair<KClass<*>, String>): SqlQueryBuilder {
    validate(tables.map(Pair<*, *>::second).distinct().size == tables.size) { "All aliases must be unique" }
    return tables.map { [clazz, alias] ->
        convertName(clazz) + " $alias"
    }.let { from(*it.toTypedArray()) }
}

/**
 * Adds a SQL join clause to the query builder using the specified alias, join type, and join condition.
 *
 * @param T The type of the entity to join. The table name is inferred from the class annotation
 *          or simple name of the class.
 * @param alias An optional alias for the table being joined. Defaults to `null` if not provided.
 * @param joinType The SQL keyword representing the type of join (e.g., "INNER JOIN", "LEFT JOIN").
 * @param joinCondition The SQL condition for the join, typically specifying the relationship
 *                      between columns of the tables being joined.
 * @return The modified instance of the SqlQueryBuilder with the join clause added.
 * @since 2.7.0
 */
inline fun <reified T> SqlQueryBuilder.join(alias: String? = null, @Language("sql") joinType: String, @Language("sql") joinCondition: String): SqlQueryBuilder {
    val clazz = T::class
    return join(convertName(clazz) + alias?.let { " $it" }, joinType, joinCondition)
}
/**
 * Adds a join clause to the SQL query being built.
 *
 * @param T The type representing the table being joined.
 * @param alias An optional alias for the joined table. Defaults to null.
 * @param joinType The type of join to apply (e.g., INNER, LEFT, RIGHT), provided as a [SqlQueryBuilder.JoinType].
 * @param joinCondition The SQL condition that defines the join logic.
 * @return The updated [SqlQueryBuilder] instance with the join clause added.
 * @since 2.7.0
 */
inline fun <reified T> SqlQueryBuilder.join(alias: String? = null, joinType: SqlQueryBuilder.JoinType, @Language("sql") joinCondition: String): SqlQueryBuilder {
    val clazz = T::class
    return join(convertName(clazz) + alias?.let { " $it" }, joinType, joinCondition)
}

/**
 * Adds a GROUP BY clause to the SQL query for the specified columns.
 *
 * @param columns A vararg of pairs where the first element is an optional table alias
 *                and the second element is a property reference representing the column
 *                to group by. The optional table alias can be null if not applicable.
 * @since 2.7.0
 */
fun SqlQueryBuilder.groupBy(vararg columns: Pair<String?, KProperty1<*, *>>) =
    groupBy(*columns.map { col -> "${col.first?.let { "$it." }}${col.second.findAnnotationAnywhere<Column>()?.name?.ifEmpty { null } ?: -col.second.name}" }.toTypedArray())
/**
 * Specifies the columns for the GROUP BY clause of the SQL query.
 *
 * @param columns The properties representing the columns to be included in the GROUP BY clause,
 *                resolved to their corresponding database column names. If no column name annotation
 *                is found, the property name is used by default.
 * @since 2.7.0
 */
fun SqlQueryBuilder.groupBy(vararg columns: KProperty1<*, *>) =
    groupBy(*columns.map { it.findAnnotationAnywhere<Column>()?.name?.ifEmpty { null } ?: -it.name }.toTypedArray())

/**
 * Adds an INSERT INTO statement to the query for the table corresponding to the specified generic type.
 *
 * The table name is determined by the `Table` annotation on the class, if present. If no `Table` annotation
 * is found, the class name is used as the table name.
 *
 * @return The instance of SqlQueryBuilder with the updated statement.
 * @since 2.7.0
 */
inline fun <reified T> SqlQueryBuilder.insertInto(): SqlQueryBuilder {
    val clazz = T::class
    return insertInto(convertName(clazz))
}
/**
 * Specifies the columns to be used in the SQL query being built.
 *
 * @param columns A variable number of pairs, where each pair consists of an optional table alias (nullable String)
 *                and a Kotlin property representing the column. The table alias, if provided, is prefixed
 *                to the column name. The column name is determined either from the `@Column` annotation
 *                on the property or defaults to the property's name.
 * @since 2.7.0
 */
fun SqlQueryBuilder.columns(vararg columns: Pair<String?, KProperty1<*, *>>) =
    columns(*columns.map { col -> "${col.first?.let { "$it." }}${col.second.findAnnotationAnywhere<Column>()?.name?.ifEmpty { null } ?: -col.second.name}" }.toTypedArray())
/**
 * Specifies the columns to be used in the SQL query being built.
 *
 * @param columns A variable number of KProperty1 references representing the properties
 *                to be mapped as columns. If the property has a `Column` annotation, the
 *                name specified in the annotation is used; otherwise, the name of the
 *                property itself is used.
 * @since 2.7.0
 */
fun SqlQueryBuilder.columns(vararg columns: KProperty1<*, *>) =
    columns(*columns.map { it.findAnnotationAnywhere<Column>()?.name?.ifEmpty { null } ?: -it.name }.toTypedArray())

/**
 * Configures an SQL `UPDATE` statement for the specified table type.
 *
 * The table name is determined by resolving annotations on the provided class type `T`.
 * This method is intended for use with classes annotated with a `@Table` annotation.
 *
 * @return The updated instance of the `SqlQueryBuilder` configured for the `UPDATE` statement.
 * @since 2.7.0
 */
inline fun <reified T> SqlQueryBuilder.update(): SqlQueryBuilder {
    val clazz = T::class
    return update(convertName(clazz))
}

/**
 * Constructs a DELETE SQL query for the table associated with the specified type [T].
 *
 * The table name is retrieved and converted based on the specified type's annotations.
 * This function is inlined and reifies the generic type parameter [T].
 *
 * @return A modified instance of [SqlQueryBuilder], representing the DELETE query.
 * @since 2.7.0
 */
inline fun <reified T> SqlQueryBuilder.deleteFrom(): SqlQueryBuilder {
    val clazz = T::class
    return deleteFrom(convertName(clazz))
}

/**
 * Adds an ORDER BY clause to the SQL query with the specified columns and their sorting directions.
 *
 * @param columns A vararg of Triple where:
 *  - The first element is an optional table alias (nullable String),
 *  - The second element is a property reference (KProperty1) to map the column,
 *  - The third element specifies the sort direction (SortDirection) for the column.
 * @since 2.7.0
 */
fun SqlQueryBuilder.orderBy(vararg columns: Triple<String?, KProperty1<*, *>, SortDirection>) =
    orderBy(*columns.map { col -> "${col.first?.let { "$it." }}${col.second.findAnnotationAnywhere<Column>()?.name?.ifEmpty { null } ?: -col.second.name}" to col.third }.toTypedArray())
/**
 * Adds an ORDER BY clause to the SQL query using the specified columns and their sort directions.
 *
 * @param columns Pairs of properties representing the columns and their sort directions.
 *                The property is resolved to its column name, either via a `Column` annotation
 *                or by using the property name directly.
 * @since 2.7.0
 */
fun SqlQueryBuilder.orderBy(vararg columns: Pair<KProperty1<*, *>, SortDirection>) =
    orderBy(*columns.map { (it.first.findAnnotationAnywhere<Column>()?.name?.ifEmpty { null } ?: -it.first.name) to it.second }.toTypedArray())
/**
 * Adds an ORDER BY clause to the SQL query based on the provided column and direction specifications.
 *
 * @param columns A variable-length argument of pairs, where each pair consists of an optional table alias (or null)
 * and a property representing the column to sort by.
 * @param direction The sorting direction (e.g., ascending or descending) to apply on the specified columns.
 * @since 2.7.0
 */
fun SqlQueryBuilder.orderBy(vararg columns: Pair<String?, KProperty1<*, *>>, direction: SortDirection = SortDirection.ASCENDING) =
    orderBy(*columns.map { col -> "${col.first?.let { "$it." }}${col.second.findAnnotationAnywhere<Column>()?.name?.ifEmpty { null } ?: -col.second.name}" to direction }.toTypedArray())
/**
 * Specifies the `ORDER BY` clause for the SQL query using the provided columns and sorting direction.
 *
 * @param columns A variable number of Kotlin property references representing the columns to be used in the `ORDER BY` clause.
 * @param direction The sorting direction to apply, either ascending or descending, represented by the [SortDirection] enum.
 * @since 2.7.0
 */
fun SqlQueryBuilder.orderBy(vararg columns: KProperty1<*, *>, direction: SortDirection = SortDirection.ASCENDING) =
    orderBy(*columns.map { (it.findAnnotationAnywhere<Column>()?.name?.ifEmpty { null } ?: -it.name) to direction }.toTypedArray())

/**
 * Builds a SQL query to truncate a table corresponding to the specified type.
 *
 * @param ifExists A boolean flag indicating whether to include a conditional check to only truncate
 * the table if it exists. Defaults to false.
 * @param dropType The drop type to use when truncating the table, indicating whether to apply
 * RESTRICT or CASCADE behavior. Defaults to SqlQueryBuilder.DropType.RESTRICT.
 * @return The SqlQueryBuilder instance with the truncate operation included.
 * @since 2.7.0
 */
inline fun <reified T> SqlQueryBuilder.truncate(ifExists: Boolean = false, dropType: SqlQueryBuilder.DropType = SqlQueryBuilder.DropType.RESTRICT): SqlQueryBuilder {
    val clazz = T::class
    return truncate(convertName(clazz), ifExists, dropType)
}

/**
 * Creates a new table in the database using the specified body for the SQL statement.
 *
 * @param T the type of the class associated with the table to be created.
 * @param body the SQL definition of the table.
 * @return the current instance of the SqlQueryBuilder for chaining operations.
 * @since 2.7.0
 */
inline fun <reified T> SqlQueryBuilder.createTable(@Language("sql") body: String): SqlQueryBuilder {
    val clazz = T::class
    return createTable(convertName(clazz), body)
}
/**
 * Alters an existing table in the database by applying the specified alterations.
 *
 * @param T The class type representing the database table to be altered.
 * @param ifExists A boolean flag indicating whether the alteration should only be applied if the table exists. Defaults to false.
 * @param alteration A SQL string specifying the alterations to be made to the table.
 * @return The updated SqlQueryBuilder instance with the applied alterations.
 * @since 2.7.0
 */
inline fun <reified T> SqlQueryBuilder.alterTable(ifExists: Boolean = false, @Language("sql") alteration: String): SqlQueryBuilder {
    val clazz = T::class
    return alterTable(convertName(clazz), ifExists, alteration)
}
/**
 * Drops a table in the SQL database corresponding to the specified type [T].
 *
 * @param ifExists A flag indicating whether the query should include a check to drop the table only if it exists.
 * @param dropType Specifies the behavior for handling dependent objects, such as `CASCADE` or `RESTRICT`.
 * @return The `SqlQueryBuilder` instance with the generated drop table query.
 * @since 2.7.0
 */
inline fun <reified T> SqlQueryBuilder.dropTable(ifExists: Boolean = false, dropType: SqlQueryBuilder.DropType = SqlQueryBuilder.DropType.RESTRICT): SqlQueryBuilder {
    val clazz = T::class
    return dropTable(convertName(clazz), ifExists, dropType)
}

/**
 * Creates an SQL index for the specified table and columns.
 *
 * @param T The table class type for which the index will be created.
 * @param indexName The name of the index to be created.
 * @param columns A comma-separated string of column names to be included in the index.
 * @return An updated instance of [SqlQueryBuilder] with the index creation SQL statement added.
 * @since 2.7.0
 */
inline fun <reified T> SqlQueryBuilder.createIndex(indexName: String, @Language("sql") columns: String): SqlQueryBuilder {
    val clazz = T::class
    return createIndex(indexName, convertName(clazz), columns)
}
/**
 * Configures the SQL query to display the structure of the table corresponding to the specified generic type.
 *
 * The generic type must be a class annotated with the `@Table` annotation, which provides metadata
 * about the table's schema and name.
 *
 * @return The current instance of [SqlQueryBuilder], allowing for further query configuration.
 * @since 2.7.0
 */
inline fun <reified T> SqlQueryBuilder.showTable(): SqlQueryBuilder {
    val clazz = T::class
    return showTable(convertName(clazz))
}
/**
 * Configures the query builder to include a SHOW COLUMNS statement for the table corresponding
 * to the generic type parameter `T`. The table name is inferred from the type's class
 * metadata, and any relevant annotations.
 *
 * @return The updated instance of [SqlQueryBuilder] with the SHOW COLUMNS configuration applied.
 * @since 2.7.0
 */
inline fun <reified T> SqlQueryBuilder.showColumnsFromTable(): SqlQueryBuilder {
    val clazz = T::class
    return showColumnsFromTable(convertName(clazz))
}
/**
 * Modifies the SQL query to include the command for displaying index information
 * for the table associated with the specified reified type parameter.
 *
 * @return An instance of [SqlQueryBuilder] with the updated query to show index information.
 * @since 2.7.0
 */
inline fun <reified T> SqlQueryBuilder.showIndexFromTable(): SqlQueryBuilder {
    val clazz = T::class
    return showColumnsFromTable(convertName(clazz))
}

/**
 * Sets the target table for the query, based on the reified type parameter `T`.
 *
 * The table name is determined by inspecting the `@Table` annotation on the class of type `T`.
 * The schema and table name are extracted from the annotation, with a fallback to the class name
 * if the annotation is not present.
 *
 * @return The current instance of SqlQueryBuilder with the target table configured.
 * @since 2.7.0
 */
inline fun <reified T> SqlQueryBuilder.onTable(): SqlQueryBuilder {
    val clazz = T::class
    return onTable(convertName(clazz))
}

/**
 * Converts the name of a given class into a formatted string based on its `@Table` annotation.
 *
 * @param clazz The class whose name is to be converted.
 * @return A string representation of the class name, including the schema if specified
 *         in the `@Table` annotation, or the simple name of the class if not annotated.
 * @since 2.7.0
 */
@UnsafeUsage
fun convertName(clazz: KClass<*>): String {
    val annotation = clazz.findAnnotation<Table>()
    return (annotation?.schema?.ifEmpty { null }?.let { "$it." } ?: String.EMPTY) + (annotation?.name?.ifEmpty { null } ?: -clazz.simpleName!!)
}