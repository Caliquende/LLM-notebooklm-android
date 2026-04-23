package com.researchflow.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ArtifactType { AUDIO, REPORT, QUIZ, FLASHCARDS, MIND_MAP, SLIDE_DECK, VIDEO }
enum class ArtifactStatus { PENDING, GENERATING, READY, FAILED }

@Entity(
    tableName = "artifacts",
    foreignKeys = [ForeignKey(
        entity = ThreadEntity::class,
        parentColumns = ["id"],
        childColumns = ["threadId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("threadId")]
)
data class ArtifactEntity(
    @PrimaryKey val id: String,
    val threadId: String,
    val type: ArtifactType,
    val taskId: String? = null,
    val status: ArtifactStatus = ArtifactStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)
