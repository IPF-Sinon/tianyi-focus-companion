package top.funcun.companion.shell.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.funcun.companion.shell.PluginManager

/** 插件展示项 */
data class PluginListItem(
    val id: Int,
    val name: String,
    val description: String,
    val version: String,
    val icon: String,
    val enabled: Boolean,
)

/**
 * 插件页。
 *
 * 展示已加载插件的真实信息（名称/描述/版本/启用状态）。
 * 未实现的插件管理功能（安装/卸载/动态开关）不展示假控件。
 */
@Composable
fun PluginScreen(
    pluginManager: PluginManager,
) {
    val plugins = remember {
        pluginManager.getBuiltinPluginInfo().mapIndexed { index, info ->
            PluginListItem(
                id = index,
                name = info.name,
                description = info.description,
                version = "v${info.version}",
                icon = pluginIcon(info.name),
                enabled = info.enabled,
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        Text(
            text = "🧩 插件",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            items(plugins, key = { it.id }) { plugin ->
                PluginRow(plugin)
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/**
 * 插件行：图标 + 信息 + 真实启用状态标签。
 */
@Composable
private fun PluginRow(plugin: PluginListItem) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            ) {
                Text(
                    text = plugin.icon,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(10.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = plugin.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = plugin.version,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = plugin.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            // 启用状态标签（真实状态，只读展示）
            Surface(
                shape = RoundedCornerShape(50),
                color = if (plugin.enabled) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            ) {
                Text(
                    text = if (plugin.enabled) "已启用" else "未启用",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (plugin.enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/** 根据插件名选 emoji 图标 */
private fun pluginIcon(name: String): String = when {
    name.contains("开屏") || name.contains("权限") -> "🔔"
    name.contains("番茄") || name.contains("专注") || name.contains("引擎") -> "🍅"
    name.contains("HTML") || name.contains("主界面") -> "🌐"
    name.contains("角色") || name.contains("天依") || name.contains("人格") -> "🎭"
    name.contains("拦截") || name.contains("锁机") || name.contains("黑名单") -> "🛡️"
    name.contains("摄像头") || name.contains("巡查") -> "📷"
    name.contains("荣誉") || name.contains("徽章") -> "🏅"
    name.contains("统计") || name.contains("数据") -> "📊"
    name.contains("市场") || name.contains("商店") -> "🏪"
    name.contains("语音") || name.contains("对话") -> "🎙️"
    name.contains("白噪音") || name.contains("声音") -> "🎵"
    name.contains("通知") -> "🔔"
    name.contains("账户") || name.contains("账号") -> "👤"
    name.contains("习惯") || name.contains("养成") -> "🌱"
    else -> "🧩"
}