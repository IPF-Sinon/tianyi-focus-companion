package top.funcun.companion.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * 依见钟勤 MIUI 风格主题。
 * 浅色、暖粉、大圆角、无阴影。
 */
private val TianyiColorScheme = lightColorScheme(
    primary = TianyiColors.Primary,
    onPrimary = TianyiColors.TextOnPrimary,
    primaryContainer = TianyiColors.PrimaryLight,
    onPrimaryContainer = TianyiColors.PrimaryDark,
    secondary = TianyiColors.PrimaryLight,
    onSecondary = TianyiColors.TextPrimary,
    background = TianyiColors.Background,
    onBackground = TianyiColors.TextPrimary,
    surface = TianyiColors.Surface,
    onSurface = TianyiColors.TextPrimary,
    surfaceVariant = TianyiColors.Background,
    onSurfaceVariant = TianyiColors.TextSecondary,
    error = TianyiColors.Error,
    onError = TianyiColors.TextOnPrimary,
    outline = TianyiColors.Divider,
)

@Composable
fun TianyiTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = TianyiColorScheme,
        shapes = TianyiShapes.toMaterial3(),
        typography = TianyiTypography,
        content = content,
    )
}
