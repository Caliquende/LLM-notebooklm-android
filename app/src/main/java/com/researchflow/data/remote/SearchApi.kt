package com.researchflow.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.*

// Generic LLM search API — provider-agnostic interface
interface SearchApi {
    @POST("search")
    suspend fun search(@Body request: SearchRequest): SearchResponse
}

@JsonClass(generateAdapter = true)
data class SearchRequest(
    val query: String,
    val provider: String,
    val model: String,
    val apiKey: String,
    val maxResults: Int = 15
)

@JsonClass(generateAdapter = true)
data class SearchResponse(
    val query: String,
    val results: List<SearchResult>
)

@JsonClass(generateAdapter = true)
data class SearchResult(
    val title: String,
    val url: String,
    val reason: String,
    val type: String // web, youtube, article, docs
)
