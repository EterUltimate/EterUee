package me.rerere.rikkahub.data.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import me.rerere.rikkahub.data.db.entity.GenMediaEntity

@Dao
interface GenMediaDAO {
    @Query("SELECT * FROM genmediaentity")
    fun getAll(): PagingSource<Int, GenMediaEntity>

    @Insert
    suspend fun insert(media: GenMediaEntity)

    @Query("DELETE FROM genmediaentity WHERE id = :id")
    suspend fun delete(id: Int)
}
