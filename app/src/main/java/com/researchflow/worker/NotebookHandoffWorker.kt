package com.researchflow.worker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.researchflow.R
import com.researchflow.ResearchFlowApp
import com.researchflow.data.local.entity.ArtifactStatus
import com.researchflow.data.local.entity.ThreadStatus
import com.researchflow.data.repository.ResearchRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class NotebookHandoffWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: ResearchRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val threadId = inputData.getString("threadId") ?: return Result.failure()

        return try {
            val thread = repository.getThread(threadId) ?: return Result.failure()

            // Create notebook if needed
            val notebookId = thread.notebookId ?: run {
                val resp = repository.createNotebook(thread.title)
                repository.setNotebookId(threadId, resp.notebookId)
                resp.notebookId
            }

            // Get sources not yet added
            // Note: This is a simplified version - in production, collect from Flow
            sendNotification("Araştırma tamamlandı", thread.title)
            repository.updateThreadStatus(threadId, ThreadStatus.COMPLETED)

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                repository.updateThreadStatus(threadId, ThreadStatus.FAILED)
                sendNotification("Araştırma başarısız", "İşlem tamamlanamadı. Yeni chat açıp tekrar dene.")
                Result.failure()
            }
        }
    }

    private fun sendNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(context, ResearchFlowApp.CHANNEL_RESEARCH)
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
