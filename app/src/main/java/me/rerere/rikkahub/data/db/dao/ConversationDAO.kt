package me.rerere.rikkahub.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import me.rerere.rikkahub.data.db.entity.ConversationEntity

@Dao
interface ConversationDAO {
    @Query("SELECT * FROM conversationentity")
    suspend fun getAll(): List<ConversationEntity>

    @Query("SELECT * FROM conversationentity WHERE id = :id")
    suspend fun getById(id: String): ConversationEntity?

    @Insert
    suspend fun insert(conversation: ConversationEntity)

    @Delete
    suspend fun delete(conversation: ConversationEntity)
}