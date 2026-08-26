package top.funcun.companion.shell.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.sp
import top.funcun.companion.sdk.slot.UISlot

/**
 * 统计页。
 *
 * 仅展示插件注册的真实内容（STATS_CARD / STATS_CHART）。
 * 未实现统计功能时不展示任何假数据。
 */
@Composable
fun StatsScreen(
    getSlotContents: (UISlot) -> List<@Composable () -> Unit>,
) {
    val cards = getSlotContents(UISlot.STATS_CARD)
    val charts = getSlotContents(UISlot.STATS_CHART)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        Text(
            text = "📊 学习统计",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(16.dp))

        if (cards.isEmpty() && charts.isEmpty()) {
            Text(
                text = "暂无统计数据。安装统计插件后在此展示。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )
        } else {
            cards.forEach { it() }
            charts.forEach { it() }
        }

        Spacer(Modifier.height(24.dp))
    }
}