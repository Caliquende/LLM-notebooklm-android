package com.researchflow.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.researchflow.data.local.entity.*
import com.researchflow.data.preferences.ApiKeyStore
import com.researchflow.data.preferences.SettingsDataStore
import com.researchflow.data.remote.SearchRequest
import com.researchflow.data.remote.SearchApi
import com.researchflow.data.repository.ResearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ResearchRepository,
    private val searchApi: SearchApi,
    private val apiKeyStore: ApiKeyStore,
    private val settingsDataStore: SettingsDataStore,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentThreadId: String? = null
    // BUG-002: Track active thread observation jobs to cancel on thread change
    private var threadObserveJob: kotlinx.coroutines.Job? = null

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            val providers = apiKeyStore.getConfiguredProviders()
            val selectedProvider = settingsDataStore.selectedProvider.first().ifBlank {
                providers.firstOrNull() ?: ""
            }
            val selectedModel = settingsDataStore.selectedModel.first().ifBlank {
                getDefaultModel(selectedProvider)
            }

            _uiState.update {
                it.copy(
                    configuredProviders = providers,
                    selectedProvider = selectedProvider,
                    selectedModel = selectedModel,
                    needsApiKey = providers.isEmpty()
                )
            }

            // Check bridge health in background
            launch {
                val healthy = repository.checkBridgeHealth()
                _uiState.update { it.copy(bridgeConnected = healthy) }
            }
        }
    }

    fun loadThread(threadId: String) {
        // BUG-002: Cancel previous observation coroutines before starting new ones
        threadObserveJob?.cancel()
        currentThreadId = threadId
        threadObserveJob = viewModelScope.launch {
            launch {
                repository.observeThread(threadId).collect { thread ->
                    _uiState.update { it.copy(thread = thread) }
                }
            }
            launch {
                repository.getMessages(threadId).collect { messages ->
                    _uiState.update { it.copy(messages = messages) }
                }
            }
            launch {
                repository.getSources(threadId).collect { sources ->
                    _uiState.update { it.copy(sources = sources, showArtifactPicker = sources.isNotEmpty()) }
                }
            }
            launch {
                repository.getArtifacts(threadId).collect { artifacts ->
                    _uiState.update { it.copy(artifacts = artifacts) }
                }
            }
        }
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun selectProvider(provider: String) {
        _uiState.update { it.copy(selectedProvider = provider, selectedModel = getDefaultModel(provider)) }
        viewModelScope.launch {
            settingsDataStore.setSelectedProvider(provider)
            settingsDataStore.setSelectedModel(getDefaultModel(provider))
        }
    }

    fun startResearch() {
        val state = _uiState.value
        if (state.query.isBlank() || state.selectedProvider.isBlank()) return

        val apiKey = apiKeyStore.getApiKey(state.selectedProvider)
        if (apiKey.isNullOrBlank()) {
            _uiState.update { it.copy(needsApiKey = true) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isResearching = true, error = null) }

            try {
                // Create or reuse thread
                val thread = if (currentThreadId != null) {
                    repository.addMessage(currentThreadId!!, MessageRole.USER, state.query)
                    repository.getThread(currentThreadId!!)!!
                } else {
                    val t = repository.createThread(state.query, state.selectedProvider, state.selectedModel)
                    currentThreadId = t.id
                    loadThread(t.id)
                    t
                }

                repository.updateThreadStatus(thread.id, ThreadStatus.RESEARCHING)

                // Web search
                val searchResponse = searchApi.search(
                    SearchRequest(
                        query = state.query,
                        provider = state.selectedProvider,
                        model = state.selectedModel,
                        apiKey = apiKey,
                        maxResults = settingsDataStore.sourceLimit.first()
                    )
                )

                // Save sources (duplicates filtered by repository)
                val newSources = repository.addSources(thread.id, searchResponse.results)

                // System message
                repository.addMessage(
                    thread.id,
                    MessageRole.SYSTEM,
                    "${searchResponse.results.size} kaynak bulundu, ${newSources.size} yeni eklendi."
                )

                // NotebookLM handoff (if bridge is healthy)
                if (_uiState.value.bridgeConnected == true) {
                    try {
                        val notebookId = thread.notebookId
                        if (notebookId == null) {
                            val nbResp = repository.createNotebook(thread.title)
                            repository.setNotebookId(thread.id, nbResp.notebookId)
                            repository.addSourcesToNotebook(nbResp.notebookId, newSources)
                        } else {
                            repository.addSourcesToNotebook(notebookId, newSources)
                        }
                        repository.addMessage(thread.id, MessageRole.SYSTEM, "Kaynaklar NotebookLM'e eklendi.")
                    } catch (e: Exception) {
                        repository.addMessage(thread.id, MessageRole.SYSTEM, "NotebookLM'e eklenemedi: ${e.message}")
                    }
                }

                repository.updateThreadStatus(thread.id, ThreadStatus.COMPLETED)
                _uiState.update { it.copy(isResearching = false, query = "", showArtifactPicker = true) }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isResearching = false,
                        error = formatResearchError(e)
                    )
                }
                currentThreadId?.let { repository.updateThreadStatus(it, ThreadStatus.FAILED) }
            }
        }
    }

    fun requestArtifact(type: ArtifactType) {
        val threadId = currentThreadId ?: return
        viewModelScope.launch {
            val artifact = repository.createArtifact(threadId, type)
            try {
                val thread = repository.getThread(threadId) ?: run {
                    repository.updateArtifactStatus(artifact.id, ArtifactStatus.FAILED)
                    return@launch
                }
                val notebookId = thread.notebookId ?: run {
                    // BUG-003: Surface error instead of silently failing
                    repository.updateArtifactStatus(artifact.id, ArtifactStatus.FAILED)
                    _uiState.update { it.copy(error = "Artifact üretmek için önce NotebookLM bağlantısı gerekli.") }
                    return@launch
                }

                val typeStr = when (type) {
                    ArtifactType.AUDIO -> "audio"
                    ArtifactType.REPORT -> "report"
                    ArtifactType.QUIZ -> "quiz"
                    ArtifactType.FLASHCARDS -> "flashcards"
                    ArtifactType.MIND_MAP -> "mind-map"
                    ArtifactType.SLIDE_DECK -> "slide-deck"
                    ArtifactType.VIDEO -> "video"
                }

                val resp = repository.generateArtifact(notebookId, typeStr)
                // BUG-005: Store taskId so we can query status later
                repository.updateArtifactTaskInfo(artifact.id, resp.taskId, ArtifactStatus.GENERATING)
            } catch (e: Exception) {
                // BUG-003: Mark artifact as failed and surface the error
                repository.updateArtifactStatus(artifact.id, ArtifactStatus.FAILED)
                _uiState.update { it.copy(error = "Artifact üretilemedi: ${e.message}") }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun startNewChat() {
        currentThreadId = null
        _uiState.update {
            it.copy(
                thread = null,
                messages = emptyList(),
                sources = emptyList(),
                artifacts = emptyList(),
                query = "",
                showArtifactPicker = false,
                error = null,
                isResearching = false
            )
        }
    }

    private fun getDefaultModel(provider: String): String = when (provider) {
        "openai" -> "gpt-4o"
        "gemini" -> "gemini-2.0-flash"
        "anthropic" -> "claude-sonnet-4-5" // I-03: updated to current Anthropic naming convention
        "groq" -> "llama-3.3-70b-versatile"
        "openrouter" -> "google/gemma-4-31b-it:free"
        else -> ""

    }

    private fun formatResearchError(error: Exception): String {
        val detail = if (error is HttpException) {
            error.response()?.errorBody()?.string()?.take(500)
        } else {
            error.message
        }?.trim()

        return when {
            detail.isNullOrBlank() -> "Arama tamamlanamadı. Sağlayıcı, model veya API anahtarını kontrol et."
            detail.contains("401", ignoreCase = true) || detail.contains("unauthorized", ignoreCase = true) ->
                "Arama tamamlanamadı: API anahtarı geçersiz veya yetkisiz."
            detail.contains("402", ignoreCase = true) || detail.contains("credits", ignoreCase = true) ->
                "Arama tamamlanamadı: sağlayıcı kredisi/limit problemi var."
            detail.contains("404", ignoreCase = true) || detail.contains("model", ignoreCase = true) ->
                "Arama tamamlanamadı: seçili model desteklenmiyor veya bulunamadı. Detay: $detail"
            detail.contains("429", ignoreCase = true) || detail.contains("rate", ignoreCase = true) ->
                "Arama tamamlanamadı: sağlayıcı rate limit verdi."
            else -> "Arama tamamlanamadı. Detay: $detail"
        }
    }
}
