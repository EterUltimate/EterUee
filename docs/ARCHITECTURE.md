# 架构说明

本文档描述当前 EterUee 的实际架构边界。历史重构计划和阶段性报告仍保留在 `docs/implementation/` 与 `docs/AI_SDK_REFACTOR_*`，但判断当前实现状态时应以本文档、Gradle 配置和源码为准。

## 总览

EterUee 当前是一个 Android-first 的本地 AI 工作区：

```text
Compose Android app
  |
  +-- ChatService
  |     conversation persistence
  |     message transformers
  |     provider dispatch
  |     local tools and MCP tools
  |
  +-- AI SDK module
  |     provider abstraction
  |     UIMessage model
  |     streaming and tool-call payloads
  |
  +-- Runtime integrations
  |     embedded Ktor Web UI
  |     Termux terminal modules
  |     Hiddify Core binding
  |
  +-- Feature modules
        document, search, tts, highlight, roleplay, material3, common
```

The app layer owns navigation, settings, persistence wiring, dependency injection, and user-facing screens. Reusable domain behavior should stay in feature modules whenever the boundary is already available.

## Module Map

| Module | Current responsibility |
| --- | --- |
| `app` | Android application, Compose screens, Navigation 3 routes, settings, Room/DataStore wiring, ChatService, WebServerManager, local tools |
| `ai` | Provider types, model metadata, `UIMessage`, request construction, streaming chunks, OpenAI-compatible chat APIs |
| `common` | Shared utilities and extensions used across Android modules |
| `document` | File parsing for PDF, DOCX, PPTX and prompt conversion support |
| `highlight` | Syntax highlighting used by message rendering |
| `search` | Search provider integration |
| `tts` | Text-to-speech provider integration |
| `web` | Ktor server library and static resource host for the bundled Web UI |
| `web-ui` | React Router 7 browser UI built with pnpm and copied into `web` resources |
| `roleplay` | Character, chat, world info, group, preset, bookmark, import/export, and roleplay UI/domain/data layers |
| `material3` | Local Material color utilities |
| `terminal-emulator` | Termux terminal emulator module from `../termux-app` |
| `terminal-view` | Termux terminal view module from `../termux-app` |

## Data And Settings

The main app persists long-lived application data through Room-backed repositories and DataStore-backed settings. Important state groups include:

- Provider settings, built-in provider defaults, model configuration, and balance-check options.
- Assistant settings: prompt, model, memory, tools, headers, custom body, and conversation isolation.
- Conversation tree state: conversations contain message nodes; nodes hold alternative `UIMessage` values for regeneration and branching.
- File/document assets used by chat, roleplay, and local transforms.
- RolePlay state in the `roleplay` module, backed by its own Room database and file storage.

The built-in EterUee provider currently uses:

```text
https://sapi.eterultimate.asia/v1
```

The legacy provider URL is still kept in code for migration/backward compatibility.

## Chat Pipeline

The runtime path is:

```text
Compose/Web UI
  -> ChatService
  -> input transformers
  -> DynamicAISDK
  -> provider implementation
  -> streaming chunks
  -> output transformers
  -> persistence and UI state
```

Input/output transformers are used for:

- Pebble template expansion.
- Think-tag extraction and reasoning part conversion.
- Document attachment conversion.
- OCR.
- Base64 image to local file conversion.
- Regex output transformation.
- Final generation cleanup.

`UIMessage` is the cross-boundary message model. It supports text, image, document, reasoning, tool call, and tool result parts. Web-facing SSE work should preserve this structure instead of flattening messages into plain text.

## Provider Layer

Provider settings live under the DataStore model and are dispatched through the AI SDK abstraction. The app currently supports OpenAI-compatible providers, Google/Gemini, and other built-in provider presets.

The provider layer should keep these properties:

- Provider-specific request construction stays below the AI SDK boundary.
- User-facing provider configuration stays in app settings.
- Built-in providers may be enabled or disabled by default, but should remain editable.
- Provider migration logic belongs near settings/data-store migration, not in UI screens.

## Local Tools And Shell

Local tools are coordinated through `LocalTools`. The shell tool is now backed by the app-local shell runtime:

```text
ShellTools
  -> LocalShellRunner
  -> /system/bin/sh
```

The interactive shell page uses Termux terminal components:

```text
ShellPage
  -> EmbeddedTermuxTerminalClient
  -> TerminalSession
  -> terminal-view / terminal-emulator
```

The shell runner is app-scoped. It uses the app external files directory as the default working directory, app cache as `TMPDIR`, and `/system/bin:/system/xbin` as `PATH`. It does not require the standalone Termux app.

## Termux Integration

`settings.gradle.kts` includes:

```kotlin
include(":terminal-emulator")
project(":terminal-emulator").projectDir = file("../termux-app/terminal-emulator")
include(":terminal-view")
project(":terminal-view").projectDir = file("../termux-app/terminal-view")
```

This means local builds require a sibling checkout of `termux/termux-app` at:

```text
../termux-app
```

The current integration consumes the terminal emulator/view modules directly. It is not a full Termux package manager environment.

## Hiddify Core Integration

Traffic control is optional and loaded by `HiddifyCoreManager`. The app checks for a gomobile binding in:

```text
../hiddify-core/bin/hiddify-core.aar
```

When the AAR is present, the app attempts to discover one of the supported mobile binding class names through reflection and exposes test/start/stop/pause/wake operations. When missing, the app still builds and the settings page reports the missing binding.

Default runtime paths:

```text
files/hiddify/config.json
files/hiddify/working
cache/hiddify
```

The integration is intentionally defensive because the exported gomobile package/class names can differ across Hiddify Core builds.

## Embedded Web UI

The `web` module embeds Ktor server dependencies and serves static assets copied from `web-ui`.

Build path:

```text
web-ui
  -> pnpm run build
  -> web-ui/copy.ts
  -> web/src/main/resources/static
  -> Android app
  -> Ktor server
```

The Web UI is a React Router 7 app using React 19, TanStack Query, ky, streamdown, shiki, and related UI/runtime libraries. Web API work should stay compatible with the Ktor routes and SSE formats documented in [STREAM_V2_USAGE_GUIDE.md](./STREAM_V2_USAGE_GUIDE.md).

## RolePlay Module

RolePlay is a separate Android library module with its own data/domain/ui layers:

```text
roleplay/data
roleplay/domain
roleplay/ui
roleplay/di
```

It is wired into `RouteActivity` with screens for:

- RolePlay main page.
- Character list/edit.
- Chat.
- World info list/edit.
- Group list/edit.
- Preset edit.
- Bookmarks.

The module supports local Room persistence, file-backed assets, Tavern-compatible codecs, visual editors, message branching/regeneration, bookmark navigation, and AI response generation through the shared AI layer.

## Navigation

The app uses AndroidX Navigation 3 with a sealed `Screen` model. `RouteActivity` owns the route registry and maps screens to Compose pages. Feature modules should expose pages or composables, while app navigation remains the integration point.

## Dependency Injection

Koin is the app-wide DI container. `appModule` registers shared runtime singletons such as:

- `AISDK` / `DynamicAISDK`
- `LocalTools`
- `UpdateChecker`
- `HiddifyCoreManager`
- `TTSManager`
- `ChatService`
- `WebServerManager`
- Firebase services

RolePlay provides its own module under `roleplay/di`.

## Release Architecture

Release builds are maintained from the `main` worktree, not the active `dev` checkout. The current release line has CI/static-analysis workflow updates that may be ahead of `dev` until branches are reconciled.

Published release assets include Windows, Linux, Android APK/AAB, manifests, and checksums. See [RELEASE.md](./RELEASE.md).
