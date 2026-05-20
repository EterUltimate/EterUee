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
flowchart LR
    User["User"]
    Chat["Chat with LLM"]
    Configure["Configure assistant"]
    Search["Run web search"]
    Parse["Attach documents"]
    Branch["Branch conversation"]
    Roleplay["Use roleplay module"]
    WebAccess["Access from browser"]

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
flowchart LR
    User["User"]
    Android["Android app\napp"]
    WebUI["React web UI\nweb-ui"]
    WebServer["Embedded Ktor server\nweb"]
    Conversation["Conversation services\napp"]
    AI["AI SDK\nai"]
    Search["Search SDK\nsearch"]
    Docs["Document parser\ndocument"]
    TTS["TTS\ntts"]
    Roleplay["Roleplay\nroleplay"]
    Store["Room / DataStore"]
    Provider["LLM / search / TTS providers"]

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
classDiagram
    class Assistant {
        id
        modelSettings
        systemPrompt
        tools
        memory
    }

    class Conversation {
        id
        title
        createdAt
        pinned
    }

    class MessageNode {
        id
        parentId
        selectIndex
    }

    class UIMessage {
        id
        role
        modelId
        createdAt
    }

    class UIMessagePart {
        type
        text
        metadata
    }

    Assistant "1" --> "0..*" Conversation
    Conversation "1" --> "1..*" MessageNode
    MessageNode "1" --> "1..*" UIMessage
    UIMessage "1" --> "1..*" UIMessagePart
```

### Chat Stream

```mermaid
sequenceDiagram
    participant User
    participant UI as Compose / Web UI
    participant Routes as ConversationRoutes
    participant SDK as AISDK
    participant Provider as AI Provider
    participant Store as Local Store

    User->>UI: Submit message
    UI->>Routes: POST /api/conversations/{id}/messages
    Routes->>Store: Persist user message
    UI->>Routes: POST /api/conversations/stream-v2/chat
    Routes->>SDK: streamText(request)
    SDK->>Provider: Stream completion
    Provider-->>SDK: Delta / tool call / usage
    SDK-->>Routes: TextChunk
    Routes-->>UI: AI SDK data stream
    Routes->>Store: Persist assistant result
```

### Message State

```mermaid
stateDiagram-v2
    [*] --> Draft
    Draft --> Persisted: submit
    Persisted --> Generating: stream request
    Generating --> Completed: finish
    Generating --> Failed: error
    Completed --> Branched: regenerate or fork
    Failed --> Draft: edit and retry
    Branched --> Persisted: select branch
```

### Deployment

```mermaid
flowchart TB
    subgraph AndroidDevice["Android device"]
        App["EterUee APK"]
        Ktor["Ktor server"]
        Static["Bundled web-ui static files"]
        Room["Room database"]
        DataStore["DataStore"]
    end

    subgraph LocalNetwork["Local network"]
        Browser["Desktop / tablet browser"]
    end

    subgraph External["External services"]
        LLM["LLM providers"]
        SearchAPI["Search APIs"]
        TTSAPI["TTS providers"]
    end

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
erDiagram
    ASSISTANT ||--o{ CONVERSATION : owns
    CONVERSATION ||--o{ MESSAGE_NODE : contains
    MESSAGE_NODE ||--o{ UI_MESSAGE : selects
    UI_MESSAGE ||--o{ MESSAGE_PART : contains
    ASSISTANT ||--o{ MEMORY : stores
    CONVERSATION ||--o{ BRANCH : exposes
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
