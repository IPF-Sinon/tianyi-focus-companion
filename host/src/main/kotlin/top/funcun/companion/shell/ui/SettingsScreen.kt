package top.funcun.companion.shell.ui

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.funcun.companion.sdk.slot.UISlot

/**
 * 设置页。
 *
 * 仅展示插件注册的真实设置项（SETTINGS_SECTION，如权限管理）。
 * 未实现的设置功能（黑名单/白噪音/深度锁机等）不展示假选项。
 */
@Composable
fun SettingsScreen(
    getSlotContents: (UISlot) -> List<@Composable () -> Unit>,
) {
    val sections = getSlotContents(UISlot.SETTINGS_SECTION)

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
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(16.dp))

        if (sections.isEmpty()) {
            Text(
                text = "暂无设置项。安装相应插件后在此展示。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            sections.forEach { it() }
        }

        Spacer(Modifier.height(24.dp))
    }
}