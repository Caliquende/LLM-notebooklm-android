package com.researchflow.ui.settings

data class SettingsUiState(
    val bridgeUrl: String = "http://10.0.2.2:8080",
    val bridgeConnected: Boolean = false,
    val notebookAuthenticated: Boolean? = null,
    val notebookAuthDetail: String? = null,
    val notebookLoginLaunching: Boolean = false,
    val notebookLoginMessage: String? = null,
    val sourceLimit: Int = 15,
    val configuredProviders: List<String> = emptyList()
)
