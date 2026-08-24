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
import top.funcun.companion.theme.TianyiColors

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
            .background(TianyiColors.Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // 开屏权限引导（插件注册到 HOME_TOP，抢占式）
        getSlotContents(UISlot.HOME_TOP).forEach { it() }

        // 标题区
        Text(
            text = "依见钟勤",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TianyiColors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "遇见天依之后，对学习一见钟情",
            fontSize = 14.sp,
            color = TianyiColors.TextSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.weight(1f))

        // 好感度/荣誉卡片区（插件注册到 HOME_CARD）
        getSlotContents(UISlot.HOME_CARD).forEach { it() }

        // 开始按钮
        Button(
            onClick = onStartFocus,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TianyiColors.Primary,
                contentColor = TianyiColors.TextPrimary,
            ),
        ) {
            Text(
                text = "开始专注",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 底部导航
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TextButton(onClick = onStats) {
                Text("统计", color = TianyiColors.TextSecondary)
            }
            TextButton(onClick = onSettings) {
                Text("设置", color = TianyiColors.TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
