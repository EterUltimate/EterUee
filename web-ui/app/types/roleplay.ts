export interface RoleplayOverviewDto {
  characterCount: number;
  groupCount: number;
  worldInfoCount: number;
  presetCount: number;
}

export interface RoleplayCharacterDto {
  id: string;
  name: string;
  description: string;
  personality: string;
  scenario: string;
  firstMessage: string;
  messageExamples: string;
  systemPrompt: string;
  postHistoryInstructions: string;
  avatarUrl: string | null;
  creator: string;
  creatorNotes: string;
  tags: string[];
  talkativeness: number;
  alternateGreetings: string[];
  characterVersion: string;
  favorite: boolean;
  chatCount: number;
  lastChatAt: number | null;
  spec: string;
  specVersion: string;
  characterBook: unknown | null;
  extensions: Record<string, unknown>;
  createdAt: number;
  updatedAt: number;
}

export interface UpsertRoleplayCharacterRequest {
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
  talkativeness?: number;
  alternateGreetings?: string[];
}

export interface RoleplayChatDto {
  id: string;
  characterId: string;
  groupId: string | null;
  title: string;
  userName: string;
  characterName: string;
  createDate: string;
  tavernChatId: string;
  messageCount: number;
  pinned: boolean;
  activeBranchId: string | null;
  rootNodes: string[];
  variables: Record<string, string>;
  tavernMetadata: unknown | null;
  extensions: Record<string, unknown>;
  createdAt: number;
  updatedAt: number;
}

export interface RoleplayMessageDto {
  id: string;
  role: "user" | "assistant" | "system" | "tool" | string;
  content: string;
  timestamp: number;
  tavernName: string;
  tavernSendDate: string;
  model: string | null;
  tokenCount: number | null;
  swipeAlternatives: string[];
  speakerId: string | null;
  speakerName: string | null;
  extra: Record<string, unknown>;
}

export interface RoleplayGroupMemberDto {
  characterId: string;
  name: string;
  priority: number;
  responseProbability: number;
  forcedResponse: boolean;
}

export interface RoleplayGroupDto {
  id: string;
  name: string;
  description: string;
  members: RoleplayGroupMemberDto[];
  activeMembers: string[];
  avatarUrl: string | null;
  createdAt: number;
  updatedAt: number;
}

export interface RoleplayChatDetailDto {
  chat: RoleplayChatDto;
  character: RoleplayCharacterDto | null;
  group: RoleplayGroupDto | null;
  messages: RoleplayMessageDto[];
}

export interface RoleplayWorldInfoEntryDto {
  id: string;
  key: string;
  keys: string[];
  secondaryKeys: string[];
  comment: string;
  content: string;
  constant: boolean;
  selective: boolean;
  order: number;
  position: string;
  tavernPosition: number;
  enabled: boolean;
  probability: number;
  useProbability: boolean;
  depth: number;
  role: number;
  displayIndex: number;
  extensions: Record<string, unknown>;
}

export interface RoleplayWorldInfoDto {
  id: string;
  name: string;
  description: string;
  entries: RoleplayWorldInfoEntryDto[];
  scanDepth: number;
  scanTrigger: string;
  selectiveLogic: string;
  extensions: Record<string, unknown>;
  originalData: unknown | null;
  createdAt: number;
  updatedAt: number;
}

export interface RoleplayPresetDto {
  id: string;
  name: string;
  description: string;
  type: string;
  parameters: Record<string, unknown>;
  createdAt: number;
  updatedAt: number;
}
