package com.eterultimate.eteruee.ui.theme

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlinx.serialization.Serializable
import com.eterultimate.eteruee.ui.hooks.rememberAmoledDarkMode
import com.eterultimate.eteruee.ui.hooks.rememberColorMode
import com.eterultimate.eteruee.ui.hooks.rememberUserSettingsState

private val ExtendLightColors = lightExtendColors()
private val ExtendDarkColors = darkExtendColors()
val LocalExtendColors = compositionLocalOf { ExtendLightColors }

val LocalDarkMode = compositionLocalOf { false }

private val AMOLED_DARK_BACKGROUND = Color(0xFF000000)

// === 璧涘崥鏈嬪厠纭紪鐮侀鑹?- 绾粦搴?+ RGB绾壊 ===
private val PureRed = Color(0xFFFF0000)
private val PureGreen = Color(0xFF00FF00)
private val PureBlue = Color(0xFF0000FF)
private val PureBlack = Color(0xFF000000)
private val PureWhite = Color(0xFFFFFFFF)
private val DimWhite = Color(0xFF888888)

// 鏆楄壊妯″紡 - 绾粦鑳屾櫙锛岀函鐧界嚎鏉★紝RGB绾壊鐐圭紑
private val CyberDarkScheme = darkColorScheme(
    primary = PureRed,
    onPrimary = PureBlack,
    primaryContainer = Color(0xFFFF0000).copy(alpha = 0.15f),
    onPrimaryContainer = PureRed,
    secondary = PureGreen,
    onSecondary = PureBlack,
    secondaryContainer = Color(0xFF00FF00).copy(alpha = 0.15f),
    onSecondaryContainer = PureGreen,
    tertiary = PureBlue,
    onTertiary = PureBlack,
    tertiaryContainer = Color(0xFF0000FF).copy(alpha = 0.15f),
    onTertiaryContainer = PureBlue,
    error = PureRed,
    onError = PureBlack,
    errorContainer = Color(0xFFFF0000).copy(alpha = 0.15f),
    onErrorContainer = PureRed,
    background = PureBlack,
    onBackground = PureWhite,
    surface = PureBlack,
    onSurface = PureWhite,
    surfaceVariant = Color(0xFF111111),
    onSurfaceVariant = DimWhite,
    outline = PureWhite,
    outlineVariant = Color(0xFF333333),
    scrim = PureBlack,
    inverseSurface = PureWhite,
    inverseOnSurface = PureBlack,
    inversePrimary = PureRed,
    surfaceDim = PureBlack,
    surfaceBright = Color(0xFF111111),
    surfaceContainerLowest = PureBlack,
    surfaceContainerLow = PureBlack,
    surfaceContainer = PureBlack,
    surfaceContainerHigh = Color(0xFF111111),
    surfaceContainerHighest = Color(0xFF222222),
)

// 浜壊妯″紡 - 鍚屾牱浣跨敤绾粦搴曪紙璧涘崥鏈嬪厠鏃犱寒鑹诧級
private val CyberLightScheme = lightColorScheme(
    primary = PureRed,
    onPrimary = PureBlack,
    primaryContainer = Color(0xFFFF0000).copy(alpha = 0.15f),
    onPrimaryContainer = PureRed,
    secondary = PureGreen,
    onSecondary = PureBlack,
    secondaryContainer = Color(0xFF00FF00).copy(alpha = 0.15f),
    onSecondaryContainer = PureGreen,
    tertiary = PureBlue,
    onTertiary = PureBlack,
    tertiaryContainer = Color(0xFF0000FF).copy(alpha = 0.15f),
    onTertiaryContainer = PureBlue,
    error = PureRed,
    onError = PureBlack,
    errorContainer = Color(0xFFFF0000).copy(alpha = 0.15f),
    onErrorContainer = PureRed,
    background = PureBlack,
    onBackground = PureWhite,
    surface = PureBlack,
    onSurface = PureWhite,
    surfaceVariant = Color(0xFF111111),
    onSurfaceVariant = DimWhite,
    outline = PureWhite,
    outlineVariant = Color(0xFF333333),
    scrim = PureBlack,
    inverseSurface = PureWhite,
    inverseOnSurface = PureBlack,
    inversePrimary = PureRed,
    surfaceDim = PureBlack,
    surfaceBright = Color(0xFF111111),
    surfaceContainerLowest = PureBlack,
    surfaceContainerLow = PureBlack,
    surfaceContainer = PureBlack,
    surfaceContainerHigh = Color(0xFF111111),
    surfaceContainerHighest = Color(0xFF222222),
)

@Serializable
enum class ColorMode {
    SYSTEM,
    LIGHT,
    DARK
}

@Composable
fun RikkahubTheme(
    content: @Composable () -> Unit
) {
    val settings by rememberUserSettingsState()

    val colorMode by rememberColorMode()
    val darkTheme = when (colorMode) {
        ColorMode.SYSTEM -> isSystemInDarkTheme()
        ColorMode.LIGHT -> false
        ColorMode.DARK -> true
    }
    val amoledDarkMode by rememberAmoledDarkMode()

    // 寮哄埗浣跨敤璧涘崥鏈嬪厠涓婚锛屼笉璧颁换浣曚腑闂村眰
    val colorScheme = if (darkTheme) CyberDarkScheme else CyberLightScheme
    val colorSchemeConverted = remember(darkTheme, amoledDarkMode, colorScheme) {
        if (darkTheme && amoledDarkMode) {
            colorScheme.copy(
                background = AMOLED_DARK_BACKGROUND,
                surface = AMOLED_DARK_BACKGROUND,
            )
        } else {
            colorScheme
        }
    }
    val extendColors = if (darkTheme) ExtendDarkColors else ExtendLightColors

    // 璋冭瘯鏃ュ織锛氱‘璁や富棰樺姞杞?
    Log.d("RikkahubTheme", "Theme loaded: dark=$darkTheme, amoled=$amoledDarkMode, " +
            "bg=${colorSchemeConverted.background}, primary=${colorSchemeConverted.primary}")

    // 鏇存柊鐘舵€佹爮鍥炬爣棰滆壊
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalDarkMode provides darkTheme,
        LocalExtendColors provides extendColors,
        LocalOverscrollFactory provides null
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorSchemeConverted,
            typography = Typography,
            shapes = CyberpunkShapes,
            content = content,
            motionScheme = MotionScheme.expressive()
        )
    }
}

val MaterialTheme.extendColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendColors.current

