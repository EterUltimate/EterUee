# EterUee

<img src="docs/icon.svg" alt="EterUee icon" width="72" />

Native Android LLM chat client.

Languages: English | [简体中文](README_ZH_CN.md) | [繁體中文](README_ZH_TW.md)

## Scope

- Android client built with Kotlin and Jetpack Compose.
- Multi-provider AI access through a shared `ai` module.
- Tree-based conversations with message branches.
- Assistants with isolated model, prompt, memory, tool, and request settings.
- Document, search, TTS, syntax highlighting, and roleplay modules.
- Embedded Ktor server that hosts the React web UI on the local network.

## Modules

| Path | Responsibility |
| --- | --- |
| `app` | Android app, Compose UI, ViewModels, persistence, web routes |
| `ai` | Provider abstraction, message model, streaming text generation |
| `common` | Shared utilities and Kotlin extensions |
| `document` | PDF, DOCX, PPTX parsing |
| `highlight` | Code syntax highlighting |
| `roleplay` | Character, chat, world info, preset, group workflows |
| `search` | Search providers and search SDK integration |
| `tts` | Text-to-speech providers |
| `web` | Embedded Ktor server and static web UI hosting |
| `web-ui` | React frontend for browser access |

## Build

`app/google-services.json` is required for Firebase-backed builds.

```bash
git clone https://github.com/EterUltimate/EterUee.git
cd EterUee
./gradlew assembleDebug
```

## Test

```bash
./gradlew test
./gradlew connectedDebugAndroidTest
```

## APK

```bash
./gradlew :app:assembleDebug
```

Debug APKs are written to:

```text
app/build/outputs/apk/debug/
```

## UML

### Use Cases

```mermaid
graph TD
    User[User]
    Chat[Chat with LLM]
    Configure[Configure assistant]
    Search[Run web search]
    Parse[Attach documents]
    Branch[Branch conversation]
    Roleplay[Use roleplay module]
    WebAccess[Access from browser]

    User --> Chat
    User --> Configure
    User --> Search
    User --> Parse
    User --> Branch
    User --> Roleplay
    User --> WebAccess
```

### Component

```mermaid
graph TD
    User[User]
    Android[Android app]
    WebUI[React web UI]
    WebServer[Embedded Ktor server]
    Conversation[Conversation services]
    AI[AI SDK]
    Search[Search SDK]
    Docs[Document parser]
    TTS[TTS]
    Roleplay[Roleplay]
    Store[Room and DataStore]
    Provider[Provider APIs]

    User --> Android
    User --> WebUI
    WebUI --> WebServer
    Android --> Conversation
    WebServer --> Conversation
    Conversation --> AI
    Conversation --> Search
    Conversation --> Docs
    Conversation --> TTS
    Conversation --> Roleplay
    Conversation --> Store
    AI --> Provider
    Search --> Provider
    TTS --> Provider
```

### Conversation Model

```mermaid
graph TD
    Assistant[Assistant]
    Conversation[Conversation]
    MessageNode[Message node]
    UIMessage[UI message]
    UIMessagePart[Message part]
    Settings[Model and prompt settings]
    Memory[Memory]
    Tools[Tools]

    Assistant --> Settings
    Assistant --> Memory
    Assistant --> Tools
    Assistant --> Conversation
    Conversation --> MessageNode
    MessageNode --> UIMessage
    UIMessage --> UIMessagePart
```

### Chat Stream

```mermaid
graph TD
    User[User]
    UI[Compose or Web UI]
    SaveUser[Persist user message]
    Stream[Start chat stream]
    SDK[AI SDK]
    Provider[AI provider]
    Delta[Stream chunks]
    SaveAssistant[Persist assistant result]

    User --> UI
    UI --> SaveUser
    SaveUser --> Stream
    Stream --> SDK
    SDK --> Provider
    Provider --> Delta
    Delta --> UI
    Delta --> SaveAssistant
```

### Message State

```mermaid
graph TD
    Draft[Draft]
    Persisted[Persisted]
    Generating[Generating]
    Completed[Completed]
    Failed[Failed]
    Branched[Branched]

    Draft --> Persisted
    Persisted --> Generating
    Generating --> Completed
    Generating --> Failed
    Completed --> Branched
    Failed --> Draft
    Branched --> Persisted
```

### Deployment

```mermaid
graph TD
    Browser[Desktop or tablet browser]
    App[EterUee APK]
    Ktor[Ktor server]
    Static[Bundled web UI files]
    Room[Room database]
    DataStore[DataStore]
    LLM[LLM providers]
    SearchAPI[Search APIs]
    TTSAPI[TTS providers]

    Browser --> Ktor
    App --> Ktor
    Ktor --> Static
    App --> Room
    App --> DataStore
    App --> LLM
    App --> SearchAPI
    App --> TTSAPI
```

### Data Relations

```mermaid
graph TD
    Assistant[Assistant]
    Conversation[Conversation]
    MessageNode[Message node]
    UIMessage[UI message]
    MessagePart[Message part]
    Memory[Memory]
    Branch[Branch]

    Assistant --> Conversation
    Conversation --> MessageNode
    MessageNode --> UIMessage
    UIMessage --> MessagePart
    Assistant --> Memory
    Conversation --> Branch
```

## Development

- Use Android Studio for Android modules.
- Use Gradle wrapper commands from the repository root.
- Use `web-ui/` for React frontend work.
- Do not commit generated build output.

## Acknowledgements

Thanks to [Rikkahub](https://github.com/rikkahub/rikkahub). Its work in the Android LLM client space is an important reference and source of inspiration for this project.

## License

Dual license:

- [AGPL v3](LICENSE) for open-source and non-commercial use.
- Commercial license for commercial use.
