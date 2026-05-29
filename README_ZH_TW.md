# EterUee

<img src="docs/icon.svg" alt="EterUee 圖示" width="72" />

EterUee 是一個原生 Android LLM 客戶端，圍繞本地優先的對話、多供應商 AI 接入、角色扮演工作流程、內建瀏覽器 UI 和應用內本地自動化工具建構。

語言：[English](README.md) | [簡體中文](README_ZH_CN.md) | 繁體中文

## 目前狀態

目前開發線是 `dev`。發布線是 `main`，除 Apple 平台外的發布產物可從 [GitHub Releases](https://github.com/EterUltimate/EterUee/releases) 取得。

近期專案已經從基礎 Android 聊天客戶端推進到更完整的本地 AI 工作區：

- 更新檢查來源改為 `https://github.com/EterUltimate/EterUee/releases`。
- 內建 EterUee 供應商預設地址為 `https://newapi.eterultimate.asia/v1`，描述為官方提供的 API。
- 透過本地相鄰倉庫 `../termux-app` 整合 `termux/termux-app` 的 terminal 模組，作為預設本地 shell 介面。
- 透過 `../hiddify-core/bin/hiddify-core.aar` 可選整合 `hiddify/hiddify-core`，用於本地流量管控實驗。
- RolePlay 模組已接入應用導航，涵蓋角色、聊天、世界書、群組、預設、書籤和視覺化編輯器。
- React Web UI 會建置進 Android `web` 模組，並由內建 Ktor 服務提供存取。

## 產品範圍

EterUee 的目標是作為完整的 Android AI 工作區執行：

- 接入 OpenAI 相容介面、Gemini 以及其他自訂供應商。
- 為 Assistant 獨立保存提示詞、模型參數、記憶選項、工具、自訂請求標頭和請求體。
- 使用樹狀對話結構，支援訊息分支和重新生成。
- 支援文件、圖片、OCR、文件轉提示詞和結構化訊息片段。
- 在啟用後使用本地工具、MCP 工具、搜尋供應商、TTS 供應商和 shell 執行。
- 使用本地 Room 資料和檔案資產執行角色扮演工作流程。
- 透過裝置內建 Web 服務向區域網路瀏覽器暴露會話介面。
- 使用嵌入式 Termux 終端視圖和應用作用域 shell runner，不要求安裝獨立 Termux 應用。
- 可選載入 Hiddify Core 進行本地流量管控實驗。

## 架構

```text
Android app (app)
  Compose UI, Navigation 3, ViewModel, Room, DataStore, WorkManager, Firebase, Koin
    |
    +-- ChatService 與訊息轉換器
    |     模板、think 標籤、文件、OCR、圖片、正則和輸出轉換
    |
    +-- AI SDK (ai)
    |     Provider 抽象、UIMessage 模型、串流生成、工具呼叫、OpenAI 相容 API
    |
    +-- 功能模組
    |     common, document, highlight, search, tts, roleplay, material3
    |
    +-- 本地執行時整合
    |     terminal-emulator, terminal-view, LocalShellRunner, HiddifyCoreManager
    |
    +-- 內建 Web 服務 (web)
          Ktor API, SSE, 來自 web-ui 的 React 靜態資源
```

核心模組職責：

| 路徑 | 職責 |
| --- | --- |
| `app` | Android 應用、Compose UI、導航、持久化裝配、設定、本地工具、Web 路由 |
| `ai` | Provider 抽象、`UIMessage`、串流生成、OpenAI 相容請求/回應處理 |
| `common` | 共用工具與 Kotlin 擴充 |
| `document` | PDF、DOCX、PPTX 解析和文件轉提示詞 |
| `highlight` | 程式碼語法高亮 |
| `search` | 搜尋供應商 SDK 整合 |
| `tts` | 文字轉語音供應商整合 |
| `roleplay` | 角色、聊天、世界書、群組、預設、書籤和 Tavern 相容工作流程 |
| `web` | 嵌入 Ktor 並託管 React Web UI 的 Android library |
| `web-ui` | React Router 7 瀏覽器前端，建置產物複製到 `web/src/main/resources/static` |
| `terminal-emulator`, `terminal-view` | 來自 `../termux-app` 的本地 Termux 模組 |
| `../hiddify-core/bin/hiddify-core.aar` | `app` 可選載入的 Hiddify Core gomobile binding |

詳見 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)。

## 倉庫布局

本專案通常配合多個相鄰工作樹/倉庫使用：

```text
C:\Users\zacza\Desktop\x\EterUee              dev 工作樹
C:\Users\zacza\Desktop\x\EterUee-main-merge   main/release 工作樹
C:\Users\zacza\Desktop\x\termux-app           Termux app 倉庫
C:\Users\zacza\Desktop\x\hiddify-core         Hiddify Core 倉庫
```

Android Gradle settings 透過以下路徑引用 Termux 模組：

```text
../termux-app/terminal-emulator
../termux-app/terminal-view
```

如果存在以下檔案，`app` 會載入 Hiddify Core：

```text
../hiddify-core/bin/hiddify-core.aar
```

缺少該 AAR 時應用仍可建置，但流量管控頁面會顯示 core binding 不可用。

## 建置

前置條件：

- 本地 Android 建置需要 JDK 17+；CI 使用 JDK 21。
- Android SDK 需要可用的 compile SDK 37。
- Firebase 建置需要 `app/google-services.json`。
- 本地 shell 模組需要相鄰的 `termux-app` 倉庫。
- 可選：`../hiddify-core/bin/hiddify-core.aar` 用於流量管控。
- `web-ui` 需要 Node 工具鏈；`web` 模組會呼叫 `npx --yes pnpm@10.24.0`。

常用命令：

```bash
./gradlew assembleDebug
./gradlew test
./gradlew lint
./gradlew :app:assembleRelease --no-daemon --console=plain
./gradlew :app:bundleRelease --no-daemon --console=plain
```

Web UI 命令：

```bash
cd web-ui
npx --yes pnpm@10.24.0 install --frozen-lockfile
npx --yes pnpm@10.24.0 run typecheck
npx --yes pnpm@10.24.0 run build
```

`web` 模組會在 Android prebuild 前自動建置並複製 Web UI。

## 發布

非 Apple 發布從 `main` 工作樹建置。目前發布流程產出：

- Windows desktop `.exe`
- Linux `.deb`
- Android `arm64-v8a` 和 `x86_64` 分架構 APK
- Android universal APK
- Android AAB
- desktop release manifests
- `SHA256SUMS.txt`

發布驗證應確認：

- `gh release view <tag> --repo EterUltimate/EterUee`
- `apksigner verify --verbose --print-certs` 驗證 APK 簽名
- `aapt dump badging` 驗證 APK 中繼資料
- release 或後續修復提交的 CI 與 Static Analysis 結果

詳見 [docs/RELEASE.md](docs/RELEASE.md)。

## 開發說明

- 優先遵循現有模組邊界；`app` 負責裝配/UI，複用邏輯放入功能模組。
- 不提交生成的建置輸出、模擬器截圖、本地 AAR 或複製出的執行時產物。
- 文件改動不要混入無關髒工作樹檔案。
- `dev` 和 `main` 是不同工作線；打 tag 或發布前必須確認目前工作樹。
- 架構級工作落地後同步更新 [docs/PROJECT_STATUS.md](docs/PROJECT_STATUS.md)。

## 文件

建議從以下文件開始：

- [docs/README.md](docs/README.md)：文件索引
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)：架構與執行時邊界
- [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)：本地設定、驗證和協作流程
- [docs/RELEASE.md](docs/RELEASE.md)：建置、發布和驗證清單
- [docs/PROJECT_STATUS.md](docs/PROJECT_STATUS.md)：目前進度與已知缺口

歷史實作報告保留在 `docs/`、`docs/bookmark/` 和 `docs/implementation/` 下。

## 致謝

感謝 [Rikkahub](https://github.com/rikkahub/rikkahub) 在 Android LLM 客戶端方向上的工作。

嵌入式 shell 整合使用來自 [termux/termux-app](https://github.com/termux/termux-app) 的本地模組。流量管控實驗使用從 [hiddify/hiddify-core](https://github.com/hiddify/hiddify-core) 建置的本地 binding。

## 授權

雙授權：

- [AGPL v3](LICENSE)：開源與非商業使用。
- 商業授權：商業用途請聯絡專案維護者。
