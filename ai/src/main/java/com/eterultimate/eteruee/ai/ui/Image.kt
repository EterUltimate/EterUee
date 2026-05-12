package com.eterultimate.eteruee.ai.ui

import kotlinx.serialization.Serializable

@Serializable
data class ImageGenerationResult(
    val items: List<ImageGenerationItem>, // 一个item代表一个图片
)

@Serializable
data class ImageGenerationItem(
    val data: String,
    val mimeType: String,
)

@Serializable
enum class ImageAspectRatio {
    SQUARE,
    LANDSCAPE,
    PORTRAIT
}

@Serializable
data class VideoGenerationResult(
    val items: List<VideoGenerationItem>,
)

@Serializable
data class VideoGenerationItem(
    val videoUrl: String,
    val coverUrl: String? = null,
)

