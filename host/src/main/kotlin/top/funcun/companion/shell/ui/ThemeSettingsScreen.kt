package top.funcun.companion.shell.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.funcun.companion.theme.ColorSchemeGenerator
import top.funcun.companion.theme.ThemeIO
import top.funcun.companion.theme.ThemeManager

/**
 * 主题设置页：配色 / 深色 / 布局 / 背景 / 导入导出。
 */
@Composable
fun ThemeSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val cfg = ThemeManager.config

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            val imported = ThemeIO.import(context, uri)
            if (imported != null) ThemeManager.apply(context, imported)
        }
    }

    val presets = listOf(
        "#4A90E2", // 蓝
        "#E8A0BF", // 粉
        "#43A047", // 绿
        "#7E57C2", // 紫
        "#F4511E", // 橙
        "#37474F", // 墨
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // 顶部栏
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← 返回") }
            Spacer(Modifier.width(4.dp))
            Text(
                text = "外观主题",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(Modifier.height(8.dp))

        // 配色
        SectionTitle("配色")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            presets.forEach { hex ->
                val color = ColorSchemeGenerator.parseColor(hex) ?: Color.Gray
                val selected = cfg.customColor.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(color)
                        .clickable { ThemeManager.update(context) { it.copy(customColor = hex) } },
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    if (selected) Text("✓", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        // 自定义颜色输入
        var hexText by remember { mutableStateOf(cfg.customColor) }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(
                value = hexText,
                onValueChange = { hexText = it },
                label = { Text("自定义颜色 #RRGGBB") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = {
                val c = ColorSchemeGenerator.parseColor(hexText)
                if (c != null) ThemeManager.update(context) { it.copy(customColor = hexText) }
            }) { Text("应用") }
        }

        // 深色模式
        SectionTitle("深色模式")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = cfg.nightModeFollowSys,
                onClick = { ThemeManager.update(context) { it.copy(nightModeFollowSys = true) } },
                label = { Text("跟随系统") },
            )
            FilterChip(
                selected = !cfg.nightModeFollowSys,
                onClick = { ThemeManager.update(context) { it.copy(nightModeFollowSys = false) } },
                label = { Text("手动") },
            )
        }
        if (!cfg.nightModeFollowSys) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !cfg.nightModeEnabled,
                    onClick = { ThemeManager.update(context) { it.copy(nightModeEnabled = false) } },
                    label = { Text("浅色") },
                )
                FilterChip(
                    selected = cfg.nightModeEnabled,
                    onClick = { ThemeManager.update(context) { it.copy(nightModeEnabled = true) } },
                    label = { Text("深色") },
                )
            }
        }

        // 布局
        SectionTitle("首页布局")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = cfg.homeLayoutStyle == "dashboard",
                onClick = { ThemeManager.update(context) { it.copy(homeLayoutStyle = "dashboard") } },
                label = { Text("仪表盘") },
            )
            FilterChip(
                selected = cfg.homeLayoutStyle == "simple",
                onClick = { ThemeManager.update(context) { it.copy(homeLayoutStyle = "simple") } },
                label = { Text("简洁") },
            )
        }

        // 底栏样式
        SectionTitle("底栏样式")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = cfg.navBarStyle == "floating",
                onClick = { ThemeManager.update(context) { it.copy(navBarStyle = "floating") } },
                label = { Text("悬浮胶囊") },
            )
            FilterChip(
                selected = cfg.navBarStyle == "standard",
                onClick = { ThemeManager.update(context) { it.copy(navBarStyle = "standard") } },
                label = { Text("常规通栏") },
            )
        }

        // 圆角
        SectionTitle("卡片圆角（${cfg.cardCornerRadius}dp）")
        Slider(
            value = cfg.cardCornerRadius.toFloat(),
            onValueChange = { v ->
                ThemeManager.update(context) { it.copy(cardCornerRadius = v.toInt()) }
            },
            valueRange = 8f..40f,
        )

        // 背景
        SectionTitle("背景")
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("启用背景图", modifier = Modifier.weight(1f))
            Switch(
                checked = cfg.isBackgroundEnabled,
                onCheckedChange = { v ->
                    ThemeManager.update(context) { it.copy(isBackgroundEnabled = v) }
                },
            )
        }

        // 导入主题包
        SectionTitle("主题包")
        Button(
            onClick = { importLauncher.launch(arrayOf("*/*")) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
        ) {
            Text("导入主题包（.fpt / .zip）")
        }
        TextButton(onClick = { ThemeManager.reset(context) }, modifier = Modifier.fillMaxWidth()) {
            Text("恢复默认主题", color = MaterialTheme.colorScheme.error)
        }

        top.funcun.companion.shell.ui.components.BottomBarSpacer()
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}