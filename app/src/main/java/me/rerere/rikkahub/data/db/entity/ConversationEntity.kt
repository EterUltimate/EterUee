package me.rerere.rikkahub.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ConversationEntity(
    @PrimaryKey val id: String,
    @ColumnInfo("title") val title: String,
    @ColumnInfo("messages") val messages: String,
    @ColumnInfo("create_at") val createAt: Long,
    @ColumnInfo("update_at") val updateAt: Long,
)