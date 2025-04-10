package me.rerere.rikkahub.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun AIIcon(
    path: String,
    name: String,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        "file:///android_asset/icons/$path.svg",
        contentDescription = name,
        modifier = modifier
            .clip(CircleShape)
            .size(24.dp),
    )
}

@Composable
fun AutoAIIcon(
    name: String,
    modifier: Modifier = Modifier,
) {
    val path = computeAIIconByName(name) ?: run {
        TextAvatar(name, modifier)
        return
    }
    AIIcon(
        path = path,
        name = name,
        modifier = modifier,
    )
}

private fun computeAIIconByName(name: String): String? {
    val lowerName = name.lowercase()
    return when {
        lowerName.contains("gpt") || lowerName.contains("openai") -> "openai"
        lowerName.contains("gemini") || lowerName.contains("google") -> "gemini-color"
        lowerName.contains("anthropic") -> "anthropic"
        lowerName.contains("claude") -> "claude-color"
        lowerName.contains("deepseek") -> "deepseek-color"
        lowerName.contains("grok") -> "grok"
        lowerName.contains("qwen") -> "qwen-color"
        else -> null
    }
}