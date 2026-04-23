package com.researchflow.data.repository

import com.researchflow.data.local.dao.*
import com.researchflow.data.local.entity.*
import com.researchflow.data.preferences.ApiKeyStore
import com.researchflow.data.preferences.SettingsDataStore
import com.researchflow.data.remote.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResearchRepository @Inject constructor(
    private val threadDao: ThreadDao,
    private val messageDao: MessageDao,
    private val sourceDao: SourceDao,
    private val artifactDao: ArtifactDao,
    private val bridgeApi: BridgeApi,
    private val apiKeyStore: ApiKeyStore,
    private val settingsDataStore: SettingsDataStore,
) {
    // Thread operations
    fun getAllThreads(): Flow<List<ThreadEntity>> = threadDao.getAllThreads()
    fun observeThread(id: String): Flow<ThreadEntity?> = threadDao.observeThread(id)
    suspend fun getThread(id: String): ThreadEntity? = threadDao.getThreadById(id)

    suspend fun createThread(query: String, providerId: String, modelId: String): ThreadEntity {
        val thread = ThreadEntity(
            id = UUID.randomUUID().toString(),
            title = query.take(80),
            providerId = providerId,
            modelId = modelId,
            status = ThreadStatus.ACTIVE
        )
        threadDao.insertThread(thread)

        val message = MessageEntity(
            id = UUID.randomUUID().toString(),
            threadId = thread.id,
            role = MessageRole.USER,
            content = query
        )
        messageDao.insertMessage(message)
        return thread
    }

    suspend fun updateThreadStatus(id: String, status: ThreadStatus) {
        threadDao.updateStatus(id, status)
    }

    suspend fun setNotebookId(threadId: String, notebookId: String) {
        threadDao.updateNotebookId(threadId, notebookId)
    }

    // Message operations
    fun getMessages(threadId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForThread(threadId)

    suspend fun addMessage(threadId: String, role: MessageRole, content: String): MessageEntity {
        val msg = MessageEntity(
            id = UUID.randomUUID().toString(),
            threadId = threadId,
            role = role,
            content = content
        )
        messageDao.insertMessage(msg)
        return msg
    }

    // Source operations
    fun getSources(threadId: String): Flow<List<SourceEntity>> =
        sourceDao.getSourcesForThread(threadId)

    suspend fun getSourceCount(threadId: String): Int = sourceDao.getSourceCount(threadId)

    suspend fun addSources(threadId: String, results: List<SearchResult>): List<SourceEntity> {
        val limit = settingsDataStore.sourceLimit.first()
        val currentCount = sourceDao.getSourceCount(threadId)
        val remaining = (limit - currentCount).coerceAtLeast(0)
        if (remaining == 0 || results.isEmpty()) return emptyList()

        val existingUrls = sourceDao.getExistingUrls(
            threadId = threadId,
            urls = results.map { it.url }.distinct()
        ).toSet()

        val newSources = selectInsertableSearchResults(results, existingUrls, remaining).map { result ->
            SourceEntity(
                id = UUID.randomUUID().toString(),
                threadId = threadId,
                url = result.url,
                title = result.title,
                type = when (result.type.lowercase()) {
                    "youtube" -> SourceType.YOUTUBE
                    "article" -> SourceType.ARTICLE
                    "docs" -> SourceType.DOCS
                    else -> SourceType.WEB
                },
                reason = result.reason
            )
        }
        sourceDao.insertSources(newSources)
        return newSources
    }

    // Artifact operations
    fun getArtifacts(threadId: String): Flow<List<ArtifactEntity>> =
        artifactDao.getArtifactsForThread(threadId)

    suspend fun createArtifact(threadId: String, type: ArtifactType): ArtifactEntity {
        val artifact = ArtifactEntity(
            id = UUID.randomUUID().toString(),
            threadId = threadId,
            type = type
        )
        artifactDao.insertArtifact(artifact)
        return artifact
    }

    suspend fun updateArtifactStatus(id: String, status: ArtifactStatus) {
        artifactDao.updateStatus(id, status)
    }

    suspend fun updateArtifactTaskInfo(id: String, taskId: String, status: ArtifactStatus) {
        artifactDao.updateTaskInfo(id, taskId, status)
    }


    // Bridge operations
    suspend fun checkBridgeHealth(): Boolean {
        return try {
            val resp = bridgeApi.healthCheck()
            resp.status == "ok"
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getNotebookAuthStatus(): AuthStatusResponse {
        return bridgeApi.getAuthStatus()
    }

    suspend fun startNotebookLogin(): AuthLoginResponse {
        return bridgeApi.startAuthLogin()
    }

    suspend fun createNotebook(title: String): CreateNotebookResponse {
        return bridgeApi.createNotebook(CreateNotebookRequest(title))
    }

    suspend fun addSourcesToNotebook(notebookId: String, sources: List<SourceEntity>): AddSourcesResponse {
        val urls = sources.map { SourceUrl(it.url, it.title, it.type.name.lowercase()) }
        val resp = bridgeApi.addSources(notebookId, AddSourcesRequest(urls))
        val skippedUrls = resp.skippedUrls.toSet()
        sources
            .filterNot { it.url in skippedUrls }
            .forEach { sourceDao.markAddedToNotebook(it.id) }
        return resp
    }

    suspend fun generateArtifact(notebookId: String, artifactType: String): GenerateArtifactResponse {
        return bridgeApi.generateArtifact(notebookId, GenerateArtifactRequest(artifactType))
    }

    // API key helpers
    fun getApiKey(providerId: String): String? = apiKeyStore.getApiKey(providerId)
    fun getConfiguredProviders(): List<String> = apiKeyStore.getConfiguredProviders()
}

internal fun selectInsertableSearchResults(
    results: List<SearchResult>,
    existingUrls: Set<String>,
    remaining: Int,
): List<SearchResult> {
    if (remaining <= 0) return emptyList()

    val selectedUrls = mutableSetOf<String>()
    return results.asSequence()
        .filter { it.url !in existingUrls }
        .filter { selectedUrls.add(it.url) }
        .take(remaining)
        .toList()
}
