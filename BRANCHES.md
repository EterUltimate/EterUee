# 分支说明

## 🌿 当前分支结构

### master (主分支)
- **状态**: ✅ 稳定,生产可用
- **架构**: 现有架构 (ChatService + Provider)
- **用途**: 日常开发、bug 修复、新功能添加
- **保护**: 需要 PR 和 Code Review 才能合并

### feature/ai-sdk-refactor (开发分支)
- **状态**: 🚧 开发中
- **架构**: AI SDK v5 最佳实践
- **用途**: 大规模重构实验
- **同步**: 定期从 master 合并更新

---

## 📊 分支对比

| 特性 | master | feature/ai-sdk-refactor |
|------|--------|------------------------|
| **Android 架构** | ChatVM → ChatService → Provider | ChatVM → UseChat Hook → AISDK → Provider |
| **Web 架构** | 手动 SSE 管理 (~1133 行) | useChat hook (~400 行) |
| **流式处理** | 自定义实现 | AI SDK 标准 API |
| **工具调用** | ❌ 未实现 | ✅ 原生支持 |
| **类型安全** | 部分 | 完整 TypeScript/Kotlin |
| **代码行数** | 较多 | 预计减少 30-65% |
| **学习曲线** | 中等 | 较低 (标准 API) |
| **维护成本** | 较高 | 较低 |

---

## 🔄 分支工作流

### 在 master 分支工作

```bash
# 切换到主分支
git checkout master

# 拉取最新代码
git pull origin master

# 进行日常开发...
# 提交更改
git add .
git commit -m "feat: 添加新功能"
git push
```

### 在开发分支工作

```bash
# 切换到开发分支
git checkout feature/ai-sdk-refactor

# 从 master 同步更新
git fetch origin master
git merge origin/master

# 进行重构开发...
# 提交更改
git add .
git commit -m "feat: 实现 AISDK 接口"
git push
```

### 准备合并到 master

当开发分支功能完成并充分测试后:

```bash
# 1. 确保开发分支是最新的
git checkout feature/ai-sdk-refactor
git fetch origin master
git merge origin/master

# 2. 解决冲突 (如果有)
# 3. 运行所有测试
# 4. 推送到远程
git push

# 5. 在 GitHub 上创建 Pull Request
# 6. Code Review
# 7. 合并到 master
```

---

## 📁 文档说明

### master 分支文档

- **[AI_SDK_REFACTOR_SUMMARY.md](./AI_SDK_REFACTOR_SUMMARY.md)**
  - 记录已完成的 Android 端基础架构
  - 分析技术挑战和暂缓原因
  - 提供下一步建议

### feature/ai-sdk-refactor 分支文档

- **[AI_SDK_REFACTOR_DEV_BRANCH_PLAN.md](./AI_SDK_REFACTOR_DEV_BRANCH_PLAN.md)**
  - 完整的重构实施计划
  - Android 和 Web 端详细方案
  - SSE 事件格式标准化规范
  - 6 周时间表和成功指标

- **[AI_SDK_REFACTOR_QUICKSTART.md](./AI_SDK_REFACTOR_QUICKSTART.md)**
  - 快速开始指南
  - 三个起始任务选项
  - 代码示例模板
  - 调试技巧和常见问题

---

## ⚠️ 注意事项

### 在 master 分支

✅ **可以做的**:
- 日常功能开发
- Bug 修复
- 性能优化
- 文档更新

❌ **不要做的**:
- 大规模架构改动
- 破坏性 API 变更
- 未经测试的实验性功能

### 在 feature/ai-sdk-refactor 分支

✅ **可以做的**:
- 大规模重构
- 架构实验
- 破坏性变更
- 新技术验证

❌ **不要做的**:
- 直接合并到 master (需经过充分测试)
- 忽略与 master 的同步
- 删除重要的回退机制

---

## 🎯 何时使用哪个分支?

### 使用 master 分支,如果:

- 你要添加一个新功能 (如批量删除对话)
- 你要修复一个 bug
- 你要优化性能
- 你要更新文档
- 你的改动不影响核心架构

### 使用 feature/ai-sdk-refactor 分支,如果:

- 你要重构聊天流式处理逻辑
- 你要集成 AI SDK v5
- 你要改变消息管理架构
- 你要实现工具调用系统
- 你的改动影响多个模块

---

## 📈 进度追踪

### master 分支进度

- [x] AI SDK 基础架构设计 (已完成)
- [x] UseChat Composable Hook (已完成)
- [ ] ChatVM 迁移 (暂缓)
- [ ] Web 端重构 (暂缓)

详见: [AI_SDK_REFACTOR_SUMMARY.md](./AI_SDK_REFACTOR_SUMMARY.md)

### feature/ai-sdk-refactor 分支进度

- [ ] 后端 SSE 格式标准化
- [ ] Android AISDK 完整实现
- [ ] Android 端迁移
- [ ] Web Provider 实现
- [ ] Web 端重构
- [ ] 工具调用支持
- [ ] 全面测试

详见: [AI_SDK_REFACTOR_DEV_BRANCH_PLAN.md](./AI_SDK_REFACTOR_DEV_BRANCH_PLAN.md)

---

## 🔗 相关链接

- [GitHub Pull Requests](https://github.com/EterUltimate/EterUee/pulls)
- [GitHub Issues](https://github.com/EterUltimate/EterUee/issues)
- [Vercel AI SDK 文档](https://sdk.vercel.ai/docs)

---

**最后更新**: 2026-05-13  
**维护者**: 开发团队
