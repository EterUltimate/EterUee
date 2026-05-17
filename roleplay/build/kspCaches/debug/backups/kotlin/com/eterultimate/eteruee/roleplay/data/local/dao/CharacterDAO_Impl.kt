package com.eterultimate.eteruee.roleplay.`data`.local.dao

import androidx.paging.PagingSource
import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.RoomRawQuery
import androidx.room.coroutines.createFlow
import androidx.room.paging.LimitOffsetPagingSource
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.eterultimate.eteruee.roleplay.`data`.local.entity.CharacterEntity
import javax.`annotation`.processing.Generated
import kotlin.Boolean
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
public class CharacterDAO_Impl(
  __db: RoomDatabase,
) : CharacterDAO {
  private val __db: RoomDatabase

  private val __insertAdapterOfCharacterEntity: EntityInsertAdapter<CharacterEntity>

  private val __deleteAdapterOfCharacterEntity: EntityDeleteOrUpdateAdapter<CharacterEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfCharacterEntity = object : EntityInsertAdapter<CharacterEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `rp_characters` (`id`,`name`,`avatarUrl`,`favorite`,`chatCount`,`lastChatAt`,`createdAt`,`updatedAt`,`jsonData`) VALUES (?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: CharacterEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        val _tmpAvatarUrl: String? = entity.avatarUrl
        if (_tmpAvatarUrl == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpAvatarUrl)
        }
        val _tmp: Int = if (entity.favorite) 1 else 0
        statement.bindLong(4, _tmp.toLong())
        statement.bindLong(5, entity.chatCount.toLong())
        val _tmpLastChatAt: Long? = entity.lastChatAt
        if (_tmpLastChatAt == null) {
          statement.bindNull(6)
        } else {
          statement.bindLong(6, _tmpLastChatAt)
        }
        statement.bindLong(7, entity.createdAt)
        statement.bindLong(8, entity.updatedAt)
        statement.bindText(9, entity.jsonData)
      }
    }
    this.__deleteAdapterOfCharacterEntity = object : EntityDeleteOrUpdateAdapter<CharacterEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `rp_characters` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: CharacterEntity) {
        statement.bindText(1, entity.id)
      }
    }
  }

  public override suspend fun insertCharacter(entity: CharacterEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfCharacterEntity.insert(_connection, entity)
  }

  public override suspend fun insertCharacters(entities: List<CharacterEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfCharacterEntity.insert(_connection, entities)
  }

  public override suspend fun deleteCharacter(entity: CharacterEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfCharacterEntity.handle(_connection, entity)
  }

  public override fun getAllCharacters(): PagingSource<Int, CharacterEntity> {
    val _sql: String = "SELECT * FROM rp_characters ORDER BY updatedAt DESC"
    val _rawQuery: RoomRawQuery = RoomRawQuery(_sql)
    return object : LimitOffsetPagingSource<CharacterEntity>(_rawQuery, __db, "rp_characters") {
      protected override suspend fun convertRows(limitOffsetQuery: RoomRawQuery, itemCount: Int): List<CharacterEntity> = performSuspending(__db, true, false) { _connection ->
        val _stmt: SQLiteStatement = _connection.prepare(limitOffsetQuery.sql)
        limitOffsetQuery.getBindingFunction().invoke(_stmt)
        try {
          val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
          val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
          val _columnIndexOfAvatarUrl: Int = getColumnIndexOrThrow(_stmt, "avatarUrl")
          val _columnIndexOfFavorite: Int = getColumnIndexOrThrow(_stmt, "favorite")
          val _columnIndexOfChatCount: Int = getColumnIndexOrThrow(_stmt, "chatCount")
          val _columnIndexOfLastChatAt: Int = getColumnIndexOrThrow(_stmt, "lastChatAt")
          val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
          val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
          val _columnIndexOfJsonData: Int = getColumnIndexOrThrow(_stmt, "jsonData")
          val _result: MutableList<CharacterEntity> = mutableListOf()
          while (_stmt.step()) {
            val _item: CharacterEntity
            val _tmpId: String
            _tmpId = _stmt.getText(_columnIndexOfId)
            val _tmpName: String
            _tmpName = _stmt.getText(_columnIndexOfName)
            val _tmpAvatarUrl: String?
            if (_stmt.isNull(_columnIndexOfAvatarUrl)) {
              _tmpAvatarUrl = null
            } else {
              _tmpAvatarUrl = _stmt.getText(_columnIndexOfAvatarUrl)
            }
            val _tmpFavorite: Boolean
            val _tmp: Int
            _tmp = _stmt.getLong(_columnIndexOfFavorite).toInt()
            _tmpFavorite = _tmp != 0
            val _tmpChatCount: Int
            _tmpChatCount = _stmt.getLong(_columnIndexOfChatCount).toInt()
            val _tmpLastChatAt: Long?
            if (_stmt.isNull(_columnIndexOfLastChatAt)) {
              _tmpLastChatAt = null
            } else {
              _tmpLastChatAt = _stmt.getLong(_columnIndexOfLastChatAt)
            }
            val _tmpCreatedAt: Long
            _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
            val _tmpUpdatedAt: Long
            _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
            val _tmpJsonData: String
            _tmpJsonData = _stmt.getText(_columnIndexOfJsonData)
            _item = CharacterEntity(_tmpId,_tmpName,_tmpAvatarUrl,_tmpFavorite,_tmpChatCount,_tmpLastChatAt,_tmpCreatedAt,_tmpUpdatedAt,_tmpJsonData)
            _result.add(_item)
          }
          _result
        } finally {
          _stmt.close()
        }
      }
    }
  }

  public override fun getAllCharactersFlow(): Flow<List<CharacterEntity>> {
    val _sql: String = "SELECT * FROM rp_characters ORDER BY updatedAt DESC"
    return createFlow(__db, false, arrayOf("rp_characters")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfAvatarUrl: Int = getColumnIndexOrThrow(_stmt, "avatarUrl")
        val _columnIndexOfFavorite: Int = getColumnIndexOrThrow(_stmt, "favorite")
        val _columnIndexOfChatCount: Int = getColumnIndexOrThrow(_stmt, "chatCount")
        val _columnIndexOfLastChatAt: Int = getColumnIndexOrThrow(_stmt, "lastChatAt")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfJsonData: Int = getColumnIndexOrThrow(_stmt, "jsonData")
        val _result: MutableList<CharacterEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: CharacterEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpAvatarUrl: String?
          if (_stmt.isNull(_columnIndexOfAvatarUrl)) {
            _tmpAvatarUrl = null
          } else {
            _tmpAvatarUrl = _stmt.getText(_columnIndexOfAvatarUrl)
          }
          val _tmpFavorite: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfFavorite).toInt()
          _tmpFavorite = _tmp != 0
          val _tmpChatCount: Int
          _tmpChatCount = _stmt.getLong(_columnIndexOfChatCount).toInt()
          val _tmpLastChatAt: Long?
          if (_stmt.isNull(_columnIndexOfLastChatAt)) {
            _tmpLastChatAt = null
          } else {
            _tmpLastChatAt = _stmt.getLong(_columnIndexOfLastChatAt)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpJsonData: String
          _tmpJsonData = _stmt.getText(_columnIndexOfJsonData)
          _item = CharacterEntity(_tmpId,_tmpName,_tmpAvatarUrl,_tmpFavorite,_tmpChatCount,_tmpLastChatAt,_tmpCreatedAt,_tmpUpdatedAt,_tmpJsonData)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getCharacterById(id: String): CharacterEntity? {
    val _sql: String = "SELECT * FROM rp_characters WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfAvatarUrl: Int = getColumnIndexOrThrow(_stmt, "avatarUrl")
        val _columnIndexOfFavorite: Int = getColumnIndexOrThrow(_stmt, "favorite")
        val _columnIndexOfChatCount: Int = getColumnIndexOrThrow(_stmt, "chatCount")
        val _columnIndexOfLastChatAt: Int = getColumnIndexOrThrow(_stmt, "lastChatAt")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfJsonData: Int = getColumnIndexOrThrow(_stmt, "jsonData")
        val _result: CharacterEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpAvatarUrl: String?
          if (_stmt.isNull(_columnIndexOfAvatarUrl)) {
            _tmpAvatarUrl = null
          } else {
            _tmpAvatarUrl = _stmt.getText(_columnIndexOfAvatarUrl)
          }
          val _tmpFavorite: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfFavorite).toInt()
          _tmpFavorite = _tmp != 0
          val _tmpChatCount: Int
          _tmpChatCount = _stmt.getLong(_columnIndexOfChatCount).toInt()
          val _tmpLastChatAt: Long?
          if (_stmt.isNull(_columnIndexOfLastChatAt)) {
            _tmpLastChatAt = null
          } else {
            _tmpLastChatAt = _stmt.getLong(_columnIndexOfLastChatAt)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpJsonData: String
          _tmpJsonData = _stmt.getText(_columnIndexOfJsonData)
          _result = CharacterEntity(_tmpId,_tmpName,_tmpAvatarUrl,_tmpFavorite,_tmpChatCount,_tmpLastChatAt,_tmpCreatedAt,_tmpUpdatedAt,_tmpJsonData)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun searchCharacters(query: String): List<CharacterEntity> {
    val _sql: String = "SELECT * FROM rp_characters WHERE name LIKE '%' || ? || '%' ORDER BY updatedAt DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, query)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfAvatarUrl: Int = getColumnIndexOrThrow(_stmt, "avatarUrl")
        val _columnIndexOfFavorite: Int = getColumnIndexOrThrow(_stmt, "favorite")
        val _columnIndexOfChatCount: Int = getColumnIndexOrThrow(_stmt, "chatCount")
        val _columnIndexOfLastChatAt: Int = getColumnIndexOrThrow(_stmt, "lastChatAt")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfJsonData: Int = getColumnIndexOrThrow(_stmt, "jsonData")
        val _result: MutableList<CharacterEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: CharacterEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpAvatarUrl: String?
          if (_stmt.isNull(_columnIndexOfAvatarUrl)) {
            _tmpAvatarUrl = null
          } else {
            _tmpAvatarUrl = _stmt.getText(_columnIndexOfAvatarUrl)
          }
          val _tmpFavorite: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfFavorite).toInt()
          _tmpFavorite = _tmp != 0
          val _tmpChatCount: Int
          _tmpChatCount = _stmt.getLong(_columnIndexOfChatCount).toInt()
          val _tmpLastChatAt: Long?
          if (_stmt.isNull(_columnIndexOfLastChatAt)) {
            _tmpLastChatAt = null
          } else {
            _tmpLastChatAt = _stmt.getLong(_columnIndexOfLastChatAt)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpJsonData: String
          _tmpJsonData = _stmt.getText(_columnIndexOfJsonData)
          _item = CharacterEntity(_tmpId,_tmpName,_tmpAvatarUrl,_tmpFavorite,_tmpChatCount,_tmpLastChatAt,_tmpCreatedAt,_tmpUpdatedAt,_tmpJsonData)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getFavoriteCharacters(): Flow<List<CharacterEntity>> {
    val _sql: String = "SELECT * FROM rp_characters WHERE favorite = 1 ORDER BY updatedAt DESC"
    return createFlow(__db, false, arrayOf("rp_characters")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfAvatarUrl: Int = getColumnIndexOrThrow(_stmt, "avatarUrl")
        val _columnIndexOfFavorite: Int = getColumnIndexOrThrow(_stmt, "favorite")
        val _columnIndexOfChatCount: Int = getColumnIndexOrThrow(_stmt, "chatCount")
        val _columnIndexOfLastChatAt: Int = getColumnIndexOrThrow(_stmt, "lastChatAt")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfJsonData: Int = getColumnIndexOrThrow(_stmt, "jsonData")
        val _result: MutableList<CharacterEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: CharacterEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpAvatarUrl: String?
          if (_stmt.isNull(_columnIndexOfAvatarUrl)) {
            _tmpAvatarUrl = null
          } else {
            _tmpAvatarUrl = _stmt.getText(_columnIndexOfAvatarUrl)
          }
          val _tmpFavorite: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfFavorite).toInt()
          _tmpFavorite = _tmp != 0
          val _tmpChatCount: Int
          _tmpChatCount = _stmt.getLong(_columnIndexOfChatCount).toInt()
          val _tmpLastChatAt: Long?
          if (_stmt.isNull(_columnIndexOfLastChatAt)) {
            _tmpLastChatAt = null
          } else {
            _tmpLastChatAt = _stmt.getLong(_columnIndexOfLastChatAt)
          }
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpJsonData: String
          _tmpJsonData = _stmt.getText(_columnIndexOfJsonData)
          _item = CharacterEntity(_tmpId,_tmpName,_tmpAvatarUrl,_tmpFavorite,_tmpChatCount,_tmpLastChatAt,_tmpCreatedAt,_tmpUpdatedAt,_tmpJsonData)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteCharacterById(id: String) {
    val _sql: String = "DELETE FROM rp_characters WHERE id = ?"
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

  public override suspend fun updateChatCount(
    id: String,
    count: Int,
    lastChatAt: Long?,
    updatedAt: Long,
  ) {
    val _sql: String = "UPDATE rp_characters SET chatCount = ?, lastChatAt = ?, updatedAt = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, count.toLong())
        _argIndex = 2
        if (lastChatAt == null) {
          _stmt.bindNull(_argIndex)
        } else {
          _stmt.bindLong(_argIndex, lastChatAt)
        }
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
