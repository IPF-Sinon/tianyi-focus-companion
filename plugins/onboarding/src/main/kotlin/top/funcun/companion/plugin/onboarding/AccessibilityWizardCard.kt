package top.funcun.companion.plugin.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 无障碍服务分步引导卡片，参考 Operit 的 AccessibilityWizardCard 模式。
 *
 * 未启用时展示「如何开启？」入口，展开后显示分步说明 + 跳转按钮；
 * 返回应用时 onResume 自动检测状态更新。
 */
@Composable
fun AccessibilityWizardCard(
    isServiceEnabled: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSteps by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (isServiceEnabled) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题行：图标 + 名称 + 状态
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "♿",
                    fontSize = 20.sp,
                    modifier = Modifier.width(28.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "无障碍服务",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (isServiceEnabled) "已启用" else "未启用 · 需要手动开启",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isServiceEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }
                if (!isServiceEnabled) {
                    TextButton(onClick = { showSteps = !showSteps }) {
                        Text(if (showSteps) "收起" else "如何开启？")
                    }
                }
            }

            // 分步说明（仅未启用且展开时显示）
            AnimatedVisibility(visible = !isServiceEnabled && showSteps) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )
                    StepText(
                        number = "1",
                        text = "点击下方按钮，打开系统无障碍设置",
                    )
                    StepText(
                        number = "2",
                        text = "点击「已下载的应用」，找到「依见钟勤」",
                    )
                    StepText(
                        number = "3",
                        text = "开启服务开关，返回本页面后会自动检测并刷新状态",
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onOpenAccessibilitySettings,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("打开无障碍设置")
                    }
                }
            }
        }
    }
}

@Composable
private fun StepText(number: String, text: String) {
    Row(
        modifier = Modifier.padding(top = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primary,
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}
