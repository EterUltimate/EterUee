# 文档索引

本目录包含 EterUee 项目的技术文档和参考资料。

## 📚 文档列表

### 📌 书签系统 (Bookmark)

- **[BOOKMARK_INTEGRATION_COMPLETE.md](./bookmark/BOOKMARK_INTEGRATION_COMPLETE.md)** - 书签系统集成完整实现总结
  - 路由配置集成
  - 消息跳转与滚动定位
  - UI增强功能（高亮、Badge、搜索、批量操作）
  - 技术亮点和代码统计

- **[BOOKMARK_FEATURE_DEMO.md](./bookmark/BOOKMARK_FEATURE_DEMO.md)** - 书签系统功能演示指南
  - 功能演示流程
  - UI组件说明
  - 开发者调试技巧
  - 真机测试清单
  - 常见问题排查

- **[BOOKMARK_TESTING_GUIDE.md](./bookmark/BOOKMARK_TESTING_GUIDE.md)** - 书签系统测试指南
  - 7个主要测试用例
  - 常见问题排查
  - 验收标准
  - 测试结果记录表

### 🛠️ 实现总结 (Implementation)

- **[IMPLEMENTATION_SUMMARY_MODULE1.md](./implementation/IMPLEMENTATION_SUMMARY_MODULE1.md)** - 模块1实现总结
- **[IMPLEMENTATION_SUMMARY_MODULE3_4.md](./implementation/IMPLEMENTATION_SUMMARY_MODULE3_4.md)** - 模块3&4实现总结
- **[IMPLEMENTATION_SUMMARY_MODULE4_UI.md](./implementation/IMPLEMENTATION_SUMMARY_MODULE4_UI.md)** - 模块4 UI集成总结
- **[IMPLEMENTATION_SUMMARY_MODULE4_COMPLETE.md](./implementation/IMPLEMENTATION_SUMMARY_MODULE4_COMPLETE.md)** - 模块4完整功能总结

### 🌿 分支管理

- **[BRANCHES.md](./BRANCHES.md)** - 项目分支策略说明
  - AI SDK重构双分支策略
  - 开发分支管理规范

### 🧪 测试报告

- **[UNIT_TEST_REPORT.md](./UNIT_TEST_REPORT.md)** - 单元测试完整报告
  - 104个测试用例，100%通过率
  - 6个模块测试覆盖详情
  - AI SDK、TTS、UI组件等重点测试
  - 编译警告汇总和改进建议

- **[ENHANCED_UNIT_TEST_REPORT.md](./ENHANCED_UNIT_TEST_REPORT.md)** - 增强单元测试报告
  - 新增33个测试用例（App、Common、Document模块）
  - Assistant和Conversation业务逻辑测试
  - LruCache工具类全面测试
  - DocumentParser解析器测试
  - 6处编译警告修复记录

### AI SDK 相关

- **[AI_SDK_USAGE.md](./AI_SDK_USAGE.md)** - AI SDK 完整使用指南
  - Android 端集成示例
  - Web 端集成示例
  - 迁移指南
  - 常见问题解答

- **[AI_SDK_TEST_REPORT.md](./AI_SDK_TEST_REPORT.md)** - AI SDK 静态测试报告
  - 编译测试结果
  - 代码质量分析
  - 修复的问题记录
  - 下一步建议

### 其他资源

- **img/** - 项目截图和图片资源
  - [assistants.png](./img/assistants.png) - 助手界面
  - [chat.png](./img/chat.png) - 聊天界面
  - [desktop.png](./img/desktop.png) - 桌面端界面
  - [models.png](./img/models.png) - 模型选择
  - [providers.png](./img/providers.png) - Provider 配置

- **[icon.png](./icon.png)** - 应用图标 (PNG)
- **[icon.svg](./icon.svg)** - 应用图标 (SVG)
- **[donate.png](./donate.png)** - 捐赠二维码

## 📖 项目主文档

以下文档位于项目根目录:

- **[README.md](../README.md)** - 项目介绍 (English)
- **[README_ZH_CN.md](../README_ZH_CN.md)** - 项目介绍 (简体中文)
- **[README_ZH_TW.md](../README_ZH_TW.md)** - 项目介绍 (繁體中文)
- **[AGENTS.md](../AGENTS.md)** - AI Agent 开发指南
- **[CLAUDE.md](../CLAUDE.md)** - Claude Code 配置

## 🔗 模块文档

各模块的独立文档:

- **[ai/README.md](../ai/README.md)** - AI 模块说明
- **[web-ui/README.md](../web-ui/README.md)** - Web UI 项目说明
- **[locale-tui/README.md](../locale-tui/README.md)** - 本地化工具说明

## 📝 文档规范

- **技术文档**: 放在 `docs/` 目录
  - AI SDK相关: `docs/` 根目录
  - 书签系统: `docs/bookmark/`
  - 实现总结: `docs/implementation/`
  - 分支管理: `docs/BRANCHES.md`
- **用户文档**: 放在根目录 (README)
- **模块文档**: 放在对应模块目录
- **配置文件**: 保持原位 (AGENTS.md, CLAUDE.md)

---

*最后更新: 2026-05-18*
