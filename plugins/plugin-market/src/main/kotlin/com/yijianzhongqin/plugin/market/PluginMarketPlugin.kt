package com.yijianzhongqin.plugin.market

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yijianzhongqin.sdk.Plugin
import com.yijianzhongqin.sdk.PluginContext
import com.yijianzhongqin.sdk.slot.UISlot
import com.yijianzhongqin.sdk.util.PluginId
import com.yijianzhongqin.sdk.util.SemVer

/**
 * 插件市场。
 * 浏览、下载、安装社区插件。
 */
class PluginMarketPlugin : Plugin {

    override val id = PluginId("com.yijianzhongqin.plugin.market")
    override val name = "插件市场"
    override val version = SemVer(1, 0, 0)
    override val description = "浏览和安装社区插件"
    override val icon = 0
    override val dependencies = emptyList<PluginId>()
    override val permissions = emptyList<String>()

    private lateinit var ctx: PluginContext

    override suspend fun onLoad(context: PluginContext) {
        ctx = context
        Log.i(TAG, "PluginMarketPlugin loaded")

        // 注册插件市场 UI 到设置页
        context.registerUI(UISlot.SETTINGS_SECTION) {
            PluginMarketSection()
        }
    }

    override suspend fun onEnable() {}
    override suspend fun onDisable() {}
    override suspend fun onUnload() {}

    companion object {
        private const val TAG = "PluginMarketPlugin"
    }
}

data class PluginItem(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val version: String,
    val isInstalled: Boolean = false,
)

@Composable
fun PluginMarketSection() {
    val plugins = remember {
        listOf(
            PluginItem("com.tianyi.plugin.weather", "天依天气", "让天依告诉你今天的天气", "天依社区", "1.0.0"),
            PluginItem("com.tianyi.plugin.music", "学习音乐", "专注时播放轻音乐", "天依社区", "1.2.0"),
            PluginItem("com.tianyi.plugin.posture", "坐姿提醒", "检测坐姿并提醒", "天依社区", "0.5.0"),
        )
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "插件市场",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))

        plugins.forEach { plugin ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = plugin.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = plugin.description,
                            fontSize = 12.sp,
                            color = Color(0xFF8A8A8A),
                        )
                        Text(
                            text = "${plugin.author} · v${plugin.version}",
                            fontSize = 10.sp,
                            color = Color(0xFFB0B0B0),
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            // 下载并安装插件
                            Log.i("PluginMarket", "Installing plugin: ${plugin.id}")
                        },
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Text(
                            if (plugin.isInstalled) "已安装" else "安装",
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}
