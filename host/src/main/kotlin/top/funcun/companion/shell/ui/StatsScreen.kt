package top.funcun.companion.shell.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import top.funcun.companion.App

/**
 * 统计页：从统计插件拉取 JSON 数据渲染。
 */
@Composable
fun StatsScreen() {
    val pluginManager = remember { App.instance.pluginManager }
    val json = remember {
        pluginManager.requestNavData("top.funcun.companion.plugin.statistics", "stats")
    }
    val data = remember(json) {
        runCatching { JSONObject(json ?: "{}") }.getOrNull()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "学习统计",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(16.dp))

        if (data == null) {
            Text(
                text = "暂无统计数据。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        // 4 宫格
        val stats = listOf(
            "今日" to data.optInt("todayMinutes", 0),
            "本周" to data.optInt("weekMinutes", 0),
            "连续" to data.optInt("streakDays", 0),
            "累计" to data.optInt("totalMinutes", 0),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            stats.take(2).forEach { s -> StatCell(s.first, s.second, Modifier.weight(1f)) }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            stats.drop(2).forEach { s -> StatCell(s.first, s.second, Modifier.weight(1f)) }
        }

        // 近 7 天柱状图
        val dailyArr = data.optJSONArray("daily")
        if (dailyArr != null && dailyArr.length() > 0) {
            Spacer(Modifier.height(20.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "近 7 天",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(14.dp))
                    WeekBars(dailyArr)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatCell(label: String, value: Int, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeekBars(arr: org.json.JSONArray) {
    val values = remember(arr) {
        (0 until arr.length()).map { arr.getJSONObject(it).optInt("minutes", 0) }
    }
    val max = values.maxOrNull() ?: 1

    Row(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        values.forEach { v ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val frac = if (max == 0) 0f else v.toFloat() / max
                Canvas(modifier = Modifier.fillMaxWidth().height(96.dp)) {
                    val cw = this.size.width
                    val ch = this.size.height
                    val h = ch * frac.coerceIn(0.05f, 1f)
                    val w = cw * 0.55f
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            ),
                        ),
                        topLeft = Offset((cw - w) / 2, ch - h),
                        size = Size(w, h),
                        cornerRadius = CornerRadius(6.dp.toPx()),
                    )
                }
            }
        }
    }
}