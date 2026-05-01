package com.eterultimate.eteruee.ai.ui

import kotlinx.serialization.Serializable

@Serializable
data class ImageGenerationResult(
    val items: List<ImageGenerationItem>, // 涓€涓猧tem浠ｈ〃涓€涓浘鐗?
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

