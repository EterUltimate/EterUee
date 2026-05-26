# /stream-v2 端点使用指南

## 📋 概述

`/api/conversations/stream-v2` 是 AI SDK v5 标准化的 SSE 流式端点,提供与 Vercel AI SDK 兼容的事件格式。

## 🔗 端点信息

- **URL**: `POST /api/conversations/stream-v2`
- **Content-Type**: `application/json`
- **Response**: Server-Sent Events (SSE)

## 📥 请求格式

```json
{
  "model": {
    "providerId": "openai",
    "modelId": "gpt-4"
  },
  "messages": [
    {
      "id": "msg_1",
      "role": "user",
      "parts": [
        {
          "type": "text",
          "text": "你好,请介绍一下你自己"
        }
      ]
    }
  ],
  "temperature": 0.7,
  "maxTokens": 2000
}
```

### 请求字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| model | Model | ✅ | 模型配置 |
| messages | UIMessage[] | ✅ | 消息历史 |
| temperature | Float | ❌ | 温度参数 (0-2) |
| topP | Float | ❌ | Top-P 采样 |
| maxTokens | Int | ❌ | 最大 token 数 |
| tools | Tool[] | ❌ | 可用工具列表 |
| customHeaders | CustomHeader[] | ❌ | 自定义请求头 |
| customBody | CustomBody[] | ❌ | 自定义请求体 |

## 📤 响应事件格式

### 1. text-delta (文本增量)

当 AI 生成文本时,会持续发送此事件:

```
event: text-delta
data: {"textDelta":"你好"}

event: text-delta
data: {"textDelta":"! 我是"}

event: text-delta
data: {"textDelta":"一个 AI 助手"}
```

**TypeScript 示例**:
```typescript
const eventSource = new EventSource('/api/conversations/stream-v2');

eventSource.addEventListener('text-delta', (event) => {
  const data = JSON.parse(event.data);
  console.log('收到文本增量:', data.textDelta);
  // 累积显示文本
});
```

### 2. tool-call (工具调用)

当 AI 需要调用工具时发送:

```
event: tool-call
data: {
  "toolCallId": "call_abc123",
  "toolName": "web_search",
  "arguments": "{\"query\":\"最新新闻\"}"
}
```

**TypeScript 示例**:
```typescript
eventSource.addEventListener('tool-call', (event) => {
  const data = JSON.parse(event.data);
  console.log('工具调用:', data.toolName);
  console.log('参数:', JSON.parse(data.arguments));
  
  // 执行工具并返回结果
  const result = executeTool(data.toolName, data.arguments);
  sendToolResult(data.toolCallId, result);
});
```

### 3. usage (使用量统计)

生成完成后发送 token 使用情况:

```
event: usage
data: {
  "promptTokens": 100,
  "completionTokens": 50,
  "totalTokens": 150
}
```

**TypeScript 示例**:
```typescript
eventSource.addEventListener('usage', (event) => {
  const data = JSON.parse(event.data);
  console.log('Token 使用量:');
  console.log('- Prompt:', data.promptTokens);
  console.log('- Completion:', data.completionTokens);
  console.log('- Total:', data.totalTokens);
});
```

### 4. finish (完成事件)

标记生成结束:

```
event: finish
data: {
  "finishReason": "stop",
  "usage": null
}
```

**finishReason 可能的值**:
- `"stop"` - 自然停止
- `"length"` - 达到最大长度
- `"content_filter"` - 内容过滤
- `"tool_calls"` - 工具调用

**TypeScript 示例**:
```typescript
eventSource.addEventListener('finish', (event) => {
  const data = JSON.parse(event.data);
  console.log('生成完成,原因:', data.finishReason);
  eventSource.close();
});
```

### 5. error (错误事件)

发生错误时发送:

```
event: error
data: {
  "error": "API key is invalid",
  "code": "AUTH_ERROR"
}
```

**TypeScript 示例**:
```typescript
eventSource.addEventListener('error', (event) => {
  const data = JSON.parse(event.data);
  console.error('错误:', data.error);
  console.error('错误代码:', data.code);
  eventSource.close();
});
```

## 💻 完整使用示例

### TypeScript/React

```typescript
import { useState, useEffect } from 'react';

function ChatStream() {
  const [messages, setMessages] = useState<string[]>([]);
  const [isGenerating, setIsGenerating] = useState(false);

  const startStream = async () => {
    setIsGenerating(true);
    setMessages([]);

    const response = await fetch('/api/conversations/stream-v2', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        model: {
          providerId: 'openai',
          modelId: 'gpt-4'
        },
        messages: [
          {
            id: 'msg_1',
            role: 'user',
            parts: [{ type: 'text', text: '写一首诗' }]
          }
        ],
        temperature: 0.7
      })
    });

    const reader = response.body!.getReader();
    const decoder = new TextDecoder();
    let buffer = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop() || '';

      for (const line of lines) {
        if (line.startsWith('event: ')) {
          const eventType = line.slice(7);
          const dataLine = lines.shift();
          
          if (dataLine?.startsWith('data: ')) {
            const data = JSON.parse(dataLine.slice(6));
            
            switch (eventType) {
              case 'text-delta':
                setMessages(prev => [...prev, data.textDelta]);
                break;
              case 'finish':
                setIsGenerating(false);
                break;
              case 'error':
                console.error('Stream error:', data.error);
                setIsGenerating(false);
                break;
            }
          }
        }
      }
    }
  };

  return (
    <div>
      <button onClick={startStream} disabled={isGenerating}>
        开始生成
      </button>
      <div>{messages.join('')}</div>
    </div>
  );
}
```

### Kotlin/Android

```kotlin
fun streamText(request: StreamTextRequest) {
    viewModelScope.launch {
        try {
            val response = httpClient.post("/api/conversations/stream-v2") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            
            response.bodyAsFlow().collect { chunk ->
                when (chunk) {
                    is TextChunk.TextDelta -> {
                        // 更新 UI
                        updateMessage(chunk.text)
                    }
                    is TextChunk.Finish -> {
                        // 生成完成
                        onGenerationComplete()
                    }
                }
            }
        } catch (e: Exception) {
            // 处理错误
            onError(e.message)
        }
    }
}
```

### Python

```python
import requests
import json

def stream_text():
    url = "http://localhost:18080/api/conversations/stream-v2"
    
    payload = {
        "model": {
            "providerId": "openai",
            "modelId": "gpt-4"
        },
        "messages": [
            {
                "id": "msg_1",
                "role": "user",
                "parts": [{"type": "text", "text": "Hello"}]
            }
        ]
    }
    
    response = requests.post(url, json=payload, stream=True)
    
    for line in response.iter_lines():
        if line:
            line_str = line.decode('utf-8')
            if line_str.startswith('event: '):
                event_type = line_str[7:]
                # 读取下一行数据
                data_line = next(response.iter_lines()).decode('utf-8')
                if data_line.startswith('data: '):
                    data = json.loads(data_line[6:])
                    
                    if event_type == 'text-delta':
                        print(data['textDelta'], end='', flush=True)
                    elif event_type == 'finish':
                        print("\n生成完成")
                        break
                    elif event_type == 'error':
                        print(f"\n错误: {data['error']}")
                        break

stream_text()
```

## 🔍 调试技巧

### 1. 使用 curl 测试

```bash
curl -N -X POST http://localhost:18080/api/conversations/stream-v2 \
  -H "Content-Type: application/json" \
  -d '{
    "model": {
      "providerId": "openai",
      "modelId": "gpt-4"
    },
    "messages": [
      {
        "id": "msg_1",
        "role": "user",
        "parts": [{"type": "text", "text": "Say hello"}]
      }
    ]
  }'
```

### 2. 浏览器开发者工具

打开 Network 标签,查看 SSE 连接:
- 检查事件类型是否正确
- 验证数据格式
- 监控延迟和性能

### 3. 日志记录

在服务器端添加日志:

```kotlin
sse("/stream-v2") {
    aiSDK.streamText(request).collect { chunk ->
        Log.d("StreamV2", "Received chunk: $chunk")
        // ... 发送事件
    }
}
```

## ⚠️ 注意事项

1. **向后兼容**: `/stream-v2` 是新端点,不影响现有的 `/stream` 端点
2. **事件顺序**: 事件按以下顺序发送:
   - 多个 `text-delta` (流式文本)
   - 可选的 `tool-call` (工具调用)
   - `usage` (使用量统计)
   - `finish` (完成信号)
3. **错误处理**: 任何错误都会发送 `error` 事件并关闭连接
4. **心跳**: Ktor 会自动发送心跳保持连接活跃
5. **超时**: 建议客户端设置合理的超时时间 (如 60 秒)

## 📊 性能指标

| 指标 | 目标值 |
|------|--------|
| 首字节时间 (TTFB) | < 500ms |
| 事件延迟 | < 100ms |
| 错误率 | < 0.1% |
| 并发连接数 | > 100 |

## 🔗 相关资源

- [AI SDK Streaming 文档](https://sdk.vercel.ai/docs/advanced/streaming)
- [SSE 规范](https://html.spec.whatwg.org/multipage/server-sent-events.html)
- [Ktor SSE 文档](https://ktor.io/docs/server-sent-events.html)
- [后端 SSE 标准化实施指南](./BACKEND_SSE_STANDARDIZATION.md)

---

**创建日期**: 2026-05-13  
**版本**: v1.0  
**状态**: ✅ 已实现
