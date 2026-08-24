package top.funcun.companion.plugin.hello

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.funcun.companion.sdk.Plugin
import top.funcun.companion.sdk.PluginContext
import top.funcun.companion.sdk.event.AppEvent
import top.funcun.companion.sdk.slot.UISlot
import top.funcun.companion.sdk.util.PluginId
import top.funcun.companion.sdk.util.SemVer

/**
 * 示例插件。
 * 演示如何通过 SDK 注册事件监听和 UI 插槽。
 */
class HelloPlugin : Plugin {

    override val id = PluginId("top.funcun.companion.plugin.hello")
    override val name = "示例插件"
    override val version = SemVer(1, 0, 0)
    override val description = "一个简单的示例插件"
    override val icon = 0
    override val dependencies = emptyList<PluginId>()
    override val permissions = emptyList<String>()

    override suspend fun onLoad(context: PluginContext) {
        Log.i(TAG, "HelloPlugin loaded!")

        // 订阅专注开始事件
        context.eventBus.subscribe<AppEvent.FocusStarted> { event ->
            Log.i(TAG, "用户开始了 ${event.targetMinutes} 分钟的专注")
        }

        // 订阅好感度变化事件
        context.eventBus.subscribe<AppEvent.AffectionChanged> { event ->
            Log.i(TAG, "好感度变化: ${event.oldValue} → ${event.newValue} (${event.reason})")
        }

        // 注册一个 UI 组件到首页卡片插槽
        context.registerUI(UISlot.HOME_CARD) {
            HelloGreeting()
        }
    }

    override suspend fun onEnable() {
        Log.i(TAG, "HelloPlugin enabled")
    }

    override suspend fun onDisable() {
        Log.i(TAG, "HelloPlugin disabled")
    }

    override suspend fun onUnload() {
        Log.i(TAG, "HelloPlugin unloaded")
    }

    companion object {
        private const val TAG = "HelloPlugin"
    }
}

@Composable
fun HelloGreeting() {
    Column(
        modifier = Modifier.padding(16.dp),
    ) {
        Text(
            text = "天依今天心情很好～",
        )
    }
}
