package com.ling.astrbot.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ling.astrbot.core.database.dao.ChatMessageDao
import com.ling.astrbot.core.database.dao.ChatSessionDao
import com.ling.astrbot.core.database.entity.ChatMessageEntity
import com.ling.astrbot.core.database.entity.ChatSessionEntity

@Database(
    entities = [ChatSessionEntity::class, ChatMessageEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class AstrbotDatabase : RoomDatabase() {
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun chatMessageDao(): ChatMessageDao
}
