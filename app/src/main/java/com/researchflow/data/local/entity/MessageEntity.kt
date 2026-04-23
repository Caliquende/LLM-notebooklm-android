package com.researchflow.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class MessageRole { USER, SYSTEM }

@Entity(
    tableName = "messages",
    foreignKeys = [ForeignKey(
        entity = ThreadEntity::class,
        parentColumns = ["id"],
        childColumns = ["threadId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("threadId")]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val threadId: String,
    val role: MessageRole,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
