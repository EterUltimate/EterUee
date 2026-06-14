# EterUee

<img src="docs/icon.svg" alt="EterUee 图标" width="72" />

EterUee 是一个原生 Android LLM 客户端，围绕本地优先的对话、多供应商 AI 接入、角色扮演工作流、内置浏览器 UI 和应用内本地自动化工具构建。

语言：[English](README.md) | 简体中文 | [繁體中文](README_ZH_TW.md)

## 当前状态

当前开发线是 `dev`。发布线是 `main`，除 Apple 平台外的发布产物可从 [GitHub Releases](https://github.com/EterUltimate/EterUee/releases) 获取。

近期项目已经从基础 Android 聊天客户端推进到更完整的本地 AI 工作区：

- 更新检查来源改为 `https://github.com/EterUltimate/EterUee/releases`。
- 内置 EterUee 提供商默认地址为 `https://newapi.eterultimate.asia/v1`，描述为官方提供的 API。
- 通过本地相邻仓库 `../termux-app` 集成 `termux/termux-app` 的 terminal 模块，作为默认本地 shell 界面。
- 托管 proot Linux 环境默认支持 Arch Linux，并提供 Ubuntu 24.04 可选 rootfs 模块。
- 通过 `../hiddify-core/bin/hiddify-core.aar` 可选集成 `hiddify/hiddify-core`，用于本地流量管控实验。
- RolePlay 模块已接入应用导航，覆盖角色、聊天、世界书、群组、预设、书签和可视化编辑器。
- React Web UI 会构建进 Android `web` 模块，并由内置 Ktor 服务提供访问。

## 产品范围

EterUee 的目标是作为完整的 Android AI 工作区运行：

- 接入 OpenAI 兼容接口、Gemini 以及其他自定义供应商。
- 为 Assistant 独立保存提示词、模型参数、记忆选项、工具、自定义请求头和请求体。
- 使用树状对话结构，支持消息分支和重新生成。
- 支持文档、图片、OCR、文档转提示词和结构化消息片段。
- 在启用后使用本地工具、MCP 工具、搜索供应商、TTS 供应商和 shell 执行。
- 使用本地 Room 数据和文件资产运行角色扮演工作流。
- 通过设备内置 Web 服务向局域网浏览器暴露会话界面。
- 使用嵌入式 Termux 终端视图和应用作用域 shell runner，不要求安装独立 Termux 应用。
- 通过 proot 安装和运行应用私有的 Arch Linux 或 Ubuntu 24.04 rootfs，用于已批准的 shell 工具。
- 可选加载 Hiddify Core 进行本地流量管控实验。

## 架构

```text
Android app (app)
  Compose UI, Navigation 3, ViewModel, Room, DataStore, WorkManager, Firebase, Koin
    |
    +-- ChatService 与消息转换器
    |     模板、think 标签、文档、OCR、图片、正则和输出转换
    |
    +-- AI SDK (ai)
    |     Provider 抽象、UIMessage 模型、流式生成、工具调用、OpenAI 兼容 API
    |
    +-- 功能模块
    |     common, document, highlight, search, tts, roleplay, material3
    |
    +-- 本地运行时集成
    |     terminal-emulator, terminal-view, LocalShellRunner, LinuxEnvironmentManager, HiddifyCoreManager
    |
    +-- 内置 Web 服务 (web)
          Ktor API, SSE, 来自 web-ui 的 React 静态资源
```

核心模块职责：

| 路径 | 职责 |
| --- | --- |
| `app` | Android 应用、Compose UI、导航、持久化装配、设置、本地工具、Web 路由 |
| `ai` | Provider 抽象、`UIMessage`、流式生成、OpenAI 兼容请求/响应处理 |
| `common` | 共享工具与 Kotlin 扩展 |
| `document` | PDF、DOCX、PPTX 解析和文档转提示词 |
| `highlight` | 代码语法高亮 |
| `search` | 搜索供应商 SDK 集成 |
| `tts` | 文本转语音供应商集成 |
| `roleplay` | 角色、聊天、世界书、群组、预设、书签和 Tavern 兼容工作流 |
| `web` | 嵌入 Ktor 并托管 React Web UI 的 Android library |
| `web-ui` | React Router 7 浏览器前端，构建产物复制到 `web/src/main/resources/static` |
| `terminal-emulator`, `terminal-view` | 来自 `../termux-app` 的本地 Termux 模块 |
| `app/src/main/java/com/eterultimate/eteruee/linux` | 托管 proot Linux runtime，支持 Arch Linux 和可选 Ubuntu 24.04 |
| `../hiddify-core/bin/hiddify-core.aar` | `app` 可选加载的 Hiddify Core gomobile 绑定 |

详见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)。

## 仓库布局

本项目通常配合多个相邻工作树/仓库使用：

```text
C:\Users\zacza\Desktop\x\EterUee              dev 工作树
C:\Users\zacza\Desktop\x\EterUee-main-merge   main/release 工作树
C:\Users\zacza\Desktop\x\termux-app           Termux app 仓库
C:\Users\zacza\Desktop\x\hiddify-core         Hiddify Core 仓库
```

Android Gradle settings 通过以下路径引用 Termux 模块：

```text
../termux-app/terminal-emulator
../termux-app/terminal-view
```

如果存在以下文件，`app` 会加载 Hiddify Core：

```text
../hiddify-core/bin/hiddify-core.aar
```

缺少该 AAR 时应用仍可构建，但流量管控页面会显示 core binding 不可用。

## 构建

前置条件：

- 本地 Android 构建和 CI 使用 JDK 26。
- Android SDK 需要可用的 compile SDK 37。
- Firebase 构建需要 `app/google-services.json`。
- 本地 shell 模块需要相邻的 `termux-app` 仓库。
- 可选：`../hiddify-core/bin/hiddify-core.aar` 用于流量管控。
- `web-ui` 需要 Node 工具链；`web` 模块会调用 `npx --yes pnpm@10.24.0`。

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

`web` 模块会在 Android prebuild 前自动构建并复制 Web UI。

## 发布

非 Apple 发布从 `main` 工作树构建。当前发布流程产出：

- Windows desktop `.exe`
- Linux `.deb`
- Android `arm64-v8a` 和 `x86_64` 分架构 APK
- Android universal APK
- Android AAB
- desktop release manifests
- `SHA256SUMS.txt`

发布验证应确认：

- `gh release view <tag> --repo EterUltimate/EterUee`
- `apksigner verify --verbose --print-certs` 验证 APK 签名
- `aapt dump badging` 验证 APK 元信息
- release 或后续修复提交的 CI 与 Static Analysis 结果

详见 [docs/RELEASE.md](docs/RELEASE.md)。

## 开发说明

- 优先遵循现有模块边界；`app` 负责装配/UI，复用逻辑放入功能模块。
- 不提交生成的构建输出、模拟器截图、本地 AAR 或复制出的运行时产物。
- 文档改动不要混入无关脏工作树文件。
- `dev` 和 `main` 是不同工作线；打 tag 或发布前必须确认当前工作树。
- 架构级工作落地后同步更新 [docs/PROJECT_STATUS.md](docs/PROJECT_STATUS.md)。

## 文档

建议从以下文档开始：

- [docs/README.md](docs/README.md)：文档索引
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)：架构与运行时边界
- [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)：本地设置、验证和协作流程
- [docs/LINUX_RUNTIME.md](docs/LINUX_RUNTIME.md)：proot Arch/Ubuntu runtime、Web API、工具 API 和插件能力说明
- [docs/RELEASE.md](docs/RELEASE.md)：构建、发布和验证清单
- [docs/PROJECT_STATUS.md](docs/PROJECT_STATUS.md)：当前进度与已知缺口

历史实现报告保留在 `docs/`、`docs/bookmark/` 和 `docs/implementation/` 下。

## 致谢

感谢 [Rikkahub](https://github.com/rikkahub/rikkahub) 在 Android LLM 客户端方向上的工作。

嵌入式 shell 集成使用来自 [termux/termux-app](https://github.com/termux/termux-app) 的本地模块。流量管控实验使用从 [hiddify/hiddify-core](https://github.com/hiddify/hiddify-core) 构建的本地绑定。

## 许可证

双许可证：

- [AGPL v3](LICENSE)：开源与非商业使用。
- 商业许可：商业用途请联系项目维护者。
