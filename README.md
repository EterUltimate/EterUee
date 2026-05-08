<div align="center">
  <img src="docs/icon.svg" alt="App Icon" width="100" />
  <h1>EterUee</h1>
  <p><strong>A native Android LLM chat client forged in cyberpunk aesthetics — pure black canvas, RGB pure colors, zero-radius corners, and neon data streams.</strong></p>
  <p>
    <a href="README_ZH_CN.md">简体中文</a> |
    <a href="README_ZH_TW.md">繁體中文</a> |
    English
  </p>
</div>

<div align="center">
  <img src="docs/img/chat.png" alt="Chat Interface" width="150" />
  <img src="docs/img/desktop.png" alt="Models Picker" width="450" />
</div>

## 🚀 Download

Build from source or grab the latest APK from <a href="https://github.com/EterUltimate/EterUee/releases">GitHub Releases</a>.

## ✨ Features

- 🎨 **Cyberpunk Design** — pure black background, RGB pure colors, zero-radius corners
- 🔄 **Universal AI Providers** — custom API / base URL / model (OpenAI, Google, Anthropic, and any OpenAI-compatible endpoint)
- 🖼️ **Multimodal Input** — Image, Text, PDF, DOCX, PPTX
- 🖥️ **Web Access** — built-in Ktor server + React web-ui for desktop/tablet use
- 🛠️ **MCP Support** — both SSE and STDIO transport for tool calling
- 🐚 **SSH & Shell** — built-in terminal for remote server interaction
- 🔀 **Message Branching** — tree-structured conversations with regenerate & switch
- 🔍 **Web Search** — Exa, Tavily, Zhipu, LinkUp, Brave, Perplexity, and more
- 🧩 **Prompt Variables** — model name, current time, date, custom placeholders
- 🤳 **QR Code Sync** — export / import provider configs via QR
- 🤖 **Agent Customization** — per-assistant system prompts, temperature, context window, headers
- 🧠 **ChatGPT-like Memory** — persistent cross-conversation memory per assistant
- 📝 **AI Translation** — one-tap message translation
- 🌐 **Custom HTTP Headers & Body** — full request customization
- 💌 **Silly Tavern Character Card Import** — .png card support
- 📡 **LAN Discovery** — auto-discover providers on local network
- 🧪 **Message Transformers** — template, regex, OCR, think-tag extraction, document-as-prompt
- 🌙 **Dark Mode Only** — immersive OLED-friendly UI

## 🏗️ Architecture

```
EterUee
├── app          — Main Android app (Compose UI, ViewModels, navigation)
├── ai           — AI SDK abstraction (OpenAI, Google, Anthropic, streaming)
├── common       — Shared utilities and Kotlin extensions
├── document     — Document parser (PDF, DOCX, PPTX)
├── highlight    — Code syntax highlighting engine
├── search       — Search SDK (Exa, Tavily, Zhipu, LinkUp, Brave, Perplexity)
├── tts          — Text-to-Speech providers
├── web          — Embedded Ktor server + static web-ui hosting
└── web-ui       — React + Vite frontend for cross-platform web access
```

## 🛠️ Tech Stack

| Category | Technology | Purpose |
|----------|------------|---------|
| Language | <a href="https://kotlinlang.org/">Kotlin</a> | Primary development language |
| UI | <a href="https://developer.android.com/jetpack/compose">Jetpack Compose</a> | Declarative Android UI |
| UI | <a href="https://m3.material.io/">Material You</a> | Design system & dynamic theming |
| Icons | <a href="https://composeicons.com/icon-libraries/lucide">compose-icons/lucide</a> | Iconography |
| DI | <a href="https://insert-koin.io/">Koin</a> | Dependency injection |
| Navigation | <a href="https://developer.android.com/develop/ui/compose/navigation">Navigation Compose</a> | In-app navigation |
| Storage | <a href="https://developer.android.com/topic/libraries/architecture/datastore">DataStore</a> | Preferences & proto storage |
| Database | <a href="https://developer.android.com/training/data-storage/room">Room</a> | Local SQLite persistence |
| Network | <a href="https://square.github.io/okhttp/">OkHttp</a> | HTTP client |
| Serialization | <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a> | JSON handling |
| Images | <a href="https://coil-kt.github.io/coil/">Coil</a> | Image loading & caching |
| Web Server | <a href="https://ktor.io/">Ktor</a> | Embedded server for web-ui |
| Web UI | React + Vite | Cross-platform web frontend |

## 🏗️ Build from Source

```bash
# 1. Clone
git clone https://github.com/EterUltimate/EterUee.git
cd EterUee

# 2. Add google-services.json (required for Firebase)
# Place your google-services.json inside the app/ directory.

# 3. Build debug APK
./gradlew assembleDebug
```

> [!TIP]
> You need a `google-services.json` file in the `app/` folder to build the app.

## 🤝 Contributing

1. **Fork** the repository
2. Create a **feature branch** (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. Open a **Pull Request**

This project is developed with <a href="https://developer.android.com/studio">Android Studio</a>. PRs are welcome!

## ⭐ Star History

If you like this project, please give it a star ⭐

<a href="https://star-history.com/#EterUltimate/EterUee&Date">
  <img src="https://api.star-history.com/svg?repos=EterUltimate/EterUee&type=Date" alt="Star History Chart" />
</a>

## 📄 License

This project is dual-licensed:

- <a href="LICENSE">AGPL v3</a> — for non-commercial and open-source use
- **Commercial License** — contact us for commercial usage

See <a href="LICENSE">LICENSE</a> for full details.
