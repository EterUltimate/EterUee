# 剩余同步任务执行计划

## 任务概览

需要完成 4 个待办同步项，按复杂度从低到高排序：

1. **sync30** (c727b6f1): FileUtils.kt 重构 - 文件工具函数拆分
2. **sync33** (0377a2de): WebView 数据重载优化
3. **sync31** (e2ca5fc9): AssistantBasicPage.kt 较大改动 - 移除温度/top p 滑动条
4. **sync37 part 2**: ImgGenPage/ImgGenVM UI 层编辑功能实现

---

## Task 1: sync30 - 文件工具函数拆分

### 目标
将 FileUtils.kt 中的工具函数进行模块化拆分，提高代码可维护性。

### 执行步骤

#### Step 1.1: 分析 rikkahub 的 FileUtils 结构
```bash
# 查看 rikkahub 的 FileUtils.kt 当前状态
git -C /path/to/rikkahub show c727b6f1:app/src/main/java/me/rerere/rikkahub/utils/FileUtils.kt
```

**关键检查点**：
- 识别哪些函数被拆分到新文件
- 确认新文件的命名和位置
- 记录函数迁移映射关系

#### Step 1.2: 对比 EterUee 的 FileUtils.kt
```bash
# 查看 EterUee 当前 FileUtils.kt
read_file app/src/main/java/com/eterultimate/eteruee/utils/FileUtils.kt
```

**差异分析**：
- 列出 EterUee 独有的函数
- 标记需要同步拆分的函数
- 确认是否有依赖关系需要调整

#### Step 1.3: 执行拆分
根据 rikkahub 的拆分模式：

1. **创建新文件**（如果需要）：
   - 例如：`FileExtensions.kt`、`PathUtils.kt`、`MediaUtils.kt` 等
   
2. **迁移函数**：
   - 将相关函数移动到对应的新文件
   - 更新 import 语句
   
3. **保留核心函数**：
   - FileUtils.kt 保留最常用的工具函数

#### Step 1.4: 更新引用
```bash
# 搜索所有引用 FileUtils 的地方
grep_code regex="import.*FileUtils" type=kotlin
```

批量更新 import 路径，确保编译通过。

#### Step 1.5: 验证
```bash
./gradlew assembleDebug
```

---

## Task 2: sync33 - WebView 数据重载优化

### 目标
优化 WebView 组件的数据重载逻辑，避免不必要的重新加载。

### 执行步骤

#### Step 2.1: 定位 WebView 相关文件
```bash
# 查找 WebView 相关组件
search_codebase key_words="WebView,reload,refresh" query="WebView component data reload optimization"
```

**预期文件**：
- `app/src/main/java/com/eterultimate/eteruee/ui/components/web/WebView.kt` 或类似路径
- 可能涉及 ViewModel 或 State 管理

#### Step 2.2: 分析 rikkahub 的优化方案
```bash
git -C /path/to/rikkahub show 0377a2de --stat
git -C /path/to/rikkahub show 0377a2de:<file_path>
```

**关注点**：
- 如何判断是否需要重载（key 变化检测）
- 是否引入了缓存机制
- State 管理的变化

#### Step 2.3: 应用优化到 EterUee

**典型优化模式**（基于常见 WebView 优化）：

```kotlin
// 优化前：每次重组都重新创建 WebView
AndroidView(
    factory = { context -> WebView(context) }
)

// 优化后：使用 key 控制重建
AndroidView(
    factory = { context -> WebView(context) },
    update = { webView ->
        // 只在必要时更新
    },
    modifier = Modifier.key(currentUrl) // 仅 URL 变化时重建
)
```

或者引入 LaunchedEffect 控制：

```kotlin
var shouldReload by remember { mutableStateOf(false) }

LaunchedEffect(url) {
    if (url != previousUrl) {
        shouldReload = true
    }
}
```

#### Step 2.4: 测试验证
- 切换不同 URL 时观察 WebView 行为
- 确认不会在不需要时重新加载
- 检查内存使用情况

---

## Task 3: sync31 - 移除温度/top p 滑动条

### 目标
从 AssistantBasicPage.kt 中移除温度和 Top P 参数的滑动条控件，简化助手基础设置界面。

### 执行步骤

#### Step 3.1: 分析 rikkahub 的改动
```bash
git -C /path/to/rikkahub show e2ca5fc9 --stat
git -C /path/to/rikkahub show e2ca5fc9:app/src/main/java/me/rerere/rikkahub/ui/pages/settings/AssistantBasicPage.kt
```

**关键问题**：
- 移除了哪些 UI 组件（Slider、Text 等）
- 是否保留了参数存储（只是隐藏 UI）
- 是否有替代的设置方式
- 相关的 ViewModel/State 是否也移除了

#### Step 3.2: 定位 EterUee 的 AssistantBasicPage
```bash
search_file query="**/AssistantBasicPage.kt"
```

预期路径：`app/src/main/java/com/eterultimate/eteruee/ui/pages/settings/AssistantBasicPage.kt`

#### Step 3.3: 读取当前实现
```kotlin
read_file app/src/main/java/com/eterultimate/eteruee/ui/pages/settings/AssistantBasicPage.kt
```

**标记需要移除的部分**：
- Temperature Slider 及相关 Label
- Top P Slider 及相关 Label
- 相关的 State 变量（如果有）
- 相关的字符串资源

#### Step 3.4: 执行移除

**示例修改**：

```kotlin
// 移除前
Column {
    // ... other settings ...
    
    // Temperature
    Text(stringResource(R.string.setting_temperature))
    Slider(
        value = temperature,
        onValueChange = { viewModel.updateTemperature(it) },
        valueRange = 0f..2f
    )
    Text("${temperature.toString().take(3)}")
    
    // Top P
    Text(stringResource(R.string.setting_top_p))
    Slider(
        value = topP,
        onValueChange = { viewModel.updateTopP(it) },
        valueRange = 0f..1f
    )
    Text("${topP.toString().take(3)}")
}

// 移除后
Column {
    // ... other settings ...
    // Temperature and Top P sliders removed
}
```

#### Step 3.5: 清理相关代码

1. **ViewModel 清理**（如果不再需要）：
   ```kotlin
   // 检查 AssistantViewModel 或类似文件
   grep_code regex="updateTemperature|updateTopP|temperature|topP" 
   path="app/src/main/java/com/eterultimate/eteruee"
   ```

2. **字符串资源清理**（可选，保持向后兼容可保留）：
   ```xml
   <!-- app/src/main/res/values/strings.xml -->
   <!-- 可以注释掉而非删除，以防后续需要 -->
   <!-- <string name="setting_temperature">Temperature</string> -->
   <!-- <string name="setting_top_p">Top P</string> -->
   ```

3. **数据模型检查**：
   - 确认 Assistant 模型中 temperature/topP 字段是否仍需保留
   - 如果只是 UI 移除但后端仍需要，保留字段

#### Step 3.6: 验证
```bash
./gradlew assembleDebug
```

手动测试：
- 打开助手设置页面
- 确认温度和 Top P 滑动条已消失
- 其他设置项正常工作

---

## Task 4: sync37 part 2 - 图片编辑 UI 层实现

### 目标
在 ImgGenPage 和 ImgGenVM 中实现图片编辑功能的 UI 交互，支持选择参考图片并调用编辑 API。

### 前置条件
已完成 sync37 part 1：
- ✅ GenMediaEntity 添加 type 和 sourcePaths 字段
- ✅ Provider 接口添加 editImage 方法
- ✅ OpenAIProvider 实现 editImage

### 执行步骤

#### Step 4.1: 分析 rikkahub 的 UI 实现
```bash
git -C /path/to/rikkahub show d7945482 --name-only | grep -i "img\|gen"
git -C /path/to/rikkahub show 5dd59e58 --name-only | grep -i "img\|gen"
```

**重点关注文件**：
- `ImgGenPage.kt` - 图片生成页面 UI
- `ImgGenVM.kt` 或 `ImgGenViewModel.kt` - ViewModel
- 可能的 Composable 组件：`ImageSelector.kt`、`ReferenceImagePreview.kt` 等

#### Step 4.2: 定位 EterUee 的相关文件
```bash
search_file query="**/ImgGen*.kt"
```

预期文件：
- `app/src/main/java/com/eterultimate/eteruee/ui/pages/ImgGenPage.kt`
- `app/src/main/java/com/eterultimate/eteruee/viewmodel/ImgGenVM.kt`

#### Step 4.3: 扩展 ImgGenVM

**需要添加的状态和方法**：

```kotlin
class ImgGenVM : ViewModel() {
    // 现有状态
    var prompt by mutableStateOf("")
    var selectedModel by mutableStateOf<Model?>(null)
    var generatedImages by mutableStateOf<List<GenMediaEntity>>(emptyList())
    
    // === 新增：编辑模式支持 ===
    
    // 选中的参考图片（用于编辑）
    var selectedReferenceImage by mutableStateOf<GenMediaEntity?>(null)
    
    // 是否为编辑模式
    val isEditMode: Boolean
        get() = selectedReferenceImage != null
    
    /**
     * 选择参考图片
     */
    fun selectReferenceImage(image: GenMediaEntity) {
        selectedReferenceImage = image
    }
    
    /**
     * 清除参考图片（退出编辑模式）
     */
    fun clearReferenceImage() {
        selectedReferenceImage = null
    }
    
    /**
     * 生成或编辑图片
     */
    fun generateOrEditImage() {
        viewModelScope.launch {
            try {
                val result = if (isEditMode) {
                    // 编辑模式：调用 editImage
                    provider.editImage(
                        providerSetting = currentProviderSetting,
                        params = ImageEditParams(
                            model = selectedModel!!,
                            prompt = prompt,
                            images = listOf(selectedReferenceImage!!.path),
                            numOfImages = 1,
                            aspectRatio = currentAspectRatio,
                            customHeaders = currentCustomHeaders,
                            customBody = currentCustomBody
                        )
                    )
                } else {
                    // 生成模式：调用 generateImage
                    provider.generateImage(
                        providerSetting = currentProviderSetting,
                        params = ImageGenerationParams(
                            model = selectedModel!!,
                            prompt = prompt,
                            numOfImages = 1,
                            aspectRatio = currentAspectRatio,
                            customHeaders = currentCustomHeaders,
                            customBody = currentCustomBody
                        )
                    )
                }
                
                // 保存生成的图片
                saveGeneratedImages(result.items)
                
                // 如果是编辑模式，清除参考图片
                if (isEditMode) {
                    clearReferenceImage()
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                showError(e.message ?: "Failed to generate/edit image")
            }
        }
    }
    
    private suspend fun saveGeneratedImages(items: List<ImageGenerationItem>) {
        // 保存到数据库
        items.forEach { item ->
            val entity = GenMediaEntity(
                path = saveBase64Image(item.data, item.mimeType),
                modelId = selectedModel!!.modelId,
                prompt = prompt,
                createAt = System.currentTimeMillis(),
                type = if (isEditMode) GenMediaEntity.TYPE_IMAGE_EDIT else GenMediaEntity.TYPE_IMAGE_GENERATION,
                sourcePaths = if (isEditMode) selectedReferenceImage?.path else null
            )
            genMediaDao.insert(entity)
        }
        
        // 刷新列表
        loadGeneratedImages()
    }
}
```

#### Step 4.4: 改造 ImgGenPage UI

**关键 UI 改动**：

```kotlin
@Composable
fun ImgGenPage(viewModel: ImgGenVM = hiltViewModel()) {
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // === 1. 提示词输入 ===
        OutlinedTextField(
            value = viewModel.prompt,
            onValueChange = { viewModel.prompt = it },
            label = { Text("Prompt") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // === 2. 模型选择 ===
        ModelSelector(
            selectedModel = viewModel.selectedModel,
            onModelSelected = { viewModel.selectedModel = it }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // === 3. 参考图片选择区域（编辑模式）===
        if (viewModel.isEditMode && viewModel.selectedReferenceImage != null) {
            ReferenceImageSection(
                referenceImage = viewModel.selectedReferenceImage!!,
                onClear = { viewModel.clearReferenceImage() }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // === 4. 已生成图片列表（可选择作为参考）===
        if (viewModel.generatedImages.isNotEmpty()) {
            Text(
                text = "Generated Images (tap to use as reference)",
                style = MaterialTheme.typography.titleSmall
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.generatedImages) { image ->
                    GeneratedImageThumbnail(
                        image = image,
                        isSelected = viewModel.selectedReferenceImage?.id == image.id,
                        onClick = {
                            if (viewModel.selectedReferenceImage?.id == image.id) {
                                viewModel.clearReferenceImage()
                            } else {
                                viewModel.selectReferenceImage(image)
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // === 5. 生成/编辑按钮 ===
        Button(
            onClick = { viewModel.generateOrEditImage() },
            enabled = viewModel.prompt.isNotBlank() && viewModel.selectedModel != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (viewModel.isEditMode) "Edit Image" else "Generate Image"
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // === 6. 生成结果展示 ===
        if (viewModel.generatedImages.isNotEmpty()) {
            Text(
                text = "Results",
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.generatedImages) { image ->
                    ImageCard(image = image)
                }
            }
        }
    }
}

/**
 * 参考图片预览区域
 */
@Composable
fun ReferenceImageSection(
    referenceImage: GenMediaEntity,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Editing based on reference image",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear reference"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            AsyncImage(
                model = File(referenceImage.path),
                contentDescription = "Reference image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}

/**
 * 已生成图片缩略图（可选择作为参考）
 */
@Composable
fun GeneratedImageThumbnail(
    image: GenMediaEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = File(image.path),
            contentDescription = "Generated image",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        
        // 选中指示器
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
            
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(32.dp)
            )
        }
    }
}
```

#### Step 4.5: 添加必要的导入和依赖

在 ImgGenPage.kt 顶部添加：

```kotlin
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import java.io.File
```

#### Step 4.6: 更新路由（如果需要）

检查是否需要添加新的路由或参数：

```kotlin
// Screen.kt 或路由定义文件
sealed class Screen(val route: String) {
    // ... existing screens ...
    object ImgGen : Screen("img_gen")
}
```

#### Step 4.7: 测试验证

**功能测试清单**：
1. ✅ 正常生成图片（无参考图）
2. ✅ 点击已生成图片设为参考图
3. ✅ 显示参考图预览和清除按钮
4. ✅ 基于参考图编辑生成新图片
5. ✅ 编辑完成后自动清除参考图
6. ✅ 新生成的图片类型正确标记为 `TYPE_IMAGE_EDIT`
7. ✅ sourcePaths 正确保存参考图路径

**边界情况**：
- 参考图被删除后的处理
- 网络失败时的错误提示
- 不支持编辑的提供商（应抛出异常或显示提示）

---

## 执行顺序建议

按照复杂度从低到高，建议执行顺序：

1. **sync30** (FileUtils 拆分) - 最简单，纯代码组织
2. **sync33** (WebView 优化) - 中等，需要理解状态管理
3. **sync31** (移除滑动条) - 较复杂，涉及多处改动
4. **sync37 part 2** (图片编辑 UI) - 最复杂，需要完整实现 UI + VM

---

## 风险控制

### 通用风险
- **数据库迁移**：sync37 涉及 GenMediaEntity 字段变更，需确认 Room migration 已配置
- **向后兼容**：移除 UI 控件（sync31）时保留数据模型字段
- **IDE 误报**：Kotlin/Compose 的 unresolved reference 多为 IDE 索引问题，以实际编译为准

### 验证策略
每个 task 完成后立即：
```bash
./gradlew assembleDebug
```

确保编译通过后再进行下一个 task。

---

## 提交规范

每个 task 独立提交，commit message 格式：

```
feat: <简短描述> (sync<N>)

- 具体改动点 1
- 具体改动点 2
- 具体改动点 3

注意：<重要说明>
```

示例：
```
feat: 文件工具函数模块化拆分 (sync30)

- 创建 FileExtensions.kt 存放扩展函数
- 创建 PathUtils.kt 存放路径处理工具
- FileUtils.kt 保留核心文件操作函数
- 更新所有引用点的 import 路径
```

---

## 完成标准

所有 4 个 task 完成后：
- ✅ 编译无错误
- ✅ 功能测试通过
- ✅ Git 历史清晰，每个 sync 独立提交
- ✅ 与 rikkahub 的功能对齐（允许 EterUee 定制化差异）
