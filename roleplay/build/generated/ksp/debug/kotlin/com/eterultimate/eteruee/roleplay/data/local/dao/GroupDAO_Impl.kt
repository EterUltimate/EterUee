package com.eterultimate.eteruee.roleplay.`data`.local.dao

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.eterultimate.eteruee.roleplay.`data`.local.entity.GroupEntity
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
public class GroupDAO_Impl(
  __db: RoomDatabase,
) : GroupDAO {
  private val __db: RoomDatabase

  private val __insertAdapterOfGroupEntity: EntityInsertAdapter<GroupEntity>

  private val __deleteAdapterOfGroupEntity: EntityDeleteOrUpdateAdapter<GroupEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfGroupEntity = object : EntityInsertAdapter<GroupEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `rp_groups` (`id`,`name`,`description`,`avatarUrl`,`membersJson`,`activeMembersJson`,`createdAt`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: GroupEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.description)
        val _tmpAvatarUrl: String? = entity.avatarUrl
        if (_tmpAvatarUrl == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpAvatarUrl)
        }
        statement.bindText(5, entity.membersJson)
        statement.bindText(6, entity.activeMembersJson)
        statement.bindLong(7, entity.createdAt)
        statement.bindLong(8, entity.updatedAt)
      }
    }
    this.__deleteAdapterOfGroupEntity = object : EntityDeleteOrUpdateAdapter<GroupEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `rp_groups` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: GroupEntity) {
        statement.bindText(1, entity.id)
      }
    }
  }

  public override suspend fun insertGroup(entity: GroupEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfGroupEntity.insert(_connection, entity)
  }

  public override suspend fun deleteGroup(entity: GroupEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfGroupEntity.handle(_connection, entity)
  }

  public override fun getAllGroups(): Flow<List<GroupEntity>> {
    val _sql: String = "SELECT * FROM rp_groups ORDER BY updatedAt DESC"
    return createFlow(__db, false, arrayOf("rp_groups")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfAvatarUrl: Int = getColumnIndexOrThrow(_stmt, "avatarUrl")
        val _columnIndexOfMembersJson: Int = getColumnIndexOrThrow(_stmt, "membersJson")
        val _columnIndexOfActiveMembersJson: Int = getColumnIndexOrThrow(_stmt, "activeMembersJson")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<GroupEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: GroupEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpAvatarUrl: String?
          if (_stmt.isNull(_columnIndexOfAvatarUrl)) {
            _tmpAvatarUrl = null
          } else {
            _tmpAvatarUrl = _stmt.getText(_columnIndexOfAvatarUrl)
          }
          val _tmpMembersJson: String
          _tmpMembersJson = _stmt.getText(_columnIndexOfMembersJson)
          val _tmpActiveMembersJson: String
          _tmpActiveMembersJson = _stmt.getText(_columnIndexOfActiveMembersJson)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = GroupEntity(_tmpId,_tmpName,_tmpDescription,_tmpAvatarUrl,_tmpMembersJson,_tmpActiveMembersJson,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getGroupById(id: String): GroupEntity? {
    val _sql: String = "SELECT * FROM rp_groups WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfAvatarUrl: Int = getColumnIndexOrThrow(_stmt, "avatarUrl")
        val _columnIndexOfMembersJson: Int = getColumnIndexOrThrow(_stmt, "membersJson")
        val _columnIndexOfActiveMembersJson: Int = getColumnIndexOrThrow(_stmt, "activeMembersJson")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: GroupEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpAvatarUrl: String?
          if (_stmt.isNull(_columnIndexOfAvatarUrl)) {
            _tmpAvatarUrl = null
          } else {
            _tmpAvatarUrl = _stmt.getText(_columnIndexOfAvatarUrl)
          }
          val _tmpMembersJson: String
          _tmpMembersJson = _stmt.getText(_columnIndexOfMembersJson)
          val _tmpActiveMembersJson: String
          _tmpActiveMembersJson = _stmt.getText(_columnIndexOfActiveMembersJson)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _result = GroupEntity(_tmpId,_tmpName,_tmpDescription,_tmpAvatarUrl,_tmpMembersJson,_tmpActiveMembersJson,_tmpCreatedAt,_tmpUpdatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun searchGroups(query: String): List<GroupEntity> {
    val _sql: String = "SELECT * FROM rp_groups WHERE name LIKE '%' || ? || '%' ORDER BY updatedAt DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, query)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfAvatarUrl: Int = getColumnIndexOrThrow(_stmt, "avatarUrl")
        val _columnIndexOfMembersJson: Int = getColumnIndexOrThrow(_stmt, "membersJson")
        val _columnIndexOfActiveMembersJson: Int = getColumnIndexOrThrow(_stmt, "activeMembersJson")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<GroupEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: GroupEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpAvatarUrl: String?
          if (_stmt.isNull(_columnIndexOfAvatarUrl)) {
            _tmpAvatarUrl = null
          } else {
            _tmpAvatarUrl = _stmt.getText(_columnIndexOfAvatarUrl)
          }
          val _tmpMembersJson: String
          _tmpMembersJson = _stmt.getText(_columnIndexOfMembersJson)
          val _tmpActiveMembersJson: String
          _tmpActiveMembersJson = _stmt.getText(_columnIndexOfActiveMembersJson)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = GroupEntity(_tmpId,_tmpName,_tmpDescription,_tmpAvatarUrl,_tmpMembersJson,_tmpActiveMembersJson,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteGroupById(id: String) {
    val _sql: String = "DELETE FROM rp_groups WHERE id = ?"
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

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
