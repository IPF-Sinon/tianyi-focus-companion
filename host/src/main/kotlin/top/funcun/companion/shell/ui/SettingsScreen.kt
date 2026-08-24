package top.funcun.companion.shell.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.funcun.companion.sdk.slot.UISlot
import top.funcun.companion.theme.TianyiColors

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    getSlotContents: (UISlot) -> List<@Composable () -> Unit>,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TianyiColors.Background)
            .padding(24.dp),
    ) {
        Text(
            text = "设置",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TianyiColors.TextPrimary,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 插件设置扩展区
        getSlotContents(UISlot.SETTINGS_SECTION).forEach { it() }

        Spacer(modifier = Modifier.weight(1f))

        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.Start),
        ) {
            Text("返回", color = TianyiColors.TextSecondary)
        }
    }
}
