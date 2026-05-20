# EterUee

<img src="docs/icon.svg" alt="EterUee 圖示" width="72" />

原生 Android LLM 聊天客戶端。

語言：[English](README.md) | [簡體中文](README_ZH_CN.md) | 繁體中文

## 範圍

- 使用 Kotlin 與 Jetpack Compose 建構的 Android 客戶端。
- 透過 `ai` 模組統一接入多類 AI 供應商。
- 樹狀對話結構，支援訊息分支。
- Assistant 獨立儲存模型、提示詞、記憶、工具、請求設定。
- 文件解析、網路搜尋、TTS、程式碼高亮、角色扮演模組。
- 內建 Ktor 服務，在區域網路中提供 React Web UI。

## 模組

| 路徑 | 職責 |
| --- | --- |
| `app` | Android 應用、Compose UI、ViewModel、持久化、Web 路由 |
| `ai` | Provider 抽象、訊息模型、串流文字生成 |
| `common` | 共用工具與 Kotlin 擴充 |
| `document` | PDF、DOCX、PPTX 解析 |
| `highlight` | 程式碼語法高亮 |
| `roleplay` | 角色、聊天、世界書、預設、群組工作流程 |
| `search` | 搜尋供應商與搜尋 SDK 整合 |
| `tts` | 文字轉語音供應商 |
| `web` | 嵌入式 Ktor 服務與靜態 Web UI 託管 |
| `web-ui` | 瀏覽器存取用 React 前端 |

## 建置

Firebase 建置需要 `app/google-services.json`。

```bash
git clone https://github.com/EterUltimate/EterUee.git
cd EterUee
./gradlew assembleDebug
```

## 測試

```bash
./gradlew test
./gradlew connectedDebugAndroidTest
```

## APK

```bash
./gradlew :app:assembleDebug
```

Debug APK 輸出目錄：

```text
app/build/outputs/apk/debug/
```

## UML

### 使用案例圖

```mermaid
flowchart LR
    User["使用者"]
    Chat["與 LLM 對話"]
    Configure["設定 Assistant"]
    Search["執行網路搜尋"]
    Parse["附加文件"]
    Branch["建立對話分支"]
    Roleplay["使用角色扮演模組"]
    WebAccess["透過瀏覽器存取"]

    User --> Chat
    User --> Configure
    User --> Search
    User --> Parse
    User --> Branch
    User --> Roleplay
    User --> WebAccess
```

### 元件圖

```mermaid
flowchart LR
    User["使用者"]
    Android["Android 應用\napp"]
    WebUI["React Web UI\nweb-ui"]
    WebServer["嵌入式 Ktor 服務\nweb"]
    Conversation["對話服務\napp"]
    AI["AI SDK\nai"]
    Search["搜尋 SDK\nsearch"]
    Docs["文件解析\ndocument"]
    TTS["TTS\ntts"]
    Roleplay["角色扮演\nroleplay"]
    Store["Room / DataStore"]
    Provider["LLM / 搜尋 / TTS 供應商"]

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

### 對話模型

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

### 聊天串流

```mermaid
sequenceDiagram
    participant User as 使用者
    participant UI as Compose / Web UI
    participant Routes as ConversationRoutes
    participant SDK as AISDK
    participant Provider as AI Provider
    participant Store as 本地儲存

    User->>UI: 提交訊息
    UI->>Routes: POST /api/conversations/{id}/messages
    Routes->>Store: 儲存使用者訊息
    UI->>Routes: POST /api/conversations/stream-v2/chat
    Routes->>SDK: streamText(request)
    SDK->>Provider: 請求串流生成
    Provider-->>SDK: Delta / tool call / usage
    SDK-->>Routes: TextChunk
    Routes-->>UI: AI SDK data stream
    Routes->>Store: 儲存助手結果
```

### 訊息狀態

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

### 部署圖

```mermaid
flowchart TB
    subgraph AndroidDevice["Android 裝置"]
        App["EterUee APK"]
        Ktor["Ktor 服務"]
        Static["內建 web-ui 靜態檔案"]
        Room["Room 資料庫"]
        DataStore["DataStore"]
    end

    subgraph LocalNetwork["區域網路"]
        Browser["桌面 / 平板瀏覽器"]
    end

    subgraph External["外部服務"]
        LLM["LLM 供應商"]
        SearchAPI["搜尋 API"]
        TTSAPI["TTS 供應商"]
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

### 資料關係

```mermaid
erDiagram
    ASSISTANT ||--o{ CONVERSATION : owns
    CONVERSATION ||--o{ MESSAGE_NODE : contains
    MESSAGE_NODE ||--o{ UI_MESSAGE : selects
    UI_MESSAGE ||--o{ MESSAGE_PART : contains
    ASSISTANT ||--o{ MEMORY : stores
    CONVERSATION ||--o{ BRANCH : exposes
```

## 開發

- Android 模組使用 Android Studio。
- Gradle 指令從倉庫根目錄執行。
- React 前端位於 `web-ui/`。
- 不提交生成的建置產物。

## 致謝

感謝 [Rikkahub](https://github.com/rikkahub/rikkahub)。它在 Android LLM 客戶端方向上的工作，是本專案重要的參考與靈感來源。

## 授權

雙授權：

- [AGPL v3](LICENSE)：開源與非商業使用。
- 商業授權：商業用途請聯絡專案維護者。
