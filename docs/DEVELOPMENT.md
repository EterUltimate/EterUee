# 开发指南

本文档面向维护者，记录当前 EterUee 的本地开发环境、常用命令、工作树边界和验证路径。

## 工作树

当前常见布局：

```text
C:\Users\zacza\Desktop\x\EterUee              dev 工作树
C:\Users\zacza\Desktop\x\EterUee-main-merge   main/release 工作树
C:\Users\zacza\Desktop\x\termux-app           Termux app 本地仓库
C:\Users\zacza\Desktop\x\hiddify-core         Hiddify Core 本地仓库
```

使用命令前先确认工作树：

```bash
git worktree list --porcelain
git status --short --branch
```

约定：

- `dev` 用于当前功能开发和文档维护。
- `main` 用于发布、tag、GitHub Release、发布后 CI 修复。
- 不要把 `dev` 上的未完成实现直接当作已发布状态。
- 不要清理或提交无关未跟踪文件，除非任务明确要求。

## 必要依赖

本地 Android 构建需要：

- JDK 26，本地构建和 CI 保持一致。
- Android SDK，compile SDK 37。
- Gradle wrapper。
- `app/google-services.json`，用于 Firebase Analytics/Crashlytics/Remote Config。
- `../termux-app`，用于 `terminal-emulator` 和 `terminal-view` 模块。

可选依赖：

- `../hiddify-core/bin/hiddify-core.aar`，用于流量管控页面加载 Hiddify Core binding。
- Android SDK build-tools 中的 `apksigner` 和 `aapt`，用于发布 APK 验证。
- GitHub CLI `gh`，用于 release 和 workflow 验证。

## Termux 本地模块

`settings.gradle.kts` 直接引用：

```text
../termux-app/terminal-emulator
../termux-app/terminal-view
```

如果缺少 `../termux-app`，Gradle 配置阶段会失败。CI 或新机器上需要先准备相邻仓库。

当前集成目标是嵌入终端视图和终端会话，不是完整复制 Termux 应用的包管理能力。

## Hiddify Core

`app/build.gradle.kts` 会在存在以下文件时引入本地 AAR：

```text
../hiddify-core/bin/hiddify-core.aar
```

缺失时：

- 构建仍可继续。
- `HiddifyCoreManager` 会报告 binding 不可用。
- 设置页的流量管控功能只能显示缺失状态。

如果要验证流量管控，需要先从 `C:\Users\zacza\Desktop\x\hiddify-core` 构建或复制 gomobile AAR 到 `bin/hiddify-core.aar`。

## Android 构建

常用命令：

```bash
./gradlew assembleDebug
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease --no-daemon --console=plain
./gradlew :app:bundleRelease --no-daemon --console=plain
```

本地 release signing 读取 `local.properties`：

```properties
storeFile=...
storePassword=...
keyAlias=...
keyPassword=...
```

没有完整签名配置时，release 任务可能无法产出可分发的签名包。

## Web UI

`web-ui` 是 React Router 7 前端。`web` 模块的 `preBuild` 会自动执行 Web UI 构建并复制静态资源。

手动验证：

```bash
cd web-ui
npx --yes pnpm@10.24.0 install --frozen-lockfile
npx --yes pnpm@10.24.0 run typecheck
npx --yes pnpm@10.24.0 run build
```

仓库中也可能出现更新的 pnpm 版本用于特定分支验证。使用前先看对应分支的 `web/build.gradle.kts` 和 CI workflow。

## 测试与检查

基础检查：

```bash
./gradlew test
./gradlew lint
git diff --check
```

Android debug 验证：

```bash
./gradlew :app:assembleDebug --no-daemon --console=plain
```

RolePlay 定向验证：

```bash
./gradlew :roleplay:testDebugUnitTest
./gradlew :roleplay:assembleDebug
```

Web UI 定向验证：

```bash
cd web-ui
npx --yes pnpm@10.24.0 run typecheck
npx --yes pnpm@10.24.0 run build
```

Workflow lint 可用：

```bash
go run github.com/rhysd/actionlint/cmd/actionlint@v1.7.7 -color
```

## 本地 Shell

Shell 功能由两层组成：

- `ShellPage` 使用 Termux terminal view/emulator 提供交互式终端。
- `ShellTools` 使用 `LocalShellRunner` 执行工具调用中的本地命令。

`LocalShellRunner` 默认使用：

```text
shell: /system/bin/sh
PATH: /system/bin:/system/xbin
working dir: app external files directory
```

Shell 工具需要用户批准后执行，适合应用作用域文件操作和轻量本地自动化。

## Provider 与更新源

当前内置 EterUee provider：

```text
https://sapi.eterultimate.asia/v1
```

更新检查：

```text
https://api.github.com/repos/EterUltimate/EterUee/releases/latest
https://github.com/EterUltimate/EterUee/releases
```

修改 provider 默认值时，需要同时检查：

- `DefaultProviders.kt`
- `PreferencesStore.kt` 中的迁移逻辑
- 本地化字符串中的描述
- README 和项目状态文档

## 文档维护

文档改动至少运行：

```bash
git diff --check -- README.md README_ZH_CN.md README_ZH_TW.md docs
```

文档中出现的链接应优先使用相对路径。新增长期维护文档时，把入口加入 [docs/README.md](./README.md)。

## 提交边界

提交前检查：

```bash
git status --short
git diff --stat
git diff --check
```

常见不要混入的文件：

- `build/`
- `roleplay/build/`
- emulator 截图和窗口 XML
- 本地 AAR/JAR
- 临时 release note 草稿
- IDE 本地状态
