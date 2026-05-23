package com.eterultimate.eteruee.roleplay.`data`.local.dao

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.eterultimate.eteruee.roleplay.`data`.local.entity.BookmarkEntity
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class BookmarkDao_Impl(
  __db: RoomDatabase,
) : BookmarkDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfBookmarkEntity: EntityInsertAdapter<BookmarkEntity>

  private val __deleteAdapterOfBookmarkEntity: EntityDeleteOrUpdateAdapter<BookmarkEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfBookmarkEntity = object : EntityInsertAdapter<BookmarkEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `rp_bookmarks` (`id`,`chatId`,`messageIndex`,`title`,`note`,`createdAt`,`updatedAt`) VALUES (?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: BookmarkEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.chatId)
        statement.bindLong(3, entity.messageIndex.toLong())
        statement.bindText(4, entity.title)
        statement.bindText(5, entity.note)
        statement.bindLong(6, entity.createdAt)
        statement.bindLong(7, entity.updatedAt)
      }
    }
    this.__deleteAdapterOfBookmarkEntity = object : EntityDeleteOrUpdateAdapter<BookmarkEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `rp_bookmarks` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: BookmarkEntity) {
        statement.bindText(1, entity.id)
      }
    }
  }

  public override suspend fun insertBookmark(bookmark: BookmarkEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfBookmarkEntity.insert(_connection, bookmark)
  }

  public override suspend fun insertBookmarks(bookmarks: List<BookmarkEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfBookmarkEntity.insert(_connection, bookmarks)
  }

  public override suspend fun deleteBookmark(bookmark: BookmarkEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfBookmarkEntity.handle(_connection, bookmark)
  }

  public override fun getBookmarksByChat(chatId: String): Flow<List<BookmarkEntity>> {
    val _sql: String = "SELECT * FROM rp_bookmarks WHERE chatId = ? ORDER BY createdAt DESC"
    return createFlow(__db, false, arrayOf("rp_bookmarks")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, chatId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfChatId: Int = getColumnIndexOrThrow(_stmt, "chatId")
        val _columnIndexOfMessageIndex: Int = getColumnIndexOrThrow(_stmt, "messageIndex")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<BookmarkEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: BookmarkEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpChatId: String
          _tmpChatId = _stmt.getText(_columnIndexOfChatId)
          val _tmpMessageIndex: Int
          _tmpMessageIndex = _stmt.getLong(_columnIndexOfMessageIndex).toInt()
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpNote: String
          _tmpNote = _stmt.getText(_columnIndexOfNote)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = BookmarkEntity(_tmpId,_tmpChatId,_tmpMessageIndex,_tmpTitle,_tmpNote,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getBookmarkById(id: String): BookmarkEntity? {
    val _sql: String = "SELECT * FROM rp_bookmarks WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfChatId: Int = getColumnIndexOrThrow(_stmt, "chatId")
        val _columnIndexOfMessageIndex: Int = getColumnIndexOrThrow(_stmt, "messageIndex")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: BookmarkEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpChatId: String
          _tmpChatId = _stmt.getText(_columnIndexOfChatId)
          val _tmpMessageIndex: Int
          _tmpMessageIndex = _stmt.getLong(_columnIndexOfMessageIndex).toInt()
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpNote: String
          _tmpNote = _stmt.getText(_columnIndexOfNote)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _result = BookmarkEntity(_tmpId,_tmpChatId,_tmpMessageIndex,_tmpTitle,_tmpNote,_tmpCreatedAt,_tmpUpdatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllBookmarks(): Flow<List<BookmarkEntity>> {
    val _sql: String = "SELECT * FROM rp_bookmarks ORDER BY updatedAt DESC"
    return createFlow(__db, false, arrayOf("rp_bookmarks")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfChatId: Int = getColumnIndexOrThrow(_stmt, "chatId")
        val _columnIndexOfMessageIndex: Int = getColumnIndexOrThrow(_stmt, "messageIndex")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<BookmarkEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: BookmarkEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpChatId: String
          _tmpChatId = _stmt.getText(_columnIndexOfChatId)
          val _tmpMessageIndex: Int
          _tmpMessageIndex = _stmt.getLong(_columnIndexOfMessageIndex).toInt()
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpNote: String
          _tmpNote = _stmt.getText(_columnIndexOfNote)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = BookmarkEntity(_tmpId,_tmpChatId,_tmpMessageIndex,_tmpTitle,_tmpNote,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteBookmarkById(id: String) {
    val _sql: String = "DELETE FROM rp_bookmarks WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateBookmark(
    id: String,
    title: String,
    note: String,
    updatedAt: Long,
  ) {
    val _sql: String = "UPDATE rp_bookmarks SET title = ?, note = ?, updatedAt = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, title)
        _argIndex = 2
        _stmt.bindText(_argIndex, note)
        _argIndex = 3
        _stmt.bindLong(_argIndex, updatedAt)
        _argIndex = 4
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
