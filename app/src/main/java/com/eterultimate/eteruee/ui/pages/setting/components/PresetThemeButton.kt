package com.eterultimate.eteruee.ui.pages.setting.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.eterultimate.eteruee.ui.theme.LocalDarkMode
import com.eterultimate.eteruee.ui.theme.PresetTheme
import com.eterultimate.eteruee.ui.theme.PresetThemes
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Tick01

@Composable
fun PresetThemeButton(
    theme: PresetTheme,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val darkMode = LocalDarkMode.current
    val scheme = theme.getColorScheme(darkMode)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = {
                    onClick()
                }
            )
            .padding(8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .size(56.dp)
            ) {
                drawRect(
                    color = scheme.surfaceContainerHigh,
                    size = size
                )
                drawRect(
                    color = scheme.primaryContainer,
                    size = size,
                    topLeft = Offset(
                        x = size.width / 2,
                        y = 0f
                    ),
                )
                drawRect(
                    color = scheme.secondaryContainer,
                    size = size,
                    topLeft = Offset(
                        x = 0f,
                        y = size.height / 2
                    ),
                )
                drawRect(
                    color = scheme.tertiaryContainer,
                    size = size,
                    topLeft = Offset(
                        x = size.width / 2,
                        y = size.height / 2
                    ),
                )
                drawCircle(
                    color = scheme.primary,
                    radius = if (selected) 14.dp.toPx() else 9.dp.toPx(),
                    center = Offset(
                        x = size.width / 2,
                        y = size.height / 2
                    )
                )
            }
            if (selected) {
                Icon(
                    HugeIcons.Tick01,
                    contentDescription = null,
                    tint = scheme.onPrimary
                )
            }
        }
        ProvideTextStyle(
            value = MaterialTheme.typography.labelMedium.copy(color = scheme.primary)
        ) {
            theme.name()
        }
    }
}

@Composable
fun PresetThemeButtonGroup(
    themeId: String,
    modifier: Modifier = Modifier,
    onChangeTheme: (String) -> Unit,
) {
    Column(
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            PresetThemes.fastForEach { theme ->
                key(theme.id) {
                    PresetThemeButton(
                        theme = theme,
                        selected = theme.id == themeId,
                        onClick = {
                            onChangeTheme(theme.id)
                        },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PresetThemeButtonPreview() {
    var themeId by remember { mutableStateOf("soft-glass") }
    PresetThemeButtonGroup(
        themeId = themeId,
        onChangeTheme = { themeId = it }
    )
}

