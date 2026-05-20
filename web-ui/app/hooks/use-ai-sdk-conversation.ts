import * as React from "react";

import { useChat } from "@ai-sdk/react";
import type { Message as AIMessage } from "@ai-sdk/react";
import { v4 as uuidv4 } from "uuid";

import {
  ETERUEE_AI_SDK_CHAT_API,
  toAISDKMessages,
  toAISDKRequestMessages,
  toEterUeeAISDKModel,
  type EterUeeStreamTextRequest,
} from "~/lib/ai-sdk-provider";
import { appendWebAuthQuery } from "~/services/api";
import type { ConversationDto, ProviderModel, UIMessagePart } from "~/types";

interface UseAISDKConversationOptions {
  conversation: ConversationDto | null;
  currentModel: ProviderModel | null;
  onPersistedSubmit: (parts: UIMessagePart[], conversationId?: string) => Promise<void>;
}

function toSelectedMessages(conversation: ConversationDto | null) {
  if (!conversation) {
    return [];
  }

  return conversation.messages.flatMap((node) => {
    const selectedMessage = node.messages[node.selectIndex] ?? node.messages[0];
    return selectedMessage ? [selectedMessage] : [];
  });
}

function toUserAIMessage(parts: UIMessagePart[]): AIMessage {
  const content = parts
    .flatMap((part) => (part.type === "text" ? [part.text] : []))
    .join("");

  return {
    id: uuidv4(),
    role: "user",
    content,
    parts: parts.flatMap((part) => (part.type === "text" ? [{ type: "text" as const, text: part.text }] : [])),
  };
}

export function useAISDKConversation({
  conversation,
  currentModel,
  onPersistedSubmit,
}: UseAISDKConversationOptions) {
  const persistedMessages = React.useMemo(() => toSelectedMessages(conversation), [conversation]);
  const initialMessages = React.useMemo(() => toAISDKMessages(conversation?.messages ?? []), [conversation]);

  const latestSubmitRef = React.useRef<{
    parts: UIMessagePart[];
    conversationId?: string;
  } | null>(null);
  const persistedSubmitRef = React.useRef(onPersistedSubmit);

  React.useEffect(() => {
    persistedSubmitRef.current = onPersistedSubmit;
  }, [onPersistedSubmit]);

  const chat = useChat({
    id: conversation?.id ?? "new-conversation",
    api: ETERUEE_AI_SDK_CHAT_API,
    initialMessages,
    streamProtocol: "data",
    keepLastMessageOnError: false,
    fetch: async (input, init) => {
      const latestSubmit = latestSubmitRef.current;
      if (latestSubmit) {
        await persistedSubmitRef.current(latestSubmit.parts, latestSubmit.conversationId);
        return new Response(
          `d:${JSON.stringify({
            finishReason: "stop",
            usage: { promptTokens: 0, completionTokens: 0 },
          })}\n`,
          {
            headers: {
              "Content-Type": "text/plain; charset=utf-8",
            },
          },
        );
      }

      const url = typeof input === "string" ? appendWebAuthQuery(input) : input;
      return fetch(url, init);
    },
    experimental_prepareRequestBody: (): EterUeeStreamTextRequest => {
      if (latestSubmitRef.current) {
        return {
          model: {
            modelId: "",
            displayName: "",
            id: "",
            type: "CHAT",
          },
          messages: [],
        };
      }

      if (!currentModel) {
        throw new Error("No chat model is selected.");
      }

      return {
        model: toEterUeeAISDKModel(currentModel),
        messages: toAISDKRequestMessages(persistedMessages),
      };
    },
  });

  React.useEffect(() => {
    chat.setMessages(initialMessages);
  }, [chat.setMessages, initialMessages]);

  const submitPersisted = React.useCallback(
    async (parts: UIMessagePart[], conversationId?: string) => {
      latestSubmitRef.current = { parts, conversationId };
      try {
        await chat.append(toUserAIMessage(parts), { allowEmptySubmit: true });
      } finally {
        latestSubmitRef.current = null;
      }
    },
    [chat],
  );

  return {
    ...chat,
    submitPersisted,
    aiSdkStatus: chat.status,
  };
}
