package com.eterultimate.eteruee.roleplay.`data`.local.dao

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.eterultimate.eteruee.roleplay.`data`.local.entity.WorldInfoEntity
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
public class WorldInfoDAO_Impl(
  __db: RoomDatabase,
) : WorldInfoDAO {
  private val __db: RoomDatabase

  private val __insertAdapterOfWorldInfoEntity: EntityInsertAdapter<WorldInfoEntity>

  private val __deleteAdapterOfWorldInfoEntity: EntityDeleteOrUpdateAdapter<WorldInfoEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfWorldInfoEntity = object : EntityInsertAdapter<WorldInfoEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `rp_world_infos` (`id`,`name`,`description`,`scanDepth`,`scanTrigger`,`selectiveLogic`,`createdAt`,`updatedAt`,`entriesJson`) VALUES (?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: WorldInfoEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.description)
        statement.bindLong(4, entity.scanDepth.toLong())
        statement.bindText(5, entity.scanTrigger)
        statement.bindText(6, entity.selectiveLogic)
        statement.bindLong(7, entity.createdAt)
        statement.bindLong(8, entity.updatedAt)
        statement.bindText(9, entity.entriesJson)
      }
    }
    this.__deleteAdapterOfWorldInfoEntity = object : EntityDeleteOrUpdateAdapter<WorldInfoEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `rp_world_infos` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: WorldInfoEntity) {
        statement.bindText(1, entity.id)
      }
    }
  }

  public override suspend fun insertWorldInfo(entity: WorldInfoEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfWorldInfoEntity.insert(_connection, entity)
  }

  public override suspend fun deleteWorldInfo(entity: WorldInfoEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfWorldInfoEntity.handle(_connection, entity)
  }

  public override fun getAllWorldInfos(): Flow<List<WorldInfoEntity>> {
    val _sql: String = "SELECT * FROM rp_world_infos ORDER BY updatedAt DESC"
    return createFlow(__db, false, arrayOf("rp_world_infos")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfScanDepth: Int = getColumnIndexOrThrow(_stmt, "scanDepth")
        val _columnIndexOfScanTrigger: Int = getColumnIndexOrThrow(_stmt, "scanTrigger")
        val _columnIndexOfSelectiveLogic: Int = getColumnIndexOrThrow(_stmt, "selectiveLogic")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfEntriesJson: Int = getColumnIndexOrThrow(_stmt, "entriesJson")
        val _result: MutableList<WorldInfoEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: WorldInfoEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpScanDepth: Int
          _tmpScanDepth = _stmt.getLong(_columnIndexOfScanDepth).toInt()
          val _tmpScanTrigger: String
          _tmpScanTrigger = _stmt.getText(_columnIndexOfScanTrigger)
          val _tmpSelectiveLogic: String
          _tmpSelectiveLogic = _stmt.getText(_columnIndexOfSelectiveLogic)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpEntriesJson: String
          _tmpEntriesJson = _stmt.getText(_columnIndexOfEntriesJson)
          _item = WorldInfoEntity(_tmpId,_tmpName,_tmpDescription,_tmpScanDepth,_tmpScanTrigger,_tmpSelectiveLogic,_tmpCreatedAt,_tmpUpdatedAt,_tmpEntriesJson)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getWorldInfoById(id: String): WorldInfoEntity? {
    val _sql: String = "SELECT * FROM rp_world_infos WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfScanDepth: Int = getColumnIndexOrThrow(_stmt, "scanDepth")
        val _columnIndexOfScanTrigger: Int = getColumnIndexOrThrow(_stmt, "scanTrigger")
        val _columnIndexOfSelectiveLogic: Int = getColumnIndexOrThrow(_stmt, "selectiveLogic")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfEntriesJson: Int = getColumnIndexOrThrow(_stmt, "entriesJson")
        val _result: WorldInfoEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpScanDepth: Int
          _tmpScanDepth = _stmt.getLong(_columnIndexOfScanDepth).toInt()
          val _tmpScanTrigger: String
          _tmpScanTrigger = _stmt.getText(_columnIndexOfScanTrigger)
          val _tmpSelectiveLogic: String
          _tmpSelectiveLogic = _stmt.getText(_columnIndexOfSelectiveLogic)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpEntriesJson: String
          _tmpEntriesJson = _stmt.getText(_columnIndexOfEntriesJson)
          _result = WorldInfoEntity(_tmpId,_tmpName,_tmpDescription,_tmpScanDepth,_tmpScanTrigger,_tmpSelectiveLogic,_tmpCreatedAt,_tmpUpdatedAt,_tmpEntriesJson)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun searchWorldInfos(query: String): List<WorldInfoEntity> {
    val _sql: String = "SELECT * FROM rp_world_infos WHERE name LIKE '%' || ? || '%' ORDER BY updatedAt DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, query)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfScanDepth: Int = getColumnIndexOrThrow(_stmt, "scanDepth")
        val _columnIndexOfScanTrigger: Int = getColumnIndexOrThrow(_stmt, "scanTrigger")
        val _columnIndexOfSelectiveLogic: Int = getColumnIndexOrThrow(_stmt, "selectiveLogic")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _columnIndexOfEntriesJson: Int = getColumnIndexOrThrow(_stmt, "entriesJson")
        val _result: MutableList<WorldInfoEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: WorldInfoEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpScanDepth: Int
          _tmpScanDepth = _stmt.getLong(_columnIndexOfScanDepth).toInt()
          val _tmpScanTrigger: String
          _tmpScanTrigger = _stmt.getText(_columnIndexOfScanTrigger)
          val _tmpSelectiveLogic: String
          _tmpSelectiveLogic = _stmt.getText(_columnIndexOfSelectiveLogic)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          val _tmpEntriesJson: String
          _tmpEntriesJson = _stmt.getText(_columnIndexOfEntriesJson)
          _item = WorldInfoEntity(_tmpId,_tmpName,_tmpDescription,_tmpScanDepth,_tmpScanTrigger,_tmpSelectiveLogic,_tmpCreatedAt,_tmpUpdatedAt,_tmpEntriesJson)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteWorldInfoById(id: String) {
    val _sql: String = "DELETE FROM rp_world_infos WHERE id = ?"
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
