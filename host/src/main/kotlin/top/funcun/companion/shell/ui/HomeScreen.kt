package top.funcun.companion.shell.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.funcun.companion.sdk.slot.UISlot

/**
 * 主页，FolkPatch 风格：
 * - Material3 主题色（surfaceContainer 底色卡片）
 * - RoundedCornerShape(24.dp) 大圆角按钮
 * - 底部导航栏
 */
@Composable
fun HomeScreen(
    onStartFocus: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
    getSlotContents: (UISlot) -> List<@Composable () -> Unit>,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // 开屏权限引导（插件注册到 HOME_TOP，抢占式）
        getSlotContents(UISlot.HOME_TOP).forEach { it() }

        // 标题区
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                Text(
                    text = "依见钟勤",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
            Text(
                text = "遇见天依之后，对学习一见钟情",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 好感度/荣誉卡片区（插件注册到 HOME_CARD）
        getSlotContents(UISlot.HOME_CARD).forEach { it() }

        Spacer(modifier = Modifier.height(24.dp))

        // 开始专注按钮
        Button(
            onClick = onStartFocus,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(24.dp),
        ) {
            Text(
                text = "开始专注",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 底部导航
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                NavigationTextButton("统计", onClick = onStats)
                NavigationTextButton("设置", onClick = onSettings)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun NavigationTextButton(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}