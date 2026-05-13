# 后端 SSE API 标准化实施指南

## 📋 概述

本文档说明如何将现有的 SSE 事件格式标准化为 AI SDK v5 兼容格式。

## 🎯 目标

1. **向后兼容**: 保留现有 `/api/conversations/{id}/stream` 端点
2. **新增标准端点**: 添加 `/api/conversations/{id}/stream-v2` 使用新格式
3. **渐进迁移**: Web 和 Android 端可以逐步迁移到新格式

## 📊 事件格式对比

### 当前格式 (v1)

```kotlin
// 快照事件
event: snapshot
data: {
  "type": "snapshot",
  "seq": 1,
  "conversation": { ... },
  "serverTime": 1234567890
}

// 节点更新事件
event: node_update
data: {
  "type": "node_update",
  "seq": 2,
  "conversationId": "...",
  "nodeId": "...",
  "nodeIndex": 0,
  "node": { ... },
  "updateAt": 1234567890,
  "isGenerating": true,
  "serverTime": 1234567890
}

// 错误事件
event: error
data: {
  "type": "error",
  "message": "错误信息"
}
```

### 标准格式 (v2 - AI SDK 兼容)

```kotlin
// 文本增量事件
event: text-delta
data: {
  "textDelta": "增量文本内容"
}

// 工具调用开始
event: tool-call-start
data: {
  "toolCallId": "call_123",
  "toolName": "web_search",
  "args": { "query": "..." }
}

// 工具调用结果
event: tool-call-result
data: {
  "toolCallId": "call_123",
  "result": "{...}"
}

// 使用量统计
event: usage
data: {
  "promptTokens": 100,
  "completionTokens": 50,
  "totalTokens": 150
}

// 完成事件
event: finish
data: {
  "finishReason": "stop",
  "usage": {
    "promptTokens": 100,
    "completionTokens": 50,
    "totalTokens": 150
  }
}

// 错误事件
event: error
data: {
  "error": "错误信息",
  "code": "GENERATION_ERROR"
}

// 元数据事件 (可选)
event: metadata
data: {
  "messageId": "...",
  "modelId": "gpt-4",
  "timestamp": 1234567890
}
```

## 🔧 实施步骤

### Step 1: 创建标准化事件 DTO

文件: `app/src/main/java/com/eterultimate/eteruee/web/dto/AISDKEvents.kt`

✅ 已创建,包含:
- `TextDeltaEvent`
- `ToolCallStartEvent`
- `ToolCallResultEvent`
- `UsageEvent`
- `FinishEvent`
- `StandardErrorEvent`
- `MetadataEvent`

### Step 2: 创建新的 SSE 端点

在 `ConversationRoutes.kt` 中添加:

```kotlin
// SSE /api/conversations/{id}/stream-v2 - AI SDK v5 compatible stream
sse("/{id}/stream-v2") {
    val id = call.parameters["id"] ?: return@sse
    val uuid = runCatching { Uuid.parse(id) }.getOrNull() ?: return@sse

    chatService.initializeConversation(uuid)
    chatService.addConversationReference(uuid)

    heartbeat {
        period = 1.seconds
    }

    try {
        // 监听 AISDK 的流式输出
        aiSDK.streamText(StreamTextRequest(
            conversationId = uuid,
            // ... 其他参数
        )).collect { chunk ->
            when (chunk) {
                is TextChunk.TextDelta -> {
                    send(
                        event = "text-delta",
                        data = JsonInstant.encodeToString(
                            TextDeltaEvent(textDelta = chunk.text)
                        )
                    )
                }
                is TextChunk.ToolCallStart -> {
                    send(
                        event = "tool-call-start",
                        data = JsonInstant.encodeToString(
                            ToolCallStartEvent(
                                toolCallId = chunk.toolCallId,
                                toolName = chunk.toolName,
                                args = chunk.args
                            )
                        )
                    )
                }
                is TextChunk.ToolCallResult -> {
                    send(
                        event = "tool-call-result",
                        data = JsonInstant.encodeToString(
                            ToolCallResultEvent(
                                toolCallId = chunk.toolCallId,
                                result = chunk.result
                            )
                        )
                    )
                }
                is TextChunk.Usage -> {
                    send(
                        event = "usage",
                        data = JsonInstant.encodeToString(
                            UsageEvent(
                                promptTokens = chunk.promptTokens,
                                completionTokens = chunk.completionTokens,
                                totalTokens = chunk.promptTokens + chunk.completionTokens
                            )
                        )
                    )
                }
                is TextChunk.Finish -> {
                    send(
                        event = "finish",
                        data = JsonInstant.encodeToString(
                            FinishEvent(
                                finishReason = chunk.finishReason,
                                usage = chunk.usage?.let { 
                                    UsageEvent(
                                        promptTokens = it.promptTokens,
                                        completionTokens = it.completionTokens,
                                        totalTokens = it.promptTokens + it.completionTokens
                                    )
                                }
                            )
                        )
                    )
                }
            }
        }
    } catch (e: Exception) {
        send(
            event = "error",
            data = JsonInstant.encodeToString(
                StandardErrorEvent(
                    error = e.message ?: "Unknown error",
                    code = "STREAM_ERROR"
                )
            )
        )
    } finally {
        chatService.removeConversationReference(uuid)
    }
}
```

### Step 3: 扩展现有 ChatService

需要修改 `ChatService` 以支持 AI SDK 的流式输出:

```kotlin
class ChatService(
    private val aiSDK: AISDK,
    // ... 其他依赖
) {
    /**
     * 流式生成对话响应 (AI SDK v2 格式)
     */
    fun streamConversationV2(conversationId: Uuid): Flow<TextChunk> {
        return flow {
            // 1. 获取对话历史
            val conversation = getConversationFlow(conversationId).first()
            
            // 2. 构建请求
            val request = StreamTextRequest(
                messages = conversation.toUIMessages(),
                model = getCurrentModel(),
                temperature = getCurrentTemperature(),
                maxTokens = getMaxTokens()
            )
            
            // 3. 调用 AISDK
            aiSDK.streamText(request).collect { chunk ->
                emit(chunk)
                
                // 4. 同时保存到数据库 (保持与 v1 兼容)
                when (chunk) {
                    is TextChunk.TextDelta -> {
                        updateMessageContent(conversationId, chunk.text)
                    }
                    is TextChunk.Finish -> {
                        finalizeGeneration(conversationId, chunk.usage)
                    }
                }
            }
        }
    }
}
```

### Step 4: 前端适配器 (可选)

如果前端暂时不想完全迁移,可以创建一个适配器层:

```typescript
// web-ui/app/lib/sse-adapter.ts

/**
 * 将 v2 格式的 SSE 事件转换为 v1 格式
 * 用于渐进式迁移
 */
export function adaptV2ToV1(event: MessageEvent): V1Event {
  const data = JSON.parse(event.data);
  
  switch (event.type) {
    case 'text-delta':
      // 累积文本,定期发送 node_update
      return {
        type: 'node_update',
        node: { /* 构造节点 */ }
      };
      
    case 'finish':
      return {
        type: 'snapshot',
        conversation: { /* 完整对话 */ }
      };
      
    default:
      return null;
  }
}
```

## ✅ 测试清单

### 单元测试

- [ ] 测试 `TextDeltaEvent` 序列化
- [ ] 测试 `ToolCallStartEvent` 序列化
- [ ] 测试 `FinishEvent` 序列化
- [ ] 测试错误处理

### 集成测试

- [ ] 测试 `/stream-v2` 端点能正确发送事件
- [ ] 测试文本增量能正确累积
- [ ] 测试工具调用流程完整
- [ ] 测试错误事件格式正确

### E2E 测试

- [ ] Web 端 useChat hook 能正确接收 v2 事件
- [ ] Android 端 UseChat Hook 能正确接收 v2 事件
- [ ] 消息能正确显示和保存
- [ ] 工具调用能正常执行

## 📈 迁移时间表

### Week 1: 后端实现
- [ ] 创建标准化事件 DTO
- [ ] 实现 `/stream-v2` 端点
- [ ] 扩展 ChatService
- [ ] 编写单元测试

### Week 2: 前端适配
- [ ] Web 端创建 SSE 适配器
- [ ] Android 端创建事件解析器
- [ ] 小范围测试

### Week 3: 全面测试
- [ ] 集成测试
- [ ] E2E 测试
- [ ] 性能测试
- [ ] Bug 修复

### Week 4: 部署
- [ ] 灰度发布
- [ ] 监控指标
- [ ] 用户反馈收集
- [ ] 完全切换 (可选)

## 🔍 监控指标

1. **事件发送成功率**: > 99.9%
2. **平均延迟**: < 100ms
3. **错误率**: < 0.1%
4. **客户端兼容性**: 100%

## 📝 注意事项

1. **向后兼容**: v1 端点必须保留至少 3 个月
2. **文档更新**: API 文档需要同步更新
3. **日志记录**: 记录所有 v2 事件以便调试
4. **性能优化**: 避免频繁的小事件发送,可以批量发送

## 🔗 相关资源

- [AI SDK Streaming 文档](https://sdk.vercel.ai/docs/advanced/streaming)
- [SSE 规范](https://html.spec.whatwg.org/multipage/server-sent-events.html)
- [Ktor SSE 文档](https://ktor.io/docs/server-sent-events.html)

---

**创建日期**: 2026-05-13  
**状态**: 📝 规划中  
**负责人**: 开发团队
