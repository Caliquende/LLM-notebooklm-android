package com.researchflow.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.*

// Bridge Server API
interface BridgeApi {
    @GET("health")
    suspend fun healthCheck(): BridgeHealthResponse

    @GET("auth/status")
    suspend fun getAuthStatus(): AuthStatusResponse

    @POST("auth/login")
    suspend fun startAuthLogin(): AuthLoginResponse

    @POST("notebook/create")
    suspend fun createNotebook(@Body request: CreateNotebookRequest): CreateNotebookResponse

    @POST("notebook/{id}/sources")
    suspend fun addSources(
        @Path("id") notebookId: String,
        @Body request: AddSourcesRequest
    ): AddSourcesResponse

    @POST("notebook/{id}/artifacts")
    suspend fun generateArtifact(
        @Path("id") notebookId: String,
        @Body request: GenerateArtifactRequest
    ): GenerateArtifactResponse

    @GET("notebook/{id}/artifacts/{taskId}/status")
    suspend fun getArtifactStatus(
        @Path("id") notebookId: String,
        @Path("taskId") taskId: String
    ): ArtifactStatusResponse
}

// Request/Response models
@JsonClass(generateAdapter = true)
data class BridgeHealthResponse(val status: String, val version: String? = null)

@JsonClass(generateAdapter = true)
data class AuthStatusResponse(
    val authenticated: Boolean,
    val status: String,
    val detail: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthLoginResponse(
    val started: Boolean,
    val alreadyRunning: Boolean = false,
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateNotebookRequest(val title: String)

@JsonClass(generateAdapter = true)
data class CreateNotebookResponse(val notebookId: String, val title: String)

@JsonClass(generateAdapter = true)
data class AddSourcesRequest(val urls: List<SourceUrl>)

@JsonClass(generateAdapter = true)
data class SourceUrl(val url: String, val title: String, val type: String)

@JsonClass(generateAdapter = true)
data class AddSourcesResponse(
    val addedCount: Int,
    val skippedCount: Int,
    val skippedUrls: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GenerateArtifactRequest(val artifactType: String)

@JsonClass(generateAdapter = true)
data class GenerateArtifactResponse(val taskId: String, val artifactType: String, val status: String)

@JsonClass(generateAdapter = true)
data class ArtifactStatusResponse(val taskId: String, val status: String, val artifactId: String? = null)
