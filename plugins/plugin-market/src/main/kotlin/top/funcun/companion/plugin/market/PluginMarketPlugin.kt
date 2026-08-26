package top.funcun.companion.plugin.market

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.funcun.companion.sdk.Plugin
import top.funcun.companion.sdk.PluginContext
import top.funcun.companion.sdk.slot.UISlot
import top.funcun.companion.sdk.util.PluginId
import top.funcun.companion.sdk.util.SemVer

/**
 * 插件市场。
 * 浏览、下载、安装社区插件。
 */
class PluginMarketPlugin : Plugin {

    override val id = PluginId("top.funcun.companion.plugin.market")
    override val name = "插件市场"
    override val version = SemVer(1, 0, 0)
    override val description = "浏览和安装社区插件"
    override val icon = 0
    override val dependencies = emptyList<PluginId>()
    override val permissions = emptyList<String>()

    private lateinit var ctx: PluginContext

    override suspend fun onLoad(context: PluginContext) {
        ctx = context
        Log.i(TAG, "PluginMarketPlugin loaded")

        // 注册插件市场 UI 到设置页
        context.registerUI(UISlot.SETTINGS_SECTION) {
            PluginMarketSection()
        }
    }

    override suspend fun onEnable() {}
    override suspend fun onDisable() {}
    override suspend fun onUnload() {}

    companion object {
        private const val TAG = "PluginMarketPlugin"
    }
}

data class PluginItem(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val version: String,
    val isInstalled: Boolean = false,
)

@Composable
fun PluginMarketSection() {
    val plugins = remember {
        listOf(
            PluginItem("com.tianyi.plugin.weather", "天依天气", "让天依告诉你今天的天气", "天依社区", "1.0.0"),
            PluginItem("com.tianyi.plugin.music", "学习音乐", "专注时播放轻音乐", "天依社区", "1.2.0"),
            PluginItem("com.tianyi.plugin.posture", "坐姿提醒", "检测坐姿并提醒", "天依社区", "0.5.0"),
        )
    }

    // FolkPatch 风格：标题 + surfaceContainer 大圆角卡片，行内分隔
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "插件市场",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
        ) {
            Column {
                plugins.forEachIndexed { index, plugin ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = plugin.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = plugin.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "${plugin.author} · v${plugin.version}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        FilledTonalButton(
                            onClick = {
                                // 下载并安装插件
                                Log.i("PluginMarket", "Installing plugin: ${plugin.id}")
                            },
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text(if (plugin.isInstalled) "已安装" else "安装")
                        }
                    }
                }
            }
        }
    }
}
