package top.funcun.companion.shell.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.funcun.companion.theme.ThemeManager

/**
 * 设置页：入口列表 + 主题设置/主题商店子页面。
 */
@Composable
fun SettingsScreen(pluginManager: top.funcun.companion.shell.PluginManager) {
    var sub by rememberSaveable { mutableStateOf(SubPage.MAIN) }

    AnimatedContent(
        targetState = sub,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "settings",
    ) { page ->
        when (page) {
            SubPage.MAIN -> SettingsMain(onTheme = { sub = SubPage.THEME }, onStore = { sub = SubPage.STORE })
            SubPage.THEME -> ThemeSettingsScreen(onBack = { sub = SubPage.MAIN })
            SubPage.STORE -> ThemeStoreScreen(onBack = { sub = SubPage.MAIN })
        }
    }
}

private enum class SubPage { MAIN, THEME, STORE }

@Composable
private fun SettingsMain(onTheme: () -> Unit, onStore: () -> Unit) {
    val appContext = LocalContext.current.applicationContext

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Text(
                text = "设置",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        item { Spacer(Modifier.height(16.dp)) }

        item { SettingsGroupCard {
            SettingsItem("🎨", "外观主题", "配色 / 背景 / 字体 / 布局") { onTheme() }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
            SettingsItem("🛍️", "主题商店", "在线下载社区主题") { onStore() }
        } }

        item { Spacer(Modifier.height(16.dp)) }
        item { SettingsGroupCard {
            val cfg = ThemeManager.config
            SettingsItem(
                "🌙", "深色模式",
                if (cfg.nightModeFollowSys) "跟随系统" else if (cfg.nightModeEnabled) "开" else "关",
            ) {
                ThemeManager.update(appContext) { c ->
                    c.copy(nightModeFollowSys = !c.nightModeFollowSys)
                }
            }
        } }

        item { Spacer(Modifier.height(16.dp)) }
        item { SettingsGroupCard {
            SettingsItem("ℹ️", "关于", "依见钟勤 · 主题系统兼容 FolkPatch") {}
        } }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsItem(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, fontSize = 20.sp)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}