import * as React from "react";

import {
  BookOpen,
  Bot,
  ChevronLeft,
  ChevronRight,
  Download,
  GitBranch,
  MessageSquare,
  Plus,
  RefreshCcw,
  Save,
  Send,
  Star,
  Trash2,
  Upload,
  Users,
} from "lucide-react";
import { toast } from "sonner";

import { Badge } from "~/components/ui/badge";
import { Button } from "~/components/ui/button";
import { Input } from "~/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "~/components/ui/select";
import { Textarea } from "~/components/ui/textarea";
import { useCurrentModel } from "~/hooks/use-current-model";
import { cn } from "~/lib/utils";
import { roleplayApi } from "~/services/roleplay";
import type {
  InsertionPosition,
  RoleplayCharacter,
  RoleplayChatMessage,
  RoleplayChatMetadata,
  RoleplayGenerationEvent,
  RoleplayGroup,
  RoleplayGroupMember,
  RoleplayMessageNode,
  RoleplayPreset,
  RoleplayPresetType,
  RoleplaySummary,
  RoleplayWorldInfo,
  RoleplayWorldInfoEntry,
  SaveRoleplayCharacterRequest,
  SaveRoleplayGroupRequest,
  SaveRoleplayPresetRequest,
} from "~/types/roleplay";

type Section = "characters" | "chat" | "groups" | "worlds" | "presets";

const sections: Array<{ id: Section; label: string; icon: React.ComponentType<{ className?: string }> }> = [
  { id: "characters", label: "Characters", icon: Bot },
  { id: "chat", label: "Chats", icon: MessageSquare },
  { id: "groups", label: "Groups", icon: Users },
  { id: "worlds", label: "World Info", icon: BookOpen },
  { id: "presets", label: "Presets", icon: Save },
];

const emptyCharacterForm: SaveRoleplayCharacterRequest = {
  name: "",
  description: "",
  personality: "",
  scenario: "",
  firstMessage: "",
  messageExamples: "",
  systemPrompt: "",
  postHistoryInstructions: "",
  creator: "",
  creatorNotes: "",
  tags: [],
  alternateGreetings: [],
  characterBook: null,
  extensions: {},
};

const emptyGroupForm: SaveRoleplayGroupRequest = {
  name: "",
  description: "",
};

const emptyPresetForm: SaveRoleplayPresetRequest = {
  name: "",
  description: "",
  type: "OPENAI",
  parameters: {},
};

function toCharacterForm(character: RoleplayCharacter): SaveRoleplayCharacterRequest {
  return {
    name: character.name,
    description: character.description,
    personality: character.personality,
    scenario: character.scenario,
    firstMessage: character.firstMessage,
    messageExamples: character.messageExamples,
    systemPrompt: character.systemPrompt,
    postHistoryInstructions: character.postHistoryInstructions,
    creator: character.creator,
    creatorNotes: character.creatorNotes,
    tags: character.tags,
    alternateGreetings: character.alternateGreetings,
    characterBook: character.characterBook ?? null,
    extensions: character.extensions ?? {},
  };
}

function parseList(value: string): string[] {
  return value
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

function formatDate(value?: string | null): string {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString();
}

function fileStem(value: string | undefined | null, fallback: string): string {
  return (value?.trim() || fallback).replace(/[\\/:*?"<>|\r\n]+/g, "_").slice(0, 80);
}

function downloadBlob(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

function createClientId(): string {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return `client-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}

function makeEmptyWorld(): RoleplayWorldInfo {
  const now = new Date().toISOString();
  return {
    id: createClientId(),
    name: "",
    description: "",
    entries: [],
    scanDepth: 4,
    scanTrigger: "ALWAYS",
    selectiveLogic: "AND",
    createdAt: now,
    updatedAt: now,
  };
}

function makeEmptyEntry(): RoleplayWorldInfoEntry {
  return {
    id: createClientId(),
    key: "",
    keys: [],
    secondaryKeys: [],
    comment: "",
    content: "",
    constant: false,
    selective: false,
    order: 0,
    position: "AFTER_SYSTEM_PROMPT",
    tavernPosition: 1,
    enabled: true,
    probability: 1,
    useProbability: false,
    depth: 4,
    role: 0,
    displayIndex: 0,
  };
}

export function meta() {
  return [{ title: "Roleplay - EterUee" }];
}

export default function RoleplayPage() {
  const { currentModel, currentProvider } = useCurrentModel();
  const [section, setSection] = React.useState<Section>("characters");
  const [summary, setSummary] = React.useState<RoleplaySummary | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [saving, setSaving] = React.useState(false);

  const [selectedCharacterId, setSelectedCharacterId] = React.useState<string | null>(null);
  const [selectedChatId, setSelectedChatId] = React.useState<string | null>(null);
  const [selectedGroupId, setSelectedGroupId] = React.useState<string | null>(null);
  const [selectedWorldId, setSelectedWorldId] = React.useState<string | null>(null);
  const [selectedPresetId, setSelectedPresetId] = React.useState<string | null>(null);

  const [characterForm, setCharacterForm] = React.useState<SaveRoleplayCharacterRequest>(emptyCharacterForm);
  const [groupForm, setGroupForm] = React.useState<SaveRoleplayGroupRequest>(emptyGroupForm);
  const [worldForm, setWorldForm] = React.useState<RoleplayWorldInfo>(makeEmptyWorld);
  const [entryForm, setEntryForm] = React.useState<RoleplayWorldInfoEntry>(makeEmptyEntry);
  const [presetForm, setPresetForm] = React.useState<SaveRoleplayPresetRequest>(emptyPresetForm);
  const [presetParametersText, setPresetParametersText] = React.useState("{}");

  const [messages, setMessages] = React.useState<RoleplayChatMessage[]>([]);
  const [messageNodes, setMessageNodes] = React.useState<RoleplayMessageNode[]>([]);
  const [branches, setBranches] = React.useState<RoleplayMessageNode[]>([]);
  const [activeBranchId, setActiveBranchId] = React.useState<string | null>(null);
  const [chatInput, setChatInput] = React.useState("");
  const [selectedMessageId, setSelectedMessageId] = React.useState<string | null>(null);
  const [selectedMessageIndex, setSelectedMessageIndex] = React.useState<number | null>(null);
  const [messageEditText, setMessageEditText] = React.useState("");
  const [swipeText, setSwipeText] = React.useState("");
  const [generationText, setGenerationText] = React.useState("");
  const [isGenerating, setIsGenerating] = React.useState(false);
  const [temperature, setTemperature] = React.useState(0.7);
  const [maxTokens, setMaxTokens] = React.useState(2048);
  const characterImportInputRef = React.useRef<HTMLInputElement>(null);
  const worldImportInputRef = React.useRef<HTMLInputElement>(null);
  const presetImportInputRef = React.useRef<HTMLInputElement>(null);

  const selectedCharacter = React.useMemo(
    () => summary?.characters.find((item) => item.id === selectedCharacterId) ?? null,
    [selectedCharacterId, summary?.characters],
  );
  const selectedGroup = React.useMemo(
    () => summary?.groups.find((item) => item.id === selectedGroupId) ?? null,
    [selectedGroupId, summary?.groups],
  );
  const selectedWorld = React.useMemo(
    () => summary?.worldInfos.find((item) => item.id === selectedWorldId) ?? null,
    [selectedWorldId, summary?.worldInfos],
  );
  const selectedPreset = React.useMemo(
    () => summary?.presets.find((item) => item.id === selectedPresetId) ?? null,
    [selectedPresetId, summary?.presets],
  );

  const refresh = React.useCallback(async () => {
    setLoading(true);
    try {
      const data = await roleplayApi.summary();
      setSummary(data);
      if (!selectedCharacterId && data.characters[0]) {
        setSelectedCharacterId(data.characters[0].id);
        setCharacterForm(toCharacterForm(data.characters[0]));
      }
      if (!selectedGroupId && data.groups[0]) {
        setSelectedGroupId(data.groups[0].id);
        setGroupForm({ name: data.groups[0].name, description: data.groups[0].description });
      }
      if (!selectedWorldId && data.worldInfos[0]) {
        setSelectedWorldId(data.worldInfos[0].id);
        setWorldForm(data.worldInfos[0]);
      }
      if (!selectedPresetId && data.presets[0]) {
        setSelectedPresetId(data.presets[0].id);
        setPresetForm({
          name: data.presets[0].name,
          description: data.presets[0].description,
          type: data.presets[0].type,
          parameters: data.presets[0].parameters,
        });
        setPresetParametersText(JSON.stringify(data.presets[0].parameters ?? {}, null, 2));
      }
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Failed to load roleplay data");
    } finally {
      setLoading(false);
    }
  }, [selectedCharacterId, selectedGroupId, selectedPresetId, selectedWorldId]);

  React.useEffect(() => {
    void refresh();
  }, [refresh]);

  React.useEffect(() => {
    if (selectedCharacter) setCharacterForm(toCharacterForm(selectedCharacter));
  }, [selectedCharacter]);

  React.useEffect(() => {
    if (selectedGroup) setGroupForm({ name: selectedGroup.name, description: selectedGroup.description });
  }, [selectedGroup]);

  React.useEffect(() => {
    if (selectedWorld) setWorldForm(selectedWorld);
  }, [selectedWorld]);

  React.useEffect(() => {
    if (!selectedPreset) return;
    setPresetForm({
      name: selectedPreset.name,
      description: selectedPreset.description,
      type: selectedPreset.type,
      parameters: selectedPreset.parameters,
    });
    setPresetParametersText(JSON.stringify(selectedPreset.parameters ?? {}, null, 2));
  }, [selectedPreset]);

  const loadChatMessages = React.useCallback(async (chatId: string) => {
    const [chat, data, branchData] = await Promise.all([
      roleplayApi.chats.get(chatId),
      roleplayApi.chats.messages(chatId),
      roleplayApi.chats.branches(chatId),
    ]);
    const nextMessages = data.nodes
      .map((node) => node.messages[node.selectedIndex] ?? node.messages[0])
      .filter((message): message is RoleplayChatMessage => Boolean(message));
    setMessageNodes(data.nodes);
    setMessages(nextMessages);
    setBranches(branchData);
    setActiveBranchId(chat.activeBranchId ?? chat.chatId);
    setSelectedMessageIndex(null);
    setSelectedMessageId(null);
    setMessageEditText("");
    setSwipeText("");
    setGenerationText("");
  }, []);

  const saveCharacter = React.useCallback(async () => {
    if (!characterForm.name?.trim()) {
      toast.error("Character name is required");
      return;
    }
    setSaving(true);
    try {
      const saved = selectedCharacterId
        ? await roleplayApi.characters.update(selectedCharacterId, characterForm)
        : await roleplayApi.characters.create(characterForm);
      setSelectedCharacterId(saved.id);
      await refresh();
      toast.success("Character saved");
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Failed to save character");
    } finally {
      setSaving(false);
    }
  }, [characterForm, refresh, selectedCharacterId]);

  const deleteCharacter = React.useCallback(async () => {
    if (!selectedCharacterId) return;
    await roleplayApi.characters.delete(selectedCharacterId);
    setSelectedCharacterId(null);
    setCharacterForm(emptyCharacterForm);
    await refresh();
  }, [refresh, selectedCharacterId]);

  const createChatForCharacter = React.useCallback(async () => {
    if (!selectedCharacterId) {
      toast.error("Select a character first");
      return;
    }
    const chat = await roleplayApi.characters.createChat(selectedCharacterId, {
      title: selectedCharacter?.name ? `${selectedCharacter.name} Chat` : "New Chat",
    });
    setSelectedChatId(chat.chatId);
    setSection("chat");
    await loadChatMessages(chat.chatId);
    await refresh();
  }, [loadChatMessages, refresh, selectedCharacter?.name, selectedCharacterId]);

  const sendChatMessage = React.useCallback(async () => {
    if (!selectedChatId || !chatInput.trim()) return;
    const text = chatInput.trim();
    setChatInput("");
    await roleplayApi.chats.append(selectedChatId, "USER", text);
    await loadChatMessages(selectedChatId);
    await refresh();
  }, [chatInput, loadChatMessages, refresh, selectedChatId]);

  const generateReply = React.useCallback(async () => {
    if (!selectedChatId) return;
    const modelId = currentModel?.id ?? currentModel?.modelId;
    if (!modelId) {
      toast.error("No chat model configured");
      return;
    }
    setIsGenerating(true);
    setGenerationText("");
    await roleplayApi.chats.generate(
      selectedChatId,
      {
        providerId: currentProvider?.id ?? "",
        modelId,
        systemPrompt: selectedCharacter?.systemPrompt ?? "",
        temperature,
        maxTokens,
      },
      {
        onEvent: (event: RoleplayGenerationEvent) => {
          if (event.type === "delta") {
            setGenerationText((current) => current + (event.delta ?? ""));
          } else if (event.type === "complete") {
            setGenerationText("");
            void loadChatMessages(selectedChatId);
            void refresh();
          } else if (event.type === "error") {
            toast.error(event.error ?? "Generation failed");
          }
        },
        onError: (error) => toast.error(error.message),
      },
    );
    setIsGenerating(false);
  }, [
    currentModel?.id,
    currentModel?.modelId,
    currentProvider?.id,
    loadChatMessages,
    maxTokens,
    refresh,
    selectedCharacter?.systemPrompt,
    selectedChatId,
    temperature,
  ]);

  const saveMessageEdit = React.useCallback(async () => {
    if (!selectedChatId || !selectedMessageId) return;
    await roleplayApi.chats.editMessage(selectedChatId, selectedMessageId, messageEditText);
    setSelectedMessageId(null);
    setSelectedMessageIndex(null);
    setMessageEditText("");
    setSwipeText("");
    await loadChatMessages(selectedChatId);
  }, [loadChatMessages, messageEditText, selectedChatId, selectedMessageId]);

  const deleteMessage = React.useCallback(async () => {
    if (!selectedChatId || !selectedMessageId) return;
    await roleplayApi.chats.deleteMessage(selectedChatId, selectedMessageId);
    setSelectedMessageId(null);
    setSelectedMessageIndex(null);
    setMessageEditText("");
    setSwipeText("");
    await loadChatMessages(selectedChatId);
  }, [loadChatMessages, selectedChatId, selectedMessageId]);

  const createBranchFromSelection = React.useCallback(async () => {
    if (!selectedChatId || selectedMessageIndex == null) return;
    await roleplayApi.chats.createBranch(selectedChatId, selectedMessageIndex);
    await loadChatMessages(selectedChatId);
    await refresh();
  }, [loadChatMessages, refresh, selectedChatId, selectedMessageIndex]);

  const switchBranch = React.useCallback(
    async (branchId: string) => {
      if (!selectedChatId) return;
      await roleplayApi.chats.selectBranch(selectedChatId, branchId);
      await loadChatMessages(selectedChatId);
      await refresh();
    },
    [loadChatMessages, refresh, selectedChatId],
  );

  const deleteBranch = React.useCallback(
    async (branchId: string) => {
      if (!selectedChatId) return;
      await roleplayApi.chats.deleteBranch(selectedChatId, branchId);
      await loadChatMessages(selectedChatId);
      await refresh();
    },
    [loadChatMessages, refresh, selectedChatId],
  );

  const addSwipe = React.useCallback(async () => {
    if (!selectedChatId || selectedMessageIndex == null || !swipeText.trim()) return;
    await roleplayApi.chats.addSwipe(selectedChatId, selectedMessageIndex, swipeText.trim());
    await loadChatMessages(selectedChatId);
  }, [loadChatMessages, selectedChatId, selectedMessageIndex, swipeText]);

  const rotateSwipe = React.useCallback(
    async (direction: "next" | "previous") => {
      if (!selectedChatId || selectedMessageIndex == null) return;
      if (direction === "next") {
        await roleplayApi.chats.nextSwipe(selectedChatId, selectedMessageIndex);
      } else {
        await roleplayApi.chats.previousSwipe(selectedChatId, selectedMessageIndex);
      }
      await loadChatMessages(selectedChatId);
    },
    [loadChatMessages, selectedChatId, selectedMessageIndex],
  );

  const saveGroup = React.useCallback(async () => {
    if (!groupForm.name.trim()) return;
    const saved = selectedGroupId
      ? await roleplayApi.groups.update(selectedGroupId, groupForm)
      : await roleplayApi.groups.create(groupForm);
    setSelectedGroupId(saved.id);
    await refresh();
  }, [groupForm, refresh, selectedGroupId]);

  const deleteGroup = React.useCallback(async () => {
    if (!selectedGroupId) return;
    await roleplayApi.groups.delete(selectedGroupId);
    setSelectedGroupId(null);
    setGroupForm(emptyGroupForm);
    await refresh();
  }, [refresh, selectedGroupId]);

  const saveWorld = React.useCallback(async () => {
    if (!worldForm.name.trim()) return;
    const saved = selectedWorldId
      ? await roleplayApi.worldInfos.update(selectedWorldId, worldForm)
      : await roleplayApi.worldInfos.create({ template: worldForm });
    setSelectedWorldId(saved.id);
    await refresh();
  }, [refresh, selectedWorldId, worldForm]);

  const deleteWorld = React.useCallback(async () => {
    if (!selectedWorldId) return;
    await roleplayApi.worldInfos.delete(selectedWorldId);
    setSelectedWorldId(null);
    setWorldForm(makeEmptyWorld());
    await refresh();
  }, [refresh, selectedWorldId]);

  const saveEntry = React.useCallback(async () => {
    if (!selectedWorldId) return;
    const exists = selectedWorld?.entries.some((item) => item.id === entryForm.id);
    const saved = exists
      ? await roleplayApi.worldInfos.updateEntry(selectedWorldId, entryForm.id, entryForm)
      : await roleplayApi.worldInfos.addEntry(selectedWorldId, entryForm);
    setWorldForm(saved);
    setEntryForm(makeEmptyEntry());
    await refresh();
  }, [entryForm, refresh, selectedWorld?.entries, selectedWorldId]);

  const savePreset = React.useCallback(async () => {
    if (!presetForm.name.trim()) return;
    let parameters: Record<string, unknown>;
    try {
      parameters = JSON.parse(presetParametersText || "{}") as Record<string, unknown>;
    } catch {
      toast.error("Preset parameters must be valid JSON");
      return;
    }
    const payload = { ...presetForm, parameters };
    const saved = selectedPresetId
      ? await roleplayApi.presets.update(selectedPresetId, payload)
      : await roleplayApi.presets.create(payload);
    setSelectedPresetId(saved.id);
    await refresh();
  }, [presetForm, presetParametersText, refresh, selectedPresetId]);

  const deletePreset = React.useCallback(async () => {
    if (!selectedPresetId) return;
    await roleplayApi.presets.delete(selectedPresetId);
    setSelectedPresetId(null);
    setPresetForm(emptyPresetForm);
    setPresetParametersText("{}");
    await refresh();
  }, [refresh, selectedPresetId]);

  const importCharacter = React.useCallback(
    async (event: React.ChangeEvent<HTMLInputElement>) => {
      const file = event.currentTarget.files?.[0];
      event.currentTarget.value = "";
      if (!file) return;
      setSaving(true);
      try {
        const { item } = await roleplayApi.characters.importFile(file);
        await refresh();
        setSelectedCharacterId(item.id);
        setCharacterForm(toCharacterForm(item));
        setSection("characters");
        toast.success("Character imported");
      } catch (error) {
        toast.error(error instanceof Error ? error.message : "Failed to import character");
      } finally {
        setSaving(false);
      }
    },
    [refresh],
  );

  const exportCharacterJson = React.useCallback(async () => {
    if (!selectedCharacterId || !selectedCharacter) return;
    try {
      const blob = await roleplayApi.characters.exportJson(selectedCharacterId);
      downloadBlob(blob, `${fileStem(selectedCharacter.name, "character")}-character.json`);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Failed to export character");
    }
  }, [selectedCharacter, selectedCharacterId]);

  const exportCharacterPng = React.useCallback(async () => {
    if (!selectedCharacterId || !selectedCharacter) return;
    try {
      const blob = await roleplayApi.characters.exportPng(selectedCharacterId);
      downloadBlob(blob, `${fileStem(selectedCharacter.name, "character")}-character.png`);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Failed to export character PNG");
    }
  }, [selectedCharacter, selectedCharacterId]);

  const importWorldInfo = React.useCallback(
    async (event: React.ChangeEvent<HTMLInputElement>) => {
      const file = event.currentTarget.files?.[0];
      event.currentTarget.value = "";
      if (!file) return;
      try {
        const { item } = await roleplayApi.worldInfos.importFile(file);
        await refresh();
        setSelectedWorldId(item.id);
        setWorldForm(item);
        setSection("worlds");
        toast.success("World info imported");
      } catch (error) {
        toast.error(error instanceof Error ? error.message : "Failed to import world info");
      }
    },
    [refresh],
  );

  const exportWorldInfo = React.useCallback(async () => {
    if (!selectedWorldId || !selectedWorld) return;
    try {
      const blob = await roleplayApi.worldInfos.exportJson(selectedWorldId);
      downloadBlob(blob, `${fileStem(selectedWorld.name, "world")}-world.json`);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Failed to export world info");
    }
  }, [selectedWorld, selectedWorldId]);

  const importPreset = React.useCallback(
    async (event: React.ChangeEvent<HTMLInputElement>) => {
      const file = event.currentTarget.files?.[0];
      event.currentTarget.value = "";
      if (!file) return;
      try {
        const { item } = await roleplayApi.presets.importFile(file);
        await refresh();
        setSelectedPresetId(item.id);
        setPresetForm({
          name: item.name,
          description: item.description,
          type: item.type,
          parameters: item.parameters,
        });
        setPresetParametersText(JSON.stringify(item.parameters ?? {}, null, 2));
        setSection("presets");
        toast.success("Preset imported");
      } catch (error) {
        toast.error(error instanceof Error ? error.message : "Failed to import preset");
      }
    },
    [refresh],
  );

  const exportPreset = React.useCallback(async () => {
    if (!selectedPresetId || !selectedPreset) return;
    try {
      const blob = await roleplayApi.presets.exportJson(selectedPresetId);
      downloadBlob(blob, `${fileStem(selectedPreset.name, "preset")}-preset.json`);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Failed to export preset");
    }
  }, [selectedPreset, selectedPresetId]);

  return (
    <main className="flex h-svh min-h-0 bg-background text-foreground">
      <input
        ref={characterImportInputRef}
        type="file"
        accept=".json,.png,application/json,image/png"
        className="hidden"
        onChange={importCharacter}
      />
      <input
        ref={worldImportInputRef}
        type="file"
        accept=".json,application/json"
        className="hidden"
        onChange={importWorldInfo}
      />
      <input
        ref={presetImportInputRef}
        type="file"
        accept=".json,application/json"
        className="hidden"
        onChange={importPreset}
      />
      <aside className="hidden w-56 shrink-0 border-r bg-sidebar p-3 md:block">
        <div className="mb-4 flex items-center justify-between">
          <div>
            <div className="text-sm font-semibold">Roleplay</div>
            <div className="text-xs text-muted-foreground">Tavern workspace</div>
          </div>
          <Button size="icon-sm" variant="ghost" onClick={() => void refresh()}>
            <RefreshCcw className="size-4" />
          </Button>
        </div>
        <nav className="space-y-1">
          {sections.map((item) => {
            const Icon = item.icon;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => setSection(item.id)}
                className={cn(
                  "flex h-9 w-full items-center gap-2 rounded-md px-2 text-left text-sm",
                  section === item.id
                    ? "bg-sidebar-accent text-sidebar-accent-foreground"
                    : "text-sidebar-foreground/80 hover:bg-sidebar-accent/70",
                )}
              >
                <Icon className="size-4" />
                {item.label}
              </button>
            );
          })}
        </nav>
      </aside>

      <section className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-14 shrink-0 items-center justify-between border-b px-4">
          <div className="min-w-0">
            <h1 className="truncate text-sm font-semibold">{sections.find((item) => item.id === section)?.label}</h1>
            <p className="truncate text-xs text-muted-foreground">
              {loading
                ? "Loading..."
                : `${summary?.characters.length ?? 0} characters, ${summary?.groups.length ?? 0} groups, ${summary?.worldInfos.length ?? 0} worlds`}
            </p>
          </div>
          <Button size="sm" variant="outline" onClick={() => void refresh()}>
            <RefreshCcw className="size-4" />
            Refresh
          </Button>
        </header>

        <div className="grid min-h-0 flex-1 grid-cols-1 md:grid-cols-[280px_minmax(0,1fr)]">
          <ResourceList
            section={section}
            summary={summary}
            selectedCharacterId={selectedCharacterId}
            selectedChatId={selectedChatId}
            selectedGroupId={selectedGroupId}
            selectedWorldId={selectedWorldId}
            selectedPresetId={selectedPresetId}
            onSelectCharacter={(character) => {
              setSelectedCharacterId(character.id);
              setCharacterForm(toCharacterForm(character));
              setSection("characters");
            }}
            onSelectChat={(chat) => {
              setSelectedChatId(chat.chatId);
              setSection("chat");
              void loadChatMessages(chat.chatId);
            }}
            onSelectGroup={(group) => {
              setSelectedGroupId(group.id);
              setGroupForm({ name: group.name, description: group.description });
              setSection("groups");
            }}
            onSelectWorld={(world) => {
              setSelectedWorldId(world.id);
              setWorldForm(world);
              setSection("worlds");
            }}
            onSelectPreset={(preset) => {
              setSelectedPresetId(preset.id);
              setPresetForm({
                name: preset.name,
                description: preset.description,
                type: preset.type,
                parameters: preset.parameters,
              });
              setPresetParametersText(JSON.stringify(preset.parameters ?? {}, null, 2));
              setSection("presets");
            }}
          />

          <div className="min-h-0 overflow-auto p-4">
            {section === "characters" ? (
              <CharacterEditor
                form={characterForm}
                selectedCharacter={selectedCharacter}
                saving={saving}
                onChange={setCharacterForm}
                onNew={() => {
                  setSelectedCharacterId(null);
                  setCharacterForm(emptyCharacterForm);
                }}
                onSave={() => void saveCharacter()}
                onDelete={() => void deleteCharacter()}
                onFavorite={async () => {
                  if (!selectedCharacterId) return;
                  await roleplayApi.characters.favorite(selectedCharacterId);
                  await refresh();
                }}
                onCreateChat={() => void createChatForCharacter()}
                onImport={() => characterImportInputRef.current?.click()}
                onExportJson={() => void exportCharacterJson()}
                onExportPng={() => void exportCharacterPng()}
              />
            ) : null}

            {section === "chat" ? (
              <ChatPanel
                selectedChatId={selectedChatId}
                modelLabel={
                  currentModel
                    ? `${currentModel.displayName || currentModel.modelId}${currentProvider ? ` / ${currentProvider.name}` : ""}`
                    : "No model"
                }
                messages={messages}
                messageNodes={messageNodes}
                branches={branches}
                activeBranchId={activeBranchId}
                generationText={generationText}
                isGenerating={isGenerating}
                input={chatInput}
                temperature={temperature}
                maxTokens={maxTokens}
                selectedMessageId={selectedMessageId}
                selectedMessageIndex={selectedMessageIndex}
                messageEditText={messageEditText}
                swipeText={swipeText}
                onInputChange={setChatInput}
                onSend={() => void sendChatMessage()}
                onGenerate={() => void generateReply()}
                onTemperatureChange={setTemperature}
                onMaxTokensChange={setMaxTokens}
                onSelectMessage={(message, index) => {
                  setSelectedMessageId(message.id);
                  setSelectedMessageIndex(index);
                  setMessageEditText(message.content);
                  setSwipeText("");
                }}
                onMessageEditChange={setMessageEditText}
                onSwipeTextChange={setSwipeText}
                onSaveMessage={() => void saveMessageEdit()}
                onDeleteMessage={() => void deleteMessage()}
                onCreateBranch={() => void createBranchFromSelection()}
                onSwitchBranch={(branchId) => void switchBranch(branchId)}
                onDeleteBranch={(branchId) => void deleteBranch(branchId)}
                onAddSwipe={() => void addSwipe()}
                onPreviousSwipe={() => void rotateSwipe("previous")}
                onNextSwipe={() => void rotateSwipe("next")}
                onClear={async () => {
                  if (!selectedChatId) return;
                  await roleplayApi.chats.clear(selectedChatId);
                  await loadChatMessages(selectedChatId);
                  await refresh();
                }}
              />
            ) : null}

            {section === "groups" ? (
              <GroupEditor
                form={groupForm}
                selectedGroup={selectedGroup}
                characters={summary?.characters ?? []}
                onChange={setGroupForm}
                onNew={() => {
                  setSelectedGroupId(null);
                  setGroupForm(emptyGroupForm);
                }}
                onSave={() => void saveGroup()}
                onDelete={() => void deleteGroup()}
                onAddMember={async (member) => {
                  if (!selectedGroupId) return;
                  await roleplayApi.groups.addMember(selectedGroupId, member);
                  await refresh();
                }}
                onRemoveMember={async (characterId) => {
                  if (!selectedGroupId) return;
                  await roleplayApi.groups.removeMember(selectedGroupId, characterId);
                  await refresh();
                }}
                onToggleMember={async (characterId) => {
                  if (!selectedGroupId) return;
                  await roleplayApi.groups.toggleMember(selectedGroupId, characterId);
                  await refresh();
                }}
                onCreateChat={async () => {
                  if (!selectedGroupId) return;
                  const chat = await roleplayApi.groups.createChat(selectedGroupId, { title: groupForm.name });
                  setSelectedChatId(chat.chatId);
                  setSection("chat");
                  await loadChatMessages(chat.chatId);
                  await refresh();
                }}
              />
            ) : null}

            {section === "worlds" ? (
              <WorldEditor
                form={worldForm}
                entryForm={entryForm}
                selectedWorld={selectedWorld}
                onChange={setWorldForm}
                onEntryChange={setEntryForm}
                onNew={() => {
                  setSelectedWorldId(null);
                  setWorldForm(makeEmptyWorld());
                }}
                onSave={() => void saveWorld()}
                onDelete={() => void deleteWorld()}
                onImport={() => worldImportInputRef.current?.click()}
                onExport={() => void exportWorldInfo()}
                onEditEntry={setEntryForm}
                onSaveEntry={() => void saveEntry()}
                onDeleteEntry={async (entryId) => {
                  if (!selectedWorldId) return;
                  const updated = await roleplayApi.worldInfos.deleteEntry(selectedWorldId, entryId);
                  setWorldForm(updated);
                  await refresh();
                }}
                onToggleEntry={async (entryId) => {
                  if (!selectedWorldId) return;
                  const updated = await roleplayApi.worldInfos.toggleEntry(selectedWorldId, entryId);
                  setWorldForm(updated);
                  await refresh();
                }}
              />
            ) : null}

            {section === "presets" ? (
              <PresetEditor
                form={presetForm}
                parametersText={presetParametersText}
                selectedPreset={selectedPreset}
                onChange={setPresetForm}
                onParametersChange={setPresetParametersText}
                onNew={() => {
                  setSelectedPresetId(null);
                  setPresetForm(emptyPresetForm);
                  setPresetParametersText("{}");
                }}
                onSave={() => void savePreset()}
                onDelete={() => void deletePreset()}
                onImport={() => presetImportInputRef.current?.click()}
                onExport={() => void exportPreset()}
              />
            ) : null}
          </div>
        </div>
      </section>
    </main>
  );
}

function ResourceList({
  section,
  summary,
  selectedCharacterId,
  selectedChatId,
  selectedGroupId,
  selectedWorldId,
  selectedPresetId,
  onSelectCharacter,
  onSelectChat,
  onSelectGroup,
  onSelectWorld,
  onSelectPreset,
}: {
  section: Section;
  summary: RoleplaySummary | null;
  selectedCharacterId: string | null;
  selectedChatId: string | null;
  selectedGroupId: string | null;
  selectedWorldId: string | null;
  selectedPresetId: string | null;
  onSelectCharacter: (character: RoleplayCharacter) => void;
  onSelectChat: (chat: RoleplayChatMetadata) => void;
  onSelectGroup: (group: RoleplayGroup) => void;
  onSelectWorld: (world: RoleplayWorldInfo) => void;
  onSelectPreset: (preset: RoleplayPreset) => void;
}) {
  const [chats, setChats] = React.useState<RoleplayChatMetadata[]>([]);

  React.useEffect(() => {
    if (!selectedCharacterId) {
      setChats([]);
      return;
    }
    roleplayApi.characters.chats(selectedCharacterId).then(setChats).catch(() => setChats([]));
  }, [selectedCharacterId, summary?.characters.length]);

  return (
    <aside className="min-h-0 overflow-auto border-r p-3">
      <ListSection title="Characters">
        {(summary?.characters ?? []).map((character) => (
          <ListButton
            key={character.id}
            selected={selectedCharacterId === character.id && section === "characters"}
            onClick={() => onSelectCharacter(character)}
            title={character.name || "Unnamed"}
            subtitle={character.description || "No description"}
            suffix={character.favorite ? <Star className="size-3 fill-current text-amber-500" /> : null}
          />
        ))}
      </ListSection>
      <ListSection title="Chats">
        {chats.map((chat) => (
          <ListButton
            key={chat.chatId}
            selected={selectedChatId === chat.chatId && section === "chat"}
            onClick={() => onSelectChat(chat)}
            title={chat.title || "Untitled chat"}
            subtitle={`${chat.messageCount} messages`}
          />
        ))}
      </ListSection>
      <ListSection title="Groups">
        {(summary?.groups ?? []).map((group) => (
          <ListButton
            key={group.id}
            selected={selectedGroupId === group.id && section === "groups"}
            onClick={() => onSelectGroup(group)}
            title={group.name || "Untitled group"}
            subtitle={`${group.members.length} members`}
          />
        ))}
      </ListSection>
      <ListSection title="World Info">
        {(summary?.worldInfos ?? []).map((world) => (
          <ListButton
            key={world.id}
            selected={selectedWorldId === world.id && section === "worlds"}
            onClick={() => onSelectWorld(world)}
            title={world.name || "Untitled world"}
            subtitle={`${world.entries.length} entries`}
          />
        ))}
      </ListSection>
      <ListSection title="Presets">
        {(summary?.presets ?? []).map((preset) => (
          <ListButton
            key={preset.id}
            selected={selectedPresetId === preset.id && section === "presets"}
            onClick={() => onSelectPreset(preset)}
            title={preset.name || "Untitled preset"}
            subtitle={preset.type}
          />
        ))}
      </ListSection>
    </aside>
  );
}

function ListSection({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="mb-5">
      <div className="mb-2 text-xs font-medium uppercase text-muted-foreground">{title}</div>
      <div className="space-y-1">{children}</div>
    </div>
  );
}

function ListButton({
  selected,
  onClick,
  title,
  subtitle,
  suffix,
}: {
  selected: boolean;
  onClick: () => void;
  title: string;
  subtitle: string;
  suffix?: React.ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={cn("w-full rounded-md px-2 py-2 text-left text-sm hover:bg-accent", selected && "bg-accent")}
    >
      <div className="flex items-center justify-between gap-2">
        <span className="truncate font-medium">{title}</span>
        {suffix}
      </div>
      <div className="truncate text-xs text-muted-foreground">{subtitle}</div>
    </button>
  );
}

function CharacterEditor({
  form,
  selectedCharacter,
  saving,
  onChange,
  onNew,
  onSave,
  onDelete,
  onFavorite,
  onCreateChat,
  onImport,
  onExportJson,
  onExportPng,
}: {
  form: SaveRoleplayCharacterRequest;
  selectedCharacter: RoleplayCharacter | null;
  saving: boolean;
  onChange: (value: SaveRoleplayCharacterRequest) => void;
  onNew: () => void;
  onSave: () => void;
  onDelete: () => void;
  onFavorite: () => void;
  onCreateChat: () => void;
  onImport: () => void;
  onExportJson: () => void;
  onExportPng: () => void;
}) {
  const update = (patch: Partial<SaveRoleplayCharacterRequest>) => onChange({ ...form, ...patch });

  return (
    <div className="mx-auto max-w-5xl space-y-4">
      <EditorToolbar
        title={selectedCharacter ? selectedCharacter.name : "New Character"}
        subtitle={selectedCharacter ? `Updated ${formatDate(selectedCharacter.updatedAt)}` : "Create a Tavern character card"}
        onNew={onNew}
        onSave={onSave}
        onDelete={onDelete}
        saveDisabled={saving}
        deleteDisabled={!selectedCharacter || saving}
        extra={
          <>
            <Button variant="outline" onClick={onImport} disabled={saving}>
              <Upload className="size-4" />
              Import
            </Button>
            <Button variant="outline" onClick={onExportJson} disabled={!selectedCharacter}>
              <Download className="size-4" />
              JSON
            </Button>
            <Button variant="outline" onClick={onExportPng} disabled={!selectedCharacter}>
              <Download className="size-4" />
              PNG
            </Button>
            <Button variant="outline" onClick={onFavorite} disabled={!selectedCharacter}>
              <Star className="size-4" />
            </Button>
            <Button variant="outline" onClick={onCreateChat} disabled={!selectedCharacter}>
              <MessageSquare className="size-4" />
              Chat
            </Button>
          </>
        }
      />
      <div className="grid gap-3 md:grid-cols-2">
        <Input value={form.name ?? ""} onChange={(event) => update({ name: event.target.value })} placeholder="Name" />
        <Input
          value={(form.tags ?? []).join(", ")}
          onChange={(event) => update({ tags: parseList(event.target.value) })}
          placeholder="Tags"
        />
      </div>
      <Textarea
        value={form.description ?? ""}
        onChange={(event) => update({ description: event.target.value })}
        placeholder="Description"
        className="min-h-24"
      />
      <div className="grid gap-3 md:grid-cols-2">
        <Textarea
          value={form.personality ?? ""}
          onChange={(event) => update({ personality: event.target.value })}
          placeholder="Personality"
          className="min-h-32"
        />
        <Textarea
          value={form.scenario ?? ""}
          onChange={(event) => update({ scenario: event.target.value })}
          placeholder="Scenario"
          className="min-h-32"
        />
      </div>
      <Textarea
        value={form.firstMessage ?? ""}
        onChange={(event) => update({ firstMessage: event.target.value })}
        placeholder="First message"
        className="min-h-24"
      />
      <Textarea
        value={form.messageExamples ?? ""}
        onChange={(event) => update({ messageExamples: event.target.value })}
        placeholder="Example dialogue"
        className="min-h-32 font-mono text-sm"
      />
      <div className="grid gap-3 md:grid-cols-2">
        <Textarea
          value={form.systemPrompt ?? ""}
          onChange={(event) => update({ systemPrompt: event.target.value })}
          placeholder="System prompt"
          className="min-h-28"
        />
        <Textarea
          value={form.postHistoryInstructions ?? ""}
          onChange={(event) => update({ postHistoryInstructions: event.target.value })}
          placeholder="Post-history instructions"
          className="min-h-28"
        />
      </div>
      <div className="grid gap-3 md:grid-cols-2">
        <Input
          value={form.creator ?? ""}
          onChange={(event) => update({ creator: event.target.value })}
          placeholder="Creator"
        />
        <Input
          value={(form.alternateGreetings ?? []).join(", ")}
          onChange={(event) => update({ alternateGreetings: parseList(event.target.value) })}
          placeholder="Alternate greetings"
        />
      </div>
      <Textarea
        value={form.creatorNotes ?? ""}
        onChange={(event) => update({ creatorNotes: event.target.value })}
        placeholder="Creator notes"
        className="min-h-20"
      />
    </div>
  );
}

function ChatPanel({
  selectedChatId,
  modelLabel,
  messages,
  messageNodes,
  branches,
  activeBranchId,
  generationText,
  isGenerating,
  input,
  temperature,
  maxTokens,
  selectedMessageId,
  selectedMessageIndex,
  messageEditText,
  swipeText,
  onInputChange,
  onSend,
  onGenerate,
  onTemperatureChange,
  onMaxTokensChange,
  onSelectMessage,
  onMessageEditChange,
  onSwipeTextChange,
  onSaveMessage,
  onDeleteMessage,
  onCreateBranch,
  onSwitchBranch,
  onDeleteBranch,
  onAddSwipe,
  onPreviousSwipe,
  onNextSwipe,
  onClear,
}: {
  selectedChatId: string | null;
  modelLabel: string;
  messages: RoleplayChatMessage[];
  messageNodes: RoleplayMessageNode[];
  branches: RoleplayMessageNode[];
  activeBranchId: string | null;
  generationText: string;
  isGenerating: boolean;
  input: string;
  temperature: number;
  maxTokens: number;
  selectedMessageId: string | null;
  selectedMessageIndex: number | null;
  messageEditText: string;
  swipeText: string;
  onInputChange: (value: string) => void;
  onSend: () => void;
  onGenerate: () => void;
  onTemperatureChange: (value: number) => void;
  onMaxTokensChange: (value: number) => void;
  onSelectMessage: (message: RoleplayChatMessage, index: number) => void;
  onMessageEditChange: (value: string) => void;
  onSwipeTextChange: (value: string) => void;
  onSaveMessage: () => void;
  onDeleteMessage: () => void;
  onCreateBranch: () => void;
  onSwitchBranch: (branchId: string) => void;
  onDeleteBranch: (branchId: string) => void;
  onAddSwipe: () => void;
  onPreviousSwipe: () => void;
  onNextSwipe: () => void;
  onClear: () => void;
}) {
  const selectedNode = selectedMessageIndex == null ? null : messageNodes[selectedMessageIndex];
  const selectedSwipeCount = selectedNode?.messages.length ?? 0;

  return (
    <div className="mx-auto flex h-full max-w-5xl flex-col gap-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="min-w-0">
          <h2 className="truncate text-lg font-semibold">Chat</h2>
          <p className="truncate text-sm text-muted-foreground">{selectedChatId ?? "Select or create a chat"}</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <Badge variant="outline">{messages.length} messages</Badge>
          <Badge variant="secondary">{modelLabel}</Badge>
          <Input
            type="number"
            value={temperature}
            step="0.1"
            min="0"
            max="2"
            onChange={(event) => onTemperatureChange(Number(event.target.value))}
            className="h-8 w-20"
          />
          <Input
            type="number"
            value={maxTokens}
            min="1"
            onChange={(event) => onMaxTokensChange(Number(event.target.value))}
            className="h-8 w-24"
          />
          <Button variant="outline" size="sm" onClick={onClear} disabled={!selectedChatId || messages.length === 0}>
            <Trash2 className="size-4" />
            Clear
          </Button>
          <Button size="sm" onClick={onGenerate} disabled={!selectedChatId || isGenerating}>
            <Bot className="size-4" />
            Generate
          </Button>
        </div>
      </div>
      {branches.length > 0 ? (
        <div className="flex flex-wrap items-center gap-2 rounded-md border bg-muted/20 p-2">
          <div className="flex items-center gap-1 text-xs font-medium text-muted-foreground">
            <GitBranch className="size-4" />
            Branches
          </div>
          {branches.map((branch) => (
            <div key={branch.id} className="flex items-center gap-1">
              <Button
                variant={branch.id === activeBranchId ? "default" : "outline"}
                size="sm"
                onClick={() => onSwitchBranch(branch.id)}
                disabled={!selectedChatId || branch.id === activeBranchId}
              >
                {branch.branchLabel || branch.id.slice(0, 8)}
              </Button>
              {branch.id !== selectedChatId ? (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => onDeleteBranch(branch.id)}
                  disabled={!selectedChatId || branches.length <= 1}
                >
                  <Trash2 className="size-4" />
                </Button>
              ) : null}
            </div>
          ))}
        </div>
      ) : null}
      <div className="min-h-[420px] flex-1 overflow-auto rounded-md border bg-muted/30 p-3">
        {messages.length === 0 && !generationText ? (
          <div className="text-sm text-muted-foreground">No messages yet.</div>
        ) : (
          <div className="space-y-2">
            {messages.map((message, index) => (
              <button
                key={`${message.id}-${index}`}
                type="button"
                onClick={() => onSelectMessage(message, index)}
                className={cn(
                  "w-full whitespace-pre-wrap rounded-md border bg-background px-3 py-2 text-left text-sm",
                  selectedMessageIndex === index && selectedMessageId === message.id && "border-primary",
                )}
              >
                <div className="mb-1 flex items-center justify-between gap-2 text-xs font-medium text-muted-foreground">
                  <span>{message.role.toLowerCase()}</span>
                  {message.swipeAlternatives?.length ? (
                    <span>{message.swipeAlternatives.length + 1} swipes</span>
                  ) : null}
                </div>
                {message.content}
              </button>
            ))}
            {generationText ? (
              <div className="whitespace-pre-wrap rounded-md border border-dashed bg-background px-3 py-2 text-sm">
                <div className="mb-1 text-xs font-medium text-muted-foreground">assistant</div>
                {generationText}
              </div>
            ) : null}
          </div>
        )}
      </div>
      <div className="flex gap-2">
        <Textarea
          value={input}
          onChange={(event) => onInputChange(event.target.value)}
          placeholder="Send a user message"
          className="min-h-16"
        />
        <Button className="h-auto self-stretch" onClick={onSend} disabled={!selectedChatId || !input.trim()}>
          <Send className="size-4" />
        </Button>
      </div>
      {selectedMessageId ? (
        <div className="space-y-2 rounded-md border p-3">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <Badge variant="outline">
              Message {selectedMessageIndex == null ? "-" : selectedMessageIndex + 1}
            </Badge>
            <div className="flex flex-wrap gap-2">
              <Button variant="outline" size="sm" onClick={onCreateBranch} disabled={selectedMessageIndex == null}>
                <GitBranch className="size-4" />
                Branch
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={onPreviousSwipe}
                disabled={selectedMessageIndex == null || selectedSwipeCount <= 1}
              >
                <ChevronLeft className="size-4" />
              </Button>
              <Button
                variant="outline"
                size="sm"
                onClick={onNextSwipe}
                disabled={selectedMessageIndex == null || selectedSwipeCount <= 1}
              >
                <ChevronRight className="size-4" />
              </Button>
            </div>
          </div>
          <Textarea
            value={messageEditText}
            onChange={(event) => onMessageEditChange(event.target.value)}
            className="min-h-24"
          />
          <div className="flex gap-2">
            <Input
              value={swipeText}
              onChange={(event) => onSwipeTextChange(event.target.value)}
              placeholder="Add swipe alternative"
            />
            <Button variant="outline" onClick={onAddSwipe} disabled={!swipeText.trim() || selectedMessageIndex == null}>
              <Plus className="size-4" />
            </Button>
          </div>
          <div className="flex justify-end gap-2">
            <Button variant="outline" onClick={onDeleteMessage}>
              <Trash2 className="size-4" />
              Delete
            </Button>
            <Button onClick={onSaveMessage}>
              <Save className="size-4" />
              Save
            </Button>
          </div>
        </div>
      ) : null}
    </div>
  );
}

function GroupEditor({
  form,
  selectedGroup,
  characters,
  onChange,
  onNew,
  onSave,
  onDelete,
  onAddMember,
  onRemoveMember,
  onToggleMember,
  onCreateChat,
}: {
  form: SaveRoleplayGroupRequest;
  selectedGroup: RoleplayGroup | null;
  characters: RoleplayCharacter[];
  onChange: (value: SaveRoleplayGroupRequest) => void;
  onNew: () => void;
  onSave: () => void;
  onDelete: () => void;
  onAddMember: (member: RoleplayGroupMember) => void;
  onRemoveMember: (characterId: string) => void;
  onToggleMember: (characterId: string) => void;
  onCreateChat: () => void;
}) {
  const [memberCharacterId, setMemberCharacterId] = React.useState("");
  const update = (patch: Partial<SaveRoleplayGroupRequest>) => onChange({ ...form, ...patch });
  const candidate = characters.find((character) => character.id === memberCharacterId);

  return (
    <div className="mx-auto max-w-5xl space-y-4">
      <EditorToolbar
        title={selectedGroup ? selectedGroup.name : "New Group"}
        subtitle={selectedGroup ? `${selectedGroup.members.length} members` : "Create a group chat roster"}
        onNew={onNew}
        onSave={onSave}
        onDelete={onDelete}
        deleteDisabled={!selectedGroup}
        extra={
          <Button variant="outline" onClick={onCreateChat} disabled={!selectedGroup}>
            <MessageSquare className="size-4" />
            Chat
          </Button>
        }
      />
      <Input value={form.name} onChange={(event) => update({ name: event.target.value })} placeholder="Group name" />
      <Textarea
        value={form.description ?? ""}
        onChange={(event) => update({ description: event.target.value })}
        placeholder="Description"
        className="min-h-24"
      />
      <div className="flex flex-wrap gap-2">
        <Select value={memberCharacterId} onValueChange={setMemberCharacterId}>
          <SelectTrigger className="w-64">
            <SelectValue placeholder="Add character" />
          </SelectTrigger>
          <SelectContent>
            {characters.map((character) => (
              <SelectItem key={character.id} value={character.id}>
                {character.name || "Unnamed"}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Button
          onClick={() => {
            if (!candidate) return;
            onAddMember({
              characterId: candidate.id,
              name: candidate.name,
              priority: 0,
              responseProbability: 1,
              forcedResponse: false,
            });
            setMemberCharacterId("");
          }}
          disabled={!selectedGroup || !candidate}
        >
          <Plus className="size-4" />
          Member
        </Button>
      </div>
      <div className="grid gap-2">
        {(selectedGroup?.members ?? []).map((member) => (
          <div key={member.characterId} className="flex flex-wrap items-center justify-between gap-2 rounded-md border p-3">
            <div>
              <div className="font-medium">{member.name || member.characterId}</div>
              <div className="text-xs text-muted-foreground">
                priority {member.priority} / probability {member.responseProbability}
              </div>
            </div>
            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={() => onToggleMember(member.characterId)}>
                {selectedGroup?.activeMembers.includes(member.characterId) ? "Disable" : "Enable"}
              </Button>
              <Button variant="outline" size="sm" onClick={() => onRemoveMember(member.characterId)}>
                <Trash2 className="size-4" />
              </Button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function WorldEditor({
  form,
  entryForm,
  selectedWorld,
  onChange,
  onEntryChange,
  onNew,
  onSave,
  onDelete,
  onImport,
  onExport,
  onEditEntry,
  onSaveEntry,
  onDeleteEntry,
  onToggleEntry,
}: {
  form: RoleplayWorldInfo;
  entryForm: RoleplayWorldInfoEntry;
  selectedWorld: RoleplayWorldInfo | null;
  onChange: (value: RoleplayWorldInfo) => void;
  onEntryChange: (value: RoleplayWorldInfoEntry) => void;
  onNew: () => void;
  onSave: () => void;
  onDelete: () => void;
  onImport: () => void;
  onExport: () => void;
  onEditEntry: (entry: RoleplayWorldInfoEntry) => void;
  onSaveEntry: () => void;
  onDeleteEntry: (entryId: string) => void;
  onToggleEntry: (entryId: string) => void;
}) {
  const update = (patch: Partial<RoleplayWorldInfo>) => onChange({ ...form, ...patch });
  const updateEntry = (patch: Partial<RoleplayWorldInfoEntry>) => onEntryChange({ ...entryForm, ...patch });

  return (
    <div className="mx-auto max-w-5xl space-y-4">
      <EditorToolbar
        title={selectedWorld ? selectedWorld.name : "New World Info"}
        subtitle={selectedWorld ? `${selectedWorld.entries.length} entries` : "Create a Tavern world book"}
        onNew={onNew}
        onSave={onSave}
        onDelete={onDelete}
        deleteDisabled={!selectedWorld}
        extra={
          <>
            <Button variant="outline" onClick={onImport}>
              <Upload className="size-4" />
              Import
            </Button>
            <Button variant="outline" onClick={onExport} disabled={!selectedWorld}>
              <Download className="size-4" />
              Export
            </Button>
          </>
        }
      />
      <div className="grid gap-3 md:grid-cols-2">
        <Input value={form.name} onChange={(event) => update({ name: event.target.value })} placeholder="Name" />
        <Input
          type="number"
          value={form.scanDepth}
          onChange={(event) => update({ scanDepth: Number(event.target.value) })}
          placeholder="Scan depth"
        />
      </div>
      <Textarea
        value={form.description}
        onChange={(event) => update({ description: event.target.value })}
        placeholder="Description"
        className="min-h-20"
      />
      <div className="grid gap-3 md:grid-cols-2">
        <Select value={form.scanTrigger} onValueChange={(value) => update({ scanTrigger: value as RoleplayWorldInfo["scanTrigger"] })}>
          <SelectTrigger>
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {["ALWAYS", "FIRST_MESSAGE", "RECURSIVE_SCAN"].map((item) => (
              <SelectItem key={item} value={item}>
                {item}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Select value={form.selectiveLogic} onValueChange={(value) => update({ selectiveLogic: value as RoleplayWorldInfo["selectiveLogic"] })}>
          <SelectTrigger>
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {["AND", "OR"].map((item) => (
              <SelectItem key={item} value={item}>
                {item}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="grid gap-3 rounded-md border p-3 md:grid-cols-2">
        <Input value={entryForm.key} onChange={(event) => updateEntry({ key: event.target.value })} placeholder="Entry key" />
        <Input
          value={entryForm.keys.join(", ")}
          onChange={(event) => updateEntry({ keys: parseList(event.target.value) })}
          placeholder="Extra keys"
        />
        <Input
          value={entryForm.secondaryKeys.join(", ")}
          onChange={(event) => updateEntry({ secondaryKeys: parseList(event.target.value) })}
          placeholder="Secondary keys"
        />
        <Select value={entryForm.position} onValueChange={(value) => updateEntry({ position: value as InsertionPosition })}>
          <SelectTrigger>
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {["AFTER_SYSTEM_PROMPT", "BEFORE_LAST_USER_MESSAGE", "AT_END"].map((item) => (
              <SelectItem key={item} value={item}>
                {item}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Textarea
          value={entryForm.content}
          onChange={(event) => updateEntry({ content: event.target.value })}
          placeholder="Content"
          className="min-h-28 md:col-span-2"
        />
        <div className="flex justify-end gap-2 md:col-span-2">
          <Button variant="outline" onClick={() => onEntryChange(makeEmptyEntry())}>
            <Plus className="size-4" />
            New Entry
          </Button>
          <Button onClick={onSaveEntry} disabled={!selectedWorld}>
            <Save className="size-4" />
            Save Entry
          </Button>
        </div>
      </div>
      <div className="grid gap-2">
        {(selectedWorld?.entries ?? []).map((entry) => (
          <div key={entry.id} className="flex flex-wrap items-center justify-between gap-2 rounded-md border p-3">
            <button type="button" onClick={() => onEditEntry(entry)} className="min-w-0 text-left">
              <div className="truncate font-medium">{entry.key || entry.keys.join(", ") || "Untitled entry"}</div>
              <div className="truncate text-xs text-muted-foreground">{entry.content}</div>
            </button>
            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={() => onToggleEntry(entry.id)}>
                {entry.enabled ? "Disable" : "Enable"}
              </Button>
              <Button variant="outline" size="sm" onClick={() => onDeleteEntry(entry.id)}>
                <Trash2 className="size-4" />
              </Button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function PresetEditor({
  form,
  parametersText,
  selectedPreset,
  onChange,
  onParametersChange,
  onNew,
  onSave,
  onDelete,
  onImport,
  onExport,
}: {
  form: SaveRoleplayPresetRequest;
  parametersText: string;
  selectedPreset: RoleplayPreset | null;
  onChange: (value: SaveRoleplayPresetRequest) => void;
  onParametersChange: (value: string) => void;
  onNew: () => void;
  onSave: () => void;
  onDelete: () => void;
  onImport: () => void;
  onExport: () => void;
}) {
  const update = (patch: Partial<SaveRoleplayPresetRequest>) => onChange({ ...form, ...patch });

  return (
    <div className="mx-auto max-w-5xl space-y-4">
      <EditorToolbar
        title={selectedPreset ? selectedPreset.name : "New Preset"}
        subtitle={selectedPreset ? selectedPreset.type : "Create generation parameters"}
        onNew={onNew}
        onSave={onSave}
        onDelete={onDelete}
        deleteDisabled={!selectedPreset}
        extra={
          <>
            <Button variant="outline" onClick={onImport}>
              <Upload className="size-4" />
              Import
            </Button>
            <Button variant="outline" onClick={onExport} disabled={!selectedPreset}>
              <Download className="size-4" />
              Export
            </Button>
          </>
        }
      />
      <div className="grid gap-3 md:grid-cols-2">
        <Input value={form.name} onChange={(event) => update({ name: event.target.value })} placeholder="Name" />
        <Select value={form.type ?? "OPENAI"} onValueChange={(value) => update({ type: value as RoleplayPresetType })}>
          <SelectTrigger>
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {["OPENAI", "CLAUDE", "GEMINI", "TEXTGEN", "KOBOLDAI"].map((type) => (
              <SelectItem key={type} value={type}>
                {type}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>
      <Textarea
        value={form.description ?? ""}
        onChange={(event) => update({ description: event.target.value })}
        placeholder="Description"
        className="min-h-20"
      />
      <Textarea
        value={parametersText}
        onChange={(event) => onParametersChange(event.target.value)}
        placeholder="Parameters JSON"
        className="min-h-80 font-mono text-sm"
      />
    </div>
  );
}

function EditorToolbar({
  title,
  subtitle,
  onNew,
  onSave,
  onDelete,
  saveDisabled,
  deleteDisabled,
  extra,
}: {
  title: string;
  subtitle: string;
  onNew: () => void;
  onSave: () => void;
  onDelete: () => void;
  saveDisabled?: boolean;
  deleteDisabled?: boolean;
  extra?: React.ReactNode;
}) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-2">
      <div className="min-w-0">
        <h2 className="truncate text-lg font-semibold">{title}</h2>
        <p className="truncate text-sm text-muted-foreground">{subtitle}</p>
      </div>
      <div className="flex flex-wrap items-center gap-2">
        {extra}
        <Button variant="outline" onClick={onNew}>
          <Plus className="size-4" />
          New
        </Button>
        <Button variant="outline" onClick={onDelete} disabled={deleteDisabled}>
          <Trash2 className="size-4" />
        </Button>
        <Button onClick={onSave} disabled={saveDisabled}>
          <Save className="size-4" />
          Save
        </Button>
      </div>
    </div>
  );
}
