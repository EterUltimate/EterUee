export type RoleplayMessageRole = "USER" | "ASSISTANT" | "SYSTEM" | "TOOL";

export interface RoleplayCharacter {
  id: string;
  name: string;
  description: string;
  personality: string;
  scenario: string;
  firstMessage: string;
  messageExamples: string;
  systemPrompt: string;
  postHistoryInstructions: string;
  avatarUrl?: string | null;
  creator: string;
  creatorNotes: string;
  tags: string[];
  alternateGreetings: string[];
  characterBook?: unknown | null;
  extensions?: Record<string, unknown>;
  createdAt: string;
  updatedAt: string;
  favorite: boolean;
  chatCount: number;
}

export interface RoleplayChatMetadata {
  chatId: string;
  characterId: string;
  groupId?: string | null;
  title: string;
  userName: string;
  characterName: string;
  createdAt: string;
  updatedAt: string;
  messageCount: number;
  pinned: boolean;
  activeBranchId?: string | null;
  rootNodes: string[];
}

export interface RoleplayChatMessage {
  id: string;
  role: RoleplayMessageRole;
  content: string;
  timestamp: string;
  tavernName: string;
  model?: string | null;
  tokenCount?: number | null;
  swipeAlternatives: string[];
  speakerId?: string | null;
  speakerName?: string | null;
  isStreaming: boolean;
}

export interface RoleplayBookmark {
  id: string;
  chatId: string;
  messageIndex: number;
  title: string;
  note: string;
  createdAt: string;
  updatedAt: string;
}

export interface RoleplayMessageNode {
  id: string;
  messages: RoleplayChatMessage[];
  selectedIndex: number;
  parentId?: string | null;
  children: string[];
  branchLabel: string;
}

export interface RoleplayMessagesResponse {
  nodes: RoleplayMessageNode[];
  count: number;
}

export interface RoleplayGroupMember {
  characterId: string;
  name: string;
  priority: number;
  responseProbability: number;
  forcedResponse: boolean;
}

export interface RoleplayGroup {
  id: string;
  name: string;
  description: string;
  members: RoleplayGroupMember[];
  activeMembers: string[];
  avatarUrl?: string | null;
  createdAt: string;
  updatedAt: string;
}

export type InsertionPosition = "AFTER_SYSTEM_PROMPT" | "BEFORE_LAST_USER_MESSAGE" | "AT_END";
export type ScanTrigger = "ALWAYS" | "FIRST_MESSAGE" | "RECURSIVE_SCAN";
export type SelectiveLogic = "AND" | "OR";

export interface RoleplayWorldInfoEntry {
  id: string;
  key: string;
  keys: string[];
  secondaryKeys: string[];
  comment: string;
  content: string;
  constant: boolean;
  selective: boolean;
  order: number;
  position: InsertionPosition;
  tavernPosition: number;
  enabled: boolean;
  probability: number;
  useProbability: boolean;
  depth: number;
  role: number;
  displayIndex: number;
}

export interface RoleplayWorldInfo {
  id: string;
  name: string;
  description: string;
  entries: RoleplayWorldInfoEntry[];
  scanDepth: number;
  scanTrigger: ScanTrigger;
  selectiveLogic: SelectiveLogic;
  createdAt: string;
  updatedAt: string;
}

export type RoleplayPresetType = "OPENAI" | "KOBOLDAI" | "TEXTGEN" | "CLAUDE" | "GEMINI";

export interface RoleplayPreset {
  id: string;
  name: string;
  description: string;
  type: RoleplayPresetType;
  parameters: Record<string, unknown>;
  createdAt: string;
  updatedAt: string;
}

export interface RoleplaySummary {
  characters: RoleplayCharacter[];
  groups: RoleplayGroup[];
  worldInfos: RoleplayWorldInfo[];
  presets: RoleplayPreset[];
}

export interface SaveRoleplayCharacterRequest {
  name: string;
  description?: string;
  personality?: string;
  scenario?: string;
  firstMessage?: string;
  messageExamples?: string;
  systemPrompt?: string;
  postHistoryInstructions?: string;
  creator?: string;
  creatorNotes?: string;
  tags?: string[];
  alternateGreetings?: string[];
  characterBook?: unknown | null;
  extensions?: Record<string, unknown>;
}

export interface RoleplayImportResult<T> {
  item: T;
}

export interface CreateRoleplayChatRequest {
  title?: string;
}

export interface SaveRoleplayGroupRequest {
  name: string;
  description?: string;
}

export interface SaveRoleplayWorldInfoRequest {
  name?: string;
  description?: string;
  template?: RoleplayWorldInfo;
}

export interface SaveRoleplayPresetRequest {
  name: string;
  description?: string;
  type?: RoleplayPresetType;
  parameters?: Record<string, unknown>;
}

export interface GenerateRoleplayRequest {
  providerId: string;
  modelId: string;
  systemPrompt?: string;
  temperature?: number;
  maxTokens?: number;
  userMessage?: string | null;
}

export interface RoleplayGenerationEvent {
  type: "delta" | "complete" | "error";
  delta?: string | null;
  message?: RoleplayChatMessage | null;
  error?: string | null;
}
