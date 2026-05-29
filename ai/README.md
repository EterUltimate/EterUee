# AI Module

`ai` is the provider abstraction and message-runtime module for EterUee. It is consumed by the Android app, RolePlay, local tools, and the embedded Web API path.

## Package Map

| Package | Responsibility |
| --- | --- |
| `core` | Shared AI primitives: roles, tools, usage, reasoning |
| `provider` | Provider settings, model metadata, provider manager, generation params |
| `provider.providers` | Concrete provider adapters such as OpenAI-compatible, Google, Claude |
| `provider.providers.openai` | OpenAI Chat Completions and Responses API request/response handling |
| `registry` | Model registry and DSL helpers |
| `sdk` | `AISDK`, default SDK implementation, subagent SDK, tool executor contracts |
| `subagent` | Subagent plan/executor/tool orchestration primitives |
| `ui` | `UIMessage`, message parts, images, streaming chunk models |
| `util` | JSON, SSE, request, key roulette, file encoding, error parsing helpers |

## Public Boundary

Use `AISDK` for generation:

```kotlin
interface AISDK {
    suspend fun generateText(request: GenerateTextRequest): GenerateTextResult
    fun streamText(request: StreamTextRequest): Flow<TextChunk>
    suspend fun generateObject(request: GenerateObjectRequest): JsonObject
}
```

Current support:

| API | Status |
| --- | --- |
| `generateText` | Implemented through the selected provider |
| `streamText` | Implemented through provider stream chunks mapped to `TextChunk` |
| `generateObject` | Declared but not implemented |

## Message Model

`UIMessage` is the cross-boundary message type. It is used by:

- ChatService and Android Compose UI.
- RolePlay chat generation.
- Web SSE/API payloads.
- Provider request builders.
- Tool-call and tool-result handling.

Do not flatten messages to plain text unless a provider adapter requires it internally.

## Provider Rules

- Provider-specific request shaping belongs under `provider.providers`.
- App/user settings belong in `app` DataStore models and settings screens.
- Built-in defaults are configured in `app/src/main/java/com/eterultimate/eteruee/data/datastore/DefaultProviders.kt`.
- Provider migrations belong near the settings/data-store migration logic.
- OpenAI-compatible providers should reuse the OpenAI adapter path where possible.

The current built-in EterUee provider base URL is:

```text
https://newapi.eterultimate.asia/v1
```

## Tools

Tools are represented by `core.Tool` and can flow through `GenerateTextRequest` / `StreamTextRequest`. Execution is handled outside provider adapters by tool executors and app-level approval policies.

The local shell tool is implemented in the `app` module, not here:

```text
app/src/main/java/com/eterultimate/eteruee/data/ai/tools/ShellTools.kt
```

## Verification

Run from the repository root:

```bash
./gradlew :ai:testDebugUnitTest
./gradlew test
git diff --check -- ai
```

When changing OpenAI-compatible request or stream behavior, also run the targeted tests under:

```text
ai/src/test/java/com/eterultimate/eteruee/ai/provider/providers/openai/
```

## Related Docs

- [../docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md)
- [../docs/AI_SDK_USAGE.md](../docs/AI_SDK_USAGE.md)
- [../docs/STREAM_V2_USAGE_GUIDE.md](../docs/STREAM_V2_USAGE_GUIDE.md)
