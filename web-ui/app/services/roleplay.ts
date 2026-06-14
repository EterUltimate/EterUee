import api, { appendWebAuthQuery } from "~/services/api";
import type {
  CreateRoleplayChatRequest,
  GenerateRoleplayRequest,
  RoleplayBookmark,
  RoleplayCharacter,
  RoleplayChatMessage,
  RoleplayChatMetadata,
  RoleplayGenerationEvent,
  RoleplayGroup,
  RoleplayGroupMember,
  RoleplayImportResult,
  RoleplayMessagesResponse,
  RoleplayMessageNode,
  RoleplayPreset,
  RoleplaySummary,
  RoleplayWorldInfo,
  RoleplayWorldInfoEntry,
  SaveRoleplayCharacterRequest,
  SaveRoleplayGroupRequest,
  SaveRoleplayPresetRequest,
  SaveRoleplayWorldInfoRequest,
} from "~/types/roleplay";

function fileFormData(file: File): FormData {
  const formData = new FormData();
  formData.append("file", file, file.name);
  return formData;
}

async function downloadRoleplayFile(path: string): Promise<Blob> {
  const response = await fetch(appendWebAuthQuery(`/api/${path}`));
  if (!response.ok) {
    let message = `Download failed (${response.status})`;
    try {
      const data = (await response.json()) as { error?: string };
      message = data.error ?? message;
    } catch {
      // Keep the status-derived message.
    }
    throw new Error(message);
  }
  return response.blob();
}

async function streamRoleplayGeneration(
  id: string,
  data: GenerateRoleplayRequest,
  callbacks: {
    onEvent: (event: RoleplayGenerationEvent) => void;
    onError?: (error: Error) => void;
  },
): Promise<void> {
  try {
    const response = await fetch(appendWebAuthQuery(`/api/roleplay/chats/${id}/generate`), {
      method: "POST",
      headers: {
        Accept: "text/event-stream",
        "Content-Type": "application/json",
      },
      body: JSON.stringify(data),
    });

    if (!response.ok) {
      throw new Error(`Generation failed (${response.status})`);
    }
    if (!response.body) {
      throw new Error("Generation response body is empty");
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = "";
    let eventData = "";

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split("\n");
      buffer = lines.pop() ?? "";

      for (const rawLine of lines) {
        const line = rawLine.replace(/\r$/, "");
        if (line.startsWith("data:")) {
          eventData += (eventData ? "\n" : "") + line.slice(5).trim();
        } else if (line === "" && eventData) {
          callbacks.onEvent(JSON.parse(eventData) as RoleplayGenerationEvent);
          eventData = "";
        }
      }
    }
  } catch (error) {
    callbacks.onError?.(error instanceof Error ? error : new Error(String(error)));
  }
}

export const roleplayApi = {
  summary: () => api.get<RoleplaySummary>("roleplay/summary"),

  characters: {
    list: () => api.get<RoleplayCharacter[]>("roleplay/characters"),
    get: (id: string) => api.get<RoleplayCharacter>(`roleplay/characters/${id}`),
    create: (data: SaveRoleplayCharacterRequest) =>
      api.post<RoleplayCharacter>("roleplay/characters", data),
    update: (id: string, data: SaveRoleplayCharacterRequest) =>
      api.put<RoleplayCharacter>(`roleplay/characters/${id}`, data),
    delete: (id: string) => api.delete<{ status: string }>(`roleplay/characters/${id}`),
    favorite: (id: string) => api.post<{ favorite: boolean }>(`roleplay/characters/${id}/favorite`),
    importFile: (file: File) =>
      api.postMultipart<RoleplayImportResult<RoleplayCharacter>>("roleplay/characters/import", fileFormData(file)),
    exportJson: (id: string, format = "v3") =>
      downloadRoleplayFile(`roleplay/characters/${id}/export.json?format=${encodeURIComponent(format)}`),
    exportPng: (id: string, format = "v3") =>
      downloadRoleplayFile(`roleplay/characters/${id}/export.png?format=${encodeURIComponent(format)}`),
    chats: (id: string) => api.get<RoleplayChatMetadata[]>(`roleplay/characters/${id}/chats`),
    createChat: (id: string, data: CreateRoleplayChatRequest = {}) =>
      api.post<RoleplayChatMetadata>(`roleplay/characters/${id}/chats`, data),
  },

  chats: {
    importFile: (file: File, characterId: string, groupId?: string | null) => {
      const query = new URLSearchParams({ characterId });
      if (groupId) query.set("groupId", groupId);
      return api.postMultipart<RoleplayImportResult<RoleplayChatMetadata>>(
        `roleplay/chats/import?${query.toString()}`,
        fileFormData(file),
      );
    },
    get: (id: string) => api.get<RoleplayChatMetadata>(`roleplay/chats/${id}`),
    delete: (id: string) => api.delete<{ status: string }>(`roleplay/chats/${id}`),
    rename: (id: string, title: string) =>
      api.put<{ status: string }>(`roleplay/chats/${id}/title`, { title }),
    pin: (id: string) => api.post<{ pinned: boolean }>(`roleplay/chats/${id}/pin`),
    messages: (id: string, offset = 0, limit = 200) =>
      api.get<RoleplayMessagesResponse>(`roleplay/chats/${id}/messages?offset=${offset}&limit=${limit}`),
    append: (id: string, role: "USER" | "ASSISTANT", content: string) =>
      api.post<RoleplayChatMessage>(`roleplay/chats/${id}/messages`, { role, content }),
    editMessage: (id: string, messageId: string, content: string) =>
      api.put<{ status: string }>(`roleplay/chats/${id}/messages/${messageId}`, { content }),
    deleteMessage: (id: string, messageId: string) =>
      api.delete<{ status: string }>(`roleplay/chats/${id}/messages/${messageId}`),
    clear: (id: string) => api.post<{ status: string }>(`roleplay/chats/${id}/clear`),
    branches: (id: string) => api.get<RoleplayMessageNode[]>(`roleplay/chats/${id}/branches`),
    createBranch: (id: string, fromMessageIndex: number) =>
      api.post<{ branchId: string }>(`roleplay/chats/${id}/branches`, { fromMessageIndex }),
    selectBranch: (id: string, branchId: string) =>
      api.post<{ status: string }>(`roleplay/chats/${id}/branches/${branchId}/select`),
    deleteBranch: (id: string, branchId: string) =>
      api.delete<{ status: string }>(`roleplay/chats/${id}/branches/${branchId}`),
    addSwipe: (id: string, index: number, content: string) =>
      api.post<{ status: string }>(`roleplay/chats/${id}/messages/${index}/swipes`, { content }),
    nextSwipe: (id: string, index: number) =>
      api.post<{ status: string }>(`roleplay/chats/${id}/messages/${index}/swipes/next`),
    previousSwipe: (id: string, index: number) =>
      api.post<{ status: string }>(`roleplay/chats/${id}/messages/${index}/swipes/previous`),
    editMessageAt: (id: string, index: number, content: string) =>
      api.put<{ status: string }>(`roleplay/chats/${id}/messages/index/${index}`, { content }),
    deleteMessageAt: (id: string, index: number) =>
      api.delete<{ status: string }>(`roleplay/chats/${id}/messages/index/${index}`),
    bookmarks: (id: string) => api.get<RoleplayBookmark[]>(`roleplay/chats/${id}/bookmarks`),
    addBookmark: (id: string, messageIndex: number, title = "", note = "") =>
      api.post<RoleplayBookmark>(`roleplay/chats/${id}/bookmarks`, { messageIndex, title, note }),
    updateBookmark: (id: string, bookmarkId: string, title: string, note = "") =>
      api.put<{ status: string }>(`roleplay/chats/${id}/bookmarks/${bookmarkId}`, {
        messageIndex: 0,
        title,
        note,
      }),
    deleteBookmark: (id: string, bookmarkId: string) =>
      api.delete<{ status: string }>(`roleplay/chats/${id}/bookmarks/${bookmarkId}`),
    exportJsonl: (id: string) => downloadRoleplayFile(`roleplay/chats/${id}/export.jsonl`),
    exportTxt: (id: string) => downloadRoleplayFile(`roleplay/chats/${id}/export.txt`),
    exportHtml: (id: string) => downloadRoleplayFile(`roleplay/chats/${id}/export.html`),
    generate: streamRoleplayGeneration,
    generateUrl: (id: string) => `/api/roleplay/chats/${id}/generate`,
  },

  groups: {
    list: () => api.get<RoleplayGroup[]>("roleplay/groups"),
    create: (data: SaveRoleplayGroupRequest) => api.post<RoleplayGroup>("roleplay/groups", data),
    update: (id: string, data: SaveRoleplayGroupRequest) =>
      api.put<RoleplayGroup>(`roleplay/groups/${id}`, data),
    delete: (id: string) => api.delete<{ status: string }>(`roleplay/groups/${id}`),
    addMember: (id: string, member: RoleplayGroupMember) =>
      api.post<RoleplayGroup>(`roleplay/groups/${id}/members`, member),
    updateMember: (id: string, characterId: string, member: RoleplayGroupMember) =>
      api.put<RoleplayGroup>(`roleplay/groups/${id}/members/${characterId}`, member),
    removeMember: (id: string, characterId: string) =>
      api.delete<RoleplayGroup>(`roleplay/groups/${id}/members/${characterId}`),
    toggleMember: (id: string, characterId: string) =>
      api.post<RoleplayGroup>(`roleplay/groups/${id}/members/${characterId}/active`),
    chats: (id: string) => api.get<RoleplayChatMetadata[]>(`roleplay/groups/${id}/chats`),
    createChat: (id: string, data: CreateRoleplayChatRequest = {}) =>
      api.post<RoleplayChatMetadata>(`roleplay/groups/${id}/chats`, data),
  },

  worldInfos: {
    list: () => api.get<RoleplayWorldInfo[]>("roleplay/world-infos"),
    create: (data: SaveRoleplayWorldInfoRequest) =>
      api.post<RoleplayWorldInfo>("roleplay/world-infos", data),
    update: (id: string, data: RoleplayWorldInfo) =>
      api.put<RoleplayWorldInfo>(`roleplay/world-infos/${id}`, data),
    delete: (id: string) => api.delete<{ status: string }>(`roleplay/world-infos/${id}`),
    addEntry: (id: string, entry: RoleplayWorldInfoEntry) =>
      api.post<RoleplayWorldInfo>(`roleplay/world-infos/${id}/entries`, entry),
    updateEntry: (id: string, entryId: string, entry: RoleplayWorldInfoEntry) =>
      api.put<RoleplayWorldInfo>(`roleplay/world-infos/${id}/entries/${entryId}`, entry),
    deleteEntry: (id: string, entryId: string) =>
      api.delete<RoleplayWorldInfo>(`roleplay/world-infos/${id}/entries/${entryId}`),
    toggleEntry: (id: string, entryId: string) =>
      api.post<RoleplayWorldInfo>(`roleplay/world-infos/${id}/entries/${entryId}/enabled`),
    match: (id: string, messages: string[], scanDepth = 4) =>
      api.post<RoleplayWorldInfoEntry[]>(`roleplay/world-infos/${id}/match`, { messages, scanDepth }),
    importFile: (file: File) =>
      api.postMultipart<RoleplayImportResult<RoleplayWorldInfo>>("roleplay/world-infos/import", fileFormData(file)),
    exportJson: (id: string) => downloadRoleplayFile(`roleplay/world-infos/${id}/export.json`),
  },

  presets: {
    list: (type?: string) =>
      api.get<RoleplayPreset[]>(`roleplay/presets${type ? `?type=${encodeURIComponent(type)}` : ""}`),
    create: (data: SaveRoleplayPresetRequest) => api.post<RoleplayPreset>("roleplay/presets", data),
    update: (id: string, data: SaveRoleplayPresetRequest) =>
      api.put<RoleplayPreset>(`roleplay/presets/${id}`, data),
    delete: (id: string) => api.delete<{ status: string }>(`roleplay/presets/${id}`),
    importFile: (file: File) =>
      api.postMultipart<RoleplayImportResult<RoleplayPreset>>("roleplay/presets/import", fileFormData(file)),
    exportJson: (id: string) => downloadRoleplayFile(`roleplay/presets/${id}/export.json`),
  },
};
