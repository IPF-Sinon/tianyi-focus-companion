package top.funcun.companion.plugin.stats

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.funcun.companion.sdk.Plugin
import top.funcun.companion.sdk.PluginContext
import top.funcun.companion.sdk.slot.UISlot
import top.funcun.companion.sdk.util.PluginId
import top.funcun.companion.sdk.util.SemVer

class StatisticsPlugin : Plugin {

    override val id = PluginId("top.funcun.companion.plugin.statistics")
    override val name = "数据统计"
    override val version = SemVer(1, 0, 0)
    override val description = "专注统计图表与周报"
    override val icon = 0
    override val dependencies = emptyList<PluginId>()
    override val permissions = emptyList<String>()

    private lateinit var ctx: PluginContext

    override suspend fun onLoad(context: PluginContext) {
        ctx = context
        Log.i(TAG, "StatisticsPlugin loaded")

        // 注册统计图表
        ctx.registerUI(UISlot.STATS_CHART) {
            FocusChart()
        }
        ctx.registerUI(UISlot.STATS_CARD) {
            StatsCard()
        }
    }

    override suspend fun onEnable() {}
    override suspend fun onDisable() {}
    override suspend fun onUnload() {}

    companion object {
        private const val TAG = "StatisticsPlugin"
    }
}

@Composable
fun FocusChart() {
    // 模拟数据：过去 7 天
    val data = listOf(25f, 45f, 30f, 60f, 20f, 50f, 35f)
    val labels = listOf("一", "二", "三", "四", "五", "六", "日")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "本周专注",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
            ) {
                val maxHeight = size.height - 20f
                val barWidth = size.width / data.size / 2f
                val barSpacing = size.width / data.size

                data.forEachIndexed { index, value ->
                    val barHeight = (value / 60f) * maxHeight
                    val x = index * barSpacing + barSpacing / 2f - barWidth / 2f

                    drawRect(
                        color = Color(0xFFE8A0BF),
                        topLeft = Offset(x, size.height - barHeight),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    )
                }
            }
        }
    }
}

@Composable
fun StatsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            StatItem("今日", "25分钟")
            StatItem("本周", "265分钟")
            StatItem("连续", "3天")
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE8A0BF),
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF8A8A8A),
        )
    }
}
