package com.eterultimate.eteruee.roleplay.`data`.local

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.eterultimate.eteruee.roleplay.`data`.local.dao.BookmarkDAO
import com.eterultimate.eteruee.roleplay.`data`.local.dao.BookmarkDAO_Impl
import com.eterultimate.eteruee.roleplay.`data`.local.dao.CharacterDAO
import com.eterultimate.eteruee.roleplay.`data`.local.dao.CharacterDAO_Impl
import com.eterultimate.eteruee.roleplay.`data`.local.dao.ChatDAO
import com.eterultimate.eteruee.roleplay.`data`.local.dao.ChatDAO_Impl
import com.eterultimate.eteruee.roleplay.`data`.local.dao.GroupDAO
import com.eterultimate.eteruee.roleplay.`data`.local.dao.GroupDAO_Impl
import com.eterultimate.eteruee.roleplay.`data`.local.dao.PresetDAO
import com.eterultimate.eteruee.roleplay.`data`.local.dao.PresetDAO_Impl
import com.eterultimate.eteruee.roleplay.`data`.local.dao.WorldInfoDAO
import com.eterultimate.eteruee.roleplay.`data`.local.dao.WorldInfoDAO_Impl
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class RolePlayDatabase_Impl : RolePlayDatabase() {
  private val _characterDAO: Lazy<CharacterDAO> = lazy {
    CharacterDAO_Impl(this)
  }

  private val _chatDAO: Lazy<ChatDAO> = lazy {
    ChatDAO_Impl(this)
  }

  private val _worldInfoDAO: Lazy<WorldInfoDAO> = lazy {
    WorldInfoDAO_Impl(this)
  }

  private val _groupDAO: Lazy<GroupDAO> = lazy {
    GroupDAO_Impl(this)
  }

  private val _bookmarkDAO: Lazy<BookmarkDAO> = lazy {
    BookmarkDAO_Impl(this)
  }

  private val _presetDAO: Lazy<PresetDAO> = lazy {
    PresetDAO_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(4, "3336025d1db0908a87c8ced5bbc81afb", "4c97db221152df8edce7f3b0dd978d9f") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `rp_characters` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `avatarUrl` TEXT, `favorite` INTEGER NOT NULL, `chatCount` INTEGER NOT NULL, `lastChatAt` INTEGER, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `jsonData` TEXT NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `rp_chats` (`id` TEXT NOT NULL, `characterId` TEXT NOT NULL, `groupId` TEXT, `title` TEXT NOT NULL, `messageCount` INTEGER NOT NULL, `pinned` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `jsonFilePath` TEXT NOT NULL, `activeBranchId` TEXT, `rootNodesJson` TEXT NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `rp_world_infos` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `scanDepth` INTEGER NOT NULL, `scanTrigger` TEXT NOT NULL, `selectiveLogic` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `entriesJson` TEXT NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `rp_groups` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `avatarUrl` TEXT, `membersJson` TEXT NOT NULL, `activeMembersJson` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `rp_bookmarks` (`id` TEXT NOT NULL, `chatId` TEXT NOT NULL, `characterId` TEXT NOT NULL, `messageId` TEXT, `nodeId` TEXT, `title` TEXT NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `color` TEXT NOT NULL, `tagsJson` TEXT NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `rp_presets` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `type` TEXT NOT NULL, `parametersJson` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '3336025d1db0908a87c8ced5bbc81afb')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `rp_characters`")
        connection.execSQL("DROP TABLE IF EXISTS `rp_chats`")
        connection.execSQL("DROP TABLE IF EXISTS `rp_world_infos`")
        connection.execSQL("DROP TABLE IF EXISTS `rp_groups`")
        connection.execSQL("DROP TABLE IF EXISTS `rp_bookmarks`")
        connection.execSQL("DROP TABLE IF EXISTS `rp_presets`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsRpCharacters: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsRpCharacters.put("id", TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpCharacters.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpCharacters.put("avatarUrl", TableInfo.Column("avatarUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpCharacters.put("favorite", TableInfo.Column("favorite", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpCharacters.put("chatCount", TableInfo.Column("chatCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpCharacters.put("lastChatAt", TableInfo.Column("lastChatAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpCharacters.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpCharacters.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpCharacters.put("jsonData", TableInfo.Column("jsonData", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysRpCharacters: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesRpCharacters: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoRpCharacters: TableInfo = TableInfo("rp_characters", _columnsRpCharacters, _foreignKeysRpCharacters, _indicesRpCharacters)
        val _existingRpCharacters: TableInfo = read(connection, "rp_characters")
        if (!_infoRpCharacters.equals(_existingRpCharacters)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |rp_characters(com.eterultimate.eteruee.roleplay.data.local.entity.CharacterEntity).
              | Expected:
              |""".trimMargin() + _infoRpCharacters + """
              |
              | Found:
              |""".trimMargin() + _existingRpCharacters)
        }
        val _columnsRpChats: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsRpChats.put("id", TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpChats.put("characterId", TableInfo.Column("characterId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpChats.put("groupId", TableInfo.Column("groupId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpChats.put("title", TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpChats.put("messageCount", TableInfo.Column("messageCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpChats.put("pinned", TableInfo.Column("pinned", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpChats.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpChats.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpChats.put("jsonFilePath", TableInfo.Column("jsonFilePath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpChats.put("activeBranchId", TableInfo.Column("activeBranchId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpChats.put("rootNodesJson", TableInfo.Column("rootNodesJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysRpChats: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesRpChats: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoRpChats: TableInfo = TableInfo("rp_chats", _columnsRpChats, _foreignKeysRpChats, _indicesRpChats)
        val _existingRpChats: TableInfo = read(connection, "rp_chats")
        if (!_infoRpChats.equals(_existingRpChats)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |rp_chats(com.eterultimate.eteruee.roleplay.data.local.entity.ChatEntity).
              | Expected:
              |""".trimMargin() + _infoRpChats + """
              |
              | Found:
              |""".trimMargin() + _existingRpChats)
        }
        val _columnsRpWorldInfos: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsRpWorldInfos.put("id", TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpWorldInfos.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpWorldInfos.put("description", TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpWorldInfos.put("scanDepth", TableInfo.Column("scanDepth", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpWorldInfos.put("scanTrigger", TableInfo.Column("scanTrigger", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpWorldInfos.put("selectiveLogic", TableInfo.Column("selectiveLogic", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpWorldInfos.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpWorldInfos.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpWorldInfos.put("entriesJson", TableInfo.Column("entriesJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysRpWorldInfos: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesRpWorldInfos: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoRpWorldInfos: TableInfo = TableInfo("rp_world_infos", _columnsRpWorldInfos, _foreignKeysRpWorldInfos, _indicesRpWorldInfos)
        val _existingRpWorldInfos: TableInfo = read(connection, "rp_world_infos")
        if (!_infoRpWorldInfos.equals(_existingRpWorldInfos)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |rp_world_infos(com.eterultimate.eteruee.roleplay.data.local.entity.WorldInfoEntity).
              | Expected:
              |""".trimMargin() + _infoRpWorldInfos + """
              |
              | Found:
              |""".trimMargin() + _existingRpWorldInfos)
        }
        val _columnsRpGroups: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsRpGroups.put("id", TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpGroups.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpGroups.put("description", TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpGroups.put("avatarUrl", TableInfo.Column("avatarUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpGroups.put("membersJson", TableInfo.Column("membersJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpGroups.put("activeMembersJson", TableInfo.Column("activeMembersJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpGroups.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpGroups.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysRpGroups: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesRpGroups: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoRpGroups: TableInfo = TableInfo("rp_groups", _columnsRpGroups, _foreignKeysRpGroups, _indicesRpGroups)
        val _existingRpGroups: TableInfo = read(connection, "rp_groups")
        if (!_infoRpGroups.equals(_existingRpGroups)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |rp_groups(com.eterultimate.eteruee.roleplay.data.local.entity.GroupEntity).
              | Expected:
              |""".trimMargin() + _infoRpGroups + """
              |
              | Found:
              |""".trimMargin() + _existingRpGroups)
        }
        val _columnsRpBookmarks: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsRpBookmarks.put("id", TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpBookmarks.put("chatId", TableInfo.Column("chatId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpBookmarks.put("characterId", TableInfo.Column("characterId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpBookmarks.put("messageId", TableInfo.Column("messageId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpBookmarks.put("nodeId", TableInfo.Column("nodeId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpBookmarks.put("title", TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpBookmarks.put("note", TableInfo.Column("note", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpBookmarks.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpBookmarks.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpBookmarks.put("color", TableInfo.Column("color", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpBookmarks.put("tagsJson", TableInfo.Column("tagsJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysRpBookmarks: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesRpBookmarks: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoRpBookmarks: TableInfo = TableInfo("rp_bookmarks", _columnsRpBookmarks, _foreignKeysRpBookmarks, _indicesRpBookmarks)
        val _existingRpBookmarks: TableInfo = read(connection, "rp_bookmarks")
        if (!_infoRpBookmarks.equals(_existingRpBookmarks)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |rp_bookmarks(com.eterultimate.eteruee.roleplay.data.local.entity.BookmarkEntity).
              | Expected:
              |""".trimMargin() + _infoRpBookmarks + """
              |
              | Found:
              |""".trimMargin() + _existingRpBookmarks)
        }
        val _columnsRpPresets: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsRpPresets.put("id", TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpPresets.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpPresets.put("description", TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpPresets.put("type", TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpPresets.put("parametersJson", TableInfo.Column("parametersJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpPresets.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRpPresets.put("updatedAt", TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysRpPresets: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesRpPresets: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoRpPresets: TableInfo = TableInfo("rp_presets", _columnsRpPresets, _foreignKeysRpPresets, _indicesRpPresets)
        val _existingRpPresets: TableInfo = read(connection, "rp_presets")
        if (!_infoRpPresets.equals(_existingRpPresets)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |rp_presets(com.eterultimate.eteruee.roleplay.data.local.entity.PresetEntity).
              | Expected:
              |""".trimMargin() + _infoRpPresets + """
              |
              | Found:
              |""".trimMargin() + _existingRpPresets)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "rp_characters", "rp_chats", "rp_world_infos", "rp_groups", "rp_bookmarks", "rp_presets")
  }

  public override fun clearAllTables() {
    super.performClear(false, "rp_characters", "rp_chats", "rp_world_infos", "rp_groups", "rp_bookmarks", "rp_presets")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(CharacterDAO::class, CharacterDAO_Impl.getRequiredConverters())
    _typeConvertersMap.put(ChatDAO::class, ChatDAO_Impl.getRequiredConverters())
    _typeConvertersMap.put(WorldInfoDAO::class, WorldInfoDAO_Impl.getRequiredConverters())
    _typeConvertersMap.put(GroupDAO::class, GroupDAO_Impl.getRequiredConverters())
    _typeConvertersMap.put(BookmarkDAO::class, BookmarkDAO_Impl.getRequiredConverters())
    _typeConvertersMap.put(PresetDAO::class, PresetDAO_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun characterDao(): CharacterDAO = _characterDAO.value

  public override fun chatDao(): ChatDAO = _chatDAO.value

  public override fun worldInfoDao(): WorldInfoDAO = _worldInfoDAO.value

  public override fun groupDao(): GroupDAO = _groupDAO.value

  public override fun bookmarkDao(): BookmarkDAO = _bookmarkDAO.value

  public override fun presetDao(): PresetDAO = _presetDAO.value
}
