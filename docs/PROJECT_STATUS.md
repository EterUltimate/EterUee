# 项目状态

本文档记录当前 EterUee 的实际进度。它不是路线图承诺，而是维护者用于判断“哪些功能已经落地、哪些仍在集成中、发布线是否健康”的状态表。

## 当前分支与发布

| 项目 | 状态 |
| --- | --- |
| 开发线 | `dev`，工作树位于 `C:\Users\zacza\Desktop\x\EterUee` |
| 发布线 | `main`，工作树位于 `C:\Users\zacza\Desktop\x\EterUee-main-merge` |
| 最新非 Apple 发布 | `v5.2.18`，发布于 GitHub Releases |
| 发布资产 | Windows EXE、Linux DEB、Android split APK、universal APK、AAB、desktop manifests、`SHA256SUMS.txt` |
| CI 状态 | `main` 最新发布后修复提交已通过 CI 与 Static Analysis |

## 已落地能力

### Android 主应用

- Kotlin + Jetpack Compose 主界面。
- AndroidX Navigation 3 路由。
- Koin 依赖注入。
- Room/DataStore 持久化。
- Firebase Analytics、Crashlytics、Remote Config 接入。
- Assistant、Conversation、MessageNode、UIMessage 为核心数据模型。
- 树状对话与消息分支。
- 主题、显示、Provider、模型、搜索、TTS、MCP、Web、文件和关于页面。
- 本地工具开关和工具调用审批。

### AI SDK 与 Provider

- `ai` 模块提供 provider 抽象、模型元信息、`UIMessage` 和流式生成结构。
- 支持 OpenAI-compatible Provider、Gemini 以及多个内置 Provider preset。
- 默认 EterUee Provider 使用 `https://sapi.eterultimate.asia/v1`。
- EterUee Provider 描述定位为官方提供的 API。
- 保留旧 EterUee Provider URL 用于迁移兼容。

### 更新检查

- 应用内更新检查来源已改为 GitHub Releases。
- 最新版本 API 使用 `https://api.github.com/repos/EterUltimate/EterUee/releases/latest`。
- 下载入口回退到 `https://github.com/EterUltimate/EterUee/releases`。

### 本地 Shell

- 已接入 `termux/termux-app` 的 `terminal-emulator` 与 `terminal-view` 模块。
- `ShellPage` 提供嵌入式终端视图。
- `LocalShellRunner` 使用 `/system/bin/sh` 执行 app-scoped 命令。
- `ShellTools` 通过 `shell_execute` 工具暴露本地 shell 执行能力，并要求用户批准。
- `LinuxEnvironmentManager` 提供共享 Termux `proot` runtime 和应用私有 rootfs。
- 默认支持 Arch Linux，新增 Ubuntu 24.04 可选 rootfs 模块。
- Web UI Device Agent、Ktor Web API、本地工具和插件能力均支持 `distribution=arch|ubuntu`。
- 当前目标是嵌入式终端、app-scoped shell 和 proot userland，不是完整 Termux 包管理环境。

### Hiddify Core / 流量管控

- `HiddifyCoreManager` 已加入 app DI。
- 设置页有流量管控入口。
- app 可选加载 `../hiddify-core/bin/hiddify-core.aar`。
- manager 通过反射兼容多种 gomobile binding class 名称。
- 支持 test/start/stop/pause/wake 的基础调用路径。
- 缺少 AAR 时 app 仍可构建，UI 显示 core binding 不可用。

### Web UI

- `web-ui` 使用 React Router 7、React 19、TanStack Query、ky、streamdown、shiki 等库。
- `web` 模块在 preBuild 阶段用 pnpm 构建 Web UI 并复制到 `web/src/main/resources/static`。
- Android app 内置 Ktor server 托管静态 Web UI 和 API/SSE 路由。
- `/api/conversations/stream-v2` 的 SSE 格式有独立使用文档。

### RolePlay

- `roleplay` 是独立 Android library module。
- 已接入 app 导航。
- 覆盖角色、聊天、世界书、群组、预设、书签和视觉编辑器。
- 使用 Room、file storage、Koin、StateFlow、Compose。
- 支持 Tavern 兼容数据编解码、消息分支、重新生成和书签跳转。

### Release

- `main` 发布线已成功发布 `v5.2.18` 非 Apple release。
- release 包含 Windows、Linux、Android APK/AAB 和校验文件。
- 发布后修复了 Static Analysis 中 OSSF Scorecard 发布结果签名失败的问题，保留 SARIF 上传。

## 进行中或需要继续收敛

| 区域 | 状态 | 下一步 |
| --- | --- | --- |
| `dev` 与 `main` 工作流差异 | `main` 的 CI/static-analysis/release workflow 更新可能领先 `dev` | 合并或 cherry-pick 发布线 workflow 修复，避免分支长期漂移 |
| Hiddify Core | app 端为可选 binding，依赖本地 AAR | 固化 AAR 构建脚本或 CI 下载/校验策略 |
| Termux/proot 集成 | 已集成 terminal view/emulator，并支持 Arch/Ubuntu rootfs | 明确是否需要扩展到包管理、PATH 扩展、用户态文件系统 |
| Web UI API | 有 stream-v2 文档和 Ktor 服务 | 保持 Web UI typed client 与 Ktor SSE/event 结构同步 |
| RolePlay | 功能面较广，文档中仍有历史分支信息 | 清理 RolePlay 文档里的旧仓库 URL、旧分支名和过期状态 |
| 文档体系 | 根 README 和 docs 入口已更新 | 后续架构变更同步维护 PROJECT_STATUS 与 ARCHITECTURE |

## 已知风险

- `settings.gradle.kts` 直接引用 `../termux-app`，新环境缺少该相邻仓库时构建会失败。
- `hiddify-core.aar` 是可选本地产物，缺失时流量管控不可用。
- `dev` 工作树当前包含多项未提交功能改动和测试截图/窗口 XML，文档提交时需要严格控制暂存范围。
- release 产物以 `main` 工作树和 GitHub Actions 为准，不能只看 `dev` 分支状态判断发布健康。
- 历史文档中仍有 `feature/roleplay`、`your-repo` 等旧描述，引用前应核对当前代码。

## 状态更新规则

- 新功能接入导航、DI、构建或发布流程时，更新本文档。
- Provider 默认值、更新源、发布资产类型变化时，更新根 README、[ARCHITECTURE.md](./ARCHITECTURE.md) 和 [RELEASE.md](./RELEASE.md)。
- 旧专题报告可以保留，但当前事实必须落到本文件或架构文档中。
