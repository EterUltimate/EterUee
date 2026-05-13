# AI SDK 重构 - 开发分支实施计划

## 📋 分支策略

- **主分支 (master)**: 保持当前架构,稳定可用
- **开发分支 (feature/ai-sdk-refactor)**: 按 AI SDK v5 最佳实践完全重写

## 🎯 重构目标

按照 Vercel AI SDK v5 的设计理念,实现:

1. **统一的流式处理**: 使用 `useChat` / `streamText` 等标准 API
2. **类型安全**: 完整的 TypeScript/Kotlin 类型约束
3. **解耦架构**: UI 层与业务逻辑层完全分离
4. **工具调用**: 原生支持 Tool Calling
5. **多模态**: 图片、视频等多媒体内容处理

---

## 🏗️ Android 端重构方案

### 核心架构变更

#### 1. 替换现有 ChatService

**当前架构**:
```
ChatPage → ChatVM → ChatService → Provider
```

**新架构**:
```
ChatPage → UseChat Hook → AISDK → Provider
```

#### 2. 创建标准 AI SDK 接口

文件: `ai/src/main/java/com/eterultimate/eteruee/ai/sdk/AISDK.kt`

```kotlin
interface AISDK {
    // 文本生成
    suspend fun generateText(request: GenerateTextRequest): GenerateTextResponse
    
    // 流式文本生成
    fun streamText(request: StreamTextRequest): Flow<TextStreamEvent>
    
    // 对象生成 (结构化输出)
    suspend fun generateObject<T>(request: GenerateObjectRequest): T
    
    // 多模态生成
    suspend fun generateImage(request: ImageGenerationRequest): ImageGenerationResponse
}
```

#### 3. 实现 Compose Hooks

文件: `app/src/main/java/com/eterultimate/eteruee/ui/hooks/UseChat.kt`

```kotlin
@Composable
fun useChat(
    conversationId: Uuid,
    api: String = "/api/conversations/$conversationId/stream",
    initialMessages: List<UIMessage> = emptyList(),
    onFinish: ((result: ChatResult) -> Unit)? = null,
    onError: ((error: Exception) -> Unit)? = null,
    onToolCall: ((toolCall: ToolCall) -> ToolResult)? = null
): ChatState {
    val messages = remember { mutableStateListOf(*initialMessages.toTypedArray()) }
    val isLoading = remember { mutableStateOf(false) }
    val input = remember { mutableStateOf("") }
    
    fun append(message: UIMessage) { /* ... */ }
    
    suspend fun handleSubmit(userInput: String, attachments: List<UIMessagePart>) {
        // 1. 添加用户消息
        // 2. 调用 AISDK.streamText
        // 3. 实时更新 assistant 消息
        // 4. 处理工具调用循环
        // 5. 调用 onFinish/onError
    }
    
    return ChatState(
        messages = messages,
        isLoading = isLoading.value,
        input = input.value,
        setInput = { input.value = it },
        append = ::append,
        handleSubmit = ::handleSubmit
    )
}
```

#### 4. 重构 ChatVM

文件: `app/src/main/java/com/eterultimate/eteruee/ui/pages/chat/ChatVM.kt`

**简化后的职责**:
- 对话管理 (创建、删除、重命名)
- 收藏功能
- 翻译功能
- 不再直接管理流式生成逻辑

```kotlin
class ChatVM(
    private val conversationRepo: ConversationRepository,
    private val aiSDK: AISDK
) : ViewModel() {
    
    // 对话列表
    val conversations: StateFlow<List<Conversation>> = conversationRepo.getAll()
    
    // 当前对话
    private val _currentConversation = MutableStateFlow<Conversation?>(null)
    val currentConversation: StateFlow<Conversation?> = _currentConversation.asStateFlow()
    
    // 批量操作
    fun deleteConversations(ids: List<Uuid>) { /* ... */ }
    fun pinConversation(id: Uuid) { /* ... */ }
    
    // 注意: sendMessage 逻辑移到 UseChat hook 中
}
```

#### 5. 优化 ChatPage

文件: `app/src/main/java/com/eterultimate/eteruee/ui/pages/chat/ChatPage.kt`

```kotlin
@Composable
fun ChatPage(id: Uuid) {
    val vm: ChatVM = koinViewModel()
    
    // 使用新的 useChat hook
    val chatState = useChat(
        conversationId = id,
        initialMessages = loadInitialMessages(id),
        onFinish = { result ->
            vm.saveConversationResult(id, result)
        },
        onError = { error ->
            toast(error.message)
        }
    )
    
    Scaffold(
        topBar = { ChatTopBar(chatState.isLoading) },
        content = { padding ->
            MessageList(
                messages = chatState.messages,
                modifier = Modifier.padding(padding)
            )
        },
        bottomBar = {
            ChatInput(
                value = chatState.input,
                onValueChange = chatState.setInput,
                onSend = { text, parts -> 
                    chatState.handleSubmit(text, parts) 
                },
                isLoading = chatState.isLoading
            )
        }
    )
}
```

---

## 🌐 Web 端重构方案

### 核心架构变更

#### 1. 安装依赖

```bash
cd web-ui
bun add @ai-sdk/react @ai-sdk/provider
```

#### 2. 创建 EterUee Provider

文件: `web-ui/app/lib/eteruee-provider.ts`

```typescript
import { createProvider } from '@ai-sdk/provider';
import api from '~/services/api';

export const eterueeProvider = createProvider({
  name: 'eteruee',
  
  async streamText({ model, messages, tools, temperature, maxTokens }) {
    // 调用后端 SSE API
    const response = await fetch(`/api/conversations/stream`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        model,
        messages: convertToBackendFormat(messages),
        tools,
        temperature,
        maxTokens
      })
    });
    
    return {
      stream: response.body!,
      rawCall: { rawPrompt: null, rawSettings: {} }
    };
  },
  
  async generateText({ model, messages }) {
    // 非流式生成
    const result = await api.post('/api/conversations/generate', {
      model,
      messages: convertToBackendFormat(messages)
    });
    
    return {
      text: result.text,
      finishReason: 'stop',
      usage: result.usage
    };
  }
});
```

#### 3. 重构 Conversations 页面

文件: `web-ui/app/routes/conversations.$id.tsx`

**当前**: ~1133 行,手动管理 SSE  
**目标**: ~400 行,使用 useChat

```typescript
import { useChat } from '@ai-sdk/react';
import { useParams } from 'react-router';

function ConversationPage() {
  const { id } = useParams();
  
  const {
    messages,
    input,
    handleInputChange,
    handleSubmit,
    isLoading,
    error,
    append,
    reload,
    stop
  } = useChat({
    id,
    api: `/api/conversations/${id}/stream`,
    initialMessages: convertBackendMessages(initialData?.messageNodes || []),
    onFinish: (message) => {
      // 保存对话结果
      api.post(`/api/conversations/${id}/save`, { message });
    },
    onError: (error) => {
      toast.error(error.message);
    },
    experimental_onToolCall: async ({ toolCall }) => {
      // 处理工具调用
      switch (toolCall.toolName) {
        case 'search':
          return await searchWeb(toolCall.args.query);
        case 'read_file':
          return await readFile(toolCall.args.path);
      }
    }
  });
  
  return (
    <div className="flex h-screen">
      <ConversationSidebar />
      <main className="flex-1 flex flex-col">
        <MessageList messages={messages} />
        <ChatInput
          value={input}
          onChange={handleInputChange}
          onSubmit={handleSubmit}
          disabled={isLoading}
        />
      </main>
    </div>
  );
}
```

#### 4. 工具调用集成

文件: `web-ui/app/hooks/use-tools.ts`

```typescript
import { experimental_useToolInvocation } from '@ai-sdk/react';

export function useConversationTools(conversationId: string) {
  const { invokeTool } = experimental_useToolInvocation({
    id: conversationId,
    onToolCall: async ({ toolCall }) => {
      switch (toolCall.toolName) {
        case 'web_search':
          const results = await searchWeb(toolCall.args.query);
          return { result: JSON.stringify(results) };
          
        case 'file_read':
          const content = await readFile(toolCall.args.path);
          return { result: content };
          
        case 'code_execute':
          const output = await executeCode(toolCall.args.code);
          return { result: output };
      }
    }
  });
  
  return { invokeTool };
}
```

---

## 🔧 后端 API 适配

### SSE 事件格式标准化

文件: `app/src/main/java/com/eterultimate/eteruee/web/routes/ConversationRoutes.kt`

**当前格式**:
```kotlin
event: snapshot
data: { type: "snapshot", conversation: {...} }

event: node_update
data: { type: "node_update", node: {...} }
```

**AI SDK 标准格式**:
```kotlin
// 文本增量
event: text-delta
data: { textDelta: "增量文本" }

// 工具调用开始
event: tool-call-start
data: { toolCallId: "xxx", toolName: "search", args: {...} }

// 工具调用结果
event: tool-call-result
data: { toolCallId: "xxx", result: "..." }

// 完成信号
event: finish
data: { finishReason: "stop", usage: { promptTokens: 100, completionTokens: 50 } }

// 错误
event: error
data: { error: "错误信息" }
```

### 实现示例

```kotlin
get("/conversations/{id}/stream") {
    val conversationId = call.parameters["id"]?.toUuid() ?: throw BadRequestException()
    
    call.respondSse {
        try {
            chatService.streamConversation(conversationId).collect { event ->
                when (event) {
                    is TextChunk -> {
                        send(
                            event = "text-delta",
                            data = json.encodeToString(TextDeltaEvent(event.text))
                        )
                    }
                    is ToolCallStart -> {
                        send(
                            event = "tool-call-start",
                            data = json.encodeToString(ToolCallStartEvent(event))
                        )
                    }
                    is ToolCallResult -> {
                        send(
                            event = "tool-call-result",
                            data = json.encodeToString(ToolCallResultEvent(event))
                        )
                    }
                    is FinishChunk -> {
                        send(
                            event = "finish",
                            data = json.encodeToString(FinishEvent(event))
                        )
                    }
                }
            }
        } catch (e: Exception) {
            send(
                event = "error",
                data = json.encodeToString(ErrorEvent(e.message ?: "Unknown error"))
            )
        }
    }
}
```

---

## 📅 实施时间表

### Week 1: 后端 API 标准化
- [ ] 设计新的 SSE 事件格式
- [ ] 实现向后兼容的适配器
- [ ] 编写 API 文档
- [ ] 单元测试

### Week 2: Android 端基础架构
- [ ] 创建 AISDK 接口
- [ ] 实现 DefaultAISDK
- [ ] 创建 UseChat Composable
- [ ] 单元测试

### Week 3: Android 端迁移
- [ ] 重构 ChatVM
- [ ] 更新 ChatPage
- [ ] 移除旧代码
- [ ] 集成测试

### Week 4: Web 端重构
- [ ] 创建 EterUee Provider
- [ ] 重构 conversations.tsx
- [ ] 实现工具调用
- [ ] 集成测试

### Week 5: 测试与优化
- [ ] 端到端测试
- [ ] 性能优化
- [ ] Bug 修复
- [ ] Code Review

### Week 6: 合并到主分支
- [ ] 准备 PR
- [ ] 最终测试
- [ ] 合并代码
- [ ] 部署

---

## ✅ 成功指标

| 指标 | 目标 | 测量方法 |
|------|------|----------|
| Web 代码行数 | 1133 → ~400 (-65%) | `wc -l` |
| Android 代码行数 | 减少 30%+ | `find -name "*.kt" \| xargs wc -l` |
| 流式处理 Bug | 减少 80%+ | Issue 追踪 |
| 新功能开发时间 | 减少 50% | 团队反馈 |
| 类型安全 | 0 any 类型 | TypeScript 严格模式 |
| 零功能回归 | 100% 功能正常 | E2E 测试套件 |

---

## ⚠️ 风险与缓解

### 风险 1: 后端 API 改动影响现有客户端
**缓解**: 
- 保留旧的 SSE 端点 (`/stream-v1`)
- 新增标准端点 (`/stream-v2`)
- 逐步迁移,提供过渡期

### 风险 2: 学习曲线陡峭
**缓解**:
- 编写详细的迁移指南
- 提供代码示例和模板
- 组织内部培训

### 风险 3: 性能退化
**缓解**:
- 基准测试对比
- 性能监控
- 优化热点路径

---

## 📚 参考资源

- [Vercel AI SDK v5 文档](https://sdk.vercel.ai/docs)
- [AI SDK Provider 规范](https://sdk.vercel.ai/providers/ai-sdk-providers)
- [React Server Components](https://react.dev/reference/rsc)
- [Jetpack Compose State Management](https://developer.android.com/jetpack/compose/state)

---

## 🔄 同步策略

### 从主分支同步更新

```bash
# 切换到开发分支
git checkout feature/ai-sdk-refactor

# 拉取主分支最新代码
git fetch origin master

# 合并主分支更新
git merge origin/master

# 解决冲突 (如果有)
# 提交合并
git commit -m "merge: sync with master"
```

### 推送开发分支

```bash
git push -u origin feature/ai-sdk-refactor
```

---

**创建日期**: 2026-05-13  
**分支**: `feature/ai-sdk-refactor`  
**状态**: 🚀 准备开始开发
