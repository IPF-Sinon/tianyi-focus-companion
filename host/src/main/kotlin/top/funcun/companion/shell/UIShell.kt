package top.funcun.companion.shell

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import top.funcun.companion.App
import top.funcun.companion.plugin.homehtml.ThemeBackBridge
import top.funcun.companion.sdk.slot.UISlot

/**
 * UI 壳。
 *
 * 主界面完全由主题插件（home-html）通过 WebView 渲染，
 * 这里仅把 HOME_TOP 插槽内容全屏呈现（主题自绘底栏与全部页面）。
 *
 * 返回键先交给主题处理（收起配置面板/切回首页），主题不消费才退出应用。
 */
@Composable
fun UIShell() {
    val pluginManager = remember { App.instance.pluginManager }
    val context = LocalContext.current

    fun getSlotContents(slot: UISlot): List<@Composable () -> Unit> =
        pluginManager.getSlotContents(slot)

    // 主题插件注册到 HOME_TOP，全屏接管界面
    val homeContents = getSlotContents(UISlot.HOME_TOP)

    // 返回键：询问主题是否消费；未消费则退出
    BackHandler(enabled = homeContents.isNotEmpty()) {
        ThemeBackBridge.dispatchBack { consumed ->
            if (!consumed) {
                (context as? Activity)?.finish()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (homeContents.isNotEmpty()) {
            homeContents.forEach { it() }
        } else {
            PlaceholderScreen()
        }
    }
}

@Composable
private fun PlaceholderScreen() {
    Surface(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "未安装主题插件（home-html）\n主界面无法渲染",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}