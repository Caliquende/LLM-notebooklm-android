package com.researchflow.data.local.dao

import androidx.room.*
import com.researchflow.data.local.entity.ArtifactEntity
import com.researchflow.data.local.entity.ArtifactStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtifactDao {
    @Query("SELECT * FROM artifacts WHERE threadId = :threadId ORDER BY createdAt ASC")
    fun getArtifactsForThread(threadId: String): Flow<List<ArtifactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtifact(artifact: ArtifactEntity)

    @Query("UPDATE artifacts SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: ArtifactStatus)

    @Query("UPDATE artifacts SET taskId = :taskId, status = :status WHERE id = :id")
    suspend fun updateTaskInfo(id: String, taskId: String, status: ArtifactStatus)
}
