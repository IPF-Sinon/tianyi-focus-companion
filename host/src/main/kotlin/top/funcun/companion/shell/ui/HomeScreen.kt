package top.funcun.companion.shell.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import top.funcun.companion.App
import top.funcun.companion.theme.ThemeManager

/**
 * 专注（首页）。
 * 主题背景层 + Hero 状态卡 + 统计摘要入口。
 */
@Composable
fun HomeScreen() {
    val pluginManager = remember { App.instance.pluginManager }

    // 统计摘要（若有统计插件则显示）
    val statsJson = remember {
        pluginManager.requestNavData("top.funcun.companion.plugin.statistics", "stats")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BackgroundLayer()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // Hero 卡
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "依见钟勤",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Text(
                        text = "遇见天依之后，对学习一见钟情",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // 统计摘要
            if (statsJson != null) {
                StatsSummaryCard(statsJson)
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ) {
                    Text(
                        text = "暂无统计。完成专注后自动记录。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsSummaryCard(statsJson: String) {
    val data = remember(statsJson) {
        runCatching { org.json.JSONObject(statsJson) }.getOrNull()
    }
    if (data == null) return

    val items = listOf(
        "今日" to (data.optInt("todayMinutes", 0).toString() + " 分"),
        "本周" to (data.optInt("weekMinutes", 0).toString() + " 分"),
        "连续" to (data.optInt("streakDays", 0).toString() + " 天"),
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            items.forEach { (label, value) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleLarge,
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
    }
}

/**
 * 主题背景层：若主题启用背景图则加载，否则用主题 background 色。
 */
@Composable
private fun BackgroundLayer() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val cfg = ThemeManager.config
    val file = remember { ThemeManager.backgroundFile(context) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (file != null) {
            androidx.compose.foundation.Image(
                painter = coil.compose.rememberAsyncImagePainter(
                    model = file,
                ),
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 1f - cfg.backgroundDim + 0.0f)),
                alpha = cfg.backgroundOpacity,
            )
        }
    }
}