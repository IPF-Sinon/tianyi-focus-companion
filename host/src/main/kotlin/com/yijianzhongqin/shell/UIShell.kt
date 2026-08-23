package com.yijianzhongqin.shell

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.yijianzhongqin.App
import com.yijianzhongqin.sdk.event.AppEvent
import com.yijianzhongqin.sdk.slot.UISlot
import com.yijianzhongqin.shell.ui.HomeScreen
import com.yijianzhongqin.shell.ui.FocusScreen
import com.yijianzhongqin.shell.ui.SettingsScreen
import com.yijianzhongqin.shell.ui.StatsScreen

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
