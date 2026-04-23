package com.researchflow.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.researchflow.data.local.entity.*
import com.researchflow.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    threadId: String? = null,
    onNavigateToSettings: () -> Unit,
    onNavigateToArchive: () -> Unit,
    onNavigateToThread: (String) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(threadId) {
        threadId?.let { viewModel.loadThread(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.thread?.title ?: "ResearchFlow",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    // Bridge status indicator
                    uiState.bridgeConnected?.let { connected ->
                        Icon(
                            Icons.Filled.Circle,
                            contentDescription = if (connected) "Bridge bağlı" else "Bridge bağlı değil",
                            tint = if (connected) StatusReady else StatusFailed,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    IconButton(onClick = { viewModel.startNewChat() }) {
                        Icon(Icons.Filled.Add, "Yeni Chat")
                    }
                    IconButton(onClick = onNavigateToArchive) {
                        Icon(Icons.Filled.History, "Arşiv")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, "Ayarlar")
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
        ) {
            // Messages & Sources area
            Box(modifier = Modifier.weight(1f)) {
                if (uiState.messages.isEmpty() && !uiState.isResearching) {
                    EmptyState()
                } else {
                    MessageList(
                        messages = uiState.messages,
                        sources = uiState.sources,
                        artifacts = uiState.artifacts,
                        isResearching = uiState.isResearching
                    )
                }
            }

            // Error banner
            uiState.error?.let { error ->
                ErrorBanner(error) { viewModel.dismissError() }
            }

            // Needs API key banner
            if (uiState.needsApiKey) {
                ApiKeyBanner(onNavigateToSettings)
            }

            // Artifact picker
            if (uiState.showArtifactPicker && uiState.thread?.status == ThreadStatus.COMPLETED) {
                ArtifactPicker { type -> viewModel.requestArtifact(type) }
            }

            // Provider selector + Input bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Provider chips
                if (uiState.configuredProviders.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        items(uiState.configuredProviders) { provider ->
                            FilterChip(
                                selected = provider == uiState.selectedProvider,
                                onClick = { viewModel.selectProvider(provider) },
                                label = { Text(provider.replaceFirstChar { it.uppercase() }) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryContainer,
                                    selectedLabelColor = OnPrimaryContainer
                                )
                            )
                        }
                    }
                }

                // Input row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = uiState.query,
                        onValueChange = { viewModel.updateQuery(it) },
                        placeholder = { Text("Neyi araştırmak istersin?") },
                        modifier = Modifier.weight(1f),
                        maxLines = 3,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = Outline,
                            focusedContainerColor = SurfaceContainerHigh,
                            unfocusedContainerColor = SurfaceContainerHigh
                        ),
                        enabled = !uiState.isResearching
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = { viewModel.startResearch() },
                        enabled = uiState.query.isNotBlank() && !uiState.isResearching && !uiState.needsApiKey,
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Primary,
                            contentColor = OnPrimary
                        )
                    ) {
                        if (uiState.isResearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = OnPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.Search, "Araştır")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.Science,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = Primary.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Neyi araştırmak istersin?",
                style = MaterialTheme.typography.headlineSmall,
                color = OnSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Bir konu gir, kaynaklar otomatik bulunup\nNotebookLM'e eklenecek.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
private fun MessageList(
    messages: List<MessageEntity>,
    sources: List<SourceEntity>,
    artifacts: List<ArtifactEntity>,
    isResearching: Boolean
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messages) { message ->
            MessageBubble(message)
        }
        if (sources.isNotEmpty()) {
            item { SourcesSection(sources) }
        }
        if (isResearching) {
            item { ResearchingIndicator() }
        }
    }
}

@Composable
private fun MessageBubble(message: MessageEntity) {
    val isUser = message.role == MessageRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) PrimaryContainer else SurfaceContainerHigh,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                color = if (isUser) OnPrimaryContainer else OnSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SourcesSection(sources: List<SourceEntity>) {
    Column {
        Text(
            "Kaynaklar (${sources.size})",
            style = MaterialTheme.typography.titleSmall,
            color = Primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        sources.forEach { source ->
            SourceCard(source)
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SourceCard(source: SourceEntity) {
    val typeColor = when (source.type) {
        SourceType.YOUTUBE -> SourceYoutube
        SourceType.ARTICLE -> SourceArticle
        SourceType.DOCS -> SourceDocs
        SourceType.WEB -> SourceWeb
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(typeColor)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(source.title, style = MaterialTheme.typography.bodySmall, color = OnSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(source.type.name, style = MaterialTheme.typography.labelSmall, color = typeColor)
            }
            if (source.addedToNotebook) {
                Icon(Icons.Filled.CheckCircle, "Eklendi", tint = StatusReady, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ArtifactPicker(onSelect: (ArtifactType) -> Unit) {
    val types = listOf(
        ArtifactType.AUDIO to "Sesli Özet",
        ArtifactType.REPORT to "Briefing Doc",
        ArtifactType.QUIZ to "Quiz",
        ArtifactType.FLASHCARDS to "Flashcards",
        ArtifactType.MIND_MAP to "Mind Map",
        ArtifactType.SLIDE_DECK to "Slide Deck"
    )

    Surface(color = SurfaceContainer) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text("Artifact Üret", style = MaterialTheme.typography.labelLarge, color = Primary)
            Spacer(Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(types) { (type, label) ->
                    AssistChip(
                        onClick = { onSelect(type) },
                        label = { Text(label) },
                        leadingIcon = {
                            Icon(
                                when (type) {
                                    ArtifactType.AUDIO -> Icons.Outlined.Headphones
                                    ArtifactType.REPORT -> Icons.Outlined.Description
                                    ArtifactType.QUIZ -> Icons.Outlined.Quiz
                                    ArtifactType.FLASHCARDS -> Icons.Outlined.Style
                                    ArtifactType.MIND_MAP -> Icons.Outlined.AccountTree
                                    ArtifactType.SLIDE_DECK -> Icons.Outlined.Slideshow
                                    else -> Icons.Outlined.AutoAwesome
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ResearchingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Primary)
        Spacer(Modifier.width(12.dp))
        Text("Araştırılıyor...", color = OnSurface.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ErrorBanner(error: String, onDismiss: () -> Unit) {
    Surface(
        color = Error.copy(alpha = 0.15f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Error, null, tint = Error, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(error, color = Error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Close, "Kapat", tint = Error, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun ApiKeyBanner(onNavigateToSettings: () -> Unit) {
    Surface(
        color = StatusPending.copy(alpha = 0.15f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Key, null, tint = StatusPending, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("API anahtarı gerekli", color = StatusPending, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            TextButton(onClick = onNavigateToSettings) {
                Text("Ayarlar", color = Primary)
            }
        }
    }
}
