package com.researchflow.ui.chat

import com.researchflow.data.local.entity.*

data class ChatUiState(
    val isLoading: Boolean = false,
    val isResearching: Boolean = false,
    val query: String = "",
    val thread: ThreadEntity? = null,
    val messages: List<MessageEntity> = emptyList(),
    val sources: List<SourceEntity> = emptyList(),
    val artifacts: List<ArtifactEntity> = emptyList(),
    val selectedProvider: String = "",
    val selectedModel: String = "",
    val configuredProviders: List<String> = emptyList(),
    val showArtifactPicker: Boolean = false,
    val error: String? = null,
    val bridgeConnected: Boolean? = null,
    val needsApiKey: Boolean = false
)
