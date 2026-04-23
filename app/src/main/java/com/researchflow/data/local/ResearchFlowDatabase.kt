package com.researchflow.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.researchflow.data.local.dao.*
import com.researchflow.data.local.entity.*

@Database(
    entities = [
        ThreadEntity::class,
        MessageEntity::class,
        SourceEntity::class,
        ArtifactEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ResearchFlowDatabase : RoomDatabase() {
    abstract fun threadDao(): ThreadDao
    abstract fun messageDao(): MessageDao
    abstract fun sourceDao(): SourceDao
    abstract fun artifactDao(): ArtifactDao
}
