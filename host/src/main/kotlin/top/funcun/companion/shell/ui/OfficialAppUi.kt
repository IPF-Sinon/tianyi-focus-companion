package top.funcun.companion.shell.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import top.funcun.companion.App

/**
 * 官方原生界面（无 UI 覆写插件时使用）。
 */
@Composable
fun OfficialAppUi() {
    var tab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    val pluginManager = remember { App.instance.pluginManager }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                AppTab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = {
                            Icon(
                                imageVector = if (tab == t) t.filled else t.outlined,
                                contentDescription = t.label,
                            )
                        },
                        label = { Text(t.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tab",
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) { t ->
            Box(Modifier.fillMaxSize()) {
                when (t) {
                    AppTab.HOME -> HomeScreen()
                    AppTab.STATS -> StatsScreen()
                    AppTab.PLUGINS -> PluginScreen(pluginManager)
                    AppTab.SETTINGS -> SettingsScreen(pluginManager)
                }
            }
        }
    }
}

enum class AppTab(
    val label: String,
    val filled: ImageVector,
    val outlined: ImageVector,
) {
    HOME("专注", Icons.Filled.Home, Icons.Outlined.Home),
    STATS("统计", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    PLUGINS("插件", Icons.Filled.Extension, Icons.Outlined.Extension),
    SETTINGS("设置", Icons.Filled.Settings, Icons.Outlined.Settings),
}