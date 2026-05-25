import type { Message as AIMessage } from "@ai-sdk/react";

import type { MessageDto, MessageNodeDto, ProviderModel, UIMessagePart } from "~/types";

export interface EterUeeAISDKModel {
  modelId: string;
  displayName: string;
  id: string;
  type: ProviderModel["type"];
  inputModalities?: ProviderModel["inputModalities"];
  outputModalities?: ProviderModel["outputModalities"];
  abilities?: ProviderModel["abilities"];
}

export interface EterUeeStreamTextRequest {
  model: EterUeeAISDKModel;
  messages: Array<{
    id: string;
    role: string;
    parts: UIMessagePart[];
    createdAt?: string;
    modelId?: string | null;
  }>;
}

type AICompatibleRole = "system" | "user" | "assistant" | "data";
type AISDKMessagePart = NonNullable<AIMessage["parts"]>[number];

function toAICompatibleRole(role: string): AICompatibleRole {
  const normalized = role.toLowerCase();
  if (
    normalized === "system" ||
    normalized === "user" ||
    normalized === "assistant" ||
    normalized === "data"
  ) {
    return normalized;
  }
  return "assistant";
}

function getTextContent(parts: UIMessagePart[]): string {
  return parts
    .flatMap((part) => (part.type === "text" ? [part.text] : []))
    .join("");
}

function toAISDKParts(parts: UIMessagePart[]): NonNullable<AIMessage["parts"]> {
  const aiParts: AISDKMessagePart[] = [];

  for (const part of parts) {
    switch (part.type) {
      case "text":
        aiParts.push({ type: "text", text: part.text });
        break;
      case "reasoning":
        aiParts.push({
          type: "reasoning",
          reasoning: part.reasoning,
          details: [{ type: "text", text: part.reasoning }],
        });
        break;
      case "image":
      case "video":
      case "audio":
      case "document":
      case "tool":
        break;
    }
  }

  return aiParts;
}

export function toAISDKMessages(messageNodes: MessageNodeDto[]): AIMessage[] {
  return messageNodes.flatMap((node) => {
    const selectedMessage = node.messages[node.selectIndex] ?? node.messages[0];
    if (!selectedMessage) {
      return [];
    }

    const content = getTextContent(selectedMessage.parts);
    const parts = toAISDKParts(selectedMessage.parts);

    return [
      {
        id: selectedMessage.id,
        role: toAICompatibleRole(selectedMessage.role),
        content,
        parts,
        createdAt: new Date(selectedMessage.createdAt),
      },
    ];
  });
}

export function toAISDKRequestMessages(messages: MessageDto[]): EterUeeStreamTextRequest["messages"] {
  return messages.map((message) => ({
    id: message.id,
    role: message.role,
    parts: message.parts,
    createdAt: message.createdAt,
    modelId: message.modelId,
  }));
}

export function toEterUeeAISDKModel(model: ProviderModel): EterUeeAISDKModel {
  return {
    modelId: model.modelId,
    displayName: model.displayName,
    id: model.id,
    type: model.type,
    inputModalities: model.inputModalities,
    outputModalities: model.outputModalities,
    abilities: model.abilities,
  };
}

export const ETERUEE_AI_SDK_CHAT_API = "/api/agent/conversations/stream-v2/chat";
