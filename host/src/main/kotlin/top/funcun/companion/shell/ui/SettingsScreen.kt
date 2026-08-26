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
 * 设置页，FolkPatch 风格：
 * - Scaffold + TopAppBar（标题+搜索/插件按钮）
 * - LazyColumn + 插件内容区
 * - surfaceContainer 底色
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    getSlotContents: (UISlot) -> List<@Composable () -> Unit>,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "设置",
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

            // 插件设置扩展区（每个插件自带卡片，对标 FolkPatch 独立分组）
            items(getSlotContents(UISlot.SETTINGS_SECTION).size) { index ->
                UISlot.SETTINGS_SECTION.let { _ ->
                    getSlotContents(UISlot.SETTINGS_SECTION)[index]()
                }
                Spacer(Modifier.height(16.dp))
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}