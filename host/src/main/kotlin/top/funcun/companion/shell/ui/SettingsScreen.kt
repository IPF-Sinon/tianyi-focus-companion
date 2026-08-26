package top.funcun.companion.shell.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.HorizontalDivider
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.funcun.companion.sdk.slot.UISlot
import top.funcun.companion.shell.components.SettingsGroup
import top.funcun.companion.shell.components.SettingsGroupTitle
import top.funcun.companion.shell.components.SettingsItem
import top.funcun.companion.shell.components.ToggleSettingItem

/**
 * 设置页，对标 Web 设计稿 page-settings：
 * 分组设置（专注 / 拦截&巡查 / 关于）+ 插件扩展区。
 */
@Composable
fun SettingsScreen(
    getSlotContents: (UISlot) -> List<@Composable () -> Unit>,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        Text(
            text = "⚙️ 设置",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(16.dp))

        // 插件设置扩展区（权限管理、插件市场等）
        getSlotContents(UISlot.SETTINGS_SECTION).forEach { it() }

        Spacer(Modifier.height(16.dp))

        // 分组：专注
        SettingsGroupTitle("专注")
        SettingsGroup {
            SettingsItem(
                icon = "⏱️",
                title = "专注时长",
                subtitle = "25 分钟",
                onClick = {},
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            )
            ToggleSettingItem(
                icon = "🔊",
                title = "白噪音",
                subtitle = "雨声 · 低",
                checked = true,
                onCheckedChange = {},
            )
        }

        Spacer(Modifier.height(20.dp))

        // 分组：拦截 & 巡查
        SettingsGroupTitle("拦截 & 巡查")
        SettingsGroup {
            SettingsItem(
                icon = "🚫",
                title = "黑名单应用",
                subtitle = "抖音, 微信, 游戏",
                onClick = {},
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            )
            ToggleSettingItem(
                icon = "🛡️",
                title = "深度锁机",
                subtitle = "遣返后锁屏 30s",
                checked = true,
                onCheckedChange = {},
            )
        }

        Spacer(Modifier.height(20.dp))

        // 分组：关于
        SettingsGroupTitle("关于")
        SettingsGroup {
            SettingsItem(
                icon = "ℹ️",
                title = "版本",
                subtitle = "依见钟勤 v0.1.0",
                onClick = {},
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            )
            SettingsItem(
                icon = "📄",
                title = "开源协议",
                subtitle = "Apache 2.0",
                onClick = {},
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}