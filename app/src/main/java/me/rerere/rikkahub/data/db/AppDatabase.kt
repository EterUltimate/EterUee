package me.rerere.rikkahub.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import me.rerere.rikkahub.data.db.dao.ConversationDAO
import me.rerere.rikkahub.data.db.dao.MemoryDAO
import me.rerere.rikkahub.data.db.entity.ConversationEntity
import me.rerere.rikkahub.data.db.entity.MemoryEntity

@Database(
    entities = [ConversationEntity::class, MemoryEntity::class],
    version = 3,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDAO

    abstract fun memoryDao(): MemoryDAO
}