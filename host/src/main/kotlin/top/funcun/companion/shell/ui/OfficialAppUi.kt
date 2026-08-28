package top.funcun.companion.shell.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import top.funcun.companion.App
import top.funcun.companion.shell.ui.components.BarTab
import top.funcun.companion.shell.ui.components.FloatingBottomBar
import top.funcun.companion.theme.ThemeManager

/**
 * 官方原生界面（无 UI 覆写插件时使用）。
 *
 * 悬浮胶囊底栏叠在内容之上（对标 FolkPatch），内容页面自行用 BottomBarSpacer 避让。
 */
@Composable
fun OfficialAppUi() {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val pluginManager = remember { App.instance.pluginManager }
    val cfg = ThemeManager.config

    val tabs = remember {
        listOf(
            BarTab("专注", Icons.Filled.Home, Icons.Outlined.Home),
            BarTab("统计", Icons.Filled.BarChart, Icons.Outlined.BarChart),
            BarTab("插件", Icons.Filled.Extension, Icons.Outlined.Extension),
            BarTab("设置", Icons.Filled.Settings, Icons.Outlined.Settings),
        )
    }

    // 返回键：非首个 Tab → 回首个 Tab
    BackHandler(enabled = index != 0) { index = 0 }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = index,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tab",
            modifier = Modifier.fillMaxSize(),
        ) { i ->
            Box(Modifier.fillMaxSize()) {
                when (i) {
                    0 -> HomeScreen()
                    1 -> StatsScreen()
                    2 -> PluginScreen(pluginManager)
                    3 -> SettingsScreen(pluginManager)
                }
            }
        }

        FloatingBottomBar(
            tabs = tabs,
            selectedIndex = index,
            onSelect = { index = it },
            floating = cfg.navBarStyle == "floating",
            compact = cfg.navBarCompact,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}