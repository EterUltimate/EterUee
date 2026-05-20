type ClientToolCall = {
  toolCallId: string;
  toolName: string;
  args: unknown;
};

export function useConversationTools(conversationId: string) {
  const invokeTool = async (toolCall: ClientToolCall) => {
    console.info("Tool call from AI SDK:", { conversationId, toolCall });
    return `Tool ${toolCall.toolName} execution is handled by the backend conversation stream.`;
  };

  return { invokeTool };
}
