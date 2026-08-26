package top.funcun.companion.shell

import androidx.activity.compose.BackHandler
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
import top.funcun.companion.sdk.event.AppEvent
import top.funcun.companion.sdk.slot.UISlot
import top.funcun.companion.shell.ui.HomeScreen
import top.funcun.companion.shell.ui.PluginScreen
import top.funcun.companion.shell.ui.SettingsScreen
import top.funcun.companion.shell.ui.StatsScreen

/**
 * UI 壳：底部 4 Tab 导航（专注/统计/插件/设置）。
 * 对标 Web 设计稿的 .bottom-nav。
 */
@Composable
fun UIShell() {
    var currentTab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    val pluginManager = remember { App.instance.pluginManager }

    fun getSlotContents(slot: UISlot): List<@Composable () -> Unit> =
        pluginManager.getSlotContents(slot)

    // 监听专注完成/中止事件，自动返回专注 Tab
    LaunchedEffect(Unit) {
        pluginManager.getEventBus().subscribe<AppEvent.FocusCompleted> {
            currentTab = AppTab.HOME
        }
        pluginManager.getEventBus().subscribe<AppEvent.FocusAborted> {
            currentTab = AppTab.HOME
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == tab) tab.filledIcon else tab.outlinedIcon,
                                contentDescription = tab.label,
                            )
                        },
                        label = { Text(tab.label) },
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
    ) { paddingValues ->
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tab_transition",
            modifier = Modifier.padding(paddingValues),
        ) { tab ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (tab) {
                    AppTab.HOME -> HomeScreen(
                        getSlotContents = ::getSlotContents,
                    )
                    AppTab.STATS -> StatsScreen(
                        getSlotContents = ::getSlotContents,
                    )
                    AppTab.PLUGINS -> PluginScreen(
                        pluginManager = pluginManager,
                    )
                    AppTab.SETTINGS -> SettingsScreen(
                        getSlotContents = ::getSlotContents,
                    )
                }
            }
        }
    }
}

enum class AppTab(
    val label: String,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector,
) {
    HOME("专注", Icons.Filled.Home, Icons.Outlined.Home),
    STATS("统计", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    PLUGINS("插件", Icons.Filled.Extension, Icons.Outlined.Extension),
    SETTINGS("设置", Icons.Filled.Settings, Icons.Outlined.Settings),
}