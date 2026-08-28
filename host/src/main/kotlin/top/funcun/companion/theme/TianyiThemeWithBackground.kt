package top.funcun.companion.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex

/**
 * 带全局背景的主题包装（对标 FolkPatch APatchThemeWithBackground）。
 *
 * - 在最底层渲染全局背景层 AppBackgroundLayer
 * - 内容盖在背景之上（zIndex 1）
 * 这样主题背景对**所有页面**生效，而非仅某几个页面。
 */
@Composable
fun TianyiThemeWithBackground(
    content: @Composable () -> Unit,
) {
    TianyiTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            AppBackgroundLayer()
            Box(modifier = Modifier.fillMaxSize().zIndex(1f)) {
                content()
            }
        }
    }
}