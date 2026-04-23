package com.researchflow.data.repository

import com.researchflow.data.remote.SearchResult
import org.junit.Assert.assertEquals
import org.junit.Test

class SourceSelectionTest {
    @Test
    fun selectInsertableSearchResults_filtersExistingAndFillsRemainingSlots() {
        val results = listOf(
            result("existing", "https://example.com/existing"),
            result("first", "https://example.com/first"),
            result("second", "https://example.com/second"),
            result("third", "https://example.com/third"),
        )

        val selected = selectInsertableSearchResults(
            results = results,
            existingUrls = setOf("https://example.com/existing"),
            remaining = 2,
        )

        assertEquals(
            listOf("https://example.com/first", "https://example.com/second"),
            selected.map { it.url },
        )
    }

    @Test
    fun selectInsertableSearchResults_deduplicatesWithinIncomingResults() {
        val results = listOf(
            result("first", "https://example.com/first"),
            result("first duplicate", "https://example.com/first"),
            result("second", "https://example.com/second"),
        )

        val selected = selectInsertableSearchResults(
            results = results,
            existingUrls = emptySet(),
            remaining = 3,
        )

        assertEquals(
            listOf("https://example.com/first", "https://example.com/second"),
            selected.map { it.url },
        )
    }

    private fun result(title: String, url: String) = SearchResult(
        title = title,
        url = url,
        reason = "Relevant source",
        type = "web",
    )
}
