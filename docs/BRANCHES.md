# 分支与工作树说明

本文档描述当前 EterUee 的分支和本地 worktree 使用方式。旧文档中的 `feature/ai-sdk-refactor` 描述已经不再代表当前工作流。

## 当前分支

| 分支 | 本地路径 | 用途 |
| --- | --- | --- |
| `dev` | `C:\Users\zacza\Desktop\x\EterUee` | 当前功能开发、文档维护、集成验证 |
| `main` | `C:\Users\zacza\Desktop\x\EterUee-main-merge` | 发布、tag、GitHub Release、发布后 CI 修复 |
| `codex/pgsql-webui-agent-roleplay` | 独立分支，无固定当前工作树要求 | 历史/阶段性集成分支，使用前先确认是否仍需保留 |

辅助 worktree：

| 路径 | 状态 | 用途 |
| --- | --- | --- |
| 历史 detached pick worktree | detached | 本地历史 cherry-pick/比对用途；含未提交改动时不要清理 |
| `C:\Users\zacza\Desktop\x\EterUee-release-build` | detached | 历史 release build/比对用途 |

相邻外部仓库：

| 路径 | 用途 |
| --- | --- |
| `C:\Users\zacza\Desktop\x\termux-app` | 提供 `terminal-emulator` 和 `terminal-view` Gradle modules |
| `C:\Users\zacza\Desktop\x\hiddify-core` | 提供可选 `bin/hiddify-core.aar` |

## 操作前检查

执行构建、发布、提交或清理前先检查：

```bash
git worktree list --porcelain
git status --short --branch
git log --oneline -5
```

在指定 worktree 上检查：

```bash
git -C C:\Users\zacza\Desktop\x\EterUee status --short --branch
git -C C:\Users\zacza\Desktop\x\EterUee-main-merge status --short --branch
```

## `dev` 工作线

`dev` 是当前主要开发线。当前正在承载：

- Provider 默认值与更新源变更。
- Termux terminal modules 集成。
- Hiddify Core 可选流量管控集成。
- RolePlay 可视化编辑器与导航集成。
- Web UI / AI SDK / shell 工具相关迭代。
- README 与 docs 重写。

在 `dev` 上可以做：

- 功能开发。
- 文档更新。
- 本地构建和测试。
- 面向下一次合并到 `main` 的集成整理。

在 `dev` 上不要做：

- 直接创建正式 release tag。
- 把 `dev` 的未完成状态描述为已发布状态。
- 清理或提交无关未跟踪截图、窗口 XML、构建输出。

## `main` 发布线

`main` 是当前发布线。发布、GitHub Release 和发布后 CI 修复应在：

```text
C:\Users\zacza\Desktop\x\EterUee-main-merge
```

当前发布线已经完成：

- `v5.2.7` 非 Apple release。
- Windows EXE、Linux DEB、Android APK/AAB、manifest、checksum assets。
- 发布后 CI 与 Static Analysis 修复。

在 `main` 上可以做：

- release version bump。
- release tag。
- GitHub Release asset 上传和校验。
- 发布后 workflow/CI 修复。
- 将已验证的 `dev` 改动合并到发布线。

在 `main` 上不要做：

- 未经验证的大规模实验性开发。
- 删除发布产物或 release tag，除非任务明确要求。
- 误把 `dev` 的未跟踪文件带入 release commit。

## 分支同步

当 `main` 上出现发布后修复，需要评估是否同步回 `dev`：

```bash
git -C C:\Users\zacza\Desktop\x\EterUee-main-merge log --oneline -5
git -C C:\Users\zacza\Desktop\x\EterUee log --oneline -5
```

适合同步回 `dev` 的改动：

- CI workflow 修复。
- Static Analysis 修复。
- release 脚本修复。
- 构建前置条件修复。
- 与当前开发线兼容的文档更新。

同步方式可以是 merge 或 cherry-pick，具体取决于当前 `dev` 是否能接受 `main` 的完整历史。

## 发布前检查

发布前应在 `main` 工作树确认：

```bash
git status --short --branch
git fetch origin main
git rev-parse HEAD origin/main
gh run list --repo EterUltimate/EterUee --branch main --limit 10
```

如果本地和远端 SHA 不一致，先解决同步问题，不要直接打 tag。

## 文档与状态

维护状态文档时：

- 根 README 描述当前项目能力和构建入口。
- [PROJECT_STATUS.md](./PROJECT_STATUS.md) 描述当前进度与风险。
- [ARCHITECTURE.md](./ARCHITECTURE.md) 描述模块和运行时边界。
- [RELEASE.md](./RELEASE.md) 描述发布流程。

如果分支布局发生变化，优先更新本文档。
