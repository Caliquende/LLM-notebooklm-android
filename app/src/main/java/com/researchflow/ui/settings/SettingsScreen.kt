package com.researchflow.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.researchflow.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayarlar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Surface,
                    titleContentColor = OnSurface
                )
            )
        },
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // API Keys Section
            Text("API Anahtarları", style = MaterialTheme.typography.titleMedium, color = Primary)

            ProviderKeyCard("OpenAI", "openai", uiState, viewModel)
            ProviderKeyCard("Google Gemini", "gemini", uiState, viewModel)
            ProviderKeyCard("Anthropic", "anthropic", uiState, viewModel)
            ProviderKeyCard("Groq", "groq", uiState, viewModel)
            ProviderKeyCard("OpenRouter", "openrouter", uiState, viewModel)


            HorizontalDivider(color = Outline.copy(alpha = 0.3f))

            // Bridge Server Section
            Text("Bridge Server", style = MaterialTheme.typography.titleMedium, color = Primary)

            var bridgeUrl by remember { mutableStateOf(uiState.bridgeUrl) }
            LaunchedEffect(uiState.bridgeUrl) { bridgeUrl = uiState.bridgeUrl }

            OutlinedTextField(
                value = bridgeUrl,
                onValueChange = { bridgeUrl = it },
                label = { Text("Bridge Server URL") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = { viewModel.setBridgeUrl(bridgeUrl) }) {
                        Icon(Icons.Filled.Save, "Kaydet")
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Outline
                )
            )

            // Bridge status
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Circle,
                    null,
                    tint = if (uiState.bridgeConnected) StatusReady else StatusFailed,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (uiState.bridgeConnected) "Bridge bağlı" else "Bridge bağlı değil",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { viewModel.checkBridge() }) {
                    Text("Test Et", color = Primary)
                }
            }

            HorizontalDivider(color = Outline.copy(alpha = 0.3f))

            Text("NotebookLM", style = MaterialTheme.typography.titleMedium, color = Primary)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Circle,
                            null,
                            tint = if (uiState.notebookAuthenticated == true) StatusReady else StatusFailed,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when (uiState.notebookAuthenticated) {
                                true -> "NotebookLM bağlı"
                                false -> "NotebookLM giriş gerekli"
                                null -> "NotebookLM durumu kontrol ediliyor"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurface
                        )
                    }

                    uiState.notebookLoginMessage?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurface.copy(alpha = 0.7f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.startNotebookLogin() },
                            enabled = uiState.bridgeConnected && !uiState.notebookLoginLaunching,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) {
                            if (uiState.notebookLoginLaunching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = OnPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("Google ile giriş")
                        }
                        TextButton(onClick = { viewModel.checkNotebookAuth() }) {
                            Text("Durumu yenile", color = Primary)
                        }
                    }
                }
            }

            HorizontalDivider(color = Outline.copy(alpha = 0.3f))

            // Source Limit
            Text("Kaynak Limiti", style = MaterialTheme.typography.titleMedium, color = Primary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${uiState.sourceLimit}", style = MaterialTheme.typography.headlineMedium, color = OnSurface)
                Spacer(Modifier.width(16.dp))
                Slider(
                    value = uiState.sourceLimit.toFloat(),
                    onValueChange = { viewModel.setSourceLimit(it.toInt()) },
                    valueRange = 5f..50f,
                    steps = 8,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Primary,
                        activeTrackColor = Primary
                    )
                )
            }
        }
    }
}

@Composable
private fun ProviderKeyCard(
    name: String,
    id: String,
    uiState: SettingsUiState,
    viewModel: SettingsViewModel
) {
    val hasKey = uiState.configuredProviders.contains(id)
    var showKey by remember { mutableStateOf(false) }
    var keyInput by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(!hasKey) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.titleSmall, color = OnSurface)
                Spacer(Modifier.weight(1f))
                if (hasKey) {
                    Icon(Icons.Filled.CheckCircle, null, tint = StatusReady, modifier = Modifier.size(20.dp))
                }
            }

            if (editing || !hasKey) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    placeholder = { Text("API anahtarını gir...") },
                    label = { Text("$name API Anahtarı") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    shape = RoundedCornerShape(8.dp),
                    trailingIcon = {
                        Row {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(
                                    if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    "Göster/Gizle"
                                )
                            }
                        }
                    }
                )

                Spacer(Modifier.height(8.dp))
                Row {
                    Button(
                        onClick = {
                            viewModel.saveApiKey(id, keyInput)
                            keyInput = ""
                            editing = false
                        },
                        enabled = keyInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text("Kaydet")
                    }
                    if (hasKey) {
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = {
                            viewModel.removeApiKey(id)
                            keyInput = ""
                        }) {
                            Text("Sil", color = Error)
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { editing = true }) {
                    Text("Anahtarı değiştir", color = Primary)
                }
            }
        }
    }
}
