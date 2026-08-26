package top.funcun.companion.shell.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.funcun.companion.sdk.slot.UISlot
import top.funcun.companion.shell.components.StatCard

/**
 * 统计页，对标 Web 设计稿 page-stats：
 * 头部 + 4 卡网格 + 周柱状图。
 */
@Composable
fun StatsScreen(
    getSlotContents: (UISlot) -> List<@Composable () -> Unit>,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        // 头部
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "📊 学习统计",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            androidx.compose.material3.Surface(
                shape = MaterialTheme.shapes.full,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = "本周",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // 4 卡网格（2×2）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatCard(
                value = "6.8",
                unit = "h",
                description = "总专注时长 ↑12%",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                value = "16",
                description = "完成番茄",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatCard(
                value = "3",
                description = "违纪次数",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                value = "7",
                description = "连续天数",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(20.dp))

        // 柱状图
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "📈 本周每日专注",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(14.dp))
                WeekBarChart()
            }
        }

        Spacer(Modifier.height(16.dp))

        // 插件统计卡片区
        getSlotContents(UISlot.STATS_CARD).forEach { it() }
        getSlotContents(UISlot.STATS_CHART).forEach { it() }

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * 周柱状图，对标设计稿 .chart-bars：
 * 渐变蓝柱 + 星期标签。
 */
@Composable
private fun WeekBarChart() {
    // 模拟数据（分钟）
    val data = listOf(64f, 88f, 52f, 106f, 74f, 40f, 94f)
    val labels = listOf("一", "二", "三", "四", "五", "六", "日")
    val maxValue = 106f

    val primary = MaterialTheme.colorScheme.primary
    val primaryLight = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            data.forEachIndexed { index, value ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val barHeightFraction = value / maxValue
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp),
                    ) {
                        val barHeight = size.height * barHeightFraction
                        val barWidth = size.width * 0.5f
                        val x = (size.width - barWidth) / 2
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(primary, primaryLight),
                            ),
                            topLeft = Offset(x, size.height - barHeight),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = labels[index],
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}