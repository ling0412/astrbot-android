package com.ling.astrbot.core.database.di

import android.content.Context
import androidx.room.Room
import com.ling.astrbot.core.database.AstrbotDatabase
import com.ling.astrbot.core.database.dao.ChatMessageDao
import com.ling.astrbot.core.database.dao.ChatSessionDao
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
    fun provideDatabase(@ApplicationContext context: Context): AstrbotDatabase =
        Room.databaseBuilder(context, AstrbotDatabase::class.java, "astrbot.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideChatSessionDao(db: AstrbotDatabase): ChatSessionDao = db.chatSessionDao()

    @Provides
    fun provideChatMessageDao(db: AstrbotDatabase): ChatMessageDao = db.chatMessageDao()
}
