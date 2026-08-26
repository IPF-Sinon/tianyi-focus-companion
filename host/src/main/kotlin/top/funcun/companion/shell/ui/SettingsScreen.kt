package top.funcun.companion.shell.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.funcun.companion.sdk.slot.UISlot

/**
 * 设置页，FolkPatch 风格：
 * - Material3 主题色
 * - 顶部返回按钮 + 标题
 * - 插件内容区由各自插件控制样式
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    getSlotContents: (UISlot) -> List<@Composable () -> Unit>,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
    ) {
        // 顶部栏：返回 + 标题
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp),
        ) {
            TextButton(
                onClick = onBack,
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = "← 返回",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "设置",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // 插件设置扩展区
        getSlotContents(UISlot.SETTINGS_SECTION).forEach { it() }

        Spacer(modifier = Modifier.weight(1f))
    }
}