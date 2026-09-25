/*
 * Copyright © 2026 Tommaso Pastorelli (TommasoP1804) | Spring-Utils
 */

package dev.tommasop1804.springutils.response

@MustUseReturnValues
enum class MultiStatusResponseType {
    WebdavXml,
    Map,
    MapGroupedByStatus,
    MapGroupedBySuccessAndFailure
}