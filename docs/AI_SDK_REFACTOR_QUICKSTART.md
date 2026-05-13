# AI SDK 重构 - 快速开始指南

## 🚀 当前状态

✅ **开发分支已创建**: `feature/ai-sdk-refactor`  
✅ **计划文档已完成**: [AI_SDK_REFACTOR_DEV_BRANCH_PLAN.md](./AI_SDK_REFACTOR_DEV_BRANCH_PLAN.md)  
✅ **已推送到远程**: GitHub

---

## 📋 下一步操作

### 1. 确认当前分支

```bash
git branch
# 应该显示:
#   master
# * feature/ai-sdk-refactor  (当前所在分支)
```

### 2. 查看计划文档

阅读详细的实施计划:
- [开发分支实施计划](./AI_SDK_REFACTOR_DEV_BRANCH_PLAN.md)
- [主分支实施总结](./AI_SDK_REFACTOR_SUMMARY.md)

### 3. 选择起始任务

根据计划,建议从以下任务之一开始:

#### 选项 A: 后端 API 标准化 (推荐优先)
**原因**: 后端 API 是 Android 和 Web 端的基础

**任务清单**:
1. 设计新的 SSE 事件格式
2. 在 `ConversationRoutes.kt` 中实现新的事件发送逻辑
3. 保留旧端点 `/stream-v1`,新增 `/stream-v2`
4. 编写单元测试

**相关文件**:
- `app/src/main/java/com/eterultimate/eteruee/web/routes/ConversationRoutes.kt`
- `app/src/main/java/com/eterultimate/eteruee/data/ai/service/ChatService.kt`

#### 选项 B: Android 端基础架构
**任务清单**:
1. 创建 `AISDK.kt` 接口
2. 实现 `DefaultAISDK.kt`
3. 创建 `UseChat.kt` Composable Hook
4. 编写单元测试

**相关文件**:
- `ai/src/main/java/com/eterultimate/eteruee/ai/sdk/AISDK.kt` (新建)
- `ai/src/main/java/com/eterultimate/eteruee/ai/sdk/DefaultAISDK.kt` (新建)
- `app/src/main/java/com/eterultimate/eteruee/ui/hooks/UseChat.kt` (新建)

#### 选项 C: Web 端 Provider
**任务清单**:
1. 确认 `@ai-sdk/react` 已安装
2. 创建 `eteruee-provider.ts`
3. 实现消息格式转换函数
4. 小范围测试 useChat hook

**相关文件**:
- `web-ui/app/lib/eteruee-provider.ts` (新建)
- `web-ui/app/routes/conversations.$id.tsx` (待重构)

---

## 💡 开发工作流

### 提交规范

使用约定式提交 (Conventional Commits):

```bash
# 新功能
git commit -m "feat: 创建 AISDK 核心接口"

# Bug 修复
git commit -m "fix: 修复 SSE 事件解析错误"

# 文档更新
git commit -m "docs: 添加 UseChat Hook 使用示例"

# 重构
git commit -m "refactor: 简化 ChatVM 职责"

# 测试
git commit -m "test: 添加 AISDK 单元测试"
```

### 分支同步

定期从主分支同步更新:

```bash
# 切换到开发分支
git checkout feature/ai-sdk-refactor

# 拉取最新代码
git fetch origin master

# 合并主分支更新
git merge origin/master

# 解决冲突 (如果有)
# 测试确保功能正常
# 提交合并
git commit -m "merge: sync with master"

# 推送
git push
```

### 测试策略

1. **单元测试**: 每个新模块都要有对应的测试
2. **集成测试**: 验证模块间协作
3. **E2E 测试**: 确保端到端功能正常
4. **回归测试**: 确保现有功能不受影响

---

## 📝 代码示例模板

### Android: AISDK 接口模板

```kotlin
package com.eterultimate.eteruee.ai.sdk

interface AISDK {
    suspend fun generateText(request: GenerateTextRequest): GenerateTextResponse
    
    fun streamText(request: StreamTextRequest): Flow<TextStreamEvent>
}

data class GenerateTextRequest(
    val model: String,
    val messages: List<UIMessage>,
    val temperature: Float? = null,
    val maxTokens: Int? = null
)

data class GenerateTextResponse(
    val text: String,
    val usage: TokenUsage?,
    val finishReason: String
)
```

### Web: Provider 模板

```typescript
import { createProvider } from '@ai-sdk/provider';

export const eterueeProvider = createProvider({
  name: 'eteruee',
  
  async streamText({ model, messages }) {
    // 实现流式生成
    return {
      stream: /* ReadableStream */,
      rawCall: { rawPrompt: null, rawSettings: {} }
    };
  }
});
```

---

## 🔍 调试技巧

### Android

1. **Logcat 过滤**:
```bash
adb logcat | grep "AISDK"
```

2. **Compose Layout Inspector**:
```
Android Studio → Tools → Layout Inspector
```

3. **网络请求监控**:
```
Android Studio → Profiler → Network
```

### Web

1. **React DevTools**:
```bash
bun add @react-devtools/core
```

2. **Network 面板**:
- Chrome DevTools → Network
- 过滤 `event-stream` 类型

3. **Console 日志**:
```typescript
console.log('[AI SDK]', messages);
```

---

## 🎯 第一周目标

完成以下任务之一:

### 目标 A: 后端 SSE 格式标准化
- [ ] 设计新的事件格式 (参考计划文档)
- [ ] 实现 `TextDeltaEvent`, `ToolCallEvent`, `FinishEvent`
- [ ] 修改 `ConversationRoutes.kt` 发送新格式
- [ ] 保留旧端点作为 fallback
- [ ] 编写至少 5 个单元测试

### 目标 B: Android AISDK 基础
- [ ] 创建 `AISDK.kt` 接口
- [ ] 实现 `DefaultAISDK.kt` (封装现有 Provider)
- [ ] 创建数据类 (`GenerateTextRequest`, `StreamTextRequest` 等)
- [ ] 编写接口文档 (KDoc)
- [ ] 编写至少 3 个单元测试

### 目标 C: Web Provider 原型
- [ ] 确认依赖已安装
- [ ] 创建 `eteruee-provider.ts`
- [ ] 实现基本的 `streamText` 方法
- [ ] 在测试页面中使用 `useChat`
- [ ] 验证消息能正确显示

---

## 📞 需要帮助?

### 参考资源

1. **Vercel AI SDK 文档**: https://sdk.vercel.ai/docs
2. **Jetpack Compose 文档**: https://developer.android.com/jetpack/compose
3. **项目现有代码**: 参考 `ai/` 模块的 Provider 实现

### 常见问题

**Q: 如何保持向后兼容?**  
A: 保留旧的 API 端点,新增标准端点,通过配置切换。

**Q: 如何处理工具调用?**  
A: 参考 AI SDK 的 `experimental_onToolCall` API,实现工具执行器。

**Q: 性能会受影响吗?**  
A: 新架构理论上更高效,但需要进行基准测试验证。

---

## ✅ 检查清单

开始开发前,确认:

- [ ] 当前在 `feature/ai-sdk-refactor` 分支
- [ ] 已阅读实施计划文档
- [ ] 已选择起始任务
- [ ] IDE 已打开并加载项目
- [ ] 开发环境配置完成 (Android SDK, Node.js, Bun)
- [ ] 了解 Git 工作流和提交规范

---

**准备好了吗?选择一个任务,开始编码吧! 🚀**

最后更新: 2026-05-13
