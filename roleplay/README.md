# RolePlay 模块

> Android 角色扮演聊天应用 - 完整的角色管理、世界书、群组和 AI 聊天功能

## 📖 简介

RolePlay 是一个功能完整的角色扮演聊天模块，支持：
- 🎭 **角色管理** - 创建、编辑、删除角色，支持头像上传
- 📚 **世界书** - 关键词匹配的世界观设定，自动注入对话上下文
- 👥 **群组聊天** - 多角色群聊，成员管理
- 💬 **AI 聊天** - 流式生成、消息管理、重新生成
- 🎨 **Material Design 3** - 现代化 UI 设计

## ✨ 特性

### 核心功能
- ✅ 完整的 CRUD 操作（角色、世界书、群组、聊天）
- ✅ AI 流式响应（SSE）
- ✅ 消息分支与重新生成
- ✅ 关键词自动匹配
- ✅ 头像文件管理
- ✅ JSONL 数据持久化
- ✅ Room 数据库存储

### 技术亮点
- 🏗️ Clean Architecture + MVVM
- 🔄 单向数据流（StateFlow）
- ⚡ Kotlin Coroutines 异步编程
- 💉 Koin 依赖注入
- 🎨 Jetpack Compose 声明式 UI
- 📱 Material Design 3

## 📁 项目结构

```
roleplay/
├── src/main/java/com/eterultimate/eteruee/roleplay/
│   ├── data/
│   │   ├── local/          # 本地数据存储
│   │   │   ├── dao/        # Room DAOs
│   │   │   ├── entity/     # Room Entities
│   │   │   ├── RolePlayDatabase.kt
│   │   │   └── RolePlayFileStorage.kt
│   │   ├── model/          # 数据模型
│   │   │   ├── Character.kt
│   │   │   ├── Chat.kt
│   │   │   ├── Group.kt
│   │   │   ├── WorldInfo.kt
│   │   │   └── Preset.kt
│   │   └── repository/     # 数据仓库
│   ├── di/                 # 依赖注入
│   │   └── RoleplayModule.kt
│   ├── domain/             # 领域层
│   │   └── service/        # 业务服务
│   │       ├── CharacterService.kt
│   │       ├── ChatService.kt
│   │       ├── GroupService.kt
│   │       └── WorldInfoService.kt
│   └── ui/                 # 表现层
│       ├── pages/          # Compose 页面
│       │   ├── character/
│       │   ├── chat/
│       │   ├── group/
│       │   ├── worldinfo/
│       │   └── RolePlayMainPage.kt
│       └── viewmodel/      # ViewModels
│           ├── CharacterListViewModel.kt
│           ├── ChatViewModel.kt
│           ├── GroupEditViewModel.kt
│           └── ...
├── FEATURE_CHECKLIST.md    # 功能清单与测试指南
├── QUICK_START.md          # 快速启动指南
├── ARCHITECTURE.md         # 架构设计文档
└── build.gradle.kts        # Gradle 配置
```

## 🚀 快速开始

### 前置要求
- Android Studio Hedgehog 或更高版本
- JDK 17+
- Android SDK API 24+

### 编译项目

```bash
# 克隆仓库
git clone https://github.com/your-repo/EterUee.git
cd EterUee

# 切换到功能分支
git checkout feature/roleplay

# 编译项目
./gradlew :app:assembleDebug
```

### 运行应用

1. 在 Android Studio 中打开项目
2. 连接设备或启动模拟器
3. 点击 Run 按钮
4. 在主界面找到 **RolePlay** 入口

### 第一个角色

1. 进入 RolePlay 模块
2. 点击 **+** 创建角色
3. 填写角色信息并保存
4. 点击角色开始聊天

详细步骤请查看 [快速启动指南](QUICK_START.md)

## 📚 文档

| 文档 | 说明 |
|------|------|
| [快速启动指南](QUICK_START.md) | 5分钟快速上手 |
| [功能清单](FEATURE_CHECKLIST.md) | 完整功能列表和测试场景 |
| [架构设计](ARCHITECTURE.md) | 技术架构和设计决策 |

## 🧪 测试

### 单元测试

```bash
# 运行所有测试
./gradlew :roleplay:test

# 运行特定测试
./gradlew :roleplay:testDebugUnitTest --tests "*CharacterServiceTest*"
```

### UI 测试

```bash
# 连接设备后运行
./gradlew :roleplay:connectedAndroidTest
```

### 手动测试

参考 [功能清单](FEATURE_CHECKLIST.md) 中的测试场景进行手动测试。

## 🛠️ 技术栈

| 类别 | 技术 |
|------|------|
| UI | Jetpack Compose, Material Design 3 |
| 状态管理 | StateFlow, Kotlin Flow |
| 异步 | Kotlin Coroutines |
| 依赖注入 | Koin |
| 数据库 | Room (SQLite) |
| 文件存储 | JSONL, File I/O |
| 网络 | OkHttp, SSE |
| 图片 | （待集成 Coil/Glide） |
| 构建 | Gradle Kotlin DSL |

## 📊 代码统计

| 指标 | 数量 |
|------|------|
| 总文件数 | 36 |
| 代码行数 | ~5,660 |
| UI 页面 | 7 |
| ViewModels | 7 |
| Services | 4 |
| 数据模型 | 5 |
| DAOs | 4 |

## 🎯 路线图

### 已完成 ✅
- [x] 角色管理（CRUD + 头像）
- [x] 世界书管理（CRUD + 关键词匹配）
- [x] 群组管理（CRUD + 成员管理）
- [x] 聊天功能（发送、接收、流式生成）
- [x] 消息操作（删除、重新生成）
- [x] 数据持久化（Room + JSONL）

### 短期计划 🚧
- [ ] Markdown 渲染支持
- [ ] 代码高亮
- [ ] 图片缓存（Coil）
- [ ] 聊天设置对话框
- [ ] 消息复制功能

### 长期愿景 🔮
- [ ] 角色卡导入/导出（PNG 元数据）
- [ ] 多模型切换
- [ ] 本地模型支持（Ollama）
- [ ] 语音合成（TTS）
- [ ] 聊天记录导出

## 🤝 贡献

欢迎贡献代码！请遵循以下步骤：

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 开启 Pull Request

### 代码规范
- 遵循 Kotlin 官方代码风格
- 使用 4 空格缩进
- 添加必要的注释
- 编写单元测试

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](../LICENSE) 文件

## 🙏 致谢

- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Koin](https://insert-koin.io/)
- [Material Design 3](https://m3.material.io/)

## 📞 联系

- 项目主页: [GitHub](https://github.com/your-repo/EterUee)
- 问题反馈: [Issues](https://github.com/your-repo/EterUee/issues)
- 邮箱: your-email@example.com

---

**Made with ❤️ using Kotlin & Jetpack Compose**
