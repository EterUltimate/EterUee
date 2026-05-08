<div align="center">
  <img src="docs/icon.svg" alt="App 圖標" width="100" />
  <h1>EterUee</h1>
  <p><strong>一款原生 Android LLM 聊天客戶端，賽博龐克美學鍛造 — 純黑畫布、RGB 純色、零圓角、霓虹數據流。</strong></p>
  <p>
    <a href="README.md">English</a> |
    繁體中文 |
    <a href="README_ZH_CN.md">简体中文</a>
  </p>
</div>

<div align="center">
  <img src="docs/img/chat.png" alt="聊天介面" width="150" />
  <img src="docs/img/desktop.png" alt="模型選擇器" width="450" />
</div>

## 🚀 下載

從源碼構建或從 <a href="https://github.com/EterUltimate/EterUee/releases">GitHub Releases</a> 下載最新 APK。

## ✨ 功能特色

- 🎨 **賽博龐克設計** — 純黑背景、RGB 純色、零圓角
- 🔄 **通用 AI 供應商** — 自定義 API / 基礎 URL / 模型（OpenAI、Google、Anthropic 及所有相容端點）
- 🖼️ **多模態輸入** — 圖片、文字、PDF、DOCX、PPTX
- 🖥️ **Web 多端訪問** — 內建 Ktor 伺服器 + React web-ui，支援桌面/平板使用
- 🛠️ **MCP 支援** — SSE 與 STDIO 雙模式工具呼叫
- 🐚 **SSH 與 Shell** — 內建終端機，支援遠端伺服器互動
- 🔀 **訊息分支** — 樹狀對話結構，支援重新生成與切換
- 🔍 **聯網搜尋** — Exa、Tavily、Zhipu、LinkUp、Brave、Perplexity 等
- 🧩 **Prompt 變數** — 模型名稱、當前時間、日期、自定義佔位符
- 🤳 **二維碼同步** — 透過二維碼匯出/匯入供應商設定
- 🤖 **智能體自定義** — 每個助手獨立系統提示、溫度、上下文視窗、請求頭
- 🧠 **類 ChatGPT 記憶** — 按助手持久化的跨對話記憶
- 📝 **AI 翻譯** — 一鍵翻譯訊息
- 🌐 **自定義 HTTP 請求頭與請求體** — 完整請求定製
- 💌 **Silly Tavern 角色卡匯入** — 支援 .png 角色卡
- 📡 **區域網路發現** — 自動發現區域網路內的供應商
- 🧪 **訊息轉換器** — 模板、正則、OCR、think 標籤提取、文件轉提示詞
- 🌙 **純深色模式** — 沉浸式 OLED 友好介面

## 🏗️ 架構

```
EterUee
├── app          — 主 Android 應用（Compose UI、ViewModel、導航）
├── ai           — AI SDK 抽象層（OpenAI、Google、Anthropic、串流）
├── common       — 共享工具類與 Kotlin 擴展
├── document     — 文件解析器（PDF、DOCX、PPTX）
├── highlight    — 程式碼語法高亮引擎
├── search       — 搜尋 SDK（Exa、Tavily、Zhipu、LinkUp、Brave、Perplexity）
├── tts          — 文字轉語音供應商
├── web          — 嵌入式 Ktor 伺服器 + 靜態 web-ui 托管
└── web-ui       — React + Vite 跨平台 Web 前端
```

## 🛠️ 技術棧

| 分類 | 技術 | 用途 |
|------|------|------|
| 語言 | <a href="https://kotlinlang.org/">Kotlin</a> | 主要開發語言 |
| UI | <a href="https://developer.android.com/jetpack/compose">Jetpack Compose</a> | 宣告式 Android UI |
| UI | <a href="https://m3.material.io/">Material You</a> | 設計系統與動態主題 |
| 圖標 | <a href="https://composeicons.com/icon-libraries/lucide">compose-icons/lucide</a> | 圖標庫 |
| 依賴注入 | <a href="https://insert-koin.io/">Koin</a> | 依賴注入 |
| 導航 | <a href="https://developer.android.com/develop/ui/compose/navigation">Navigation Compose</a> | 應用內導航 |
| 儲存 | <a href="https://developer.android.com/topic/libraries/architecture/datastore">DataStore</a> | 偏好設定與協議儲存 |
| 資料庫 | <a href="https://developer.android.com/training/data-storage/room">Room</a> | 本地 SQLite 持久化 |
| 網路 | <a href="https://square.github.io/okhttp/">OkHttp</a> | HTTP 客戶端 |
| 序列化 | <a href="https://github.com/Kotlin/kotlinx.serialization">kotlinx.serialization</a> | JSON 處理 |
| 圖片 | <a href="https://coil-kt.github.io/coil/">Coil</a> | 圖片載入與快取 |
| Web 伺服器 | <a href="https://ktor.io/">Ktor</a> | web-ui 嵌入式伺服器 |
| Web 前端 | React + Vite | 跨平台 Web 前端 |

## 🏗️ 源碼構建

```bash
# 1. 克隆倉庫
git clone https://github.com/EterUltimate/EterUee.git
cd EterUee

# 2. 添加 google-services.json（Firebase 必需）
# 將你的 google-services.json 放入 app/ 目錄下。

# 3. 構建 Debug APK
./gradlew assembleDebug
```

> [!TIP]
> 你需要在 `app/` 資料夾下添加 `google-services.json` 檔案才能構建應用。

## 🤝 貢獻指南

1. **Fork** 本倉庫
2. 建立 **功能分支**（`git checkout -b feature/amazing-feature`）
3. **提交** 變更（`git commit -m 'Add amazing feature'`）
4. **推送** 到分支（`git push origin feature/amazing-feature`）
5. 發起 **Pull Request**

本專案使用 <a href="https://developer.android.com/studio">Android Studio</a> 開發，歡迎提交 PR！

## ⭐ Star History

如果喜歡這個專案，請給個 Star ⭐

<a href="https://star-history.com/#EterUltimate/EterUee&Date">
  <img src="https://api.star-history.com/svg?repos=EterUltimate/EterUee&type=Date" alt="Star History Chart" />
</a>

## 📄 許可證

本專案採用雙許可模式：

- <a href="LICENSE">AGPL v3</a> — 非商業及開源使用
- **商業許可** — 商業用途請聯繫我們

詳見 <a href="LICENSE">LICENSE</a> 檔案。
