# 图片生成相册删除交互重构

## 概述

本次重构将图片生成相册(Image Gallery)的删除功能从列表项直接操作迁移到预览界面的"更多"菜单中,提升了用户体验和界面整洁度。

## 修改内容

### 1. 移除列表项上的删除按钮

**文件**: `app/src/main/java/com/eterultimate/eteruee/ui/pages/imggen/ImgGenPage.kt`

**位置**: `ImageGalleryScreen` 组件中的卡片底部操作栏(原第690-700行)

**变更**:
- ❌ 移除了直接显示在列表项上的删除按钮(IconButton with Delete01 icon)
- ✅ 保留了复制提示词和保存图片两个常用操作按钮

**原因**: 
- 减少误触风险
- 保持列表项界面简洁
- 删除是危险操作,应该放在更隐蔽的位置

### 2. 增强 ImagePreviewDialog 组件

**文件**: `app/src/main/java/com/eterultimate/eteruee/ui/components/ui/ImagePreviewDialog.kt`

**新增功能**:

#### 2.1 添加删除回调参数
```kotlin
fun ImagePreviewDialog(
    images: List<String>,
    onDismissRequest: () -> Unit,
    onDeleteImage: ((Int) -> Unit)? = null,  // 新增:可选的删除回调
)
```

#### 2.2 右上角"更多"菜单
- 添加了 `MoreVertical` 图标按钮(三个点)
- 点击后显示下拉菜单(DropdownMenu)
- 菜单中包含"删除"选项,使用红色文字和删除图标突出显示

#### 2.3 删除逻辑
```kotlin
DropdownMenuItem(
    text = { Text("删除", color = MaterialTheme.colorScheme.error) },
    leadingIcon = {
        Icon(
            HugeIcons.Delete01,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )
    },
    onClick = {
        showMoreMenu = false
        onDeleteImage(state.currentPage)  // 传递当前图片索引
    }
)
```

**新增导入**:
- `androidx.compose.material3.DropdownMenu`
- `androidx.compose.material3.DropdownMenuItem`
- `androidx.compose.material3.Text`
- `androidx.compose.runtime.getValue`
- `androidx.compose.runtime.mutableStateOf`
- `androidx.compose.runtime.remember`
- `androidx.compose.runtime.setValue`
- `me.rerere.hugeicons.stroke.Delete01`
- `me.rerere.hugeicons.stroke.MoreVertical`

### 3. 更新调用处

**文件**: `app/src/main/java/com/eterultimate/eteruee/ui/pages/imggen/ImgGenPage.kt`

#### 3.1 ImageGenScreen 中的预览对话框(第302-311行)
```kotlin
if (showPreview) {
    ImagePreviewDialog(
        images = listOf(File(image.filePath).toUri().toString()),
        onDismissRequest = { showPreview = false },
        onDeleteImage = { index ->
            vm.deleteImage(image)
            showPreview = false
        }
    )
}
```

#### 3.2 ImageGalleryScreen 中的预览对话框(第698-707行)
```kotlin
if (showPreview) {
    ImagePreviewDialog(
        images = listOf(File(it.filePath).toUri().toString()),
        onDismissRequest = { showPreview = false },
        onDeleteImage = { index ->
            vm.deleteImage(it)
            showPreview = false
        }
    )
}
```

## 用户体验改进

### Before (之前)
- ❌ 删除按钮直接显示在列表项上,容易误触
- ❌ 三个操作按钮(复制、保存、删除)挤在一起,界面拥挤
- ❌ 没有二次确认机制

### After (之后)
- ✅ 删除功能隐藏在"更多"菜单中,需要主动点击才能看到
- ✅ 列表项只保留最常用的两个操作(复制、保存)
- ✅ 预览界面提供了完整的图片查看体验,删除操作更加自然
- ✅ 删除后立即关闭预览对话框,避免空状态

## 技术细节

### 状态管理
- 使用 `mutableStateOf(false)` 管理菜单展开状态
- 点击删除后自动关闭菜单(`showMoreMenu = false`)
- 删除后自动关闭预览对话框(`showPreview = false`)

### 布局设计
- **右上角**: "更多"菜单按钮(白色图标,适配深色背景)
- **底部中央**: 下载/保存按钮(保持原有位置)
- 使用 `zIndex(1f)` 确保控件始终在图片上方

### 向后兼容
- `onDeleteImage` 参数是可选的(`? = null`)
- 只有当传入删除回调时才显示删除菜单项
- 其他使用该组件的地方不受影响

## 测试建议

1. **基本功能测试**
   - [ ] 点击图片打开预览对话框
   - [ ] 点击右上角"更多"按钮,菜单正常展开
   - [ ] 点击"删除"选项,图片被正确删除
   - [ ] 删除后预览对话框自动关闭
   - [ ] 返回列表,已删除的图片不再显示

2. **边界情况测试**
   - [ ] 删除最后一张图片后的行为
   - [ ] 删除第一张图片后的行为
   - [ ] 快速连续点击删除按钮
   - [ ] 在菜单展开时点击其他地方,菜单应关闭

3. **UI/UX 测试**
   - [ ] "更多"按钮在深色/浅色主题下都清晰可见
   - [ ] 删除菜单项使用红色突出显示
   - [ ] 菜单动画流畅
   - [ ] 触摸反馈正常

## 相关文件

- `app/src/main/java/com/eterultimate/eteruee/ui/pages/imggen/ImgGenPage.kt`
- `app/src/main/java/com/eterultimate/eteruee/ui/components/ui/ImagePreviewDialog.kt`
- `app/src/main/java/com/eterultimate/eteruee/ui/pages/imggen/ImgGenVM.kt` (deleteImage 方法)

## 注意事项

⚠️ **编译错误说明**: 

当前项目存在一些与本次修改无关的编译错误,这些是由于 rikkahub 2.2.0-2.2.1 同步时,`ImgGenVM.kt` 中缺少视频生成相关的属性和方法导致的:
- `isVideoMode`
- `videoDuration`
- `videoResolution`
- `videoAspectRatio`
- `generateAudio`
- `setVideoMode()`
- `updateVideoDuration()`
- `updateVideoResolution()`
- `updateVideoAspectRatio()`
- `updateGenerateAudio()`

这些错误需要在后续的视频生成功能实现时修复,不影响本次删除交互重构的正确性。

## 提交信息建议

```
refactor(imggen): move delete action to preview dialog menu

- Remove delete button from gallery list items
- Add "more" menu (three dots) in ImagePreviewDialog top-right corner
- Integrate delete functionality into the dropdown menu
- Pass onDeleteImage callback to preview dialogs in both screens
- Improve UX by reducing accidental deletions and cleaning up UI

Closes #xxx
```
