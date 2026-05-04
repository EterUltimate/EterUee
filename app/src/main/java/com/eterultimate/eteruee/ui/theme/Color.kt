package com.eterultimate.eteruee.ui.theme

import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class ExtendColors(
    val red1: Color,
    val red2: Color,
    val red3: Color,
    val red4: Color,
    val red5: Color,
    val red6: Color,
    val red7: Color,
    val red8: Color,
    val red9: Color,
    val red10: Color,
    val orange1: Color,
    val orange2: Color,
    val orange3: Color,
    val orange4: Color,
    val orange5: Color,
    val orange6: Color,
    val orange7: Color,
    val orange8: Color,
    val orange9: Color,
    val orange10: Color,
    val green1: Color,
    val green2: Color,
    val green3: Color,
    val green4: Color,
    val green5: Color,
    val green6: Color,
    val green7: Color,
    val green8: Color,
    val green9: Color,
    val green10: Color,
    val blue1: Color,
    val blue2: Color,
    val blue3: Color,
    val blue4: Color,
    val blue5: Color,
    val blue6: Color,
    val blue7: Color,
    val blue8: Color,
    val blue9: Color,
    val blue10: Color,
    val gray1: Color,
    val gray2: Color,
    val gray3: Color,
    val gray4: Color,
    val gray5: Color,
    val gray6: Color,
    val gray7: Color,
    val gray8: Color,
    val gray9: Color,
    val gray10: Color,
)

fun lightExtendColors(): ExtendColors = ExtendColors(
    red1 = Color(255, 236, 232),
    red2 = Color(253, 205, 197),
    red3 = Color(251, 172, 163),
    red4 = Color(249, 137, 129),
    red5 = Color(247, 101, 96),
    red6 = Color(245, 63, 63),
    red7 = Color(203, 39, 45),
    red8 = Color(161, 21, 30),
    red9 = Color(119, 8, 19),
    red10 = Color(77, 0, 10),
    orange1 = Color(255, 247, 232),
    orange2 = Color(255, 228, 186),
    orange3 = Color(255, 207, 139),
    orange4 = Color(255, 182, 93),
    orange5 = Color(255, 154, 46),
    orange6 = Color(255, 125, 0),
    orange7 = Color(210, 95, 0),
    orange8 = Color(166, 69, 0),
    orange9 = Color(121, 46, 0),
    orange10 = Color(77, 27, 0),
    green1 = Color(232, 255, 234),
    green2 = Color(175, 240, 181),
    green3 = Color(123, 225, 136),
    green4 = Color(76, 210, 99),
    green5 = Color(35, 195, 67),
    green6 = Color(0, 180, 42),
    green7 = Color(0, 154, 41),
    green8 = Color(0, 128, 38),
    green9 = Color(0, 102, 34),
    green10 = Color(0, 77, 28),
    blue1 = Color(232, 247, 255),
    blue2 = Color(195, 231, 254),
    blue3 = Color(159, 212, 253),
    blue4 = Color(123, 192, 252),
    blue5 = Color(87, 169, 251),
    blue6 = Color(52, 145, 250),
    blue7 = Color(32, 108, 207),
    blue8 = Color(17, 75, 163),
    blue9 = Color(6, 48, 120),
    blue10 = Color(0, 26, 77),
    gray1 = Color(247, 248, 250),
    gray2 = Color(242, 243, 245),
    gray3 = Color(229, 230, 235),
    gray4 = Color(201, 205, 212),
    gray5 = Color(169, 174, 184),
    gray6 = Color(134, 144, 156),
    gray7 = Color(107, 119, 133),
    gray8 = Color(78, 89, 105),
    gray9 = Color(39, 46, 59),
    gray10 = Color(29, 33, 41),
)

fun darkExtendColors(): ExtendColors = ExtendColors(
    red1 = Color(0xFF330000),
    red2 = Color(0xFF550000),
    red3 = Color(0xFF770000),
    red4 = Color(0xFF990000),
    red5 = Color(0xFFBB0000),
    red6 = Color(0xFFFF0000),
    red7 = Color(0xFFFF3333),
    red8 = Color(0xFFFF6666),
    red9 = Color(0xFFFF9999),
    red10 = Color(0xFFFFCCCC),
    orange1 = Color(0xFF331400),
    orange2 = Color(0xFF552200),
    orange3 = Color(0xFF773300),
    orange4 = Color(0xFF994400),
    orange5 = Color(0xFFBB5500),
    orange6 = Color(0xFFFF6600),
    orange7 = Color(0xFFFF8833),
    orange8 = Color(0xFFFFAA66),
    orange9 = Color(0xFFFFCC99),
    orange10 = Color(0xFFFFEECC),
    green1 = Color(0xFF003300),
    green2 = Color(0xFF005500),
    green3 = Color(0xFF007700),
    green4 = Color(0xFF009900),
    green5 = Color(0xFF00BB00),
    green6 = Color(0xFF00FF00),
    green7 = Color(0xFF33FF33),
    green8 = Color(0xFF66FF66),
    green9 = Color(0xFF99FF99),
    green10 = Color(0xFFCCFFCC),
    blue1 = Color(0xFF000033),
    blue2 = Color(0xFF000055),
    blue3 = Color(0xFF000077),
    blue4 = Color(0xFF000099),
    blue5 = Color(0xFF0000BB),
    blue6 = Color(0xFF0000FF),
    blue7 = Color(0xFF3333FF),
    blue8 = Color(0xFF6666FF),
    blue9 = Color(0xFF9999FF),
    blue10 = Color(0xFFCCCCFF),
    gray1 = Color(0xFF000000),
    gray2 = Color(0xFF0A0A0A),
    gray3 = Color(0xFF141414),
    gray4 = Color(0xFF1E1E1E),
    gray5 = Color(0xFF282828),
    gray6 = Color(0xFF404040),
    gray7 = Color(0xFF666666),
    gray8 = Color(0xFF999999),
    gray9 = Color(0xFFCCCCCC),
    gray10 = Color(0xFFFFFFFF),
)

object CustomColors {
    var black = false

    val topBarColors: TopAppBarColors
        @Composable get() {
            return if (!LocalDarkMode.current) TopAppBarDefaults.topAppBarColors(
                containerColor = colorScheme.surfaceContainer,
                scrolledContainerColor = colorScheme.surfaceContainer
            ) else TopAppBarDefaults.topAppBarColors()
        }

    val cardColors: CardColors
        @Composable get() = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainer)

    val cardColorsOnSurfaceContainer: CardColors
        @Composable get() = CardDefaults.cardColors(containerColor = colorScheme.surfaceBright)

    val listItemColors: ListItemColors
        @Composable get() = ListItemDefaults.colors(containerColor = colorScheme.surfaceBright)
}

