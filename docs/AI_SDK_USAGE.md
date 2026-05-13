# AI SDK 使用指南

本文档说明如何在 EterUee 项目中使用新的 AI SDK。

## 目录

- [Android 端使用](#android-端使用)
- [Web 端使用](#web-端使用)
- [迁移指南](#迁移指南)

---

## Android 端使用

### 1. 创建 AISDK 实例

```kotlin
// 在 Koin 模块中配置
val aiModule = module {
    single<AISDK> { 
        DefaultAISDK(
            provider = get<Provider<*>>(),
            providerSetting = get<ProviderSetting>()
        )
    }
}
```

### 2. 在 Composable 中使用 useChat Hook

```kotlin
@Composable
fun ChatScreen(conversationId: Uuid) {
    val aiSDK: AISDK = koinInject()
    val model: Model = getCurrentModel()
    
    val chatState = useChat(
        conversationId = conversationId,
        aiSDK = aiSDK,
        model = model,
        onFinish = { result ->
            println("生成完成: ${result.text}")
            // 保存到数据库等操作
        },
        onError = { error ->
            println("错误: ${error.message}")
        }
    )
    
    Column {
        // 消息列表
        LazyColumn {
            items(chatState.messages) { message ->
                MessageBubble(message = message)
            }
        }
        
        // 输入框
        ChatInput(
            onSend = { text, attachments ->
                chatState.handleSubmit(text, attachments)
            },
            isLoading = chatState.isLoading
        )
    }
}
```

### 3. 直接调用 generateText (非流式)

```kotlin
viewModelScope.launch {
    try {
        val result = aiSDK.generateText(
            request = GenerateTextRequest(
                model = currentModel,
                messages = listOf(
                    UIMessage.user("你好")
                ),
                temperature = 0.7f
            )
        )
        
        println("回复: ${result.text}")
        println("Token 使用: ${result.usage}")
    } catch (e: AISDKException) {
        println("错误: ${e.message}")
    }
}
```

### 4. 流式生成

```kotlin
viewModelScope.launch {
    aiSDK.streamText(
        request = StreamTextRequest(
            model = currentModel,
            messages = messages
        )
    ).collect { chunk ->
        when (chunk) {
            is TextChunk.TextDelta -> {
                // 更新 UI 显示增量文本
                updateMessage(chunk.text)
            }
            is TextChunk.Usage -> {
                // 处理 token 使用信息
                println("Tokens: ${chunk.tokenUsage}")
            }
            is TextChunk.Finish -> {
                // 生成完成
                println("完成")
            }
            is TextChunk.ToolCall -> {
                // 处理工具调用
                executeTool(chunk)
            }
        }
    }
}
```

---

## Web 端使用

### 1. 安装依赖

```bash
cd web-ui
bun install
```

已安装的包:
- `@ai-sdk/react` - React hooks
- `@ai-sdk/provider` - Provider 接口

### 2. 配置 Provider

在 `app/lib/ai-sdk-provider.ts` 中已经创建了默认 Provider:

```typescript
import { eterueeProvider } from '~/lib/ai-sdk-provider';

// 获取模型实例
const model = eterueeProvider.chatModel('gpt-4');
```

### 3. 使用 useChat Hook (推荐)

```typescript
import { useChat } from '@ai-sdk/react';
import { eterueeProvider } from '~/lib/ai-sdk-provider';

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
    api: '/api/conversations/stream',
    id: id,
    model: eterueeProvider.chatModel('gpt-4'),
    initialMessages: [],
    onFinish: (message) => {
      console.log('生成完成:', message);
    },
    onError: (error) => {
      console.error('错误:', error);
    }
  });
  
  return (
    <div>
      {/* 消息列表 */}
      <div className="messages">
        {messages.map((message) => (
          <div key={message.id}>
            <strong>{message.role}:</strong>
            <p>{message.content}</p>
          </div>
        ))}
      </div>
      
      {/* 输入框 */}
      <form onSubmit={handleSubmit}>
        <input
          value={input}
          onChange={handleInputChange}
          placeholder="输入消息..."
          disabled={isLoading}
        />
        <button type="submit" disabled={isLoading}>
          {isLoading ? '发送中...' : '发送'}
        </button>
      </form>
      
      {/* 错误提示 */}
      {error && <div className="error">{error.message}</div>}
    </div>
  );
}
```

### 4. 使用 generateText (非流式)

```typescript
import { generateText } from 'ai';
import { eterueeProvider } from '~/lib/ai-sdk-provider';

async function sendMessage(prompt: string) {
  const { text, usage, finishReason } = await generateText({
    model: eterueeProvider.chatModel('gpt-4'),
    prompt: prompt,
    temperature: 0.7,
  });
  
  console.log('回复:', text);
  console.log('Token 使用:', usage);
}
```

### 5. 流式生成

```typescript
import { streamText } from 'ai';
import { eterueeProvider } from '~/lib/ai-sdk-provider';

async function streamMessage(prompt: string) {
  const result = await streamText({
    model: eterueeProvider.chatModel('gpt-4'),
    prompt: prompt,
  });
  
  for await (const chunk of result.textStream) {
    console.log('增量:', chunk);
  }
}
```

### 6. 工具调用

```typescript
import { experimental_useToolInvocation } from '@ai-sdk/react';

function ToolExample() {
  const { invokeTool } = experimental_useToolInvocation({
    id: 'conversation-id',
    onToolCall: async ({ toolCall }) => {
      switch (toolCall.toolName) {
        case 'search':
          return await searchWeb(toolCall.args.query);
        case 'calculate':
          return eval(toolCall.args.expression);
        default:
          throw new Error(`Unknown tool: ${toolCall.toolName}`);
      }
    }
  });
  
  return <button onClick={() => invokeTool('search', { query: 'AI news' })}>
    搜索
  </button>;
}
```

---

## 迁移指南

### Android 端迁移步骤

#### Step 1: 保留现有代码

新的 AI SDK 是**可选的**,现有的 `ChatService` 和 `Provider` 接口仍然可以正常工作。

#### Step 2: 逐步替换

1. **创建新的 ViewModel** (可选):
   ```kotlin
   class NewChatVM(
       private val aiSDK: AISDK,
       // ...
   ) : ViewModel() {
       // 使用 useChat hook 的逻辑
   }
   ```

2. **测试新功能**:
   - 在新页面或功能模块中使用 AI SDK
   - 确保所有功能正常

3. **逐步迁移旧代码**:
   - 将 `ChatVM` 中的流式处理逻辑替换为 `useChat`
   - 保持向后兼容

#### Step 3: 清理旧代码 (可选)

当所有功能都迁移完成后,可以考虑:
- 移除 `ChatService` 中的流式处理逻辑
- 简化 `ChatVM`

### Web 端迁移步骤

#### Step 1: 安装依赖

```bash
cd web-ui
bun install
```

#### Step 2: 创建新的聊天组件

不要直接修改 `conversations.tsx`,而是创建一个新的组件:

```typescript
// app/components/new-chat.tsx
import { useChat } from '@ai-sdk/react';

export function NewChat() {
  // 使用 useChat
}
```

#### Step 3: 测试新组件

- 确保 SSE 连接正常
- 测试消息发送和接收
- 测试错误处理

#### Step 4: 替换旧组件

当新组件稳定后:
1. 备份 `conversations.tsx`
2. 用新实现替换
3. 测试所有功能

#### Step 5: 回退方案

如果遇到问题:
```bash
git checkout HEAD -- app/routes/conversations.tsx
```

---

## 常见问题

### Q1: AI SDK 与现有代码冲突怎么办?

**A**: 新的 AI SDK 是独立模块,不会与现有代码冲突。可以并行使用,逐步迁移。

### Q2: 如何处理工具调用?

**A**: 
- Android: 在 `useChat` 中监听 `TextChunk.ToolCall`
- Web: 使用 `experimental_useToolInvocation` hook

### Q3: 性能有影响吗?

**A**: AI SDK 只是封装层,底层仍然使用现有的 OkHttp/SSE,性能几乎没有影响。

### Q4: 需要修改后端 API 吗?

**A**: 不需要。Provider 适配器已经处理了格式转换。

---

## 下一步

- [ ] 完善工具调用支持
- [ ] 添加多模态支持(图片、视频)
- [ ] 实现结构化对象生成
- [ ] 添加缓存层
- [ ] 集成 Analytics

---

## 参考资源

- [Vercel AI SDK 官方文档](https://ai-sdk.dev/docs)
- [AI SDK GitHub](https://github.com/vercel/ai)
- [项目计划文档](./plans/AI_SDK_重构方案.md)
