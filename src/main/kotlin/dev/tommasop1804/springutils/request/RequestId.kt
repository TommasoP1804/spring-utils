/*
 * Copyright © 2026 Tommaso Pastorelli (TommasoP1804) | Spring-Utils
 */

package dev.tommasop1804.springutils.request

import dev.tommasop1804.kutils.*
import dev.tommasop1804.kutils.classes.identifiers.*
import dev.tommasop1804.kutils.classes.identifiers.Ulid.Companion.toUlid

/**
 * Represents a request identifier composed of a string value that adheres to a specific format.
 * This class is designed to parse and extract meaningful components from the identifier.
 * It implements the [CharSequence] interface for compatibility with string manipulation.
 *
 * @property value The string representation of the request identifier.
 * @since 2.1.0
 * @author Tommaso Pastorelli
 */
@JvmInline
@Suppress("unused")
value class RequestId internal constructor(val value: String): CharSequence {
    /**
     * Represents the length of the underlying string value for this instance of [RequestId].
     *
     * This property provides the total number of characters in the `value` property of the [RequestId].
     * It complies with the [CharSequence] contract by returning the count of characters stored in the `value`.
     * @since 2.1.0
     */
    override val length: Int get() = value.length

    /**
     * Extracts the application name from the `value` string of the enclosing [RequestId].
     *
     * The value is assumed to be structured such that sections are delimited by a colon (`:`).
     * This property retrieves the second segment (index 1) after splitting the value by the colon delimiter.
     *
     * @since 2.1.0
     */
    val applicationAcronym: String get() = (value / Char.COLON)[1]

    /**
     * Retrieves the ULID component of the `RequestId` value.
     *
     * The `RequestId` is expected to consist of multiple parts separated by a colon (`:`).
     * This property extracts and converts the third part into a `ULID`.
     *
     * @return The ULID representation of the third component of the `RequestId` value.
     * @since 2.1.0
     */
    val ulid: Ulid get() = (value / Char.COLON)[2].toUlid()()

    /**
     * Retrieves the `Instant` representation of the timestamp from the underlying ULID.
     *
     * The `Instant` is extracted from the ULID, which encodes the timestamp part of the identifier.
     * This allows for operations or comparisons based on the creation time of the ULID.
     * @since 2.1.0
     */
    val instant: java.time.Instant get() = ulid.instant

    /**
     * Retrieves the character at the specified index from the underlying string value.
     *
     * @param index The position of the character to retrieve. Must be within the bounds of the string.
     * @return The character at the specified index.
     * @throws IndexOutOfBoundsException If the index is out of range.
     * @since 2.1.0
     */
    override fun get(index: Int) = value[index]

    /**
     * Returns a new character sequence that is a subsequence of this sequence.
     *
     * The subsequence starts at the specified `startIndex` and ends right before the specified `endIndex`.
     *
     * @param startIndex the start index of the subsequence, inclusive.
     * @param endIndex the end index of the subsequence, exclusive.
     * @since 2.1.0
     */
    override fun subSequence(startIndex: Int, endIndex: Int) = value.subSequence(startIndex, endIndex)

    override fun toString() = value
}