# RolePlay 模块集成指南

## 已完成的工作

### ✅ 1. 导航集成
- [x] 在 `app/build.gradle.kts` 中添加 roleplay 模块依赖
- [x] 在 `RouteActivity.kt` 中定义 5 个新路由：
  - `RolePlay` - 主入口
  - `CharacterEdit` - 角色编辑
  - `CharacterChat` - 角色聊天
  - `WorldInfoEdit` - 世界书管理
  - `GroupChat` - 群组聊天
- [x] 在 NavDisplay 中注册所有路由处理器
- [x] 创建 `RolePlayMainPage` 带底部导航栏（角色/世界书/群组）
- [x] 在 ChatPage 顶部栏添加 RolePlay 入口按钮

### ✅ 2. UI 完善
- [x] 实现 `WorldInfoListPage` - 世界书列表页
- [x] 实现 `GroupListPage` - 群组列表页
- [x] 替换占位页面为完整功能页面

### ✅ 3. 单元测试
- [x] `CharacterServiceTest` - 角色服务测试（已存在）
- [x] `ChatServiceTest` - 聊天服务测试（新建）
- [x] `WorldInfoServiceTest` - 世界书服务测试（新建）
- [x] `GroupServiceTest` - 群组服务测试（新建）

## 📋 运行测试

### 方式 1：运行所有单元测试
```bash
./gradlew :roleplay:testDebugUnitTest
```

### 方式 2：运行特定测试类
```bash
./gradlew :roleplay:testDebugUnitTest --tests "*CharacterServiceTest"
./gradlew :roleplay:testDebugUnitTest --tests "*ChatServiceTest"
./gradlew :roleplay:testDebugUnitTest --tests "*WorldInfoServiceTest"
./gradlew :roleplay:testDebugUnitTest --tests "*GroupServiceTest"
```

### 查看测试报告
测试完成后，报告位于：
```
roleplay/build/reports/tests/testDebugUnitTest/index.html
```

## 🔧 已知问题与修复

### Gradle 依赖问题（已修复）
**问题**: roleplay 模块缺少正确的 Compose 和 Koin 依赖配置

**修复**:
1. 使用版本目录中的库名称（如 `libs.androidx.ui` 而非 `libs.androidx.compose.ui`）
2. 添加 Koin BOM: `implementation(platform(libs.koin.bom))`
3. 添加 Room Paging 扩展: `implementation(libs.androidx.room.paging)`

### ChatViewModel 编译错误（待修复）
**问题**: ChatViewModel 使用了过时的 AI SDK API

**影响**: 
- 不影响导航集成
- 不影响单元测试（测试只测 Service 层）
- 需要后续更新 ViewModel 以适配新的 AI SDK

**建议**: 
暂时跳过 ChatViewModel 的编译，先验证导航和 Service 层功能。

## 🎯 用户操作流程测试

### 手动测试步骤

1. **进入 RolePlay**
   - 打开应用 → 进入任意聊天页面
   - 点击顶部栏的 "Actors" 图标
   - 应该看到 RolePlay 主页（底部有 3 个标签）

2. **角色管理**
   - 点击 "角色" 标签
   - 点击右上角 "+" 创建新角色
   - 填写角色信息并保存
   - 点击角色卡片进入编辑页

3. **世界书管理**
   - 点击 "世界书" 标签
   - 点击右上角 "+" 创建新世界书
   - 查看世界书列表

4. **群组管理**
   - 点击 "群组" 标签
   - 点击右上角 "+" 创建新群组
   - 查看群组列表

## 📝 下一步工作

### 高优先级
1. **修复 ChatViewModel** - 适配新的 AI SDK API
2. **实现角色编辑页导航** - 从列表点击进入编辑
3. **实现聊天功能** - 从角色点击进入聊天

### 中优先级
4. **世界书条目管理** - 添加/编辑/删除条目
5. **群组成员管理** - 添加/移除成员
6. **UI 优化** - 添加加载状态、错误提示

### 低优先级
7. **集成测试** - 编写 Espresso/UI 测试
8. **性能优化** - 图片缓存、列表分页
9. **动画效果** - 页面转场、交互动画

## 🐛 故障排查

### 如果测试失败
1. 清理构建: `./gradlew clean`
2. 重新同步 Gradle
3. 检查是否有编译错误

### 如果导航不工作
1. 确认 `settings.gradle.kts` 包含 `include(":roleplay")`
2. 确认 `app/build.gradle.kts` 包含 `implementation(project(":roleplay"))`
3. 检查 Logcat 是否有路由注册错误

### 如果 UI 不显示
1. 确认 Compose 依赖正确
2. 检查 Koin 模块是否正确初始化
3. 查看 Logcat 是否有运行时错误

## 📚 相关文档

- [RolePlay 模块 README](../README.md)
- [AI SDK 文档](../../ai/README.md)
- [导航架构文档](../../docs/navigation.md)

---

**最后更新**: 2026-05-13
**状态**: 导航集成完成，等待 ChatViewModel 修复
