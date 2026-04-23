package com.researchflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ThreadStatus { ACTIVE, RESEARCHING, COMPLETED, FAILED }

@Entity(tableName = "threads")
data class ThreadEntity(
    @PrimaryKey val id: String,
    val userId: String = "local",
    val title: String,
    val notebookId: String? = null,
    val status: ThreadStatus = ThreadStatus.ACTIVE,
    val providerId: String,
    val modelId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
