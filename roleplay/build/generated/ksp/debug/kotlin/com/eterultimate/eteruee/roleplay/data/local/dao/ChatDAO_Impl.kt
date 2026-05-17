package com.eterultimate.eteruee.roleplay.`data`.local.dao

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.eterultimate.eteruee.roleplay.`data`.local.entity.ChatEntity
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
public class ChatDAO_Impl(
  __db: RoomDatabase,
) : ChatDAO {
  private val __db: RoomDatabase

  private val __insertAdapterOfChatEntity: EntityInsertAdapter<ChatEntity>

  private val __deleteAdapterOfChatEntity: EntityDeleteOrUpdateAdapter<ChatEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfChatEntity = object : EntityInsertAdapter<ChatEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `rp_chats` (`id`,`characterId`,`groupId`,`title`,`messageCount`,`pinned`,`createdAt`,`updatedAt`,`jsonFilePath`,`activeBranchId`,`rootNodesJson`) VALUES (?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ChatEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.characterId)
        val _tmpGroupId: String? = entity.groupId
        if (_tmpGroupId == null) {
          statement.bindNull(3)
        } else {
          statement.bindText(3, _tmpGroupId)
        }
        statement.bindText(4, entity.title)
        statement.bindLong(5, entity.messageCount.toLong())
        val _tmp: Int = if (entity.pinned) 1 else 0
        statement.bindLong(6, _tmp.toLong())
        statement.bindLong(7, entity.createdAt)
        statement.bindLong(8, entity.updatedAt)
        statement.bindText(9, entity.jsonFilePath)
        val _tmpActiveBranchId: String? = entity.activeBranchId
        if (_tmpActiveBranchId == null) {
          statement.bindNull(10)
        } else {
          statement.bindText(10, _tmpActiveBranchId)
        }
        statement.bindText(11, entity.rootNodesJson)
      }
    }
    this.__deleteAdapterOfChatEntity = object : EntityDeleteOrUpdateAdapter<ChatEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `rp_chats` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: ChatEntity) {
        statement.bindText(1, entity.id)
      }
    }
  }

  public override suspend fun insertChat(entity: ChatEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfChatEntity.insert(_connection, entity)
  }

  public override suspend fun insertChats(entities: List<ChatEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfChatEntity.insert(_connection, entities)
  }

  public override suspend fun deleteChat(entity: ChatEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfChatEntity.handle(_connection, entity)
  }

  public override fun getChatsByCharacter(characterId: String): Flow<List<ChatEntity>> {
    val _sql: String = "SELECT * FROM rp_chats WHERE characterId = ? ORDER BY updatedAt DESC"
    return createFlow(__db, false, arrayOf("rp_chats")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, characterId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCharacterId: Int = getColumnIndexOrThrow(_stmt, "characterId")
        val _columnIndexOfGroupId: Int = getColumnIndexOrThrow(_stmt, "groupId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfMessageCount: Int = getColumnIndexOrThrow(_stmt, "messageCount")
        val _columnIndexOfPinned: Int = getColumnIndexOrThrow(_stmt, "pinned")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfJsonFilePath: Int = getColumnIndexOrThrow(_stmt, "jsonFilePath")
        val _columnIndexOfActiveBranchId: Int = getColumnIndexOrThrow(_stmt, "activeBranchId")
        val _columnIndexOfRootNodesJson: Int = getColumnIndexOrThrow(_stmt, "rootNodesJson")
        val _result: MutableList<ChatEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ChatEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpCharacterId: String
          _tmpCharacterId = _stmt.getText(_columnIndexOfCharacterId)
          val _tmpGroupId: String?
          if (_stmt.isNull(_columnIndexOfGroupId)) {
            _tmpGroupId = null
          } else {
            _tmpGroupId = _stmt.getText(_columnIndexOfGroupId)
          }
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpMessageCount: Int
          _tmpMessageCount = _stmt.getLong(_columnIndexOfMessageCount).toInt()
          val _tmpPinned: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfPinned).toInt()
          _tmpPinned = _tmp != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpJsonFilePath: String
          _tmpJsonFilePath = _stmt.getText(_columnIndexOfJsonFilePath)
          val _tmpActiveBranchId: String?
          if (_stmt.isNull(_columnIndexOfActiveBranchId)) {
            _tmpActiveBranchId = null
          } else {
            _tmpActiveBranchId = _stmt.getText(_columnIndexOfActiveBranchId)
          }
          val _tmpRootNodesJson: String
          _tmpRootNodesJson = _stmt.getText(_columnIndexOfRootNodesJson)
          _item = ChatEntity(_tmpId,_tmpCharacterId,_tmpGroupId,_tmpTitle,_tmpMessageCount,_tmpPinned,_tmpCreatedAt,_tmpUpdatedAt,_tmpJsonFilePath,_tmpActiveBranchId,_tmpRootNodesJson)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getChatsByGroup(groupId: String): Flow<List<ChatEntity>> {
    val _sql: String = "SELECT * FROM rp_chats WHERE groupId = ? ORDER BY updatedAt DESC"
    return createFlow(__db, false, arrayOf("rp_chats")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, groupId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCharacterId: Int = getColumnIndexOrThrow(_stmt, "characterId")
        val _columnIndexOfGroupId: Int = getColumnIndexOrThrow(_stmt, "groupId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfMessageCount: Int = getColumnIndexOrThrow(_stmt, "messageCount")
        val _columnIndexOfPinned: Int = getColumnIndexOrThrow(_stmt, "pinned")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfJsonFilePath: Int = getColumnIndexOrThrow(_stmt, "jsonFilePath")
        val _columnIndexOfActiveBranchId: Int = getColumnIndexOrThrow(_stmt, "activeBranchId")
        val _columnIndexOfRootNodesJson: Int = getColumnIndexOrThrow(_stmt, "rootNodesJson")
        val _result: MutableList<ChatEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ChatEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpCharacterId: String
          _tmpCharacterId = _stmt.getText(_columnIndexOfCharacterId)
          val _tmpGroupId: String?
          if (_stmt.isNull(_columnIndexOfGroupId)) {
            _tmpGroupId = null
          } else {
            _tmpGroupId = _stmt.getText(_columnIndexOfGroupId)
          }
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpMessageCount: Int
          _tmpMessageCount = _stmt.getLong(_columnIndexOfMessageCount).toInt()
          val _tmpPinned: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfPinned).toInt()
          _tmpPinned = _tmp != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpJsonFilePath: String
          _tmpJsonFilePath = _stmt.getText(_columnIndexOfJsonFilePath)
          val _tmpActiveBranchId: String?
          if (_stmt.isNull(_columnIndexOfActiveBranchId)) {
            _tmpActiveBranchId = null
          } else {
            _tmpActiveBranchId = _stmt.getText(_columnIndexOfActiveBranchId)
          }
          val _tmpRootNodesJson: String
          _tmpRootNodesJson = _stmt.getText(_columnIndexOfRootNodesJson)
          _item = ChatEntity(_tmpId,_tmpCharacterId,_tmpGroupId,_tmpTitle,_tmpMessageCount,_tmpPinned,_tmpCreatedAt,_tmpUpdatedAt,_tmpJsonFilePath,_tmpActiveBranchId,_tmpRootNodesJson)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getChatById(id: String): ChatEntity? {
    val _sql: String = "SELECT * FROM rp_chats WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCharacterId: Int = getColumnIndexOrThrow(_stmt, "characterId")
        val _columnIndexOfGroupId: Int = getColumnIndexOrThrow(_stmt, "groupId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfMessageCount: Int = getColumnIndexOrThrow(_stmt, "messageCount")
        val _columnIndexOfPinned: Int = getColumnIndexOrThrow(_stmt, "pinned")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfJsonFilePath: Int = getColumnIndexOrThrow(_stmt, "jsonFilePath")
        val _columnIndexOfActiveBranchId: Int = getColumnIndexOrThrow(_stmt, "activeBranchId")
        val _columnIndexOfRootNodesJson: Int = getColumnIndexOrThrow(_stmt, "rootNodesJson")
        val _result: ChatEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpCharacterId: String
          _tmpCharacterId = _stmt.getText(_columnIndexOfCharacterId)
          val _tmpGroupId: String?
          if (_stmt.isNull(_columnIndexOfGroupId)) {
            _tmpGroupId = null
          } else {
            _tmpGroupId = _stmt.getText(_columnIndexOfGroupId)
          }
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpMessageCount: Int
          _tmpMessageCount = _stmt.getLong(_columnIndexOfMessageCount).toInt()
          val _tmpPinned: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfPinned).toInt()
          _tmpPinned = _tmp != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpJsonFilePath: String
          _tmpJsonFilePath = _stmt.getText(_columnIndexOfJsonFilePath)
          val _tmpActiveBranchId: String?
          if (_stmt.isNull(_columnIndexOfActiveBranchId)) {
            _tmpActiveBranchId = null
          } else {
            _tmpActiveBranchId = _stmt.getText(_columnIndexOfActiveBranchId)
          }
          val _tmpRootNodesJson: String
          _tmpRootNodesJson = _stmt.getText(_columnIndexOfRootNodesJson)
          _result = ChatEntity(_tmpId,_tmpCharacterId,_tmpGroupId,_tmpTitle,_tmpMessageCount,_tmpPinned,_tmpCreatedAt,_tmpUpdatedAt,_tmpJsonFilePath,_tmpActiveBranchId,_tmpRootNodesJson)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getPinnedChats(): Flow<List<ChatEntity>> {
    val _sql: String = "SELECT * FROM rp_chats WHERE pinned = 1 ORDER BY updatedAt DESC"
    return createFlow(__db, false, arrayOf("rp_chats")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCharacterId: Int = getColumnIndexOrThrow(_stmt, "characterId")
        val _columnIndexOfGroupId: Int = getColumnIndexOrThrow(_stmt, "groupId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfMessageCount: Int = getColumnIndexOrThrow(_stmt, "messageCount")
        val _columnIndexOfPinned: Int = getColumnIndexOrThrow(_stmt, "pinned")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfJsonFilePath: Int = getColumnIndexOrThrow(_stmt, "jsonFilePath")
        val _columnIndexOfActiveBranchId: Int = getColumnIndexOrThrow(_stmt, "activeBranchId")
        val _columnIndexOfRootNodesJson: Int = getColumnIndexOrThrow(_stmt, "rootNodesJson")
        val _result: MutableList<ChatEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ChatEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpCharacterId: String
          _tmpCharacterId = _stmt.getText(_columnIndexOfCharacterId)
          val _tmpGroupId: String?
          if (_stmt.isNull(_columnIndexOfGroupId)) {
            _tmpGroupId = null
          } else {
            _tmpGroupId = _stmt.getText(_columnIndexOfGroupId)
          }
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpMessageCount: Int
          _tmpMessageCount = _stmt.getLong(_columnIndexOfMessageCount).toInt()
          val _tmpPinned: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfPinned).toInt()
          _tmpPinned = _tmp != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpJsonFilePath: String
          _tmpJsonFilePath = _stmt.getText(_columnIndexOfJsonFilePath)
          val _tmpActiveBranchId: String?
          if (_stmt.isNull(_columnIndexOfActiveBranchId)) {
            _tmpActiveBranchId = null
          } else {
            _tmpActiveBranchId = _stmt.getText(_columnIndexOfActiveBranchId)
          }
          val _tmpRootNodesJson: String
          _tmpRootNodesJson = _stmt.getText(_columnIndexOfRootNodesJson)
          _item = ChatEntity(_tmpId,_tmpCharacterId,_tmpGroupId,_tmpTitle,_tmpMessageCount,_tmpPinned,_tmpCreatedAt,_tmpUpdatedAt,_tmpJsonFilePath,_tmpActiveBranchId,_tmpRootNodesJson)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun searchChats(query: String): List<ChatEntity> {
    val _sql: String = "SELECT * FROM rp_chats WHERE title LIKE '%' || ? || '%' ORDER BY updatedAt DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, query)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfCharacterId: Int = getColumnIndexOrThrow(_stmt, "characterId")
        val _columnIndexOfGroupId: Int = getColumnIndexOrThrow(_stmt, "groupId")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfMessageCount: Int = getColumnIndexOrThrow(_stmt, "messageCount")
        val _columnIndexOfPinned: Int = getColumnIndexOrThrow(_stmt, "pinned")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfJsonFilePath: Int = getColumnIndexOrThrow(_stmt, "jsonFilePath")
        val _columnIndexOfActiveBranchId: Int = getColumnIndexOrThrow(_stmt, "activeBranchId")
        val _columnIndexOfRootNodesJson: Int = getColumnIndexOrThrow(_stmt, "rootNodesJson")
        val _result: MutableList<ChatEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: ChatEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpCharacterId: String
          _tmpCharacterId = _stmt.getText(_columnIndexOfCharacterId)
          val _tmpGroupId: String?
          if (_stmt.isNull(_columnIndexOfGroupId)) {
            _tmpGroupId = null
          } else {
            _tmpGroupId = _stmt.getText(_columnIndexOfGroupId)
          }
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpMessageCount: Int
          _tmpMessageCount = _stmt.getLong(_columnIndexOfMessageCount).toInt()
          val _tmpPinned: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfPinned).toInt()
          _tmpPinned = _tmp != 0
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpJsonFilePath: String
          _tmpJsonFilePath = _stmt.getText(_columnIndexOfJsonFilePath)
          val _tmpActiveBranchId: String?
          if (_stmt.isNull(_columnIndexOfActiveBranchId)) {
            _tmpActiveBranchId = null
          } else {
            _tmpActiveBranchId = _stmt.getText(_columnIndexOfActiveBranchId)
          }
          val _tmpRootNodesJson: String
          _tmpRootNodesJson = _stmt.getText(_columnIndexOfRootNodesJson)
          _item = ChatEntity(_tmpId,_tmpCharacterId,_tmpGroupId,_tmpTitle,_tmpMessageCount,_tmpPinned,_tmpCreatedAt,_tmpUpdatedAt,_tmpJsonFilePath,_tmpActiveBranchId,_tmpRootNodesJson)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteChatById(id: String) {
    val _sql: String = "DELETE FROM rp_chats WHERE id = ?"
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

  public override suspend fun deleteChatsByCharacter(characterId: String) {
    val _sql: String = "DELETE FROM rp_chats WHERE characterId = ?"
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

  public override suspend fun updateMessageCount(
    id: String,
    count: Int,
    updatedAt: Long,
  ) {
    val _sql: String = "UPDATE rp_chats SET messageCount = ?, updatedAt = ? WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, count.toLong())
        _argIndex = 2
        _stmt.bindLong(_argIndex, updatedAt)
        _argIndex = 3
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
