package com.researchflow.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.researchflow.data.preferences.ApiKeyStore
import com.researchflow.data.preferences.SettingsDataStore
import com.researchflow.data.repository.ResearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val apiKeyStore: ApiKeyStore,
    private val settingsDataStore: SettingsDataStore,
    private val repository: ResearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // I-04: Guard to prevent multiple simultaneous bridge health checks
    private var bridgeCheckJob: kotlinx.coroutines.Job? = null

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsDataStore.bridgeUrl.collect { url ->
                _uiState.update { it.copy(bridgeUrl = url) }
            }
        }
        viewModelScope.launch {
            settingsDataStore.sourceLimit.collect { limit ->
                _uiState.update { it.copy(sourceLimit = limit) }
            }
        }
        refreshProviders()
        checkBridge()
        checkNotebookAuth()
    }

    private fun refreshProviders() {
        _uiState.update { it.copy(configuredProviders = apiKeyStore.getConfiguredProviders()) }
    }

    fun saveApiKey(providerId: String, key: String) {
        apiKeyStore.setApiKey(providerId, key)
        refreshProviders()
    }

    fun removeApiKey(providerId: String) {
        apiKeyStore.removeApiKey(providerId)
        refreshProviders()
    }

    fun setBridgeUrl(url: String) {
        viewModelScope.launch {
            settingsDataStore.setBridgeUrl(url)
            checkBridge()
            checkNotebookAuth()
        }
    }

    fun setSourceLimit(limit: Int) {
        viewModelScope.launch { settingsDataStore.setSourceLimit(limit) }
    }

    fun checkBridge() {
        // I-04: Guard — skip if a check is already in-flight
        if (bridgeCheckJob?.isActive == true) return
        bridgeCheckJob = viewModelScope.launch {
            val healthy = repository.checkBridgeHealth()
            _uiState.update { it.copy(bridgeConnected = healthy) }
        }
    }

    fun checkNotebookAuth() {
        viewModelScope.launch {
            try {
                val status = repository.getNotebookAuthStatus()
                _uiState.update {
                    it.copy(
                        notebookAuthenticated = status.authenticated,
                        notebookAuthDetail = status.detail,
                        notebookLoginMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        notebookAuthenticated = false,
                        notebookAuthDetail = e.message,
                        notebookLoginMessage = null
                    )
                }
            }
        }
    }

    fun startNotebookLogin() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    notebookLoginLaunching = true,
                    notebookLoginMessage = "NotebookLM giriş akışı başlatılıyor..."
                )
            }
            try {
                val response = repository.startNotebookLogin()
                _uiState.update {
                    it.copy(
                        notebookLoginLaunching = false,
                        notebookLoginMessage = response.message ?: "Tarayıcıda Google girişini tamamla."
                    )
                }
                delay(3_000)
                checkNotebookAuth()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        notebookLoginLaunching = false,
                        notebookLoginMessage = e.message ?: "NotebookLM giriş akışı başlatılamadı."
                    )
                }
            }
        }
    }
}
