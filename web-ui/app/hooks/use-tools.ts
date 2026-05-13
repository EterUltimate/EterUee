import { experimental_useToolInvocation } from '@ai-sdk/react';
import api from '~/services/api';

export function useConversationTools(conversationId: string) {
  const { invokeTool } = experimental_useToolInvocation({
    id: conversationId,
    onToolCall: async ({ toolCall }) => {
      // 在 EterUee 中,工具调用通常由后端处理并通过 SSE 同步状态
      // 但如果要在前端直接执行,可以在这里处理
      console.log('Tool call from AI SDK:', toolCall);

      // 示例: 调用后端工具审批接口(如果需要前端参与)
      /*
      const result = await api.post(`/conversations/${conversationId}/tool-execution`, {
        toolCallId: toolCall.toolCallId,
        toolName: toolCall.toolName,
        args: toolCall.args
      });
      return result.json();
      */

      return `Tool ${toolCall.toolName} execution not implemented in frontend.`;
    }
  });

  return { invokeTool };
}
