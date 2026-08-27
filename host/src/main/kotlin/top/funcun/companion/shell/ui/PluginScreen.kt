package top.funcun.companion.shell.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import top.funcun.companion.sdk.ConfigField
import top.funcun.companion.sdk.ConfigFieldType
import top.funcun.companion.sdk.ConfigSchema
import top.funcun.companion.shell.PluginManager

/**
 * 插件页：卡片式展示插件，支持配置展开/收起、动作、卸载、界面插件切换。
 */
@Composable
fun PluginScreen(pluginManager: PluginManager) {
    val plugins = remember { pluginManager.getBuiltinPluginInfo() }
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Text(
                text = "插件",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        item { Spacer(Modifier.height(16.dp)) }

        items(plugins, key = { it.id }) { info ->
            val isUiOverride = pluginManager.isUiOverride(info.id)
            val isActiveOverride = pluginManager.getActiveUiOverride()?.id?.value == info.id

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = info.icon, fontSize = 24.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = info.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${info.description} · v${info.version}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        // 徽标
                        if (info.builtin) {
                            Badge(text = "内置", primary = false)
                        }
                        if (isUiOverride) {
                            Badge(text = if (isActiveOverride) "界面生效中" else "界面插件", primary = true)
                        }
                    }

                    // 按钮组
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (info.hasConfig) {
                            ConfigToggleButton(info.id, pluginManager)
                        }
                        info.actions.forEach { action ->
                            TextButton(
                                onClick = { pluginManager.invokeAction(info.id, action.id) },
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Text("${action.icon} ${action.label}")
                            }
                        }
                        if (isUiOverride && !isActiveOverride) {
                            TextButton(
                                onClick = {
                                    pluginManager.setActiveUiOverride(info.id)
                                    scope.launch { /* 触发重组 */ }
                                },
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Text("启用此界面")
                            }
                        }
                        if (!info.builtin) {
                            TextButton(
                                onClick = { pluginManager.uninstall(info.id) },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                            ) {
                                Text("卸载")
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun Badge(text: String, primary: Boolean) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (primary) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (primary) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/**
 * 配置展开/收起。
 */
@Composable
private fun ConfigToggleButton(pluginId: String, pluginManager: PluginManager) {
    var expanded by remember { mutableStateOf(false) }
    val schema = remember(pluginId) { pluginManager.getConfigSchema(pluginId) }

    Column {
        TextButton(
            onClick = { expanded = !expanded },
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(if (expanded) "收起配置" else "配置")
        }

        AnimatedVisibility(visible = expanded) {
            val s = schema
            if (s != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    ConfigForm(s, pluginId, pluginManager)
                }
            }
        }
    }
}

/**
 * 把 ConfigSchema 渲染成原生表单。
 */
@Composable
private fun ConfigForm(schema: ConfigSchema, pluginId: String, pluginManager: PluginManager) {
    Column(modifier = Modifier.padding(16.dp)) {
        schema.sections.forEach { section ->
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            section.fields.forEach { field ->
                ConfigFieldRow(field, pluginId, pluginManager)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigFieldRow(field: ConfigField, pluginId: String, pluginManager: PluginManager) {
    val current = remember(field.key) {
        pluginManager.readConfigValue(pluginId, field.key, field.defaultValue)
    }
    var value by remember(field.key, current) { mutableStateOf(current) }

    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        when (field.type) {
            ConfigFieldType.BOOLEAN -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = field.label, style = MaterialTheme.typography.bodyLarge)
                        if (field.description.isNotEmpty()) {
                            Text(
                                text = field.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Switch(
                        checked = value == "true",
                        onCheckedChange = { checked ->
                            val v = if (checked) "true" else "false"
                            value = v
                            pluginManager.writeConfigValue(pluginId, field.key, v)
                        },
                    )
                }
            }

            ConfigFieldType.TEXT -> {
                Text(text = field.label, style = MaterialTheme.typography.bodyLarge)
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                TextButton(onClick = { pluginManager.writeConfigValue(pluginId, field.key, value) }) {
                    Text("保存")
                }
            }

            ConfigFieldType.INT -> {
                Text(text = field.label, style = MaterialTheme.typography.bodyLarge)
                OutlinedTextField(
                    value = value,
                    onValueChange = { v -> if (v.all { it.isDigit() } || v.isEmpty()) value = v },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                TextButton(onClick = { pluginManager.writeConfigValue(pluginId, field.key, value) }) {
                    Text("保存")
                }
            }

            ConfigFieldType.SELECT -> {
                Text(text = field.label, style = MaterialTheme.typography.bodyLarge)
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = field.options.firstOrNull { it.value == value }?.label ?: value,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        readOnly = true,
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        field.options.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.label) },
                                onClick = {
                                    value = opt.value
                                    pluginManager.writeConfigValue(pluginId, field.key, opt.value)
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }

            ConfigFieldType.MULTI_SELECT -> {
                Text(text = field.label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = field.options.joinToString(" / ") { it.label },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}