import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

/**
 * 计时器卡片，对标设计稿 .timer-card：
 * 大数字倒计时 + SVG 环形进度 + 控制按钮。
 *
 * @param minutes 当前剩余分钟
 * @param seconds 当前剩余秒
 * @param totalSeconds 总时长（秒），用于进度
 * @param elapsedSeconds 已过秒数，用于进度
 * @param isRunning 是否运行中
 * @param onPauseResume 暂停/继续
 * @param onRestart 重置
 * @param onStop 停止
 */
@Composable
fun TimerCard(
    minutes: Int,
    seconds: Int,
    progress: Float, // 0..1 剩余比例
    isRunning: Boolean,
    modeLabel: String = "25:00 · 无限",
    onPauseResume: () -> Unit,
    onRestart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 标签行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "🍅 专注中",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    shape = RoundedCornerShape(percent = 50),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = modeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // 环形进度 + 大数字
            Box(contentAlignment = Alignment.Center) {
                ProgressRing(
                    progress = progress,
                    size = 180.dp,
                    strokeWidth = 6.dp,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = minutes.toString().padStart(2, '0'),
                        fontSize = 60.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = ":",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    Text(
                        text = seconds.toString().padStart(2, '0'),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 控制按钮
            Row(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onRestart,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Replay,
                        contentDescription = "重置",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(16.dp))
                // 主按钮：暂停/继续（Surface 不支持 Brush color，改用 Box + background）
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.Replay,
                        contentDescription = if (isRunning) "暂停" else "继续",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(Modifier.width(16.dp))
                IconButton(
                    onClick = onStop,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = "停止",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

/**
 * 环形进度，对标设计稿 SVG ring：
 * 渐变描边圆环 + 中心圆点。
 */
@Composable
private fun ProgressRing(
    progress: Float,
    size: androidx.compose.ui.unit.Dp,
    strokeWidth: androidx.compose.ui.unit.Dp,
) {
    val strokePx = with(androidx.compose.ui.platform.LocalDensity.current) { strokeWidth.toPx() }
    val primary = MaterialTheme.colorScheme.primary
    val primaryLight = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    val track = MaterialTheme.colorScheme.surfaceVariant

    androidx.compose.foundation.Canvas(
        modifier = Modifier.size(size),
    ) {
        val diameter = min(this.size.width, this.size.height)
        val radius = (diameter - strokePx) / 2
        val stroke = Stroke(width = strokePx, cap = StrokeCap.Round)

        // 轨道
        drawArc(
            color = track,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(strokePx / 2, strokePx / 2),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = stroke,
        )
        // 进度（渐变用两层近似）
        drawArc(
            color = primaryLight,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            topLeft = Offset(strokePx / 2, strokePx / 2),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = stroke,
        )
        // 中心圆点
        drawCircle(
            color = Color.White,
            radius = 6.dp.toPx(),
            center = center,
        )
        drawCircle(
            color = primary,
            radius = 4.dp.toPx(),
            center = center,
        )
    }
}