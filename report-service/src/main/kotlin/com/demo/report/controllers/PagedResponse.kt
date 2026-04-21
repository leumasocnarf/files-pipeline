package com.demo.report.controllers

import org.springframework.data.domain.Page

/**
 * A simplified page wrapper that exposes only the essential pagination metadata
 * replacing Spring's verbose [org.springframework.data.domain.Page] serialization.
 *
 * @param T the type of elements in this page
 * @property content the items on the current page
 * @property page zero-based page index
 * @property size maximum number of items per page
 * @property totalElements total number of items across all pages
 * @property totalPages total number of pages available
 */
data class PagedResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

/** Converts a Spring [Page] into a [PagedResponse], discarding redundant metadata. */
fun <T : Any> Page<T>.toPagedResponse() = PagedResponse(
    content = content,
    page = number,
    size = size,
    totalElements = totalElements,
    totalPages = totalPages,
)