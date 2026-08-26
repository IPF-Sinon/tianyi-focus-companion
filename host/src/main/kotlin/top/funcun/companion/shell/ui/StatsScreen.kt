package top.funcun.companion.shell.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.funcun.companion.sdk.slot.UISlot

/**
 * 统计页，FolkPatch 风格：
 * - Scaffold + TopAppBar（标题+返回）
 * - LazyColumn + 插件统计卡片
 * - surfaceContainer 底色
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    getSlotContents: (UISlot) -> List<@Composable () -> Unit>,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "统计",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(start = 4.dp),
                    ) {
                        Text(
                            "← 返回",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = Color.Transparent,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // 统计卡片（插件注册）
            item {
                getSlotContents(UISlot.STATS_CARD).forEach { it() }
            }

            // 统计图表（插件注册）
            item {
                Spacer(Modifier.height(16.dp))
                getSlotContents(UISlot.STATS_CHART).forEach { it() }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}