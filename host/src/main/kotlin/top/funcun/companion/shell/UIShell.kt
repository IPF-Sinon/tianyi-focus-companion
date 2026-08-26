package top.funcun.companion.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import top.funcun.companion.App
import top.funcun.companion.sdk.event.AppEvent
import top.funcun.companion.sdk.slot.UISlot
import top.funcun.companion.shell.ui.HomeScreen
import top.funcun.companion.shell.ui.FocusScreen
import top.funcun.companion.shell.ui.SettingsScreen
import top.funcun.companion.shell.ui.StatsScreen

/**
 * UI 壳。
 * 负责导航与插槽渲染，插件注册的 UI 组件会渲染到对应位置。
 */
@Composable
fun UIShell() {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    val pluginManager = remember { App.instance.pluginManager }

    fun getSlotContents(slot: UISlot): List<@Composable () -> Unit> =
        pluginManager.getSlotContents(slot)

    // 系统返回键导航：非主页 → 返回主页；主页 → 退出
    BackHandler(enabled = currentScreen != Screen.HOME) {
        currentScreen = Screen.HOME
    }

    // 监听专注完成/中止事件，自动返回首页
    LaunchedEffect(Unit) {
        pluginManager.getEventBus().subscribe<AppEvent.FocusCompleted> {
            currentScreen = Screen.HOME
        }
        pluginManager.getEventBus().subscribe<AppEvent.FocusAborted> {
            currentScreen = Screen.HOME
        }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen_transition",
    ) { screen ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (screen) {
                Screen.HOME -> HomeScreen(
                    onStartFocus = { currentScreen = Screen.FOCUS },
                    onStats = { currentScreen = Screen.STATS },
                    onSettings = { currentScreen = Screen.SETTINGS },
                    getSlotContents = ::getSlotContents,
                )
                Screen.FOCUS -> FocusScreen(
                    getSlotContents = ::getSlotContents,
                )
                Screen.STATS -> StatsScreen(
                    onBack = { currentScreen = Screen.HOME },
                    getSlotContents = ::getSlotContents,
                )
                Screen.SETTINGS -> SettingsScreen(
                    onBack = { currentScreen = Screen.HOME },
                    getSlotContents = ::getSlotContents,
                )
            }
        }
    }
}

enum class Screen {
    HOME,
    FOCUS,
    STATS,
    SETTINGS,
}
