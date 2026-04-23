package com.researchflow.di

import android.content.Context
import androidx.room.Room
import com.researchflow.data.local.ResearchFlowDatabase
import com.researchflow.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ResearchFlowDatabase {
        return Room.databaseBuilder(
            context,
            ResearchFlowDatabase::class.java,
            "researchflow.db"
        ).build()
    }

    @Provides fun provideThreadDao(db: ResearchFlowDatabase): ThreadDao = db.threadDao()
    @Provides fun provideMessageDao(db: ResearchFlowDatabase): MessageDao = db.messageDao()
    @Provides fun provideSourceDao(db: ResearchFlowDatabase): SourceDao = db.sourceDao()
    @Provides fun provideArtifactDao(db: ResearchFlowDatabase): ArtifactDao = db.artifactDao()
}
