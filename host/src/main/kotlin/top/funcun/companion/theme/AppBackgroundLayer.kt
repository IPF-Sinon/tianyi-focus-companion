package top.funcun.companion.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter

/**
 * 全局背景层（对标 FolkPatch BackgroundLayer）。
 *
 * - 底色：MaterialTheme background
 * - 有背景图时：Image（Crop）+ 可选 blur + dim 遮罩
 *   - blur 仅在 Android 12+ 生效（Modifier.blur）
 *   - dim 按 isDualBackgroundDimEnabled 分别取 day/night，否则取单值
 *
 * 由 TianyiThemeWithBackground 挂在所有界面之下，保证主题背景全局生效。
 */
@Composable
fun AppBackgroundLayer() {
    val context = LocalContext.current
    val cfg = ThemeManager.config
    val file = remember { ThemeManager.backgroundFile(context) }
    val systemDark = isSystemInDarkTheme()
    val dark = if (cfg.nightModeFollowSys) systemDark else cfg.nightModeEnabled

    val dim = when {
        !cfg.isDualBackgroundDimEnabled -> cfg.backgroundDim
        dark -> cfg.backgroundNightDim
        else -> cfg.backgroundDayDim
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (file != null) {
            val base = Modifier
                .fillMaxSize()
                .let { if (cfg.backgroundBlur > 0f) it.blur(cfg.backgroundBlur.dp) else it }
            Image(
                painter = rememberAsyncImagePainter(model = file),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = cfg.backgroundOpacity,
                modifier = base,
            )
            // dim 遮罩
            if (dim > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = dim.coerceIn(0f, 1f))),
                )
            }
        }
    }
}