# AI SDK 重构方案

## 核心目标

在**不改变现有功能**的前提下,借鉴 Vercel AI SDK v5 的设计理念优化架构:

1. **统一 API 抽象**: 建立类似 `generateText`、`streamText` 的统一接口
2. **简化流式处理**: 使用更优雅的 Flow/Hooks 处理 streaming
3. **类型安全增强**: 强化消息类型、工具调用、错误处理的类型约束
4. **解耦 UI 与逻辑**: 分离聊天状态管理与 UI 渲染

---

## 阶段一: Android 原生界面重构 (Kotlin + Jetpack Compose)

### 1.1 创建 AI SDK Core 抽象层

**新增文件**: `ai/src/main/java/com/eterultimate/eteruee/ai/sdk/AISDK.kt`

```kotlin
interface AISDK {
    suspend fun generateText(request: GenerateTextRequest): GenerateTextResult
    fun streamText(request: StreamTextRequest): Flow<TextChunk>
    suspend fun generateObject(request: GenerateObjectRequest): JsonObject
}

data class GenerateTextRequest(
    val model: Model,
    val messages: List<UIMessage>,
    val temperature: Float? = null,
    val maxTokens: Int? = null,
    val tools: List<Tool> = emptyList(),
    val customHeaders: List<CustomHeader> = emptyList()
)

data class GenerateTextResult(
    val text: String,
    val usage: TokenUsage?,
    val finishReason: FinishReason?
)
```

**实现类**: `ai/src/main/java/com/eterultimate/eteruee/ai/sdk/DefaultAISDK.kt`

- 封装现有的 `Provider` 接口
- 统一错误处理 (将各 Provider 的错误转换为标准格式)
- 添加请求/响应日志记录

### 1.2 创建 Compose Hooks (类似 useChat)

**新增文件**: `app/src/main/java/com/eterultimate/eteruee/ui/hooks/UseChat.kt`

```kotlin
@Composable
fun useChat(
    conversationId: Uuid,
    initialMessages: List<UIMessage> = emptyList(),
    onFinish: ((result: GenerateTextResult) -> Unit)? = null,
    onError: ((error: Exception) -> Unit)? = null
): ChatState {
    val scope = rememberCoroutineScope()
    val messages = remember { mutableStateListOf(*initialMessages.toTypedArray()) }
    val isLoading = remember { mutableStateOf(false) }
    
    fun appendMessage(message: UIMessage) { /* ... */ }
    
    suspend fun handleSubmit(userMessage: String) {
        // 1. 添加用户消息
        // 2. 调用 AISDK.streamText
        // 3. 实时更新 assistant 消息
        // 4. 处理工具调用
        // 5. 调用 onFinish/onError
    }
    
    return ChatState(
        messages = messages,
        isLoading = isLoading.value,
        appendMessage = ::appendMessage,
        handleSubmit = ::handleSubmit
    )
}
```

### 1.3 重构 ChatVM

**修改文件**: `app/src/main/java/com/eterultimate/eteruee/ui/pages/chat/ChatVM.kt`

**当前问题**:
- 直接依赖 `ChatService`,耦合度高
- 消息处理逻辑分散在多处
- 流式更新通过 `MutableStateFlow` 手动管理

**重构方案**:
```kotlin
class ChatVM(
    id: String,
    private val aiSDK: AISDK,  // 注入新的 SDK
    // ... 其他依赖保持不变
) : ViewModel() {
    
    // 使用 useChat hook 的等价物
    private val chatState = ChatStateHolder(conversationId)
    
    val messages: StateFlow<List<UIMessage>> = chatState.messages
    val isLoading: StateFlow<Boolean> = chatState.isLoading
    
    fun sendMessage(text: String, attachments: List<UIMessagePart>) {
        viewModelScope.launch {
            chatState.handleSubmit(text, attachments)
        }
    }
    
    // 保留原有功能: 对话管理、收藏、翻译等
    // 但移除底层的流式处理逻辑
}
```

### 1.4 优化 ChatPage UI

**修改文件**: `app/src/main/java/com/eterultimate/eteruee/ui/pages/chat/ChatPage.kt`

**改进点**:
- 使用 `useChat` 返回的状态替代直接从 VM 读取
- 简化消息列表渲染逻辑
- 提取消息气泡为独立组件

```kotlin
@Composable
fun ChatPage(id: Uuid, text: String?, files: List<Uri>, nodeId: Uuid? = null) {
    val vm: ChatVM = koinViewModel(parameters = { parametersOf(id.toString()) })
    val chatState = vm.chatState  // 从 VM 获取状态
    
    Scaffold(
        topBar = { /* ... */ },
        content = { padding ->
            MessageList(
                messages = chatState.messages.collectAsStateWithLifecycle().value,
                modifier = Modifier.padding(padding)
            )
        },
        bottomBar = {
            ChatInput(
                onSend = { text, parts -> vm.sendMessage(text, parts) },
                isLoading = chatState.isLoading.collectAsStateWithLifecycle().value
            )
        }
    )
}
```

### 1.5 工具调用支持

**新增文件**: `ai/src/main/java/com/eterultimate/eteruee/ai/sdk/ToolExecutor.kt`

```kotlin
interface ToolExecutor {
    suspend fun execute(toolCall: ToolCall): ToolResult
}

// 自动处理多轮工具调用
suspend fun streamTextWithTools(
    sdk: AISDK,
    request: StreamTextRequest,
    executor: ToolExecutor
): Flow<TextChunk> {
    // 1. 流式生成文本
    // 2. 检测工具调用
    // 3. 执行工具
    // 4. 将结果作为新消息继续生成
    // 5. 重复直到无工具调用
}
```

---

## 阶段二: Web 前端界面重构 (React + TypeScript)

### 2.1 安装 AI SDK v5

**修改文件**: `web-ui/package.json`

```json
{
  "dependencies": {
    "@ai-sdk/react": "^5.0.0",
    "@ai-sdk/provider": "^5.0.0",
    // 保留现有依赖
  }
}
```

运行: `bun install`

### 2.2 创建 AI SDK Provider 适配器

**新增文件**: `web-ui/app/lib/ai-sdk-provider.ts`

```typescript
import { createProvider } from '@ai-sdk/provider';
import api from '~/services/api';

// 将现有的 SSE API 适配为 AI SDK Provider
export const eterueeProvider = createProvider({
  name: 'eteruee',
  async generateText({ model, messages, tools }) {
    // 调用后端 /api/conversations/{id}/generate
    const response = await api.post('/conversations/generate', {
      model,
      messages,
      tools
    });
    return response.json();
  },
  
  async streamText({ model, messages, tools }) {
    // 使用现有的 SSE 连接
    return {
      stream: api.sse(`/conversations/${conversationId}/stream`),
      // 转换 SSE 事件为 AI SDK 格式
    };
  }
});
```

### 2.3 使用 useChat Hook

**修改文件**: `web-ui/app/routes/conversations.tsx`

**当前实现** (约 1133 行):
- 手动管理 SSE 连接
- 手动处理消息增量更新
- 复杂的状态管理逻辑

**重构后**:
```typescript
import { useChat } from '@ai-sdk/react';

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
    initialMessages: conversation?.messageNodes.flatMap(node => node.messages) || [],
    onFinish: (message) => {
      // 保存对话到后端
      api.post(`/conversations/${id}/save`, { message });
    },
    onError: (error) => {
      toast.error(error.message);
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

### 2.4 工具调用集成

**新增文件**: `web-ui/app/hooks/use-tools.ts`

```typescript
import { experimental_useToolInvocation } from '@ai-sdk/react';

export function useConversationTools(conversationId: string) {
  const { invokeTool } = experimental_useToolInvocation({
    id: conversationId,
    onToolCall: async ({ toolCall }) => {
      switch (toolCall.toolName) {
        case 'search':
          return await searchWeb(toolCall.args.query);
        case 'read_file':
          return await readFile(toolCall.args.path);
        // ... 其他工具
      }
    }
  });
  
  return { invokeTool };
}
```

### 2.5 流式对象生成 (可选)

如果未来需要支持结构化输出 (如 JSON schema):

```typescript
import { useObject } from '@ai-sdk/react';

function StructuredOutputExample() {
  const { object, submit, isLoading } = useObject({
    api: '/api/generate-object',
    schema: z.object({
      title: z.string(),
      summary: z.string(),
      tags: z.array(z.string())
    })
  });
  
  return (
    <div>
      {object && (
        <>
          <h1>{object.title}</h1>
          <p>{object.summary}</p>
        </>
      )}
    </div>
  );
}
```

---

## 阶段三: 后端 API 适配 (可选但推荐)

### 3.1 标准化 SSE 事件格式

**修改文件**: `app/src/main/java/com/eterultimate/eteruee/web/routes/ConversationRoutes.kt`

确保 SSE 事件符合 AI SDK 期望的格式:

```kotlin
// 当前格式可能需要调整
data class AISDKStreamEvent(
    val type: "text-delta" | "tool-call" | "finish",
    val data: JsonElement
)

// 在 SSE 路由中发送
sse("/{id}/stream") {
    chatService.streamConversation(uuid).collect { chunk ->
        send(
            event = when (chunk) {
                is TextChunk -> "text-delta"
                is ToolCallChunk -> "tool-call"
                is FinishChunk -> "finish"
            },
            data = json.encodeToString(chunk)
        )
    }
}
```

### 3.2 添加工具注册端点

**新增路由**: `POST /api/tools`

```kotlin
post("/tools") {
    val tools = mcpManager.getAllTools()
    call.respond(tools.map { it.toAISKDSchema() })
}
```

---

## 实施步骤与时间估算

### Week 1: Android 端基础架构
- [ ] 1.1 创建 AISDK 接口和默认实现 (2天)
- [ ] 1.2 实现 UseChat Composable (2天)
- [ ] 1.3 单元测试 AISDK (1天)

### Week 2: Android 端迁移
- [ ] 2.1 重构 ChatVM 使用新 SDK (2天)
- [ ] 2.2 更新 ChatPage UI (1天)
- [ ] 2.3 工具调用支持 (2天)

### Week 3: Web 端集成
- [ ] 3.1 安装 AI SDK v5 并配置 (1天)
- [ ] 3.2 创建 Provider 适配器 (2天)
- [ ] 3.3 重构 conversations.tsx 使用 useChat (3天)

### Week 4: 测试与优化
- [ ] 4.1 端到端测试 (2天)
- [ ] 4.2 性能优化 (1天)
- [ ] 4.3 文档更新 (1天)
- [ ] 4.4 Code Review 与修复 (1天)

---

## 风险与缓解措施

### 风险 1: AI SDK v5 与现有 SSE 格式不兼容
**缓解**: 
- 先在 Web 端创建适配器层,不直接修改后端
- 逐步迁移,保留回退机制

### 风险 2: Android 端引入新抽象层增加复杂度
**缓解**:
- 保持向后兼容,旧代码仍可工作
- 提供清晰的迁移指南
- 编写详细的单元测试

### 风险 3: 工具调用逻辑复杂
**缓解**:
- 分阶段实现: 先支持文本,再支持工具
- 参考 AI SDK 官方示例
- 充分测试边界情况

---

## 成功指标

1. **代码行数减少**: Web 端 conversations.tsx 从 1133 行减少到 ~400 行
2. **bug 率降低**: 流式处理相关 bug 减少 50%+
3. **开发效率提升**: 新增聊天功能开发时间减少 30%
4. **类型安全**: TypeScript/Kotlin 编译时错误捕获率提升
5. **零功能回归**: 所有现有功能正常工作

---

## 后续优化方向

1. **多模态支持**: 图片、视频生成的统一 API
2. **缓存层**: 智能缓存常用响应
3. **离线支持**: 本地消息队列 + 同步
4. **Analytics**: 集成 AI SDK 的遥测功能
5. **A/B Testing**: 不同模型配置的实验框架