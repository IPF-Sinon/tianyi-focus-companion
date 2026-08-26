package top.funcun.companion.shell.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.funcun.companion.sdk.slot.UISlot
import top.funcun.companion.shell.components.CompanionHeader
import top.funcun.companion.shell.components.SpeechBubble
import top.funcun.companion.shell.components.TimerCard
import top.funcun.companion.shell.components.StatCard

/**
 * 专注页（首页），对标 Web 设计稿 page-home：
 * 天依头部 → 台词气泡 → 计时器卡 → 3 宫格统计摘要。
 */
@Composable
fun HomeScreen(
    getSlotContents: (UISlot) -> List<@Composable () -> Unit>,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        // 开屏权限引导（首次启动，跳过/完成后不渲染）
        getSlotContents(UISlot.HOME_TOP).forEach { it() }

        // 天依头部
        CompanionHeader()

        Spacer(Modifier.height(12.dp))

        // 台词气泡
        SpeechBubble(
            text = "“今天也很认真呢！还有 12 分钟就可以休息啦，加油～”",
            timeTag = "刚刚 · 第 3 个番茄",
        )

        Spacer(Modifier.height(16.dp))

        // 计时器卡（demo 状态：12:48 运行中，进度 50%）
        TimerCard(
            minutes = 12,
            seconds = 48,
            progress = 0.5f,
            isRunning = true,
            modeLabel = "25:00 · 无限",
            onPauseResume = {},
            onRestart = {},
            onStop = {},
        )

        Spacer(Modifier.height(16.dp))

        // 3 宫格统计摘要
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
        ) {
            StatCard(
                value = "1.2",
                unit = "h",
                description = "今日专注",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                value = "7",
                unit = "🔥",
                description = "连续天数",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                value = "★ 3",
                description = "剩余红星",
                modifier = Modifier.weight(1f),
            )
        }

        // 好感度/荣誉卡片区（插件注册）
        Spacer(Modifier.height(16.dp))
        getSlotContents(UISlot.HOME_CARD).forEach { it() }

        Spacer(Modifier.height(24.dp))
    }
}