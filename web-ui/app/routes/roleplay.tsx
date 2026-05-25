import * as React from "react";

import { Link } from "react-router";
import {
  BookMarked,
  Bot,
  MessageSquare,
  Plus,
  RefreshCw,
  Send,
  Settings2,
  Star,
  Trash2,
  UserRound,
} from "lucide-react";
import { toast } from "sonner";

import { Avatar, AvatarFallback, AvatarImage } from "~/components/ui/avatar";
import { Badge } from "~/components/ui/badge";
import { Button } from "~/components/ui/button";
import { ButtonGroup } from "~/components/ui/button-group";
import { Input } from "~/components/ui/input";
import { ScrollArea } from "~/components/ui/scroll-area";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "~/components/ui/select";
import { Skeleton } from "~/components/ui/skeleton";
import { Textarea } from "~/components/ui/textarea";
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "~/components/ui/tooltip";
import { resolveFileUrl } from "~/lib/files";
import { cn } from "~/lib/utils";
import { roleplayApi } from "~/services/api";
import type {
  RoleplayCharacterDto,
  RoleplayChatDetailDto,
  RoleplayChatDto,
  RoleplayMessageDto,
  RoleplayOverviewDto,
  RoleplayPresetDto,
  RoleplayWorldInfoDto,
  UpsertRoleplayCharacterRequest,
} from "~/types";

type RoleplaySection = "characters" | "chats" | "worlds" | "presets";
type ComposeRole = "user" | "assistant";

interface CharacterDraft {
  name: string;
  description: string;
  personality: string;
  scenario: string;
  firstMessage: string;
  tags: string;
}

interface WorldInfoDraft {
  name: string;
  description: string;
}

interface PresetDraft {
  name: string;
  description: string;
  type: string;
  parameters: string;
}

const sectionItems: Array<{
  id: RoleplaySection;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
}> = [
  { id: "characters", label: "Characters", icon: UserRound },
  { id: "chats", label: "Chats", icon: MessageSquare },
  { id: "worlds", label: "World Info", icon: BookMarked },
  { id: "presets", label: "Presets", icon: Settings2 },
];

const emptyCharacterDraft: CharacterDraft = {
  name: "",
  description: "",
  personality: "",
  scenario: "",
  firstMessage: "",
  tags: "",
};

const emptyWorldInfoDraft: WorldInfoDraft = {
  name: "",
  description: "",
};

const emptyPresetDraft: PresetDraft = {
  name: "",
  description: "",
  type: "OPENAI",
  parameters: '{\n  "temperature": 0.7,\n  "max_tokens": 2048\n}',
};

export function meta() {
  return [
    { title: "EterUee Roleplay" },
    {
      name: "description",
      content: "Tavern-style character, chat, world info, and preset workspace.",
    },
  ];
}

function getInitials(name: string): string {
  const normalized = name.trim();
  if (!normalized) return "RP";
  return normalized.slice(0, 2).toUpperCase();
}

function formatTime(value: number | null | undefined): string {
  if (!value) return "Never";
  return new Intl.DateTimeFormat(undefined, {
    month: "short",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function splitList(value: string): string[] {
  return value
    .split(/[,;\n]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function getCharacterPayload(draft: CharacterDraft): UpsertRoleplayCharacterRequest {
  return {
    name: draft.name.trim(),
    description: draft.description,
    personality: draft.personality,
    scenario: draft.scenario,
    firstMessage: draft.firstMessage,
    tags: splitList(draft.tags),
  };
}

function truncateText(value: string, maxLength: number): string {
  const normalized = value.trim().replace(/\s+/g, " ");
  if (normalized.length <= maxLength) return normalized;
  return `${normalized.slice(0, maxLength)}...`;
}

function roleLabel(message: RoleplayMessageDto): string {
  if (message.speakerName) return message.speakerName;
  if (message.tavernName) return message.tavernName;
  return message.role === "user" ? "User" : "Character";
}

function EmptyPane({ title, description }: { title: string; description: string }) {
  return (
    <div className="flex min-h-[220px] flex-1 items-center justify-center p-6 text-center">
      <div className="max-w-sm space-y-2">
        <div className="text-sm font-medium">{title}</div>
        <div className="text-sm text-muted-foreground">{description}</div>
      </div>
    </div>
  );
}

function StatBadge({ label, value }: { label: string; value: number }) {
  return (
    <div className="min-w-0 rounded-md border bg-background px-3 py-2">
      <div className="text-xs text-muted-foreground">{label}</div>
      <div className="text-lg font-semibold leading-6">{value}</div>
    </div>
  );
}

function CharacterAvatar({ character }: { character: RoleplayCharacterDto }) {
  return (
    <Avatar size="lg" className="rounded-md">
      {character.avatarUrl ? (
        <AvatarImage src={resolveFileUrl(character.avatarUrl)} alt={character.name} />
      ) : null}
      <AvatarFallback className="rounded-md">{getInitials(character.name)}</AvatarFallback>
    </Avatar>
  );
}

function LoadingList() {
  return (
    <div className="space-y-2 p-3">
      {[0, 1, 2, 3].map((item) => (
        <Skeleton key={item} className="h-16 w-full rounded-md" />
      ))}
    </div>
  );
}

export default function RoleplayPage() {
  const [section, setSection] = React.useState<RoleplaySection>("characters");
  const [overview, setOverview] = React.useState<RoleplayOverviewDto | null>(null);
  const [characters, setCharacters] = React.useState<RoleplayCharacterDto[]>([]);
  const [worldInfos, setWorldInfos] = React.useState<RoleplayWorldInfoDto[]>([]);
  const [presets, setPresets] = React.useState<RoleplayPresetDto[]>([]);
  const [chats, setChats] = React.useState<RoleplayChatDto[]>([]);
  const [chatDetail, setChatDetail] = React.useState<RoleplayChatDetailDto | null>(null);
  const [selectedCharacterId, setSelectedCharacterId] = React.useState<string | null>(null);
  const [selectedChatId, setSelectedChatId] = React.useState<string | null>(null);
  const [selectedWorldInfoId, setSelectedWorldInfoId] = React.useState<string | null>(null);
  const [selectedPresetId, setSelectedPresetId] = React.useState<string | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [chatsLoading, setChatsLoading] = React.useState(false);
  const [chatLoading, setChatLoading] = React.useState(false);
  const [busy, setBusy] = React.useState(false);
  const [characterDraft, setCharacterDraft] = React.useState<CharacterDraft>(emptyCharacterDraft);
  const [chatTitle, setChatTitle] = React.useState("");
  const [composeRole, setComposeRole] = React.useState<ComposeRole>("user");
  const [composeText, setComposeText] = React.useState("");
  const [worldInfoDraft, setWorldInfoDraft] =
    React.useState<WorldInfoDraft>(emptyWorldInfoDraft);
  const [presetDraft, setPresetDraft] = React.useState<PresetDraft>(emptyPresetDraft);

  const selectedCharacter = React.useMemo(
    () => characters.find((item) => item.id === selectedCharacterId) ?? null,
    [characters, selectedCharacterId],
  );
  const selectedWorldInfo = React.useMemo(
    () => worldInfos.find((item) => item.id === selectedWorldInfoId) ?? null,
    [selectedWorldInfoId, worldInfos],
  );
  const selectedPreset = React.useMemo(
    () => presets.find((item) => item.id === selectedPresetId) ?? null,
    [presets, selectedPresetId],
  );

  const refreshWorkspace = React.useCallback(async () => {
    setLoading(true);
    try {
      const [nextOverview, nextCharacters, nextWorldInfos, nextPresets] = await Promise.all([
        roleplayApi.get<RoleplayOverviewDto>("overview"),
        roleplayApi.get<RoleplayCharacterDto[]>("characters"),
        roleplayApi.get<RoleplayWorldInfoDto[]>("world-infos"),
        roleplayApi.get<RoleplayPresetDto[]>("presets"),
      ]);
      setOverview(nextOverview);
      setCharacters(nextCharacters);
      setWorldInfos(nextWorldInfos);
      setPresets(nextPresets);

      setSelectedCharacterId((current) => {
        if (current && nextCharacters.some((item) => item.id === current)) return current;
        return nextCharacters[0]?.id ?? null;
      });
      setSelectedWorldInfoId((current) => {
        if (current && nextWorldInfos.some((item) => item.id === current)) return current;
        return nextWorldInfos[0]?.id ?? null;
      });
      setSelectedPresetId((current) => {
        if (current && nextPresets.some((item) => item.id === current)) return current;
        return nextPresets[0]?.id ?? null;
      });
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Failed to load roleplay workspace");
    } finally {
      setLoading(false);
    }
  }, []);

  const loadChats = React.useCallback(async (characterId: string | null) => {
    if (!characterId) {
      setChats([]);
      setSelectedChatId(null);
      return;
    }

    setChatsLoading(true);
    try {
      const nextChats = await roleplayApi.get<RoleplayChatDto[]>(`characters/${characterId}/chats`);
      setChats(nextChats);
      setSelectedChatId((current) => {
        if (current && nextChats.some((item) => item.id === current)) return current;
        return nextChats[0]?.id ?? null;
      });
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Failed to load chats");
      setChats([]);
    } finally {
      setChatsLoading(false);
    }
  }, []);

  const loadChatDetail = React.useCallback(async (chatId: string | null) => {
    if (!chatId) {
      setChatDetail(null);
      return;
    }

    setChatLoading(true);
    try {
      setChatDetail(await roleplayApi.get<RoleplayChatDetailDto>(`chats/${chatId}`));
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Failed to load chat");
      setChatDetail(null);
    } finally {
      setChatLoading(false);
    }
  }, []);

  React.useEffect(() => {
    void refreshWorkspace();
  }, [refreshWorkspace]);

  React.useEffect(() => {
    void loadChats(selectedCharacterId);
  }, [loadChats, selectedCharacterId]);

  React.useEffect(() => {
    void loadChatDetail(selectedChatId);
  }, [loadChatDetail, selectedChatId]);

  const handleCreateCharacter = React.useCallback(
    async (event: React.FormEvent<HTMLFormElement>) => {
      event.preventDefault();
      const payload = getCharacterPayload(characterDraft);
      if (!payload.name) {
        toast.error("Character name is required");
        return;
      }

      setBusy(true);
      try {
        const created = await roleplayApi.post<RoleplayCharacterDto>("characters", payload);
        setCharacterDraft(emptyCharacterDraft);
        setCharacters((current) => [created, ...current]);
        setSelectedCharacterId(created.id);
        setSection("chats");
        toast.success("Character created");
        void refreshWorkspace();
      } catch (error) {
        toast.error(error instanceof Error ? error.message : "Failed to create character");
      } finally {
        setBusy(false);
      }
    },
    [characterDraft, refreshWorkspace],
  );

  const handleToggleFavorite = React.useCallback(async (characterId: string) => {
    setBusy(true);
    try {
      const response = await roleplayApi.post<{ favorite: boolean }>(
        `characters/${characterId}/favorite`,
      );
      setCharacters((current) =>
        current.map((item) =>
          item.id === characterId ? { ...item, favorite: response.favorite } : item,
        ),
      );
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Failed to update favorite");
    } finally {
      setBusy(false);
    }
  }, []);

  const handleCreateChat = React.useCallback(
    async (event: React.FormEvent<HTMLFormElement>) => {
      event.preventDefault();
      if (!selectedCharacterId) {
        toast.error("Select a character first");
        return;
      }

      setBusy(true);
      try {
        const created = await roleplayApi.post<RoleplayChatDto>("chats", {
          characterId: selectedCharacterId,
          title: chatTitle.trim(),
        });
        setChatTitle("");
        setChats((current) => [created, ...current]);
        setSelectedChatId(created.id);
        setSection("chats");
        toast.success("Chat created");
        void refreshWorkspace();
      } catch (error) {
        toast.error(error instanceof Error ? error.message : "Failed to create chat");
      } finally {
        setBusy(false);
      }
    },
    [chatTitle, refreshWorkspace, selectedCharacterId],
  );

  const handleDeleteChat = React.useCallback(
    async (chatId: string) => {
      setBusy(true);
      try {
        await roleplayApi.delete<void>(`chats/${chatId}`);
        setChats((current) => current.filter((item) => item.id !== chatId));
        setSelectedChatId((current) => (current === chatId ? null : current));
        toast.success("Chat deleted");
        void refreshWorkspace();
      } catch (error) {
        toast.error(error instanceof Error ? error.message : "Failed to delete chat");
      } finally {
        setBusy(false);
      }
    },
    [refreshWorkspace],
  );

  const handleTogglePinChat = React.useCallback(async (chatId: string) => {
    setBusy(true);
    try {
      const response = await roleplayApi.post<{ pinned: boolean }>(`chats/${chatId}/pin`);
      setChats((current) =>
        current.map((item) => (item.id === chatId ? { ...item, pinned: response.pinned } : item)),
      );
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Failed to pin chat");
    } finally {
      setBusy(false);
    }
  }, []);

  const handleAppendMessage = React.useCallback(
    async (event: React.FormEvent<HTMLFormElement>) => {
      event.preventDefault();
      if (!selectedChatId || !composeText.trim()) return;

      setBusy(true);
      try {
        const created = await roleplayApi.post<RoleplayMessageDto>(`chats/${selectedChatId}/messages`, {
          content: composeText,
          role: composeRole,
        });
        setComposeText("");
        setChatDetail((current) =>
          current
            ? {
                ...current,
                messages: [...current.messages, created],
                chat: {
                  ...current.chat,
                  messageCount: current.chat.messageCount + 1,
                  updatedAt: Date.now(),
                },
              }
            : current,
        );
        setChats((current) =>
          current.map((item) =>
            item.id === selectedChatId
              ? { ...item, messageCount: item.messageCount + 1, updatedAt: Date.now() }
              : item,
          ),
        );
      } catch (error) {
        toast.error(error instanceof Error ? error.message : "Failed to append message");
      } finally {
        setBusy(false);
      }
    },
    [composeRole, composeText, selectedChatId],
  );

  const handleDeleteMessage = React.useCallback(
    async (messageId: string) => {
      if (!selectedChatId) return;

      setBusy(true);
      try {
        await roleplayApi.delete<void>(`chats/${selectedChatId}/messages/${messageId}`);
        setChatDetail((current) =>
          current
            ? {
                ...current,
                messages: current.messages.filter((item) => item.id !== messageId),
                chat: {
                  ...current.chat,
                  messageCount: Math.max(current.chat.messageCount - 1, 0),
                  updatedAt: Date.now(),
                },
              }
            : current,
        );
      } catch (error) {
        toast.error(error instanceof Error ? error.message : "Failed to delete message");
      } finally {
        setBusy(false);
      }
    },
    [selectedChatId],
  );

  const handleCreateWorldInfo = React.useCallback(
    async (event: React.FormEvent<HTMLFormElement>) => {
      event.preventDefault();
      if (!worldInfoDraft.name.trim()) {
        toast.error("World info name is required");
        return;
      }

      setBusy(true);
      try {
        const created = await roleplayApi.post<RoleplayWorldInfoDto>("world-infos", {
          name: worldInfoDraft.name.trim(),
          description: worldInfoDraft.description,
        });
        setWorldInfoDraft(emptyWorldInfoDraft);
        setWorldInfos((current) => [created, ...current]);
        setSelectedWorldInfoId(created.id);
        toast.success("World info created");
        void refreshWorkspace();
      } catch (error) {
        toast.error(error instanceof Error ? error.message : "Failed to create world info");
      } finally {
        setBusy(false);
      }
    },
    [refreshWorkspace, worldInfoDraft],
  );

  const handleCreatePreset = React.useCallback(
    async (event: React.FormEvent<HTMLFormElement>) => {
      event.preventDefault();
      if (!presetDraft.name.trim()) {
        toast.error("Preset name is required");
        return;
      }

      let parameters: Record<string, unknown>;
      try {
        const parsed = JSON.parse(presetDraft.parameters || "{}") as unknown;
        if (!parsed || Array.isArray(parsed) || typeof parsed !== "object") {
          throw new Error("Preset parameters must be a JSON object");
        }
        parameters = parsed as Record<string, unknown>;
      } catch (error) {
        toast.error(error instanceof Error ? error.message : "Invalid preset parameters");
        return;
      }

      setBusy(true);
      try {
        const created = await roleplayApi.post<RoleplayPresetDto>("presets", {
          name: presetDraft.name.trim(),
          description: presetDraft.description,
          type: presetDraft.type,
          parameters,
        });
        setPresetDraft(emptyPresetDraft);
        setPresets((current) => [created, ...current]);
        setSelectedPresetId(created.id);
        toast.success("Preset created");
        void refreshWorkspace();
      } catch (error) {
        toast.error(error instanceof Error ? error.message : "Failed to create preset");
      } finally {
        setBusy(false);
      }
    },
    [presetDraft, refreshWorkspace],
  );

  return (
    <TooltipProvider>
      <div className="flex h-svh min-h-0 flex-col overflow-hidden bg-background text-foreground">
        <header className="flex h-14 shrink-0 items-center gap-3 border-b px-4">
          <div className="flex size-9 shrink-0 items-center justify-center rounded-md border bg-muted">
            <Bot className="size-4" />
          </div>
          <div className="min-w-0 flex-1">
            <h1 className="truncate text-sm font-semibold">Roleplay</h1>
            <p className="truncate text-xs text-muted-foreground">
              Tavern-style characters, chats, world info, and presets
            </p>
          </div>
          <Button variant="outline" size="sm" asChild>
            <Link to="/agent">Agent</Link>
          </Button>
          <Button
            variant="outline"
            size="icon-sm"
            onClick={() => void refreshWorkspace()}
            disabled={loading || busy}
            aria-label="Refresh roleplay workspace"
          >
            <RefreshCw className={cn("size-4", loading && "animate-spin")} />
          </Button>
        </header>

        <div className="grid min-h-0 flex-1 grid-cols-1 overflow-hidden lg:grid-cols-[4rem_21rem_minmax(0,1fr)_22rem]">
          <nav className="flex gap-2 overflow-x-auto border-b p-2 lg:flex-col lg:overflow-x-visible lg:border-r lg:border-b-0">
            {sectionItems.map((item) => {
              const Icon = item.icon;
              const active = section === item.id;
              return (
                <Tooltip key={item.id}>
                  <TooltipTrigger asChild>
                    <Button
                      variant={active ? "secondary" : "ghost"}
                      size="icon-sm"
                      className="shrink-0"
                      onClick={() => setSection(item.id)}
                      aria-label={item.label}
                    >
                      <Icon className="size-4" />
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent side="right">{item.label}</TooltipContent>
                </Tooltip>
              );
            })}
          </nav>

          <aside className="min-h-0 border-b lg:border-r lg:border-b-0">
            <div className="flex h-full min-h-0 flex-col">
              <div className="shrink-0 border-b p-3">
                <div className="grid grid-cols-2 gap-2">
                  <StatBadge label="Characters" value={overview?.characterCount ?? 0} />
                  <StatBadge label="World Info" value={overview?.worldInfoCount ?? 0} />
                  <StatBadge label="Presets" value={overview?.presetCount ?? 0} />
                  <StatBadge label="Groups" value={overview?.groupCount ?? 0} />
                </div>
              </div>

              {loading ? (
                <LoadingList />
              ) : (
                <ScrollArea className="min-h-0 flex-1">
                  {section === "characters" ? (
                    <CharactersPane
                      characters={characters}
                      selectedCharacterId={selectedCharacterId}
                      busy={busy}
                      draft={characterDraft}
                      onDraftChange={setCharacterDraft}
                      onCreateCharacter={handleCreateCharacter}
                      onSelectCharacter={(id) => {
                        setSelectedCharacterId(id);
                        setSection("chats");
                      }}
                      onToggleFavorite={(id) => void handleToggleFavorite(id)}
                    />
                  ) : null}

                  {section === "chats" ? (
                    <ChatsPane
                      characters={characters}
                      chats={chats}
                      selectedCharacterId={selectedCharacterId}
                      selectedChatId={selectedChatId}
                      chatTitle={chatTitle}
                      busy={busy}
                      loading={chatsLoading}
                      onSelectCharacter={setSelectedCharacterId}
                      onSelectChat={setSelectedChatId}
                      onChatTitleChange={setChatTitle}
                      onCreateChat={handleCreateChat}
                      onDeleteChat={(id) => void handleDeleteChat(id)}
                      onTogglePinChat={(id) => void handleTogglePinChat(id)}
                    />
                  ) : null}

                  {section === "worlds" ? (
                    <WorldInfoPane
                      worldInfos={worldInfos}
                      selectedWorldInfoId={selectedWorldInfoId}
                      busy={busy}
                      draft={worldInfoDraft}
                      onDraftChange={setWorldInfoDraft}
                      onCreateWorldInfo={handleCreateWorldInfo}
                      onSelectWorldInfo={setSelectedWorldInfoId}
                    />
                  ) : null}

                  {section === "presets" ? (
                    <PresetsPane
                      presets={presets}
                      selectedPresetId={selectedPresetId}
                      busy={busy}
                      draft={presetDraft}
                      onDraftChange={setPresetDraft}
                      onCreatePreset={handleCreatePreset}
                      onSelectPreset={setSelectedPresetId}
                    />
                  ) : null}
                </ScrollArea>
              )}
            </div>
          </aside>

          <main className="flex min-h-0 flex-col overflow-hidden">
            <ChatWorkspace
              character={selectedCharacter}
              chatDetail={chatDetail}
              loading={chatLoading}
              busy={busy}
              composeRole={composeRole}
              composeText={composeText}
              onComposeRoleChange={setComposeRole}
              onComposeTextChange={setComposeText}
              onAppendMessage={handleAppendMessage}
              onDeleteMessage={(id) => void handleDeleteMessage(id)}
            />
          </main>

          <aside className="min-h-0 border-t lg:border-l lg:border-t-0">
            <InspectorPane
              character={selectedCharacter}
              chat={chatDetail?.chat ?? null}
              worldInfo={selectedWorldInfo}
              preset={selectedPreset}
              section={section}
            />
          </aside>
        </div>
      </div>
    </TooltipProvider>
  );
}

function CharactersPane({
  characters,
  selectedCharacterId,
  busy,
  draft,
  onDraftChange,
  onCreateCharacter,
  onSelectCharacter,
  onToggleFavorite,
}: {
  characters: RoleplayCharacterDto[];
  selectedCharacterId: string | null;
  busy: boolean;
  draft: CharacterDraft;
  onDraftChange: React.Dispatch<React.SetStateAction<CharacterDraft>>;
  onCreateCharacter: (event: React.FormEvent<HTMLFormElement>) => void;
  onSelectCharacter: (id: string) => void;
  onToggleFavorite: (id: string) => void;
}) {
  return (
    <div className="space-y-3 p-3">
      <form className="space-y-2 rounded-md border bg-muted/30 p-3" onSubmit={onCreateCharacter}>
        <div className="flex items-center gap-2 text-sm font-medium">
          <Plus className="size-4" />
          New character
        </div>
        <Input
          value={draft.name}
          onChange={(event) => onDraftChange((current) => ({ ...current, name: event.target.value }))}
          placeholder="Name"
        />
        <Textarea
          value={draft.description}
          onChange={(event) =>
            onDraftChange((current) => ({ ...current, description: event.target.value }))
          }
          placeholder="Description"
          className="min-h-20 resize-none text-sm"
        />
        <Textarea
          value={draft.personality}
          onChange={(event) =>
            onDraftChange((current) => ({ ...current, personality: event.target.value }))
          }
          placeholder="Personality"
          className="min-h-16 resize-none text-sm"
        />
        <Input
          value={draft.tags}
          onChange={(event) => onDraftChange((current) => ({ ...current, tags: event.target.value }))}
          placeholder="Tags, comma separated"
        />
        <Button type="submit" size="sm" className="w-full" disabled={busy}>
          <Plus className="size-4" />
          Create
        </Button>
      </form>

      <div className="space-y-2">
        {characters.length === 0 ? (
          <EmptyPane
            title="No characters"
            description="Create or import Tavern character cards from the native app."
          />
        ) : (
          characters.map((character) => (
            <button
              key={character.id}
              type="button"
              onClick={() => onSelectCharacter(character.id)}
              className={cn(
                "flex w-full min-w-0 items-start gap-3 rounded-md border p-3 text-left transition-colors hover:bg-accent",
                selectedCharacterId === character.id && "border-primary bg-accent",
              )}
            >
              <CharacterAvatar character={character} />
              <div className="min-w-0 flex-1">
                <div className="flex min-w-0 items-center gap-2">
                  <div className="truncate text-sm font-medium">{character.name || "Unnamed"}</div>
                  {character.favorite ? <Star className="size-3 fill-current text-primary" /> : null}
                </div>
                <div className="mt-1 line-clamp-2 text-xs text-muted-foreground">
                  {character.description || "No description"}
                </div>
                <div className="mt-2 flex flex-wrap gap-1">
                  {character.tags.slice(0, 3).map((tag) => (
                    <Badge key={tag} variant="secondary" className="max-w-full truncate">
                      {tag}
                    </Badge>
                  ))}
                  <Badge variant="outline">{character.chatCount} chats</Badge>
                </div>
              </div>
              <Button
                type="button"
                variant="ghost"
                size="icon-xs"
                onClick={(event) => {
                  event.stopPropagation();
                  onToggleFavorite(character.id);
                }}
                aria-label="Toggle favorite"
              >
                <Star className={cn("size-3", character.favorite && "fill-current")} />
              </Button>
            </button>
          ))
        )}
      </div>
    </div>
  );
}

function ChatsPane({
  characters,
  chats,
  selectedCharacterId,
  selectedChatId,
  chatTitle,
  busy,
  loading,
  onSelectCharacter,
  onSelectChat,
  onChatTitleChange,
  onCreateChat,
  onDeleteChat,
  onTogglePinChat,
}: {
  characters: RoleplayCharacterDto[];
  chats: RoleplayChatDto[];
  selectedCharacterId: string | null;
  selectedChatId: string | null;
  chatTitle: string;
  busy: boolean;
  loading: boolean;
  onSelectCharacter: (id: string) => void;
  onSelectChat: (id: string) => void;
  onChatTitleChange: (value: string) => void;
  onCreateChat: (event: React.FormEvent<HTMLFormElement>) => void;
  onDeleteChat: (id: string) => void;
  onTogglePinChat: (id: string) => void;
}) {
  return (
    <div className="space-y-3 p-3">
      <form className="space-y-2 rounded-md border bg-muted/30 p-3" onSubmit={onCreateChat}>
        <Select value={selectedCharacterId ?? ""} onValueChange={onSelectCharacter}>
          <SelectTrigger className="w-full">
            <SelectValue placeholder="Select character" />
          </SelectTrigger>
          <SelectContent>
            {characters.map((character) => (
              <SelectItem key={character.id} value={character.id}>
                {character.name || "Unnamed"}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Input
          value={chatTitle}
          onChange={(event) => onChatTitleChange(event.target.value)}
          placeholder="Chat title"
        />
        <Button type="submit" size="sm" className="w-full" disabled={busy || !selectedCharacterId}>
          <Plus className="size-4" />
          New chat
        </Button>
      </form>

      {loading ? (
        <LoadingList />
      ) : chats.length === 0 ? (
        <EmptyPane
          title="No chats"
          description="Create a chat for the selected character to start a Tavern transcript."
        />
      ) : (
        <div className="space-y-2">
          {chats.map((chat) => (
            <button
              key={chat.id}
              type="button"
              onClick={() => onSelectChat(chat.id)}
              className={cn(
                "flex w-full min-w-0 items-start gap-3 rounded-md border p-3 text-left transition-colors hover:bg-accent",
                selectedChatId === chat.id && "border-primary bg-accent",
              )}
            >
              <div className="flex size-9 shrink-0 items-center justify-center rounded-md bg-muted">
                <MessageSquare className="size-4" />
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex min-w-0 items-center gap-2">
                  <div className="truncate text-sm font-medium">{chat.title || "Untitled Chat"}</div>
                  {chat.pinned ? <Star className="size-3 fill-current" /> : null}
                </div>
                <div className="mt-1 truncate text-xs text-muted-foreground">
                  {chat.characterName || "Character"} / {chat.messageCount} messages
                </div>
                <div className="mt-2 text-xs text-muted-foreground">{formatTime(chat.updatedAt)}</div>
              </div>
              <div className="flex shrink-0 flex-col gap-1">
                <Button
                  type="button"
                  variant="ghost"
                  size="icon-xs"
                  onClick={(event) => {
                    event.stopPropagation();
                    onTogglePinChat(chat.id);
                  }}
                  aria-label="Toggle pin"
                >
                  <Star className={cn("size-3", chat.pinned && "fill-current")} />
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon-xs"
                  onClick={(event) => {
                    event.stopPropagation();
                    onDeleteChat(chat.id);
                  }}
                  aria-label="Delete chat"
                >
                  <Trash2 className="size-3" />
                </Button>
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

function WorldInfoPane({
  worldInfos,
  selectedWorldInfoId,
  busy,
  draft,
  onDraftChange,
  onCreateWorldInfo,
  onSelectWorldInfo,
}: {
  worldInfos: RoleplayWorldInfoDto[];
  selectedWorldInfoId: string | null;
  busy: boolean;
  draft: WorldInfoDraft;
  onDraftChange: React.Dispatch<React.SetStateAction<WorldInfoDraft>>;
  onCreateWorldInfo: (event: React.FormEvent<HTMLFormElement>) => void;
  onSelectWorldInfo: (id: string) => void;
}) {
  return (
    <div className="space-y-3 p-3">
      <form className="space-y-2 rounded-md border bg-muted/30 p-3" onSubmit={onCreateWorldInfo}>
        <div className="flex items-center gap-2 text-sm font-medium">
          <BookMarked className="size-4" />
          New world info
        </div>
        <Input
          value={draft.name}
          onChange={(event) => onDraftChange((current) => ({ ...current, name: event.target.value }))}
          placeholder="Name"
        />
        <Textarea
          value={draft.description}
          onChange={(event) =>
            onDraftChange((current) => ({ ...current, description: event.target.value }))
          }
          placeholder="Description"
          className="min-h-20 resize-none text-sm"
        />
        <Button type="submit" size="sm" className="w-full" disabled={busy}>
          <Plus className="size-4" />
          Create
        </Button>
      </form>

      {worldInfos.length === 0 ? (
        <EmptyPane title="No world info" description="Create lorebooks for Tavern prompt injection." />
      ) : (
        <div className="space-y-2">
          {worldInfos.map((worldInfo) => (
            <button
              key={worldInfo.id}
              type="button"
              onClick={() => onSelectWorldInfo(worldInfo.id)}
              className={cn(
                "w-full rounded-md border p-3 text-left transition-colors hover:bg-accent",
                selectedWorldInfoId === worldInfo.id && "border-primary bg-accent",
              )}
            >
              <div className="truncate text-sm font-medium">{worldInfo.name}</div>
              <div className="mt-1 line-clamp-2 text-xs text-muted-foreground">
                {worldInfo.description || "No description"}
              </div>
              <div className="mt-2 flex gap-1">
                <Badge variant="outline">{worldInfo.entries.length} entries</Badge>
                <Badge variant="secondary">{worldInfo.scanTrigger}</Badge>
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

function PresetsPane({
  presets,
  selectedPresetId,
  busy,
  draft,
  onDraftChange,
  onCreatePreset,
  onSelectPreset,
}: {
  presets: RoleplayPresetDto[];
  selectedPresetId: string | null;
  busy: boolean;
  draft: PresetDraft;
  onDraftChange: React.Dispatch<React.SetStateAction<PresetDraft>>;
  onCreatePreset: (event: React.FormEvent<HTMLFormElement>) => void;
  onSelectPreset: (id: string) => void;
}) {
  return (
    <div className="space-y-3 p-3">
      <form className="space-y-2 rounded-md border bg-muted/30 p-3" onSubmit={onCreatePreset}>
        <div className="flex items-center gap-2 text-sm font-medium">
          <Settings2 className="size-4" />
          New preset
        </div>
        <Input
          value={draft.name}
          onChange={(event) => onDraftChange((current) => ({ ...current, name: event.target.value }))}
          placeholder="Name"
        />
        <Select
          value={draft.type}
          onValueChange={(value) => onDraftChange((current) => ({ ...current, type: value }))}
        >
          <SelectTrigger className="w-full">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {["OPENAI", "CLAUDE", "GEMINI", "KOBOLDAI", "TEXTGEN"].map((type) => (
              <SelectItem key={type} value={type}>
                {type}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Textarea
          value={draft.parameters}
          onChange={(event) =>
            onDraftChange((current) => ({ ...current, parameters: event.target.value }))
          }
          spellCheck={false}
          className="min-h-28 resize-none font-mono text-xs"
        />
        <Button type="submit" size="sm" className="w-full" disabled={busy}>
          <Plus className="size-4" />
          Create
        </Button>
      </form>

      {presets.length === 0 ? (
        <EmptyPane title="No presets" description="Create model presets for Tavern generation settings." />
      ) : (
        <div className="space-y-2">
          {presets.map((preset) => (
            <button
              key={preset.id}
              type="button"
              onClick={() => onSelectPreset(preset.id)}
              className={cn(
                "w-full rounded-md border p-3 text-left transition-colors hover:bg-accent",
                selectedPresetId === preset.id && "border-primary bg-accent",
              )}
            >
              <div className="truncate text-sm font-medium">{preset.name}</div>
              <div className="mt-1 line-clamp-2 text-xs text-muted-foreground">
                {preset.description || `${Object.keys(preset.parameters).length} parameters`}
              </div>
              <div className="mt-2">
                <Badge variant="secondary">{preset.type}</Badge>
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

function ChatWorkspace({
  character,
  chatDetail,
  loading,
  busy,
  composeRole,
  composeText,
  onComposeRoleChange,
  onComposeTextChange,
  onAppendMessage,
  onDeleteMessage,
}: {
  character: RoleplayCharacterDto | null;
  chatDetail: RoleplayChatDetailDto | null;
  loading: boolean;
  busy: boolean;
  composeRole: ComposeRole;
  composeText: string;
  onComposeRoleChange: (role: ComposeRole) => void;
  onComposeTextChange: (value: string) => void;
  onAppendMessage: (event: React.FormEvent<HTMLFormElement>) => void;
  onDeleteMessage: (id: string) => void;
}) {
  return (
    <div className="flex min-h-0 flex-1 flex-col">
      <div className="flex shrink-0 items-center gap-3 border-b px-4 py-3">
        {character ? <CharacterAvatar character={character} /> : null}
        <div className="min-w-0 flex-1">
          <div className="truncate text-sm font-semibold">
            {chatDetail?.chat.title || character?.name || "Select a character"}
          </div>
          <div className="truncate text-xs text-muted-foreground">
            {chatDetail
              ? `${chatDetail.chat.characterName || "Character"} / ${chatDetail.messages.length} messages`
              : "Create a chat to begin a Tavern transcript"}
          </div>
        </div>
        {chatDetail?.chat.pinned ? <Badge variant="secondary">Pinned</Badge> : null}
      </div>

      {loading ? (
        <div className="space-y-4 p-4">
          <Skeleton className="h-20 w-4/5 rounded-md" />
          <Skeleton className="ml-auto h-20 w-3/5 rounded-md" />
          <Skeleton className="h-24 w-5/6 rounded-md" />
        </div>
      ) : !chatDetail ? (
        <EmptyPane
          title="No active Tavern chat"
          description="Select a character, create a chat, then write user and character turns."
        />
      ) : (
        <ScrollArea className="min-h-0 flex-1">
          <div className="mx-auto flex w-full max-w-4xl flex-col gap-3 px-4 py-4">
            {chatDetail.messages.length === 0 ? (
              <EmptyPane
                title="Empty transcript"
                description="Append user and character messages to build a Tavern-compatible chat."
              />
            ) : (
              chatDetail.messages.map((message) => {
                const isUser = message.role === "user";
                return (
                  <div
                    key={message.id}
                    className={cn(
                      "group flex w-full items-start gap-2",
                      isUser && "flex-row-reverse",
                    )}
                  >
                    <div
                      className={cn(
                        "flex size-8 shrink-0 items-center justify-center rounded-md border bg-muted",
                        isUser && "bg-primary text-primary-foreground",
                      )}
                    >
                      {isUser ? <UserRound className="size-4" /> : <Bot className="size-4" />}
                    </div>
                    <div
                      className={cn(
                        "min-w-0 max-w-[78%] rounded-md border bg-background px-3 py-2 shadow-xs",
                        isUser && "bg-primary text-primary-foreground",
                      )}
                    >
                      <div className="mb-1 flex items-center gap-2 text-xs opacity-75">
                        <span className="font-medium">{roleLabel(message)}</span>
                        <span>{formatTime(message.timestamp)}</span>
                        {message.swipeAlternatives.length > 0 ? (
                          <Badge variant="outline">{message.swipeAlternatives.length + 1} swipes</Badge>
                        ) : null}
                      </div>
                      <div className="whitespace-pre-wrap break-words text-sm leading-6">
                        {message.content}
                      </div>
                    </div>
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon-xs"
                      className="opacity-0 transition-opacity group-hover:opacity-100"
                      onClick={() => onDeleteMessage(message.id)}
                      aria-label="Delete message"
                    >
                      <Trash2 className="size-3" />
                    </Button>
                  </div>
                );
              })
            )}
          </div>
        </ScrollArea>
      )}

      <form className="shrink-0 border-t p-3" onSubmit={onAppendMessage}>
        <div className="mx-auto flex w-full max-w-4xl flex-col gap-2">
          <ButtonGroup>
            <Button
              type="button"
              variant={composeRole === "user" ? "default" : "outline"}
              size="sm"
              onClick={() => onComposeRoleChange("user")}
            >
              User
            </Button>
            <Button
              type="button"
              variant={composeRole === "assistant" ? "default" : "outline"}
              size="sm"
              onClick={() => onComposeRoleChange("assistant")}
            >
              Character
            </Button>
          </ButtonGroup>
          <div className="flex items-end gap-2">
            <Textarea
              value={composeText}
              onChange={(event) => onComposeTextChange(event.target.value)}
              placeholder="Write the next Tavern message"
              className="max-h-40 min-h-20 resize-none"
              disabled={!chatDetail}
            />
            <Button
              type="submit"
              size="icon-lg"
              disabled={busy || !chatDetail || composeText.trim().length === 0}
              aria-label="Append message"
            >
              <Send className="size-4" />
            </Button>
          </div>
        </div>
      </form>
    </div>
  );
}

function InspectorPane({
  character,
  chat,
  worldInfo,
  preset,
  section,
}: {
  character: RoleplayCharacterDto | null;
  chat: RoleplayChatDto | null;
  worldInfo: RoleplayWorldInfoDto | null;
  preset: RoleplayPresetDto | null;
  section: RoleplaySection;
}) {
  return (
    <ScrollArea className="h-full">
      <div className="space-y-4 p-4">
        <div>
          <div className="text-sm font-semibold">Inspector</div>
          <div className="text-xs text-muted-foreground">Active Tavern object metadata</div>
        </div>

        {character ? (
          <section className="space-y-3 rounded-md border p-3">
            <div className="flex items-start gap-3">
              <CharacterAvatar character={character} />
              <div className="min-w-0 flex-1">
                <div className="truncate text-sm font-medium">{character.name || "Unnamed"}</div>
                <div className="text-xs text-muted-foreground">
                  {character.spec} / {character.specVersion}
                </div>
              </div>
            </div>
            <FieldBlock label="Description" value={character.description} />
            <FieldBlock label="Personality" value={character.personality} />
            <FieldBlock label="Scenario" value={character.scenario} />
            <FieldBlock label="First Message" value={character.firstMessage} />
            <FieldBlock label="System Prompt" value={character.systemPrompt} />
            <div className="flex flex-wrap gap-1">
              {character.tags.length === 0 ? (
                <Badge variant="outline">No tags</Badge>
              ) : (
                character.tags.map((tag) => (
                  <Badge key={tag} variant="secondary">
                    {tag}
                  </Badge>
                ))
              )}
            </div>
          </section>
        ) : null}

        {chat ? (
          <section className="space-y-2 rounded-md border p-3">
            <div className="text-sm font-medium">Chat</div>
            <dl className="grid grid-cols-2 gap-2 text-xs">
              <InfoTerm label="Messages" value={String(chat.messageCount)} />
              <InfoTerm label="Pinned" value={chat.pinned ? "Yes" : "No"} />
              <InfoTerm label="Created" value={formatTime(chat.createdAt)} />
              <InfoTerm label="Updated" value={formatTime(chat.updatedAt)} />
            </dl>
          </section>
        ) : null}

        {section === "worlds" && worldInfo ? (
          <section className="space-y-3 rounded-md border p-3">
            <div>
              <div className="text-sm font-medium">{worldInfo.name}</div>
              <div className="text-xs text-muted-foreground">
                {worldInfo.entries.length} entries / scan depth {worldInfo.scanDepth}
              </div>
            </div>
            <FieldBlock label="Description" value={worldInfo.description} />
            <div className="flex flex-wrap gap-1">
              <Badge variant="secondary">{worldInfo.scanTrigger}</Badge>
              <Badge variant="outline">{worldInfo.selectiveLogic}</Badge>
            </div>
            {worldInfo.entries.slice(0, 6).map((entry) => (
              <div key={entry.id} className="rounded-md border bg-muted/30 p-2">
                <div className="truncate text-xs font-medium">{entry.comment || entry.key || "Entry"}</div>
                <div className="mt-1 line-clamp-2 text-xs text-muted-foreground">
                  {entry.content || truncateText(entry.keys.join(", "), 120)}
                </div>
              </div>
            ))}
          </section>
        ) : null}

        {section === "presets" && preset ? (
          <section className="space-y-3 rounded-md border p-3">
            <div>
              <div className="text-sm font-medium">{preset.name}</div>
              <div className="text-xs text-muted-foreground">{preset.type}</div>
            </div>
            <FieldBlock label="Description" value={preset.description} />
            <pre className="max-h-72 overflow-auto rounded-md bg-muted p-3 text-xs">
              {JSON.stringify(preset.parameters, null, 2)}
            </pre>
          </section>
        ) : null}

        {!character && !worldInfo && !preset ? (
          <EmptyPane title="Nothing selected" description="Select a Tavern object to inspect it." />
        ) : null}
      </div>
    </ScrollArea>
  );
}

function FieldBlock({ label, value }: { label: string; value: string }) {
  if (!value.trim()) return null;

  return (
    <div>
      <div className="mb-1 text-xs font-medium text-muted-foreground">{label}</div>
      <div className="whitespace-pre-wrap break-words rounded-md bg-muted/50 p-2 text-xs leading-5">
        {value}
      </div>
    </div>
  );
}

function InfoTerm({ label, value }: { label: string; value: string }) {
  return (
    <div className="min-w-0 rounded-md bg-muted/50 p-2">
      <dt className="truncate text-muted-foreground">{label}</dt>
      <dd className="truncate font-medium">{value}</dd>
    </div>
  );
}
