# EterUee

<img src="docs/icon.svg" alt="EterUee icon" width="72" />

EterUee is a native Android LLM client built around local-first conversations, provider-agnostic AI access, roleplay workflows, an embedded browser UI, and app-local automation tools.

Languages: English | [简体中文](README_ZH_CN.md) | [繁體中文](README_ZH_TW.md)

## Current State

The active development line is `dev`. The published release line is `main`, with non-Apple release artifacts available from [GitHub Releases](https://github.com/EterUltimate/EterUee/releases).

Recent work has moved the project beyond a basic Android chat client:

- Update checks now use `https://github.com/EterUltimate/EterUee/releases`.
- The built-in EterUee provider defaults to `https://newapi.eterultimate.asia/v1` and is described as the official API.
- `termux/termux-app` terminal modules are integrated as the default local shell surface through local sibling checkout `../termux-app`.
- Managed proot Linux environments support Arch Linux by default and Ubuntu 24.04 as an optional rootfs module.
- `hiddify/hiddify-core` is integrated as an optional local traffic-control core through `../hiddify-core/bin/hiddify-core.aar`.
- The roleplay module is wired into the app navigation and continues to cover characters, chats, world info, groups, presets, bookmarks, and visual editors.
- The React Web UI is built into the Android `web` module and served by the embedded Ktor server.

## Product Scope

EterUee is intended to run as a full Android AI workspace:

- Chat with OpenAI-compatible providers, Gemini, and other configured providers.
- Manage assistants with isolated prompts, model settings, memory options, tools, headers, and request bodies.
- Keep tree-based conversations with message branching and regeneration.
- Attach documents and images, run OCR/document transforms, and preserve structured message parts.
- Use local tools, MCP tools, search providers, TTS providers, and shell execution when enabled.
- Run roleplay workflows with local Room data and file-backed assets.
- Expose conversations through a local browser UI hosted from the device.
- Use an embedded Termux terminal view and an app-scoped shell runner without requiring the standalone Termux app.
- Install and run app-private Arch Linux or Ubuntu 24.04 rootfs environments through proot for approved shell tools.
- Optionally load Hiddify Core for local traffic-control experiments.

## Architecture

```text
Android app (app)
  Compose UI, Navigation 3, ViewModels, Room, DataStore, WorkManager, Firebase, Koin
    |
    +-- ChatService and transformers
    |     Template, thinking-tag, document, OCR, image, regex, and output transforms
    |
    +-- AI SDK (ai)
    |     Provider abstraction, UIMessage model, streaming, tool calls, OpenAI-compatible APIs
    |
    +-- Feature modules
    |     common, document, highlight, search, tts, roleplay, material3
    |
    +-- Local runtime integrations
    |     terminal-emulator, terminal-view, LocalShellRunner, LinuxEnvironmentManager, HiddifyCoreManager
    |
    +-- Embedded web server (web)
          Ktor API, SSE, static React Web UI resources from web-ui
```

Core module responsibilities:

| Path | Responsibility |
| --- | --- |
| `app` | Android application, Compose UI, navigation, persistence wiring, settings, local tools, web routes |
| `ai` | Provider abstraction, `UIMessage`, streaming generation, OpenAI-compatible request/response handling |
| `common` | Shared utilities and Kotlin extensions |
| `document` | PDF, DOCX, PPTX parsing and document-to-prompt support |
| `highlight` | Code syntax highlighting |
| `search` | Search provider SDK integration |
| `tts` | Text-to-speech provider integration |
| `roleplay` | Character, chat, world info, group, preset, bookmark, and Tavern-compatible workflows |
| `web` | Android library that embeds Ktor and serves the bundled React Web UI |
| `web-ui` | React Router 7 browser frontend copied into `web/src/main/resources/static` |
| `terminal-emulator`, `terminal-view` | Local Termux modules from `../termux-app` |
| `app/src/main/java/com/eterultimate/eteruee/linux` | Managed proot Linux runtime for Arch Linux and optional Ubuntu 24.04 |
| `../hiddify-core/bin/hiddify-core.aar` | Optional local Hiddify Core gomobile binding consumed by `app` |

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the detailed architecture map.

## Repository Layout

This repo is commonly used with several sibling checkouts:

```text
C:\Users\zacza\Desktop\x\EterUee              dev worktree
C:\Users\zacza\Desktop\x\EterUee-main-merge   main/release worktree
C:\Users\zacza\Desktop\x\termux-app           Termux app checkout
C:\Users\zacza\Desktop\x\hiddify-core         Hiddify Core checkout
```

The Android Gradle settings reference Termux modules with:

```text
../termux-app/terminal-emulator
../termux-app/terminal-view
```

The app consumes Hiddify Core when this file exists:

```text
../hiddify-core/bin/hiddify-core.aar
```

If the AAR is missing, the app still builds, but the traffic-control page reports that the core binding is unavailable.

## Build

Requirements:

- JDK 26 for local Android builds and CI.
- Android SDK with compile SDK 37 available.
- `app/google-services.json` for Firebase-backed builds.
- The sibling `termux-app` checkout for the local terminal modules.
- Optional: `../hiddify-core/bin/hiddify-core.aar` for traffic control.
- Node tooling for `web-ui`; Gradle invokes `npx --yes pnpm@10.24.0` from the `web` module.

Common commands:

```bash
./gradlew assembleDebug
./gradlew test
./gradlew lint
./gradlew :app:assembleRelease --no-daemon --console=plain
./gradlew :app:bundleRelease --no-daemon --console=plain
```

Web UI commands:

```bash
cd web-ui
npx --yes pnpm@10.24.0 install --frozen-lockfile
npx --yes pnpm@10.24.0 run typecheck
npx --yes pnpm@10.24.0 run build
```

The `web` module automatically builds and copies the Web UI before Android prebuild.

## Release

Published non-Apple releases are built from the `main` worktree. The current release process produces:

- Windows desktop `.exe`
- Linux `.deb`
- Android split APKs for `arm64-v8a` and `x86_64`
- Android universal APK
- Android AAB
- Desktop release manifests
- `SHA256SUMS.txt`

Release verification should confirm:

- `gh release view <tag> --repo EterUltimate/EterUee`
- APK signatures with `apksigner verify --verbose --print-certs`
- APK metadata with `aapt dump badging`
- CI and Static Analysis runs for the release/follow-up commit

See [docs/RELEASE.md](docs/RELEASE.md) for the release checklist.

## Development Notes

- Prefer the current module boundaries; keep `app` as orchestration/UI and put reusable provider/runtime logic in feature modules.
- Do not commit generated build output, emulator screenshots, local AARs, or copied runtime artifacts.
- Keep unrelated dirty worktree files out of documentation-only changes.
- Treat `dev` and `main` as separate working lines; verify the active worktree before tagging or publishing.
- Update [docs/PROJECT_STATUS.md](docs/PROJECT_STATUS.md) when architecture-level work lands.

## Documentation

Start with:

- [docs/README.md](docs/README.md): documentation index
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md): architecture and runtime boundaries
- [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md): local setup, validation, and contribution workflow
- [docs/LINUX_RUNTIME.md](docs/LINUX_RUNTIME.md): proot Arch/Ubuntu runtime, Web API, tool API, and plugin capability notes
- [docs/RELEASE.md](docs/RELEASE.md): build, publish, and verification checklist
- [docs/PROJECT_STATUS.md](docs/PROJECT_STATUS.md): current progress and known gaps

Historical implementation reports remain under `docs/`, `docs/bookmark/`, and `docs/implementation/`.

## Acknowledgements

Thanks to [Rikkahub](https://github.com/rikkahub/rikkahub) for prior work in the Android LLM client space.

The embedded shell integration uses local modules from [termux/termux-app](https://github.com/termux/termux-app). Traffic-control experiments use a local binding built from [hiddify/hiddify-core](https://github.com/hiddify/hiddify-core).

## License

Dual license:

- [AGPL v3](LICENSE) for open-source and non-commercial use.
- Commercial license for commercial use.
