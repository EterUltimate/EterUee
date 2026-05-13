/**
 * EterUee AI SDK 集成指南
 * 
 * 重要说明:
 * - 我们不使用 @ai-sdk/provider 的 createProvider
 * - 而是直接使用 useChat hook 并配置 SSE API 端点
 * - 这是因为后端已经提供了完整的 SSE 流式 API
 * 
 * 使用方法:
 * ```typescript
 * import { useChat } from '@ai-sdk/react';
 * 
 * const { messages, input, handleInputChange, handleSubmit, isLoading } = useChat({
 *   id: conversationId,
 *   api: `/api/conversations/${conversationId}/stream`,
 *   initialMessages: convertBackendMessagesToAI_SDK(initialMessages),
 *   onFinish: (message) => {
 *     // 保存对话
 *   },
 *   onError: (error) => {
 *     toast.error(error.message);
 *   }
 * });
 * ```
 */

import type { MessageDto, MessageNodeDto } from '~/types';

/**
 * 将后端消息格式转换为 AI SDK 格式
 * 
 * 后端格式: MessageDto { role, parts: UIMessagePart[], ... }
 * AI SDK 格式: { role, content: string | Array<{type, text, ...}>, ... }
 */
export function convertBackendMessagesToAI_SDK(
  messageNodes: MessageNodeDto[]
): Array<{ id: string; role: string; content: string }> {
  const messages: Array<{ id: string; role: string; content: string }> = [];
  
  for (const node of messageNodes) {
    // 使用 selectIndex 选择当前显示的消息
    const selectedIndex = node.selectIndex ?? 0;
    const message = node.messages[selectedIndex];
    
    if (!message) continue;
    
    // 提取文本内容
    let textContent = '';
    for (const part of message.parts) {
      if (part.type === 'text' && typeof part.text === 'string') {
        textContent += part.text;
      }
    }
    
    messages.push({
      id: message.id,
      role: message.role.toLowerCase(), // 'user', 'assistant', 'system'
      content: textContent,
    });
  }
  
  return messages;
}

/**
 * 注意: 对于 SSE 事件解析,useChat hook 会自动处理标准的 SSE 格式。
 * 后端的 SSE 事件应该符合以下格式:
 * 
 * event: message
 * data: {"text": "增量文本"}
 * 
 * 如果后端使用自定义事件类型,需要在 conversations.tsx 中手动处理。
 */
