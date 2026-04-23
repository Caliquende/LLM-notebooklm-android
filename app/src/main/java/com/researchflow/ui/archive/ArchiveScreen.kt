package com.researchflow.ui.archive

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.researchflow.data.local.entity.ThreadEntity
import com.researchflow.data.local.entity.ThreadStatus
import com.researchflow.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    onBack: () -> Unit,
    onThreadClick: (String) -> Unit,
    viewModel: ArchiveViewModel = hiltViewModel()
) {
    val threads by viewModel.threads.collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Araştırma Arşivi") },
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
        if (threads.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.List, null, modifier = Modifier.size(48.dp), tint = OnSurface.copy(alpha = 0.4f))
                    Spacer(Modifier.height(8.dp))
                    Text("Henüz araştırma yok", color = OnSurface.copy(alpha = 0.5f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(threads) { thread ->
                    ThreadCard(thread) { onThreadClick(thread.id) }
                }
            }
        }
    }
}

@Composable
private fun ThreadCard(thread: ThreadEntity, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale("tr")) }
    val statusColor = when (thread.status) {
        ThreadStatus.COMPLETED -> StatusReady
        ThreadStatus.FAILED -> StatusFailed
        ThreadStatus.RESEARCHING -> StatusGenerating
        ThreadStatus.ACTIVE -> StatusPending
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    thread.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = OnSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${thread.providerId} · ${dateFormat.format(Date(thread.createdAt))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnSurface.copy(alpha = 0.5f)
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.Circle, null, tint = statusColor, modifier = Modifier.size(10.dp))
        }
    }
}
