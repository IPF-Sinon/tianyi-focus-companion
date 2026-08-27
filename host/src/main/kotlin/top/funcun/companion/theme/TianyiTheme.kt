package top.funcun.companion.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * 依见钟勤 Material3 主题。
 *
 * 配色来源（按 [ThemeManager.config] 决定）：
 * - 动态取色（Android 12+ 且 useSystemDynamicColor）
 * - 否则由种子色（customColor）+ 风格（colorStyle）经 MaterialKolor 生成
 *
 * 深色模式：nightModeFollowSys 跟随系统，否则用 nightModeEnabled。
 * 圆角：由 cardCornerRadius 决定。
 */
@Composable
fun TianyiTheme(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val cfg = ThemeManager.config

    val systemDark = isSystemInDarkTheme()
    val darkTheme = if (cfg.nightModeFollowSys) systemDark else cfg.nightModeEnabled

    val colorScheme = when {
        cfg.useSystemDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> ColorSchemeGenerator.generate(cfg.customColor, darkTheme, cfg.colorStyle)
    }

    val radius = cfg.cardCornerRadius.dp
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape((radius.value * 0.7f).dp),
        large = RoundedCornerShape(radius),
        extraLarge = RoundedCornerShape(radius + 8.dp),
    )

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = shapes,
        typography = TianyiTypography,
        content = content,
    )
}