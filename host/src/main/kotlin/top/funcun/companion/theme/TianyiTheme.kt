package top.funcun.companion.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * 依见钟勤 Material3 主题。
 *
 * 支持：
 * - 浅色/暗色自动切换（跟随系统）
 * - 动态取色（Android 12+ Material You），通过 [dynamicColor] 开启
 * - 默认蓝白 Tianyi 品牌色作为 fallback
 */
private val TianyiLightColorScheme = lightColorScheme(
    primary = TianyiColors.Primary,
    onPrimary = TianyiColors.TextOnPrimary,
    primaryContainer = TianyiColors.CardSurface,
    onPrimaryContainer = TianyiColors.Primary,
    secondary = TianyiColors.PrimaryLight,
    onSecondary = TianyiColors.TextPrimary,
    tertiary = TianyiColors.PrimaryLight,
    background = TianyiColors.Background,
    onBackground = TianyiColors.TextPrimary,
    surface = TianyiColors.Surface,
    onSurface = TianyiColors.TextPrimary,
    surfaceVariant = TianyiColors.CardSurface,
    onSurfaceVariant = TianyiColors.TextSecondary,
    surfaceContainer = TianyiColors.CardSurface,
    surfaceContainerLow = TianyiColors.Surface,
    surfaceContainerHigh = TianyiColors.CardSurface,
    error = TianyiColors.Error,
    onError = TianyiColors.TextOnPrimary,
    outline = TianyiColors.Divider,
    outlineVariant = TianyiColors.Divider,
)

private val TianyiDarkColorScheme = darkColorScheme(
    primary = TianyiDarkColors.Primary,
    onPrimary = TianyiDarkColors.TextOnPrimary,
    primaryContainer = TianyiDarkColors.Surface,
    onPrimaryContainer = TianyiDarkColors.PrimaryDark,
    secondary = TianyiDarkColors.PrimaryLight,
    onSecondary = TianyiDarkColors.TextPrimary,
    tertiary = TianyiDarkColors.Primary,
    background = TianyiDarkColors.Background,
    onBackground = TianyiDarkColors.TextPrimary,
    surface = TianyiDarkColors.Surface,
    onSurface = TianyiDarkColors.TextPrimary,
    surfaceVariant = TianyiDarkColors.CardSurface,
    onSurfaceVariant = TianyiDarkColors.TextSecondary,
    surfaceContainer = TianyiDarkColors.CardSurface,
    surfaceContainerLow = TianyiDarkColors.Surface,
    surfaceContainerHigh = TianyiDarkColors.CardSurface,
    error = TianyiDarkColors.Error,
    onError = TianyiDarkColors.TextOnPrimary,
    outline = TianyiDarkColors.Divider,
    outlineVariant = TianyiDarkColors.Divider,
)

/**
 * 依见钟勤主题。
 *
 * @param dynamicColor 是否启用 Material You 动态取色（Android 12+）。
 *                     默认 true；设为 false 则始终使用 Tianyi 蓝白配色。
 * @param darkTheme 是否使用暗色模式。默认跟随系统。
 * @param content 内容。
 */
@Composable
fun TianyiTheme(
    dynamicColor: Boolean = true,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> TianyiDarkColorScheme
        else -> TianyiLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = TianyiShapes.toMaterial3(),
        typography = TianyiTypography,
        content = content,
    )
}