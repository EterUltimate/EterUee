# 书签功能测试指南

## 🧪 测试环境准备

### 前置条件
- ✅ Android Studio 已安装
- ✅ 模拟器或真机已连接
- ✅ 项目已成功编译（`BUILD SUCCESSFUL`）

---

## 📋 测试用例清单

### 测试用例 1: 添加书签

**步骤**：
1. 打开应用，进入任意聊天页面
2. 长按任意消息（用户或助手消息均可）
3. 在弹出的菜单中点击"添加书签"
4. 在对话框中输入标题（例如："重要回复"）
5. 输入备注（例如："这个回答很有用"）
6. 点击"保存"按钮

**预期结果**：
- ✅ 对话框关闭
- ✅ 无错误提示
- ✅ 书签已保存到数据库

**验证方法**：
```bash
# 查看日志，确认没有错误
adb logcat | grep -i bookmark
```

---

### 测试用例 2: 查看书签列表

**步骤**：
1. 在聊天页面，点击右上角的书签图标（🔖）
2. 进入书签管理页面

**预期结果**：
- ✅ 显示所有已添加的书签
- ✅ 每个书签卡片显示：
  - 标题（或默认名称 "Bookmark #xxxx"）
  - 备注（如果有）
  - 消息索引
  - 创建时间
- ✅ 如果没有书签，显示空状态提示

**边界情况**：
- [ ] 测试无书签时的空状态
- [ ] 测试有多个书签时的列表滚动

---

### 测试用例 3: 编辑书签

**步骤**：
1. 在书签列表页面，找到任意书签
2. 点击卡片右上角的"更多"按钮（⋮）
3. 选择"编辑"
4. 修改标题和备注
5. 点击"保存"

**预期结果**：
- ✅ 对话框关闭
- ✅ 书签信息已更新
- ✅ 列表自动刷新显示新内容

**验证方法**：
- 重新进入编辑对话框，确认修改已保存

---

### 测试用例 4: 删除书签

**步骤**：
1. 在书签列表页面，找到任意书签
2. 点击"更多"按钮（⋮）
3. 选择"删除"（红色文字）

**预期结果**：
- ✅ 书签从列表中移除
- ✅ 数据库中已删除该书签

**验证方法**：
- 返回聊天页再进入书签页，确认书签已消失

---

### 测试用例 5: 点击书签跳转（接口测试）

**步骤**：
1. 在书签列表页面，点击任意书签卡片（非菜单区域）

**预期结果**：
- ✅ 触发 `onBookmarkClick` 回调
- ✅ 传递正确的 `messageIndex` 参数
- ⚠️ 实际滚动需要调用方实现

**调试方法**：
在 BookmarkPage 中添加日志：
```kotlin
onClick = {
    Log.d("BookmarkTest", "Clicked bookmark at index: ${bookmark.messageIndex}")
    onBookmarkClick(bookmark.messageIndex)
}
```

查看日志确认回调被触发：
```bash
adb logcat | grep BookmarkTest
```

---

### 测试用例 6: 边界情况测试

#### 6.1 空标题和备注
**步骤**：
1. 添加书签时不输入标题和备注
2. 直接点击"保存"

**预期结果**：
- ✅ 书签成功创建
- ✅ 显示默认名称 "Bookmark #xxxxxxxx"
- ✅ 备注区域不显示（因为为空）

#### 6.2 超长文本
**步骤**：
1. 输入超过100字符的标题
2. 输入超过500字符的备注

**预期结果**：
- ✅ 正常保存
- ✅ UI 正确显示（可能需要截断或滚动）

#### 6.3 特殊字符
**步骤**：
1. 标题和备注中包含 emoji、中文、特殊符号

**预期结果**：
- ✅ 正常保存和显示
- ✅ 无乱码或崩溃

#### 6.4 快速连续操作
**步骤**：
1. 快速连续点击多个书签的删除按钮
2. 快速切换编辑和保存

**预期结果**：
- ✅ 无崩溃
- ✅ 数据一致性保持

---

### 测试用例 7: 性能测试

#### 7.1 大量书签
**步骤**：
1. 添加 100+ 个书签
2. 滚动浏览列表

**预期结果**：
- ✅ 滚动流畅（60 FPS）
- ✅ 无卡顿或掉帧
- ✅ 内存占用合理

**监控方法**：
```bash
# 监控 FPS
adb shell dumpsys SurfaceFlinger --latency-clear
```

#### 7.2 大数据量聊天
**步骤**：
1. 在有 500+ 条消息的聊天中添加书签
2. 点击书签跳转

**预期结果**：
- ✅ messageIndex 计算正确
- ✅ 无索引越界错误

---

## 🔍 常见问题排查

### 问题 1: 书签按钮不显示

**可能原因**：
- ChatPage 未传入 `onShowBookmarks` 参数

**解决方法**：
检查 ChatPage 调用处是否包含：
```kotlin
ChatPage(
    chatId = chatId,
    onBackClick = { /* ... */ },
    onShowBookmarks = { /* 导航到书签页 */ }
)
```

### 问题 2: 点击书签无反应

**可能原因**：
- `onBookmarkClick` 回调未实现
- LazyColumn 滚动逻辑未集成

**解决方法**：
1. 确认 BookmarkPage 传入了 `onBookmarkClick`
2. 在回调中添加日志验证是否触发
3. 实现 LazyColumn 的滚动逻辑

### 问题 3: 书签数据丢失

**可能原因**：
- 数据库迁移失败
- BookmarkService 未正确注入

**解决方法**：
1. 检查 RolePlayDatabase 版本是否为 5
2. 确认 MIGRATION_4_5 已执行
3. 查看 Koin 注入日志

### 问题 4: 编译错误

**常见错误**：
```
Unresolved reference: detectTapGestures
```

**解决方法**：
确保导入了正确的包：
```kotlin
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
```

---

## 📊 测试结果记录表

| 测试用例 | 状态 | 备注 |
|---------|------|------|
| TC1: 添加书签 | ⬜ 待测试 | |
| TC2: 查看列表 | ⬜ 待测试 | |
| TC3: 编辑书签 | ⬜ 待测试 | |
| TC4: 删除书签 | ⬜ 待测试 | |
| TC5: 点击跳转 | ⬜ 待测试 | 需集成滚动逻辑 |
| TC6.1: 空标题备注 | ⬜ 待测试 | |
| TC6.2: 超长文本 | ⬜ 待测试 | |
| TC6.3: 特殊字符 | ⬜ 待测试 | |
| TC6.4: 快速操作 | ⬜ 待测试 | |
| TC7.1: 大量书签 | ⬜ 待测试 | |
| TC7.2: 大数据聊天 | ⬜ 待测试 | |

**图例**：
- ⬜ 待测试
- ✅ 通过
- ❌ 失败
- ⚠️ 部分通过

---

## 🎯 验收标准

### 必须满足（P0）
- [x] 可以添加书签
- [x] 可以查看书签列表
- [x] 可以编辑书签
- [x] 可以删除书签
- [x] TopAppBar 有书签入口
- [x] 点击书签触发回调
- [x] 无崩溃或严重错误

### 应该满足（P1）
- [ ] 点击书签能滚动到对应消息
- [ ] 空状态友好提示
- [ ] 错误处理完善
- [ ] 性能流畅（60 FPS）

### 可以满足（P2）
- [ ] 消息高亮效果
- [ ] 书签数量 Badge
- [ ] 搜索功能
- [ ] 批量操作

---

## 📝 测试报告模板

```markdown
# 书签功能测试报告

**测试日期**: YYYY-MM-DD  
**测试人员**: [姓名]  
**测试设备**: [设备型号 + Android 版本]  
**应用版本**: [版本号]

## 测试概况
- 总测试用例数: XX
- 通过: XX
- 失败: XX
- 跳过: XX

## 发现的问题

### 问题 1: [问题描述]
- **严重程度**: P0/P1/P2
- **复现步骤**: ...
- **预期结果**: ...
- **实际结果**: ...
- **截图/日志**: ...

### 问题 2: ...

## 测试结论
- [ ] 通过，可以发布
- [ ] 有条件通过，需修复 P0 问题
- [ ] 不通过，需修复多个问题

## 建议
...
```

---

## 🚀 自动化测试（可选）

### UI 测试示例

```kotlin
@RunWith(AndroidJUnit4::class)
class BookmarkUITest {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<RouteActivity>()
    
    @Test
    fun testAddBookmark() {
        // 导航到聊天页
        composeTestRule.onNodeWithText("聊天").performClick()
        
        // 长按消息
        composeTestRule.onNodeWithText("消息内容")
            .performLongClick()
        
        // 点击添加书签
        composeTestRule.onNodeWithText("添加书签").performClick()
        
        // 输入标题
        composeTestRule.onNodeWithLabel("标题（可选）")
            .performTextInput("测试书签")
        
        // 保存
        composeTestRule.onNodeWithText("保存").performClick()
        
        // 验证书签已添加
        composeTestRule.onNodeWithText("测试书签").assertExists()
    }
    
    @Test
    fun testDeleteBookmark() {
        // ... 添加书签
        
        // 点击更多
        composeTestRule.onNodeWithContentDescription("更多").performClick()
        
        // 删除
        composeTestRule.onNodeWithText("删除").performClick()
        
        // 验证已删除
        composeTestRule.onNodeWithText("测试书签").assertDoesNotExist()
    }
}
```

---

## 📞 联系方式

如在测试过程中遇到问题，请联系：
- 开发负责人: [姓名]
- 邮箱: [email]
- Slack: [#channel-name]

---

**最后更新**: 2026-05-13
