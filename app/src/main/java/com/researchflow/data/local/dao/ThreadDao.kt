package com.researchflow.data.local.dao

import androidx.room.*
import com.researchflow.data.local.entity.ThreadEntity
import com.researchflow.data.local.entity.ThreadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ThreadDao {
    @Query("SELECT * FROM threads ORDER BY updatedAt DESC")
    fun getAllThreads(): Flow<List<ThreadEntity>>

    @Query("SELECT * FROM threads WHERE status = :status ORDER BY updatedAt DESC")
    fun getThreadsByStatus(status: ThreadStatus): Flow<List<ThreadEntity>>

    @Query("SELECT * FROM threads WHERE id = :id")
    suspend fun getThreadById(id: String): ThreadEntity?

    @Query("SELECT * FROM threads WHERE id = :id")
    fun observeThread(id: String): Flow<ThreadEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThread(thread: ThreadEntity)

    @Update
    suspend fun updateThread(thread: ThreadEntity)

    @Query("UPDATE threads SET status = :status, updatedAt = :now WHERE id = :id")
    suspend fun updateStatus(id: String, status: ThreadStatus, now: Long = System.currentTimeMillis())

    @Query("UPDATE threads SET notebookId = :notebookId, updatedAt = :now WHERE id = :id")
    suspend fun updateNotebookId(id: String, notebookId: String, now: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteThread(thread: ThreadEntity)
}
