package com.eterultimate.eteruee.roleplay.`data`.local.dao

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.eterultimate.eteruee.roleplay.`data`.local.entity.PresetEntity
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
public class PresetDAO_Impl(
  __db: RoomDatabase,
) : PresetDAO {
  private val __db: RoomDatabase

  private val __insertAdapterOfPresetEntity: EntityInsertAdapter<PresetEntity>

  private val __deleteAdapterOfPresetEntity: EntityDeleteOrUpdateAdapter<PresetEntity>

  private val __updateAdapterOfPresetEntity: EntityDeleteOrUpdateAdapter<PresetEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfPresetEntity = object : EntityInsertAdapter<PresetEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `rp_presets` (`id`,`name`,`description`,`type`,`parametersJson`,`createdAt`,`updatedAt`) VALUES (?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: PresetEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.description)
        statement.bindText(4, entity.type)
        statement.bindText(5, entity.parametersJson)
        statement.bindLong(6, entity.createdAt)
        statement.bindLong(7, entity.updatedAt)
      }
    }
    this.__deleteAdapterOfPresetEntity = object : EntityDeleteOrUpdateAdapter<PresetEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `rp_presets` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: PresetEntity) {
        statement.bindText(1, entity.id)
      }
    }
    this.__updateAdapterOfPresetEntity = object : EntityDeleteOrUpdateAdapter<PresetEntity>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `rp_presets` SET `id` = ?,`name` = ?,`description` = ?,`type` = ?,`parametersJson` = ?,`createdAt` = ?,`updatedAt` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: PresetEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.description)
        statement.bindText(4, entity.type)
        statement.bindText(5, entity.parametersJson)
        statement.bindLong(6, entity.createdAt)
        statement.bindLong(7, entity.updatedAt)
        statement.bindText(8, entity.id)
      }
    }
  }

  public override suspend fun insertPreset(preset: PresetEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfPresetEntity.insert(_connection, preset)
  }

  public override suspend fun insertPresets(presets: List<PresetEntity>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfPresetEntity.insert(_connection, presets)
  }

  public override suspend fun deletePreset(preset: PresetEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfPresetEntity.handle(_connection, preset)
  }

  public override suspend fun updatePreset(preset: PresetEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfPresetEntity.handle(_connection, preset)
  }

  public override suspend fun getPresetById(id: String): PresetEntity? {
    val _sql: String = "SELECT * FROM rp_presets WHERE id = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfParametersJson: Int = getColumnIndexOrThrow(_stmt, "parametersJson")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: PresetEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpParametersJson: String
          _tmpParametersJson = _stmt.getText(_columnIndexOfParametersJson)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _result = PresetEntity(_tmpId,_tmpName,_tmpDescription,_tmpType,_tmpParametersJson,_tmpCreatedAt,_tmpUpdatedAt)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllPresets(): Flow<List<PresetEntity>> {
    val _sql: String = "SELECT * FROM rp_presets ORDER BY updatedAt DESC"
    return createFlow(__db, false, arrayOf("rp_presets")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfParametersJson: Int = getColumnIndexOrThrow(_stmt, "parametersJson")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<PresetEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: PresetEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpParametersJson: String
          _tmpParametersJson = _stmt.getText(_columnIndexOfParametersJson)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = PresetEntity(_tmpId,_tmpName,_tmpDescription,_tmpType,_tmpParametersJson,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllPresetsList(): List<PresetEntity> {
    val _sql: String = "SELECT * FROM rp_presets ORDER BY updatedAt DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfParametersJson: Int = getColumnIndexOrThrow(_stmt, "parametersJson")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<PresetEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: PresetEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpParametersJson: String
          _tmpParametersJson = _stmt.getText(_columnIndexOfParametersJson)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = PresetEntity(_tmpId,_tmpName,_tmpDescription,_tmpType,_tmpParametersJson,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getPresetsByType(type: String): Flow<List<PresetEntity>> {
    val _sql: String = "SELECT * FROM rp_presets WHERE type = ? ORDER BY updatedAt DESC"
    return createFlow(__db, false, arrayOf("rp_presets")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, type)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfParametersJson: Int = getColumnIndexOrThrow(_stmt, "parametersJson")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<PresetEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: PresetEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpParametersJson: String
          _tmpParametersJson = _stmt.getText(_columnIndexOfParametersJson)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = PresetEntity(_tmpId,_tmpName,_tmpDescription,_tmpType,_tmpParametersJson,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getPresetsByTypeList(type: String): List<PresetEntity> {
    val _sql: String = "SELECT * FROM rp_presets WHERE type = ? ORDER BY updatedAt DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, type)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfParametersJson: Int = getColumnIndexOrThrow(_stmt, "parametersJson")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<PresetEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: PresetEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpParametersJson: String
          _tmpParametersJson = _stmt.getText(_columnIndexOfParametersJson)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = PresetEntity(_tmpId,_tmpName,_tmpDescription,_tmpType,_tmpParametersJson,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun searchPresets(query: String): List<PresetEntity> {
    val _sql: String = "SELECT * FROM rp_presets WHERE name LIKE '%' || ? || '%' OR description LIKE '%' || ? || '%' ORDER BY updatedAt DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, query)
        _argIndex = 2
        _stmt.bindText(_argIndex, query)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfType: Int = getColumnIndexOrThrow(_stmt, "type")
        val _columnIndexOfParametersJson: Int = getColumnIndexOrThrow(_stmt, "parametersJson")
        val _columnIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _columnIndexOfUpdatedAt: Int = getColumnIndexOrThrow(_stmt, "updatedAt")
        val _result: MutableList<PresetEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: PresetEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpType: String
          _tmpType = _stmt.getText(_columnIndexOfType)
          val _tmpParametersJson: String
          _tmpParametersJson = _stmt.getText(_columnIndexOfParametersJson)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt)
          val _tmpUpdatedAt: Long
          _tmpUpdatedAt = _stmt.getLong(_columnIndexOfUpdatedAt)
          _item = PresetEntity(_tmpId,_tmpName,_tmpDescription,_tmpType,_tmpParametersJson,_tmpCreatedAt,_tmpUpdatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getPresetCount(): Int {
    val _sql: String = "SELECT COUNT(*) FROM rp_presets"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getPresetCountByType(type: String): Int {
    val _sql: String = "SELECT COUNT(*) FROM rp_presets WHERE type = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, type)
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deletePresetById(id: String) {
    val _sql: String = "DELETE FROM rp_presets WHERE id = ?"
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
