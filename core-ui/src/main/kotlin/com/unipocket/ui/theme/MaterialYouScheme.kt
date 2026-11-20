package com.unipocket.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamiccolor.DynamicColor
import com.materialkolor.dynamiccolor.MaterialDynamicColors
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.SchemeTonalSpot

/**
 * 由**一个种子色**算出整套 Material 3 配色（浅色/深色）。
 *
 * 用 Google `material-color-utilities`（MIT，Kotlin 移植）：HCT 把种子色摊成调色板，
 * 再按 M3 规范取各角色色值——和 Android 系统动态取色同一套算法，
 * 所以给定任意种子色都能得到"原生感"配色，而不是随手挑的色。
 *
 * 采用的是最标准的 TONAL_SPOT 变体（系统默认风格）。
 */
object MaterialYouScheme {

    fun fromSeed(seedArgb: Int, dark: Boolean): ColorScheme {
        val scheme = SchemeTonalSpot(Hct.fromInt(seedArgb), dark, 0.0)
        val colors = MaterialDynamicColors()

        fun role(color: DynamicColor): Color = Color(color.getArgb(scheme))

        return ColorScheme(
            primary = role(colors.primary()),
            onPrimary = role(colors.onPrimary()),
            primaryContainer = role(colors.primaryContainer()),
            onPrimaryContainer = role(colors.onPrimaryContainer()),
            inversePrimary = role(colors.inversePrimary()),

            secondary = role(colors.secondary()),
            onSecondary = role(colors.onSecondary()),
            secondaryContainer = role(colors.secondaryContainer()),
            onSecondaryContainer = role(colors.onSecondaryContainer()),

            tertiary = role(colors.tertiary()),
            onTertiary = role(colors.onTertiary()),
            tertiaryContainer = role(colors.tertiaryContainer()),
            onTertiaryContainer = role(colors.onTertiaryContainer()),

            background = role(colors.background()),
            onBackground = role(colors.onBackground()),

            surface = role(colors.surface()),
            onSurface = role(colors.onSurface()),
            surfaceVariant = role(colors.surfaceVariant()),
            onSurfaceVariant = role(colors.onSurfaceVariant()),
            surfaceTint = role(colors.surfaceTint()),
            inverseSurface = role(colors.inverseSurface()),
            inverseOnSurface = role(colors.inverseOnSurface()),
            surfaceBright = role(colors.surfaceBright()),
            surfaceDim = role(colors.surfaceDim()),
            surfaceContainer = role(colors.surfaceContainer()),
            surfaceContainerHigh = role(colors.surfaceContainerHigh()),
            surfaceContainerHighest = role(colors.surfaceContainerHighest()),
            surfaceContainerLow = role(colors.surfaceContainerLow()),
            surfaceContainerLowest = role(colors.surfaceContainerLowest()),

            error = role(colors.error()),
            onError = role(colors.onError()),
            errorContainer = role(colors.errorContainer()),
            onErrorContainer = role(colors.onErrorContainer()),

            outline = role(colors.outline()),
            outlineVariant = role(colors.outlineVariant()),
            scrim = role(colors.scrim()),
        )
    }
}
