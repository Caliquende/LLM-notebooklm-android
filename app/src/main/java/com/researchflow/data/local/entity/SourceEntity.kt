package com.researchflow.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SourceType { WEB, YOUTUBE, ARTICLE, DOCS }

@Entity(
    tableName = "sources",
    foreignKeys = [ForeignKey(
        entity = ThreadEntity::class,
        parentColumns = ["id"],
        childColumns = ["threadId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("threadId"), Index("url")]
)
data class SourceEntity(
    @PrimaryKey val id: String,
    val threadId: String,
    val url: String,
    val title: String,
    val type: SourceType = SourceType.WEB,
    val reason: String = "",
    val addedToNotebook: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
