package top.funcun.companion.shell.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.funcun.companion.theme.ThemeManager

/**
 * 底部占位：悬浮底栏时避让胶囊高度 + 导航栏 inset，常规底栏时留小间距。
 * 各滚动页面在内容末尾调用。
 */
@Composable
fun BottomBarSpacer() {
    val floating = ThemeManager.config.navBarStyle == "floating"
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val h = if (floating) 96.dp + navInset else 16.dp
    Spacer(Modifier.height(h))
}