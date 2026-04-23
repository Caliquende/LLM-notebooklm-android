package com.researchflow.data.local.dao

import androidx.room.*
import com.researchflow.data.local.entity.SourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {
    @Query("SELECT * FROM sources WHERE threadId = :threadId ORDER BY createdAt ASC")
    fun getSourcesForThread(threadId: String): Flow<List<SourceEntity>>

    @Query("SELECT COUNT(*) FROM sources WHERE threadId = :threadId")
    suspend fun getSourceCount(threadId: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM sources WHERE threadId = :threadId AND url = :url)")
    suspend fun sourceExists(threadId: String, url: String): Boolean

    @Query("SELECT url FROM sources WHERE threadId = :threadId AND url IN (:urls)")
    suspend fun getExistingUrls(threadId: String, urls: List<String>): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSource(source: SourceEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSources(sources: List<SourceEntity>)

    @Query("UPDATE sources SET addedToNotebook = 1 WHERE id = :id")
    suspend fun markAddedToNotebook(id: String)
}
