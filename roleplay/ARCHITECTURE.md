# 角色管理模块 - 架构设计文档

## 📐 整体架构

```
┌─────────────────────────────────────────────────┐
│              Presentation Layer                  │
│  (UI + ViewModel)                                │
├─────────────────────────────────────────────────┤
│              Domain Layer                        │
│  (Services + Business Logic)                     │
├─────────────────────────────────────────────────┤
│              Data Layer                          │
│  (Repository + DAO + File Storage)               │
└─────────────────────────────────────────────────┘
```

采用 **Clean Architecture** + **MVVM** 模式，确保：
- ✅ 关注点分离
- ✅ 可测试性
- ✅ 可维护性
- ✅ 可扩展性

---

## 🏗️ 分层详解

### 1. Presentation Layer（表现层）

#### UI Components（UI 组件）

**位置**: `roleplay/src/main/java/.../ui/pages/`

| 页面 | 文件 | 职责 |
|------|------|------|
| 主页面 | `RolePlayMainPage.kt` | 底部导航栏，切换三个标签页 |
| 角色列表 | `CharacterListPage.kt` | 展示角色卡片，搜索，删除 |
| 角色编辑 | `CharacterEditPage.kt` | 创建/编辑角色，头像管理 |
| 世界书列表 | `WorldInfoListPage.kt` | 展示世界书条目 |
| 世界书编辑 | `WorldInfoEditPage.kt` | 创建/编辑世界书 |
| 群组列表 | `GroupListPage.kt` | 展示群组，成员统计 |
| 群组编辑 | `GroupEditPage.kt` | 创建/编辑群组，成员管理 |
| 聊天页面 | `ChatPage.kt` | 消息展示，发送，流式生成 |

**技术栈**:
- Jetpack Compose
- Material Design 3
- StateFlow 响应式更新

#### ViewModels（视图模型）

**位置**: `roleplay/src/main/java/.../ui/viewmodel/`

| ViewModel | 职责 | 关键状态 |
|-----------|------|----------|
| `CharacterListViewModel` | 管理角色列表状态 | `characters: List<Character>` |
| `CharacterEditViewModel` | 管理角色编辑状态 | `character: Character?` |
| `WorldInfoListViewModel` | 管理世界书列表 | `worldInfos: List<WorldInfo>` |
| `WorldInfoEditViewModel` | 管理世界书编辑 | `worldInfo: WorldInfo?` |
| `GroupListViewModel` | 管理群组列表 | `groups: List<Group>` |
| `GroupEditViewModel` | 管理群组编辑 | `group: Group?` |
| `ChatViewModel` | 管理聊天状态 | `messages: List<ChatMessage>` |

**特点**:
- 使用 `StateFlow` 暴露状态
- 通过 Koin 注入 Service
- 处理用户交互和业务逻辑

---

### 2. Domain Layer（领域层）

#### Services（服务）

**位置**: `roleplay/src/main/java/.../domain/service/`

| Service | 接口 | 实现 | 职责 |
|---------|------|------|------|
| CharacterService | `CharacterService.kt` | `CharacterServiceImpl.kt` | 角色 CRUD、头像管理 |
| WorldInfoService | `WorldInfoService.kt` | `WorldInfoServiceImpl.kt` | 世界书 CRUD、关键词匹配 |
| GroupService | `GroupService.kt` | `GroupServiceImpl.kt` | 群组 CRUD、成员管理 |
| ChatService | `ChatService.kt` | `ChatServiceImpl.kt` | 消息 CRUD、AI 流式生成 |

**核心功能**:

**CharacterService**:
```kotlin
interface CharacterService {
    suspend fun getAllCharacters(): List<Character>
    suspend fun getCharacter(id: Uuid): Character?
    suspend fun createCharacter(character: Character): Character
    suspend fun updateCharacter(character: Character): Character
    suspend fun deleteCharacter(id: Uuid)
    suspend fun uploadAvatar(characterId: Uuid, imageBytes: ByteArray): String
}
```

**ChatService**:
```kotlin
interface ChatService {
    suspend fun getMessages(chatId: Uuid): List<ChatMessage>
    suspend fun sendMessage(chatId: Uuid, content: String): ChatMessage
    suspend fun regenerateMessage(messageId: Uuid): ChatMessage
    suspend fun deleteMessage(messageId: Uuid)
    fun streamResponse(chatId: Uuid, userMessage: String): Flow<String>
}
```

**设计原则**:
- 接口与实现分离
- 业务逻辑封装在 Service 层
- 不依赖 Android 框架（便于单元测试）

---

### 3. Data Layer（数据层）

#### Models（数据模型）

**位置**: `roleplay/src/main/java/.../data/model/`

| 模型 | 文件 | 说明 |
|------|------|------|
| Character | `Character.kt` | 角色信息（姓名、描述、性格等） |
| WorldInfo | `WorldInfo.kt` | 世界书条目（关键词、内容） |
| Group | `Group.kt` | 群组信息（名称、成员列表） |
| Chat | `Chat.kt` | 聊天会话和消息 |
| Preset | `Preset.kt` | 预设配置 |

**示例**:
```kotlin
data class Character(
    val id: Uuid = Uuid.random(),
    val name: String,
    val description: String,
    val personality: String,
    val greeting: String,
    val avatarPath: String?,
    val systemPrompt: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

#### Entities（数据库实体）

**位置**: `roleplay/src/main/java/.../data/local/entity/`

Room Entity，与 SQLite 表映射：
- `CharacterEntity`
- `ChatEntity`
- `WorldInfoEntity`
- `GroupEntity`

**转换逻辑**:
```kotlin
// Entity → Model
fun CharacterEntity.toModel(): Character { ... }

// Model → Entity
fun Character.toEntity(): CharacterEntity { ... }
```

#### DAOs（数据访问对象）

**位置**: `roleplay/src/main/java/.../data/local/dao/`

| DAO | 文件 | 主要方法 |
|-----|------|----------|
| CharacterDAO | `CharacterDAO.kt` | `getAll()`, `insert()`, `update()`, `delete()` |
| ChatDAO | `ChatDAO.kt` | `getMessages()`, `insertMessage()`, `deleteMessage()` |
| WorldInfoDAO | `WorldInfoDAO.kt` | `getAll()`, `getByKeyword()`, `insert()`, `update()` |
| GroupDAO | `GroupDAO.kt` | `getAll()`, `insert()`, `update()`, `delete()` |

**特点**:
- 使用 Room 注解
- 支持协程（suspend 函数）
- 返回 Flow 支持响应式查询

#### Database（数据库）

**位置**: `roleplay/src/main/java/.../data/local/RolePlayDatabase.kt`

```kotlin
@Database(
    entities = [
        CharacterEntity::class,
        ChatEntity::class,
        WorldInfoEntity::class,
        GroupEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class RolePlayDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDAO
    abstract fun chatDao(): ChatDAO
    abstract fun worldInfoDao(): WorldInfoDAO
    abstract fun groupDao(): GroupDAO
    
    companion object {
        const val DATABASE_NAME = "roleplay.db"
    }
}
```

#### File Storage（文件存储）

**位置**: `roleplay/src/main/java/.../data/local/RolePlayFileStorage.kt`

**职责**:
- 存储角色头像（PNG/JPG）
- 存储聊天记录（JSONL 格式）
- 管理文件目录结构

**目录结构**:
```
/data/data/com.eterultimate.eteruee/files/roleplay/
├── characters/
│   ├── {uuid}.jsonl       # 角色元数据
│   └── avatars/
│       └── {uuid}.png     # 角色头像
├── chats/
│   └── {chatId}.jsonl     # 聊天记录
├── worldinfos/
│   └── {uuid}.jsonl       # 世界书数据
└── groups/
    └── {uuid}.jsonl       # 群组数据
```

**JSONL 格式示例**:
```jsonl
{"id":"uuid-1","name":"AI助手","description":"...","createdAt":1234567890}
{"id":"uuid-2","name":"魔法师","description":"...","createdAt":1234567891}
```

---

## 🔌 依赖注入（Koin）

**位置**: `roleplay/src/main/java/.../di/RoleplayModule.kt`

```kotlin
val roleplayModule = module {
    // Database
    single {
        Room.databaseBuilder(
            androidContext(),
            RolePlayDatabase::class.java,
            RolePlayDatabase.DATABASE_NAME
        ).build()
    }
    
    // DAOs
    single { get<RolePlayDatabase>().characterDao() }
    single { get<RolePlayDatabase>().chatDao() }
    single { get<RolePlayDatabase>().worldInfoDao() }
    single { get<RolePlayDatabase>().groupDao() }
    
    // File Storage
    single { RolePlayFileStorage(androidContext()) }
    
    // Services
    single<CharacterService> { CharacterServiceImpl(...) }
    single<ChatService> { ChatServiceImpl(...) }
    single<WorldInfoService> { WorldInfoServiceImpl(...) }
    single<GroupService> { GroupServiceImpl(...) }
    
    // ViewModels
    viewModel { CharacterListViewModel(get()) }
    viewModel { CharacterEditViewModel(get()) }
    viewModel { ChatViewModel(get(), get()) }
    // ... 其他 ViewModel
}
```

**注册到应用**:
```kotlin
// EterUeeApp.kt
modules(appModule, viewModelModule, dataSourceModule, repositoryModule, roleplayModule)
```

---

## 🔄 数据流示例

### 场景：发送聊天消息

```
1. 用户在 ChatPage 输入消息并点击发送
   ↓
2. ChatPage 调用 viewModel.sendMessage(text)
   ↓
3. ChatViewModel 调用 chatService.sendMessage(chatId, text)
   ↓
4. ChatServiceImpl:
   a. 创建用户消息对象
   b. 保存到数据库 (chatDao.insertMessage())
   c. 保存到文件 (fileStorage.saveMessage())
   d. 调用 AI API 获取回复（流式）
   ↓
5. AI 返回流式数据 (Flow<String>)
   ↓
6. ChatServiceImpl 逐块更新消息
   a. 更新数据库
   b. 通过 Flow 发射更新
   ↓
7. ChatViewModel 接收 Flow，更新 uiState
   ↓
8. ChatPage 观察 uiState，自动刷新 UI
   ↓
9. 用户看到逐字显示的 AI 回复
```

**代码示意**:
```kotlin
// ChatViewModel
fun sendMessage(content: String) {
    viewModelScope.launch {
        try {
            // 发送消息
            chatService.sendMessage(currentChatId, content)
            
            // 监听流式响应
            chatService.streamResponse(currentChatId, content)
                .collect { chunk ->
                    _uiState.update { 
                        it.copy(streamingContent = chunk) 
                    }
                }
        } catch (e: Exception) {
            _uiState.update { 
                it.copy(errorMessage = e.message) 
            }
        }
    }
}

// ChatPage
LaunchedEffect(viewModel.uiState) {
    viewModel.uiState.collect { state ->
        // 自动更新 UI
    }
}
```

---

## 🎯 关键设计决策

### 1. 为什么使用 JSONL 而不是 JSON？

**优点**:
- ✅ 增量写入（无需读取整个文件）
- ✅ 追加性能好
- ✅ 适合日志/聊天场景
- ✅ 易于解析单条记录

**缺点**:
- ❌ 不支持嵌套结构
- ❌ 需要自定义解析逻辑

### 2. 为什么同时使用 Room 和文件存储？

**Room**:
- 结构化数据查询（搜索、过滤、排序）
- 事务支持
- 类型安全

**文件存储**:
- 大文本/二进制数据（头像、长对话）
- 灵活的数据格式
- 易于备份/导出

### 3. 为什么使用 UUID 而不是自增 ID？

**优点**:
- ✅ 分布式友好
- ✅ 安全性更高（不可猜测）
- ✅ 合并数据时无冲突
- ✅ Kotlin 原生支持 (`kotlin.uuid.Uuid`)

### 4. 为什么使用 Flow 而不是 LiveData？

**优点**:
- ✅ Kotlin 协程原生支持
- ✅ 更强大的操作符
- ✅ 冷流特性（按需执行）
- ✅ 更好的测试性

---

## 🧪 测试策略

### 单元测试

**测试 Service 层**:
```kotlin
@Test
fun testCreateCharacter() = runTest {
    val service = CharacterServiceImpl(...)
    val character = Character(name = "Test", ...)
    
    val result = service.createCharacter(character)
    
    assertEquals(character.name, result.name)
}
```

**测试 ViewModel**:
```kotlin
@Test
fun testSendMessage() = runTest {
    val viewModel = ChatViewModel(mockService, ...)
    
    viewModel.sendMessage("Hello")
    
    assertEquals(1, viewModel.uiState.value.messages.size)
}
```

### 集成测试

**测试数据库**:
```kotlin
@Test
fun testDatabaseInsert() = runTest {
    val dao = database.characterDao()
    
    dao.insert(characterEntity)
    
    val result = dao.getAll()
    assertEquals(1, result.size)
}
```

### UI 测试

**测试页面交互**:
```kotlin
@Test
fun testSendButton() {
    composeTestRule.setContent {
        ChatPage(...)
    }
    
    composeTestRule.onNodeWithText("发送").performClick()
    
    composeTestRule.onNodeWithText("Hello").assertIsDisplayed()
}
```

---

## 📊 性能优化

### 1. 列表渲染

- 使用 `LazyColumn` 延迟加载
- 图片异步加载（待实现 Coil/Glide）
- 避免在列表中创建对象

### 2. 数据库查询

- 添加索引（keywords, createdAt）
- 分页查询（limit/offset）
- 使用 Flow 避免重复查询

### 3. 内存管理

- ViewModel 自动清理
- Flow 自动取消协程
- 图片缓存限制

### 4. 网络请求

- SSE 流式传输（减少等待时间）
- 请求去重
- 错误重试机制

---

## 🔒 安全考虑

### 1. 数据存储

- 使用应用私有目录（其他应用无法访问）
- 敏感数据加密（待实现）
- 定期备份提醒

### 2. API Key 管理

- 不在代码中硬编码
- 使用 Android Keystore（待实现）
- 用户自行配置

### 3. 权限管理

- 仅请求必要权限（存储、网络）
- 运行时权限请求
- 权限拒绝优雅降级

---

## 🚀 扩展性设计

### 1. 新增数据类型

**步骤**:
1. 创建 Model 类
2. 创建 Entity 类
3. 创建 DAO 接口
4. 在 Database 中注册
5. 创建 Service
6. 创建 ViewModel
7. 创建 UI 页面

### 2. 更换 AI 提供商

**当前**: 通过 `ai` 模块抽象  
**扩展**: 实现新的 `AiProvider` 接口

### 3. 添加云同步

**方案**:
- 使用 Firebase Firestore
- 或自建后端 API
- 实现 `SyncService` 接口

---

## 📝 总结

角色管理模块采用现代化的 Android 开发架构：

✅ **Clean Architecture** - 清晰的层次划分  
✅ **MVVM** - 关注点分离  
✅ **Jetpack Compose** - 声明式 UI  
✅ **Kotlin Coroutines + Flow** - 异步编程  
✅ **Room** - 类型安全的数据库  
✅ **Koin** - 轻量级依赖注入  
✅ **Material Design 3** - 现代化 UI  

这种设计确保了：
- 🎯 **可维护性** - 代码结构清晰
- 🧪 **可测试性** - 各层独立可测
- 🚀 **可扩展性** - 易于添加新功能
- ⚡ **高性能** - 优化的数据流

---

**下一步**: 
- 阅读 [快速启动指南](QUICK_START.md)
- 查看 [功能清单](FEATURE_CHECKLIST.md)
- 开始编码实践！
