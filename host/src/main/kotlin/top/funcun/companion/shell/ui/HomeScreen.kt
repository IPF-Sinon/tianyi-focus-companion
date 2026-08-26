package top.funcun.companion.shell.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.funcun.companion.sdk.slot.UISlot

/**
 * 主界面（专注 Tab）。
 *
 * 优先渲染 home-html 插件注册到 HOME_TOP 的全屏 HTML 主界面（WebView）。
 * 若未注册（如用户移除插件），显示简洁占位页，不展示未实现功能的假 UI。
 */
@Composable
fun HomeScreen(
    getSlotContents: (UISlot) -> List<@Composable () -> Unit>,
) {
    val homeContents = getSlotContents(UISlot.HOME_TOP)

    if (homeContents.isNotEmpty()) {
        // HTML 主界面插件已注册 → 全屏渲染（插件内部再处理开屏权限引导）
        Box(modifier = Modifier.fillMaxSize()) {
            homeContents.forEach { it() }
        }
        return
    }

    // 兜底占位：无 HTML 主界面插件时的简洁提示
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Text(
            text = "依见钟勤",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "未安装 HTML 主界面插件，请安装 home-html 插件后使用自定义主界面。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}