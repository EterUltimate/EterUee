# 角色管理模块 - 快速启动指南

## 🚀 5分钟快速开始

### 1. 编译项目

```bash
# 清理并编译
./gradlew clean :app:assembleDebug

# 或者只编译 roleplay 模块（更快）
./gradlew :roleplay:assembleDebug
```

**预期输出**: `BUILD SUCCESSFUL`

### 2. 安装到设备

```bash
# 通过 ADB 安装
adb install app/build/outputs/apk/debug/app-debug.apk

# 或者在 Android Studio 中点击 Run 按钮
```

### 3. 运行应用

1. 打开应用
2. 在主界面找到 **RolePlay** 入口（通常在聊天页面底部或侧边栏）
3. 点击进入 RolePlay 模块

### 4. 创建第一个角色

1. 确保选中"角色"标签（底部导航栏第一个）
2. 点击右下角的 **+** 按钮（FAB）
3. 填写角色信息：
   ```
   姓名: AI助手
   描述: 一个友好的AI助手
   性格: 乐于助人、专业
   开场白: 你好！我是你的AI助手，有什么可以帮助你的吗？
   ```
4. （可选）点击头像区域上传角色图片
5. 点击顶部工具栏的 **✓** 保存

### 5. 开始聊天

1. 在角色列表中点击刚创建的角色
2. 进入聊天界面
3. 在底部输入框输入消息，例如："你好！"
4. 点击发送按钮（或按回车）
5. 等待 AI 回复（会看到流式生成效果）

### 6. 探索其他功能

#### 世界书
1. 切换到底部导航栏的"世界书"标签
2. 点击 **+** 创建世界书条目
3. 设置关键词和内容
4. 在聊天中发送包含关键词的消息，观察自动注入效果

#### 群组
1. 切换到"群组"标签
2. 点击 **+** 创建群组
3. 添加多个角色作为成员
4. 进入群聊，体验多角色对话

---

## 💡 常用操作速查

### 角色管理

| 操作 | 方法 |
|------|------|
| 创建角色 | 角色列表 → 点击 FAB (+) |
| 编辑角色 | 点击角色卡片 → 修改 → 保存 |
| 删除角色 | 长按角色卡片 → 确认删除 |
| 搜索角色 | 在角色列表顶部搜索框输入关键词 |

### 聊天操作

| 操作 | 方法 |
|------|------|
| 发送消息 | 输入文本 → 点击发送按钮 |
| 重新生成 | 点击 AI 消息下方的"重新生成"按钮 |
| 删除消息 | 点击消息下方的"删除"按钮 |
| 返回角色列表 | 点击左上角返回箭头 |

### 世界书操作

| 操作 | 方法 |
|------|------|
| 创建条目 | 世界书列表 → 点击 FAB (+) |
| 编辑条目 | 点击条目 → 修改 → 保存 |
| 删除条目 | 长按条目 → 确认删除 |
| 激活/停用 | 在编辑页面切换"激活"开关 |

### 群组操作

| 操作 | 方法 |
|------|------|
| 创建群组 | 群组列表 → 点击 FAB (+) |
| 添加成员 | 在编辑页面点击"添加成员" → 选择角色 |
| 移除成员 | 在编辑页面点击成员旁的 ✕ |
| 删除群组 | 长按群组卡片 → 确认删除 |

---

## 🔍 常见问题

### Q1: 编译失败怎么办？

**A**: 检查以下几点：
```bash
# 1. 清理构建缓存
./gradlew clean

# 2. 重新编译
./gradlew :app:assembleDebug

# 3. 查看详细错误日志
./gradlew :app:assembleDebug --stacktrace
```

### Q2: 找不到 RolePlay 入口？

**A**: 
- 检查是否在正确的分支（`feature/roleplay`）
- 查看主界面的底部导航栏或侧边菜单
- 搜索代码中的 `Screen.RolePlay`

### Q3: AI 不回复消息？

**A**: 
- 检查网络连接
- 确认已配置 AI 提供商和 API Key
- 查看 Logcat 是否有错误日志
- 尝试重新生成消息

### Q4: 头像上传失败？

**A**: 
- 检查存储权限是否授予
- 确认图片格式支持（JPG/PNG）
- 检查文件大小（建议 < 5MB）

### Q5: 数据丢失？

**A**: 
- 数据存储在应用私有目录，卸载应用会清除数据
- 建议定期备份数据文件夹：
  ```bash
  adb pull /data/data/com.eterultimate.eteruee/files/roleplay ./backup
  ```

---

## 📱 真机调试技巧

### 查看日志

```bash
# 过滤 RolePlay 相关日志
adb logcat | grep -i "roleplay\|character\|chat"

# 或者在 Android Studio 的 Logcat 中过滤
```

### 数据库检查

```bash
# 导出数据库文件
adb pull /data/data/com.eterultimate.eteruee/databases/roleplay.db ./

# 使用 DB Browser for SQLite 查看
```

### 文件存储检查

```bash
# 查看角色头像等文件
adb shell ls /data/data/com.eterultimate.eteruee/files/roleplay/characters/

# 拉取到本地
adb pull /data/data/com.eterultimate.eteruee/files/roleplay/ ./local_backup
```

---

## 🎨 自定义配置

### 修改主题颜色

编辑 `app/src/main/res/values/colors.xml` 或使用 Material Theme Builder。

### 调整 AI 参数

在 `ChatViewModel` 中可以修改默认参数：
```kotlin
// 温度（创造性）
val temperature = 0.7f

// 最大 token
val maxTokens = 2048

// 上下文长度
val contextSize = 10
```

### 自定义数据存储路径

在 `RolePlayFileStorage` 中修改：
```kotlin
private val baseDir = File(context.filesDir, "roleplay")
```

---

## 📚 相关文档

- [功能清单与测试指南](FEATURE_CHECKLIST.md)
- [架构设计文档](ARCHITECTURE.md) - 待创建
- [API 参考](API_REFERENCE.md) - 待创建

---

## 🆘 获取帮助

如果遇到问题：

1. **查看日志**: `adb logcat`
2. **检查编译错误**: 查看 Gradle 输出
3. **搜索代码**: 使用 IDE 的全局搜索
4. **查阅文档**: 阅读相关 Kotlin 文件的注释

---

## ✅ 检查清单

在开始使用前，确认：

- [ ] 项目编译成功
- [ ] 应用安装到设备
- [ ] 能进入 RolePlay 模块
- [ ] 能创建角色
- [ ] 能发送消息并收到 AI 回复
- [ ] 世界书功能正常
- [ ] 群组功能正常

全部勾选后，就可以开始使用了！🎉
