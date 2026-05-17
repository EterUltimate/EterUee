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
public class BookmarkDAO_Impl(
  __db: RoomDatabase,
) : BookmarkDAO {
  private val __db: RoomDatabase

  private val __insertAdapterOfBookmarkEntity: EntityInsertAdapter<BookmarkEntity>

  private val __deleteAdapterOfBookmarkEntity: EntityDeleteOrUpdateAdapter<BookmarkEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfBookmarkEntity = object : EntityInsertAdapter<BookmarkEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `rp_bookmarks` (`id`,`chatId`,`characterId`,`messageId`,`nodeId`,`title`,`note`,`createdAt`,`updatedAt`,`color`,`tagsJson`) VALUES (?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: BookmarkEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.chatId)
        statement.bindText(3, entity.characterId)
        val _tmpMessageId: String? = entity.messageId
        if (_tmpMessageId == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpMessageId)
        }
        val _tmpNodeId: String? = entity.nodeId
        if (_tmpNodeId == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpNodeId)
        }
        statement.bindText(6, entity.title)
        statement.bindText(7, entity.note)
        statement.bindLong(8, entity.createdAt)
        statement.bindLong(9, entity.updatedAt)
        statement.bindText(10, entity.color)
        statement.bindText(11, entity.tagsJson)
      }
    }
    this.__deleteAdapterOfBookmarkEntity = object : EntityDeleteOrUpdateAdapter<BookmarkEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `rp_bookmarks` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: BookmarkEntity) {
        statement.bindText(1, entity.id)
      }
    }
  }

  public override suspend fun insertBookmark(entity: BookmarkEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfBookmarkEntity.insert(_connection, entity)
  }

  public override suspend fun insertBookmarks(entities: List<BookmarkEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfBookmarkEntity.insert(_connection, entities)
  }

  public override suspend fun deleteBookmark(entity: BookmarkEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfBookmarkEntity.handle(_connection, entity)
  }

  public override fun getBookmarksByCharacter(characterId: String): Flow<List<BookmarkEntity>> {
    val _sql: String = "SELECT * FROM rp_bookmarks WHERE characterId = ? ORDER BY createdAt DESC"
    return createFlow(__db, false, arrayOf("rp_bookmarks")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, characterId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfChatId: Int = getColumnIndexOrThrow(_stmt, "chatId")
        val _columnIndexOfCharacterId: Int = getColumnIndexOrThrow(_stmt, "characterId")
        val _columnIndexOfMessageId: Int = getColumnIndexOrThrow(_stmt, "messageId")
        val _columnIndexOfNodeId: Int = getColumnIndexOrThrow(_stmt, "nodeId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfColor: Int = getColumnIndexOrThrow(_stmt, "color")
        val _columnIndexOfTagsJson: Int = getColumnIndexOrThrow(_stmt, "tagsJson")
        val _result: MutableList<BookmarkEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: BookmarkEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpChatId: String
          _tmpChatId = _stmt.getText(_columnIndexOfChatId)
          val _tmpCharacterId: String
          _tmpCharacterId = _stmt.getText(_columnIndexOfCharacterId)
          val _tmpMessageId: String?
          if (_stmt.isNull(_columnIndexOfMessageId)) {
            _tmpMessageId = null
          } else {
            _tmpMessageId = _stmt.getText(_columnIndexOfMessageId)
          }
          val _tmpNodeId: String?
          if (_stmt.isNull(_columnIndexOfNodeId)) {
            _tmpNodeId = null
          } else {
            _tmpNodeId = _stmt.getText(_columnIndexOfNodeId)
          }
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpNote: String
          _tmpNote = _stmt.getText(_columnIndexOfNote)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpColor: String
          _tmpColor = _stmt.getText(_columnIndexOfColor)
          val _tmpTagsJson: String
          _tmpTagsJson = _stmt.getText(_columnIndexOfTagsJson)
          _item = BookmarkEntity(_tmpId,_tmpChatId,_tmpCharacterId,_tmpMessageId,_tmpNodeId,_tmpTitle,_tmpNote,_tmpCreatedAt,_tmpUpdatedAt,_tmpColor,_tmpTagsJson)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
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
        val _columnIndexOfCharacterId: Int = getColumnIndexOrThrow(_stmt, "characterId")
        val _columnIndexOfMessageId: Int = getColumnIndexOrThrow(_stmt, "messageId")
        val _columnIndexOfNodeId: Int = getColumnIndexOrThrow(_stmt, "nodeId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfColor: Int = getColumnIndexOrThrow(_stmt, "color")
        val _columnIndexOfTagsJson: Int = getColumnIndexOrThrow(_stmt, "tagsJson")
        val _result: MutableList<BookmarkEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: BookmarkEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpChatId: String
          _tmpChatId = _stmt.getText(_columnIndexOfChatId)
          val _tmpCharacterId: String
          _tmpCharacterId = _stmt.getText(_columnIndexOfCharacterId)
          val _tmpMessageId: String?
          if (_stmt.isNull(_columnIndexOfMessageId)) {
            _tmpMessageId = null
          } else {
            _tmpMessageId = _stmt.getText(_columnIndexOfMessageId)
          }
          val _tmpNodeId: String?
          if (_stmt.isNull(_columnIndexOfNodeId)) {
            _tmpNodeId = null
          } else {
            _tmpNodeId = _stmt.getText(_columnIndexOfNodeId)
          }
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpNote: String
          _tmpNote = _stmt.getText(_columnIndexOfNote)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpColor: String
          _tmpColor = _stmt.getText(_columnIndexOfColor)
          val _tmpTagsJson: String
          _tmpTagsJson = _stmt.getText(_columnIndexOfTagsJson)
          _item = BookmarkEntity(_tmpId,_tmpChatId,_tmpCharacterId,_tmpMessageId,_tmpNodeId,_tmpTitle,_tmpNote,_tmpCreatedAt,_tmpUpdatedAt,_tmpColor,_tmpTagsJson)
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
        val _columnIndexOfCharacterId: Int = getColumnIndexOrThrow(_stmt, "characterId")
        val _columnIndexOfMessageId: Int = getColumnIndexOrThrow(_stmt, "messageId")
        val _columnIndexOfNodeId: Int = getColumnIndexOrThrow(_stmt, "nodeId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfColor: Int = getColumnIndexOrThrow(_stmt, "color")
        val _columnIndexOfTagsJson: Int = getColumnIndexOrThrow(_stmt, "tagsJson")
        val _result: BookmarkEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpChatId: String
          _tmpChatId = _stmt.getText(_columnIndexOfChatId)
          val _tmpCharacterId: String
          _tmpCharacterId = _stmt.getText(_columnIndexOfCharacterId)
          val _tmpMessageId: String?
          if (_stmt.isNull(_columnIndexOfMessageId)) {
            _tmpMessageId = null
          } else {
            _tmpMessageId = _stmt.getText(_columnIndexOfMessageId)
          }
          val _tmpNodeId: String?
          if (_stmt.isNull(_columnIndexOfNodeId)) {
            _tmpNodeId = null
          } else {
            _tmpNodeId = _stmt.getText(_columnIndexOfNodeId)
          }
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpNote: String
          _tmpNote = _stmt.getText(_columnIndexOfNote)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpColor: String
          _tmpColor = _stmt.getText(_columnIndexOfColor)
          val _tmpTagsJson: String
          _tmpTagsJson = _stmt.getText(_columnIndexOfTagsJson)
          _result = BookmarkEntity(_tmpId,_tmpChatId,_tmpCharacterId,_tmpMessageId,_tmpNodeId,_tmpTitle,_tmpNote,_tmpCreatedAt,_tmpUpdatedAt,_tmpColor,_tmpTagsJson)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun searchBookmarks(query: String): List<BookmarkEntity> {
    val _sql: String = "SELECT * FROM rp_bookmarks WHERE title LIKE '%' || ? || '%' OR note LIKE '%' || ? || '%' ORDER BY createdAt DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, query)
        _argIndex = 2
        _stmt.bindText(_argIndex, query)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfChatId: Int = getColumnIndexOrThrow(_stmt, "chatId")
        val _columnIndexOfCharacterId: Int = getColumnIndexOrThrow(_stmt, "characterId")
        val _columnIndexOfMessageId: Int = getColumnIndexOrThrow(_stmt, "messageId")
        val _columnIndexOfNodeId: Int = getColumnIndexOrThrow(_stmt, "nodeId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfColor: Int = getColumnIndexOrThrow(_stmt, "color")
        val _columnIndexOfTagsJson: Int = getColumnIndexOrThrow(_stmt, "tagsJson")
        val _result: MutableList<BookmarkEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: BookmarkEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpChatId: String
          _tmpChatId = _stmt.getText(_columnIndexOfChatId)
          val _tmpCharacterId: String
          _tmpCharacterId = _stmt.getText(_columnIndexOfCharacterId)
          val _tmpMessageId: String?
          if (_stmt.isNull(_columnIndexOfMessageId)) {
            _tmpMessageId = null
          } else {
            _tmpMessageId = _stmt.getText(_columnIndexOfMessageId)
          }
          val _tmpNodeId: String?
          if (_stmt.isNull(_columnIndexOfNodeId)) {
            _tmpNodeId = null
          } else {
            _tmpNodeId = _stmt.getText(_columnIndexOfNodeId)
          }
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpNote: String
          _tmpNote = _stmt.getText(_columnIndexOfNote)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpColor: String
          _tmpColor = _stmt.getText(_columnIndexOfColor)
          val _tmpTagsJson: String
          _tmpTagsJson = _stmt.getText(_columnIndexOfTagsJson)
          _item = BookmarkEntity(_tmpId,_tmpChatId,_tmpCharacterId,_tmpMessageId,_tmpNodeId,_tmpTitle,_tmpNote,_tmpCreatedAt,_tmpUpdatedAt,_tmpColor,_tmpTagsJson)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getBookmarksByTag(tag: String): List<BookmarkEntity> {
    val _sql: String = "SELECT * FROM rp_bookmarks WHERE tagsJson LIKE '%' || ? || '%' ORDER BY createdAt DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, tag)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfChatId: Int = getColumnIndexOrThrow(_stmt, "chatId")
        val _columnIndexOfCharacterId: Int = getColumnIndexOrThrow(_stmt, "characterId")
        val _columnIndexOfMessageId: Int = getColumnIndexOrThrow(_stmt, "messageId")
        val _columnIndexOfNodeId: Int = getColumnIndexOrThrow(_stmt, "nodeId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfNote: Int = getColumnIndexOrThrow(_stmt, "note")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfColor: Int = getColumnIndexOrThrow(_stmt, "color")
        val _columnIndexOfTagsJson: Int = getColumnIndexOrThrow(_stmt, "tagsJson")
        val _result: MutableList<BookmarkEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: BookmarkEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpChatId: String
          _tmpChatId = _stmt.getText(_columnIndexOfChatId)
          val _tmpCharacterId: String
          _tmpCharacterId = _stmt.getText(_columnIndexOfCharacterId)
          val _tmpMessageId: String?
          if (_stmt.isNull(_columnIndexOfMessageId)) {
            _tmpMessageId = null
          } else {
            _tmpMessageId = _stmt.getText(_columnIndexOfMessageId)
          }
          val _tmpNodeId: String?
          if (_stmt.isNull(_columnIndexOfNodeId)) {
            _tmpNodeId = null
          } else {
            _tmpNodeId = _stmt.getText(_columnIndexOfNodeId)
          }
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpNote: String
          _tmpNote = _stmt.getText(_columnIndexOfNote)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpColor: String
          _tmpColor = _stmt.getText(_columnIndexOfColor)
          val _tmpTagsJson: String
          _tmpTagsJson = _stmt.getText(_columnIndexOfTagsJson)
          _item = BookmarkEntity(_tmpId,_tmpChatId,_tmpCharacterId,_tmpMessageId,_tmpNodeId,_tmpTitle,_tmpNote,_tmpCreatedAt,_tmpUpdatedAt,_tmpColor,_tmpTagsJson)
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

  public override suspend fun deleteBookmarksByCharacter(characterId: String) {
    val _sql: String = "DELETE FROM rp_bookmarks WHERE characterId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, characterId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteBookmarksByChat(chatId: String) {
    val _sql: String = "DELETE FROM rp_bookmarks WHERE chatId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, chatId)
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
