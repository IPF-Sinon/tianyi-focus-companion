package top.funcun.companion.shell.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.funcun.companion.App
import top.funcun.companion.shell.ui.components.BottomBarSpacer

/**
 * 专注（首页）：Hero 渐变卡（呼吸动画）+ 统计摘要。
 * 背景由全局 AppBackgroundLayer 提供，这里不再重复。
 */
@Composable
fun HomeScreen() {
    val pluginManager = remember { App.instance.pluginManager }
    val statsJson = remember {
        pluginManager.requestNavData("top.funcun.companion.plugin.statistics", "stats")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        HeroCard()

        Spacer(Modifier.height(16.dp))

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

        BottomBarSpacer()
    }
}

@Composable
private fun HeroCard() {
    val transition = rememberInfiniteTransition(label = "hero")
    val breath by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breath",
    )

    val primary = MaterialTheme.colorScheme.primary
    val gradient = Brush.linearGradient(
        colors = listOf(
            primary.copy(alpha = breath),
            primary.copy(alpha = 0.78f),
        ),
    )

    Card(
        modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(gradient),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "🍅 专注中心",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "依见钟勤",
                    style = MaterialTheme.typography.headlineMedium,
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