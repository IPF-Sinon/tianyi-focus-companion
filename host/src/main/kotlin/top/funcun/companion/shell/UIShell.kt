package top.funcun.companion.shell

import androidx.compose.runtime.*
import top.funcun.companion.App
import top.funcun.companion.shell.ui.OfficialAppUi

/**
 * UI 壳。
 *
 * 分派逻辑：
 * - 有生效的 UI 覆写插件 → 渲染该插件的界面（第三方接管）
 * - 否则 → 官方原生 Compose 界面（+ 主题引擎）
 */
@Composable
fun UIShell() {
    val pluginManager = remember { App.instance.pluginManager }
    val uiOverride = remember { pluginManager.getActiveUiOverride() }

    if (uiOverride != null) {
        // 第三方界面插件接管
        uiOverride.RenderApp(pluginManager.uiHostApi)
        return
    }

    OfficialAppUi()
}