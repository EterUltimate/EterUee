# Subagent 功能实现计划

## 概述

在两个模块（app 和 roleplay）的对话中实现 Subagent 功能：
- **主模型（Master）**：负责理解用户意图，生成执行计划（Plan），决定需要调用哪些工具/skill/MCP
- **Subagent**：负责实际执行工具调用、skill 和 MCP，将结果返回给主模型

## 架构设计

```
用户输入
    ↓
主模型生成 Plan（需要哪些工具）
    ↓
Subagent 执行工具调用/skill/MCP
    ↓
工具结果返回给主模型
    ↓
主模型生成最终回复
```

## 任务列表

### Task 1: AI 模块核心扩展

在 `ai` 模块中创建 Subagent 核心基础设施：

**文件**: `ai/src/main/java/com/eterultimate/eteruee/ai/subagent/SubagentPlan.kt`
- 定义 `SubagentPlan` 数据类（包含任务列表、工具调用计划）
- 定义 `PlanStep`（工具名称、参数、执行顺序）

**文件**: `ai/src/main/java/com/eterultimate/eteruee/ai/subagent/SubagentExecutor.kt`
- 创建 `SubagentExecutor` 接口和实现
- 负责解析主模型生成的 plan，按顺序执行工具调用
- 收集所有工具结果，返回给主模型

**文件**: `ai/src/main/java/com/eterultimate/eteruee/ai/subagent/SubagentToolExecutor.kt`
- 扩展 `ToolExecutor`，支持 plan 模式下的批量工具执行
- 处理工具依赖关系（串行/并行执行）

### Task 2: AI SDK 扩展

**文件**: `ai/src/main/java/com/eterultimate/eteruee/ai/sdk/SubagentAISDK.kt`
- 扩展 `AISDK`，添加 `streamTextWithSubagent()` 方法
- 实现主模型 → plan → subagent 执行 → 主模型总结的完整流程
- 支持流式输出（plan 生成、工具执行状态、最终结果）

### Task 3: App 模块集成

**文件**: `app/src/main/java/com/eterultimate/eteruee/data/ai/ChatSubagentExecutor.kt`
- 创建 App 模块的 Subagent 执行器实现
- 复用现有的 `ChatToolExecutor` 来执行工具
- 集成 MCP、skill、local tools、web search

**文件**: `app/src/main/java/com/eterultimate/eteruee/ui/hooks/ChatStateHolder.kt`
- 扩展 `ChatStateHolder`，添加 subagent 模式支持
- 在 `handleSubmit` 中支持选择是否启用 subagent 模式

**文件**: `app/src/main/java/com/eterultimate/eteruee/ui/pages/chat/ChatVM.kt`
- 在 `ChatVM` 中配置 subagent 执行器
- 添加 subagent 启用状态（可从设置中控制）

### Task 4: Roleplay 模块集成

**文件**: `roleplay/src/main/java/com/eterultimate/eteruee/roleplay/domain/subagent/RoleplaySubagentExecutor.kt`
- 创建 Roleplay 模块的 Subagent 执行器
- 集成 AI SDK 的 subagent 功能
- 支持角色配置中的工具调用

**文件**: `roleplay/src/main/java/com/eterultimate/eteruee/roleplay/ui/viewmodel/ChatViewModel.kt`
- 扩展 `ChatViewModel` 的 `generateAIResponse` 方法
- 添加 subagent 模式支持
- 更新 UI 状态显示 subagent 执行进度

### Task 5: UI 更新

**文件**: `app/src/main/java/com/eterultimate/eteruee/ui/pages/chat/ChatPage.kt`
- 添加 subagent 模式切换按钮/指示器
- 显示 plan 生成状态、工具执行状态

**文件**: `roleplay/src/main/java/com/eterultimate/eteruee/roleplay/ui/pages/chat/ChatPage.kt`
- 添加 subagent 状态显示
- 显示工具调用进度

### Task 6: 数据模型更新

**文件**: `ai/src/main/java/com/eterultimate/eteruee/ai/ui/UIMessage.kt`
- 扩展 `UIMessagePart` 添加 `SubagentPlan` part 类型
- 用于在消息中显示 plan 内容

### Task 7: 测试与验证

- 验证主模型能正确生成 plan
- 验证 subagent 能正确执行工具调用
- 验证工具结果能正确返回给主模型
- 验证流式输出正常工作

## 关键设计决策

1. **Plan 格式**: 使用 JSON 格式，主模型输出结构化 plan
2. **执行模式**: 支持串行和并行工具执行
3. **错误处理**: 工具执行失败时，subagent 返回错误信息，主模型决定如何处理
4. **流式输出**: plan 生成、每个工具执行、最终结果都流式输出到 UI

## 文件变更清单

### 新增文件
- `ai/src/main/java/com/eterultimate/eteruee/ai/subagent/SubagentPlan.kt`
- `ai/src/main/java/com/eterultimate/eteruee/ai/subagent/SubagentExecutor.kt`
- `ai/src/main/java/com/eterultimate/eteruee/ai/subagent/SubagentToolExecutor.kt`
- `ai/src/main/java/com/eterultimate/eteruee/ai/sdk/SubagentAISDK.kt`
- `app/src/main/java/com/eterultimate/eteruee/data/ai/ChatSubagentExecutor.kt`
- `roleplay/src/main/java/com/eterultimate/eteruee/roleplay/domain/subagent/RoleplaySubagentExecutor.kt`

### 修改文件
- `ai/src/main/java/com/eterultimate/eteruee/ai/ui/UIMessage.kt`
- `app/src/main/java/com/eterultimate/eteruee/ui/hooks/ChatStateHolder.kt`
- `app/src/main/java/com/eterultimate/eteruee/ui/pages/chat/ChatVM.kt`
- `app/src/main/java/com/eterultimate/eteruee/ui/pages/chat/ChatPage.kt`
- `roleplay/src/main/java/com/eterultimate/eteruee/roleplay/ui/viewmodel/ChatViewModel.kt`
- `roleplay/src/main/java/com/eterultimate/eteruee/roleplay/ui/pages/chat/ChatPage.kt`
