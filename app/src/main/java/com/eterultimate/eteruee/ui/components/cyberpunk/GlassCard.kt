package com.eterultimate.eteruee.ui.components.cyberpunk

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eterultimate.eteruee.ui.theme.presets.NeonCyan
import com.eterultimate.eteruee.ui.theme.presets.PanelBlack

/**
 * 纭湕璧涘崥鏈嬪厠闈㈡澘
 * 鐩磋 + 閿愬埄杈规 + 瑙掓爣瑁呴グ锛屾棤鍦嗚
 */
@Composable
fun CyberpunkPanel(
    modifier: Modifier = Modifier,
    backgroundColor: Color = PanelBlack.copy(alpha = 0.85f),
    borderColor: Color = NeonCyan.copy(alpha = 0.4f),
    borderWidth: Dp = 1.dp,
    blurRadius: Dp = 0.dp,
    contentPadding: Dp = 16.dp,
    showBrackets: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(color = backgroundColor)
            .border(width = borderWidth, color = borderColor)
            .then(
                if (showBrackets) Modifier.cornerBrackets(
                    color = borderColor,
                    strokeWidth = 2.dp,
                    bracketLength = 10.dp
                ) else Modifier
            )
    ) {
        // 鑳屾櫙妯＄硦灞?(Android 12+ 鏀寔)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadius > 0.dp) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(blurRadius)
            )
        }

        // 鍐呭灞?
        Box(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * 绠€鍖栫増闈㈡澘
 */
@Composable
fun PanelSimple(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    CyberpunkPanel(
        modifier = modifier,
        content = content
    )
}

