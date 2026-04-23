package com.researchflow.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.researchflow.ui.archive.ArchiveScreen
import com.researchflow.ui.chat.ChatScreen
import com.researchflow.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable

// Type-safe navigation destinations
@Serializable object ChatDest
@Serializable data class ThreadDest(val threadId: String)
@Serializable object SettingsDest
@Serializable object ArchiveDest

@Composable
fun ResearchFlowNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = ChatDest) {
        composable<ChatDest> {
            ChatScreen(
                onNavigateToSettings = { navController.navigate(SettingsDest) },
                onNavigateToArchive = { navController.navigate(ArchiveDest) },
                onNavigateToThread = { threadId ->
                    navController.navigate(ThreadDest(threadId))
                }
            )
        }
        composable<ThreadDest> { backStackEntry ->
            val dest = backStackEntry.toRoute<ThreadDest>()
            ChatScreen(
                threadId = dest.threadId,
                onNavigateToSettings = { navController.navigate(SettingsDest) },
                onNavigateToArchive = { navController.navigate(ArchiveDest) },
                onNavigateToThread = { }
            )
        }
        composable<SettingsDest> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable<ArchiveDest> {
            ArchiveScreen(
                onBack = { navController.popBackStack() },
                onThreadClick = { threadId ->
                    navController.navigate(ThreadDest(threadId))
                }
            )
        }
    }
}
