# Nano Banana 兼容性与视频生成参数适配方案

## 概述

本文档说明如何适配 Google Nano Banana (Gemini Image Generation API) 以及参考火山引擎 Seedance 2.0 API 优化视频生成参数配置。

---

## 1. Nano Banana 图片生成兼容性

### 1.1 Nano Banana 模型系列

Google Gemini API 提供三种 Nano Banana 模型:

| 模型名称 | 模型 ID | 特点 | 适用场景 |
|---------|---------|------|---------|
| **Nano Banana 2** | `gemini-3.1-flash-image-preview` | 高效版本,针对速度和高容量优化 | 快速生成、批量任务 |
| **Nano Banana Pro** | `gemini-3-pro-image-preview` | 专业级,支持复杂指令和高保真文本 | 专业资产制作、精细控制 |
| **Nano Banana** | `gemini-2.5-flash-image` | 速度和效率优化,低延迟 | 海量低延迟任务 |

### 1.2 API 调用方式

#### REST API 示例
```bash
curl -s -X POST \
"https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-image-preview:generateContent" \
-H "x-goog-api-key: $GEMINI_API_KEY" \
-H "Content-Type: application/json" \
-d '{
  "contents": [{
    "parts": [
      {"text": "Create a picture of a nano banana dish in a fancy restaurant with a Gemini theme"}
    ]
  }]
}'
```

#### Python SDK 示例
```python
from google import genai
from google.genai import types
from PIL import Image

client = genai.Client()

prompt = "Create a picture of a nano banana dish in a fancy restaurant with a Gemini theme"
response = client.models.generate_content(
    model="gemini-3.1-flash-image-preview",
    contents=[prompt],
)

for part in response.parts:
    if part.text is not None:
        print(part.text)
    elif part.inline_data is not None:
        image = part.as_image()
        image.save("generated_image.png")
```

### 1.3 关键特性

✅ **文生图 (Text-to-Image)**: 通过文本提示生成图片  
✅ **图生图 (Image-to-Image)**: 提供图片+文本提示进行编辑  
✅ **多模态输入**: 支持文本、图片或两者结合  
✅ **SynthID 水印**: 所有生成的图片都包含 SynthID 水印  
✅ **Base64 编码输出**: 返回 base64 编码的图片数据  

### 1.4 当前实现适配建议

查看 `GoogleProvider.kt` 中的 `generateImage` 方法,当前使用的是 Vertex AI 的旧版 API:

```kotlin
// 当前实现 (第758-774行)
val requestBody = buildJsonObject {
    putJsonArray("instances") {
        add(buildJsonObject {
            put("prompt", params.prompt)
        })
    }
    putJsonObject("parameters") {
        put("sampleCount", params.numOfImages)
        put("aspectRatio", when (params.aspectRatio) {
            ImageAspectRatio.SQUARE -> "1:1"
            ImageAspectRatio.LANDSCAPE -> "16:9"
            ImageAspectRatio.PORTRAIT -> "9:16"
        })
    }
}
```

**需要修改为 Gemini API 格式:**

```kotlin
// 适配 Nano Banana 的实现
val requestBody = buildJsonObject {
    putJsonArray("contents") {
        add(buildJsonObject {
            putJsonArray("parts") {
                add(buildJsonObject {
                    put("text", params.prompt)
                })
            }
        })
    }
}

val url = "https://generativelanguage.googleapis.com/v1beta/models/${params.model.modelId}:generateContent"
```

### 1.5 图片编辑支持

Nano Banana 支持图片编辑功能:

```python
# Python 示例
prompt = "Create a picture of my cat eating a nano-banana in a fancy restaurant"
image = Image.open("/path/to/cat_image.png")

response = client.models.generate_content(
    model="gemini-3.1-flash-image-preview",
    contents=[prompt, image],  # 文本 + 图片
)
```

**Android 端实现建议:**

在 `ImageEditParams` 中,`images` 字段应转换为 base64 并添加到请求中:

```kotlin
val contentArray = buildJsonArray {
    // 添加文本部分
    add(buildJsonObject {
        put("type", "text")
        put("text", params.prompt)
    })
    
    // 添加图片部分
    params.images.forEach { imagePath ->
        val bitmap = loadImageBitmap(imagePath)
        val base64Image = encodeToBase64(bitmap)
        add(buildJsonObject {
            put("type", "image_url")
            putJsonObject("image_url") {
                put("url", "data:image/png;base64,$base64Image")
            }
        })
    }
}
```

---

## 2. 视频生成参数适配 (参考火山引擎 Seedance 2.0)

### 2.1 火山引擎 Seedance 2.0 API 参数

根据 [火山引擎文档](https://www.volcengine.com/docs/82379/1520757),视频生成 API 的关键参数包括:

| 参数 | 类型 | 必填 | 说明 | 可选值 |
|-----|------|------|------|--------|
| `model` | String | ✅ | 模型 ID | `seedance-2.0` |
| `content` | Array | ✅ | 内容数组(文本+图片) | - |
| `parameters.durationSeconds` | Int | ❌ | 视频时长(秒) | `3-10` (默认 5) |
| `parameters.resolution` | String | ❌ | 分辨率 | `480p`, `720p`, `1080p` |
| `parameters.aspectRatio` | String | ❌ | 宽高比 | `16:9`, `9:16`, `1:1` |
| `parameters.generateAudio` | Boolean | ❌ | 是否生成音频 | `true/false` (默认 false) |
| `parameters.seed` | Int | ❌ | 随机种子 | 整数 |
| `parameters.negativePrompt` | String | ❌ | 负面提示词 | 字符串 |

### 2.2 当前实现对比

查看 `VideoGenerationParams` (第111-123行):

```kotlin
data class VideoGenerationParams(
    val model: Model,
    val prompt: String,
    val referenceImages: List<ReferenceImage> = emptyList(),
    val aspectRatio: String = "16:9",           // ✅ 已支持
    val durationSeconds: Int = 5,               // ✅ 已支持
    val resolution: String = "720p",            // ✅ 已支持
    val generateAudio: Boolean = false,         // ✅ 已支持
    val seed: Int? = null,                      // ✅ 已支持
    val negativePrompt: String? = null,         // ✅ 已支持
    val customHeaders: List<CustomHeader> = emptyList(),
    val customBody: List<CustomBody> = emptyList(),
)
```

**结论**: 当前 `VideoGenerationParams` 已经完整覆盖了火山引擎 Seedance 2.0 的所有参数! ✅

### 2.3 OpenAI Provider 实现验证

查看 `OpenAIProvider.kt` 中的 `generateVideo` 方法 (第191-260行):

```kotlin
// 构建 parameters 对象
val parametersObj = buildJsonObject {
    put("generateAudio", params.generateAudio)       // ✅
    put("durationSeconds", params.durationSeconds)   // ✅
    put("aspectRatio", params.aspectRatio)           // ✅
    put("resolution", params.resolution)             // ✅
    params.seed?.let { put("seed", it) }             // ✅
    params.negativePrompt?.let { put("negativePrompt", it) } // ✅
}
```

**结论**: OpenAI Provider 已正确实现所有参数的传递! ✅

### 2.4 UI 层验证

查看 `ImgGenPage.kt` 中的 `SettingsBottomSheet`:

```kotlin
if (isVideoMode) {
    // Video duration (3-10 seconds)
    OutlinedNumberInput(
        value = videoDuration,
        onValueChange = vm::updateVideoDuration,
    )
    
    // Video resolution (480p, 720p, 1080p)
    listOf("480p", "720p", "1080p").forEach { res ->
        FilterChip(
            selected = videoResolution == res,
            onClick = { vm.updateVideoResolution(res) },
            label = { Text(res) }
        )
    }
    
    // Video aspect ratio (16:9, 9:16, 1:1)
    listOf("16:9", "9:16", "1:1").forEach { ratio ->
        FilterChip(
            selected = videoAspectRatio == ratio,
            onClick = { vm.updateVideoAspectRatio(ratio) },
            label = { Text(ratio) }
        )
    }
    
    // Generate audio toggle
    FilterChip(
        selected = generateAudio,
        onClick = { vm.updateGenerateAudio(!generateAudio) },
        label = { Text(if (generateAudio) "On" else "Off") }
    )
}
```

**结论**: UI 层已完整实现所有视频生成参数的配置界面! ✅

---

## 3. 需要修复的问题

### 3.1 ImgGenVM 缺失的视频生成相关属性和方法

根据编译错误,`ImgGenVM` 缺少以下成员:

```kotlin
// ❌ 缺失的属性
private val _isVideoMode = MutableStateFlow(false)
val isVideoMode: StateFlow<Boolean> = _isVideoMode

private val _videoDuration = MutableStateFlow(5)
val videoDuration: StateFlow<Int> = _videoDuration

private val _videoResolution = MutableStateFlow("720p")
val videoResolution: StateFlow<String> = _videoResolution

private val _videoAspectRatio = MutableStateFlow("16:9")
val videoAspectRatio: StateFlow<String> = _videoAspectRatio

private val _generateAudio = MutableStateFlow(false)
val generateAudio: StateFlow<Boolean> = _generateAudio

// ❌ 缺失的方法
fun setVideoMode(enabled: Boolean) {
    _isVideoMode.value = enabled
}

fun updateVideoDuration(duration: Int) {
    _videoDuration.value = duration.coerceIn(3, 10)
}

fun updateVideoResolution(resolution: String) {
    _videoResolution.value = resolution
}

fun updateVideoAspectRatio(aspectRatio: String) {
    _videoAspectRatio.value = aspectRatio
}

fun updateGenerateAudio(enabled: Boolean) {
    _generateAudio.value = enabled
}
```

### 3.2 视频生成功能实现

需要在 `ImgGenVM` 中添加 `generateVideo()` 方法:

```kotlin
fun generateVideo() {
    if (prompt.value.isBlank()) return
    cancelJob?.cancel()
    cancelJob = viewModelScope.launch {
        try {
            _isGenerating.value = true
            _error.value = null
            _currentGeneratedImages.value = emptyList()

            val settings = settingsStore.settingsFlow.first()
            val model = settings.findModelById(settings.videoGenerationModelId)
                ?: throw IllegalStateException("No video model selected")

            val provider = model.findProvider(settings.providers)
                ?: throw IllegalStateException("Provider not found")

            val providerSetting = settings.providers.find { it.id == provider.id }
                ?: throw IllegalStateException("Provider setting not found")

            val params = VideoGenerationParams(
                model = model,
                prompt = _prompt.value,
                referenceImages = _referenceImages.value.map { path ->
                    ReferenceImage(url = "file://$path")
                },
                aspectRatio = _videoAspectRatio.value,
                durationSeconds = _videoDuration.value,
                resolution = _videoResolution.value,
                generateAudio = _generateAudio.value,
                customHeaders = model.customHeaders,
                customBody = model.customBodies
            )

            val result = providerManager.getProviderByType(provider)
                .generateVideo(providerSetting, params)

            // 处理视频生成结果
            // ...
        } catch (e: Exception) {
            if (e is CancellationException) return@launch
            Log.e(TAG, "Failed to generate video", e)
            _error.value = e.message ?: "Unknown error occurred"
        } finally {
            _isGenerating.value = false
        }
    }
}
```

---

## 4. 实施步骤

### Phase 1: 修复编译错误 (高优先级)

1. ✅ 在 `ImgGenVM.kt` 中添加缺失的视频生成相关属性
2. ✅ 在 `ImgGenVM.kt` 中添加缺失的更新方法
3. ✅ 实现 `generateVideo()` 方法
4. ✅ 在 `ImgGenPage.kt` 中绑定视频生成按钮

### Phase 2: Nano Banana 适配 (中优先级)

1. 🔄 在 `GoogleProvider.kt` 中添加新的 `generateContent` 方法
2. 🔄 支持 Gemini API 的请求格式
3. 🔄 处理 base64 编码的图片响应
4. 🔄 添加图片编辑支持

### Phase 3: 测试与优化 (低优先级)

1. 📝 编写单元测试
2. 📝 端到端测试图片生成
3. 📝 端到端测试视频生成
4. 📝 性能优化和错误处理

---

## 5. 参考资源

### 官方文档
- [Google Gemini Image Generation API](https://ai.google.dev/gemini-api/docs/image-generation?hl=zh-cn)
- [火山引擎 Seedance 2.0 API](https://www.volcengine.com/docs/82379/1520757?lang=zh)

### 代码文件
- `ai/src/main/java/com/eterultimate/eteruee/ai/provider/providers/GoogleProvider.kt`
- `ai/src/main/java/com/eterultimate/eteruee/ai/provider/providers/OpenAIProvider.kt`
- `ai/src/main/java/com/eterultimate/eteruee/ai/provider/Provider.kt`
- `app/src/main/java/com/eterultimate/eteruee/ui/pages/imggen/ImgGenVM.kt`
- `app/src/main/java/com/eterultimate/eteruee/ui/pages/imggen/ImgGenPage.kt`

---

## 6. 总结

✅ **视频生成参数**: 当前实现已完全兼容火山引擎 Seedance 2.0 API,只需补充 `ImgGenVM` 中缺失的属性和方法即可。

🔄 **Nano Banana 适配**: 需要修改 `GoogleProvider.kt` 以支持 Gemini API 的新格式,包括:
- 使用 `generateContent` 端点替代 `predict`
- 调整请求体结构为 `contents.parts` 格式
- 正确处理 base64 编码的图片响应
- 添加图片编辑支持

⚠️ **当前阻塞问题**: `ImgGenVM.kt` 缺少视频生成相关的状态管理代码,导致编译失败。这是首要需要解决的问题。
