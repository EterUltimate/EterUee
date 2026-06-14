# EterUee 文档索引

本目录记录 EterUee 当前架构、开发流程、发布流程和历史专题文档。根目录 README 面向用户和贡献者快速了解项目；`docs/` 面向维护者，用于说明模块边界、构建前置条件、验证路径和当前进度。

## 当前维护文档

| 文档 | 内容 |
| --- | --- |
| [ARCHITECTURE.md](./ARCHITECTURE.md) | 当前 Android、AI SDK、Web UI、RolePlay、Termux shell、Hiddify Core 的架构边界 |
| [DEVELOPMENT.md](./DEVELOPMENT.md) | 本地环境、相邻仓库、构建命令、测试命令、工作树注意事项 |
| [LINUX_RUNTIME.md](./LINUX_RUNTIME.md) | proot Arch/Ubuntu rootfs、Web API、工具 API、插件能力和运行时路径 |
| [RELEASE.md](./RELEASE.md) | 非 Apple release 构建、发布、资产验证、CI 验证流程 |
| [PROJECT_STATUS.md](./PROJECT_STATUS.md) | 当前进度、已落地功能、进行中集成、已知缺口 |
| [BRANCHES.md](./BRANCHES.md) | 当前 `dev` / `main` / 辅助 worktree 的用途和操作边界 |

## 主要专题文档

| 文档 | 内容 |
| --- | --- |
| [AI_SDK_USAGE.md](./AI_SDK_USAGE.md) | AI SDK 使用方式、Android 调用示例、迁移参考 |
| [STREAM_V2_USAGE_GUIDE.md](./STREAM_V2_USAGE_GUIDE.md) | `/api/conversations/stream-v2` 的 SSE 事件格式和使用示例 |
| [BACKEND_SSE_STANDARDIZATION.md](./BACKEND_SSE_STANDARDIZATION.md) | Web 后端 SSE 标准化记录 |
| [NANO_BANANA_AND_VIDEO_GEN_COMPATIBILITY.md](./NANO_BANANA_AND_VIDEO_GEN_COMPATIBILITY.md) | 图像/视频生成兼容性记录 |
| [CHATSERVICE_FLOW_MAP_FIX.md](./CHATSERVICE_FLOW_MAP_FIX.md) | ChatService 流程图修复记录 |
| [UNIT_TEST_REPORT.md](./UNIT_TEST_REPORT.md) | 历史单元测试报告 |
| [ENHANCED_UNIT_TEST_REPORT.md](./ENHANCED_UNIT_TEST_REPORT.md) | 增强单元测试报告 |
| [APK_RELEASE_NOTES_5.2.1.md](./APK_RELEASE_NOTES_5.2.1.md) | v5.2.1 Android APK 发布说明 |

## RolePlay 与书签文档

| 文档 | 内容 |
| --- | --- |
| [../roleplay/README.md](../roleplay/README.md) | RolePlay 模块入口说明 |
| [../roleplay/ARCHITECTURE.md](../roleplay/ARCHITECTURE.md) | RolePlay 模块架构 |
| [../roleplay/QUICK_START.md](../roleplay/QUICK_START.md) | RolePlay 快速开始 |
| [bookmark/BOOKMARK_INTEGRATION_COMPLETE.md](./bookmark/BOOKMARK_INTEGRATION_COMPLETE.md) | 书签系统集成总结 |
| [bookmark/BOOKMARK_FEATURE_DEMO.md](./bookmark/BOOKMARK_FEATURE_DEMO.md) | 书签功能演示 |
| [bookmark/BOOKMARK_TESTING_GUIDE.md](./bookmark/BOOKMARK_TESTING_GUIDE.md) | 书签测试指南 |

## 模块入口文档

| 文档 | 内容 |
| --- | --- |
| [../ai/README.md](../ai/README.md) | AI SDK、Provider、`UIMessage` 和工具边界 |
| [../web-ui/README.md](../web-ui/README.md) | React Web UI 构建、嵌入和 Ktor 服务边界 |
| [../roleplay/README.md](../roleplay/README.md) | RolePlay 模块能力、持久化和验证入口 |
| [../locale-tui/README.md](../locale-tui/README.md) | 本地化工具说明 |

## 历史实现报告

`docs/implementation/` 和 `docs/AI_SDK_REFACTOR_*` 下的文档记录了阶段性方案、实现摘要和旧分支计划。这些文件保留作历史资料；判断当前实现状态时优先阅读：

1. [PROJECT_STATUS.md](./PROJECT_STATUS.md)
2. [ARCHITECTURE.md](./ARCHITECTURE.md)
3. 当前代码与 Gradle 配置

## 图片与资源

| 路径 | 内容 |
| --- | --- |
| [img/](./img/) | 应用截图和界面资源 |
| [icon.svg](./icon.svg) | 应用图标 SVG |
| [icon.png](./icon.png) | 应用图标 PNG |
| [donate.png](./donate.png) | 捐赠二维码 |

## 根目录文档

| 文档 | 内容 |
| --- | --- |
| [../README.md](../README.md) | English README |
| [../README_ZH_CN.md](../README_ZH_CN.md) | 简体中文 README |
| [../README_ZH_TW.md](../README_ZH_TW.md) | 繁体中文 README |
| [../AGENTS.md](../AGENTS.md) | Agent 开发指南和仓库约束 |
| [../CLAUDE.md](../CLAUDE.md) | Claude Code 相关配置 |

## 文档维护规则

- README 只写当前稳定事实和关键入口，不承载长篇阶段性报告。
- 架构级改动同步更新 [ARCHITECTURE.md](./ARCHITECTURE.md) 和 [PROJECT_STATUS.md](./PROJECT_STATUS.md)。
- 构建、CI、发布命令变化同步更新 [DEVELOPMENT.md](./DEVELOPMENT.md) 和 [RELEASE.md](./RELEASE.md)。
- 旧报告不要直接删除，除非确认其中信息已经迁移且不再需要追溯。
- 不把生成的构建输出、模拟器截图、本地 AAR、临时日志写入 docs 目录作为正式文档。
