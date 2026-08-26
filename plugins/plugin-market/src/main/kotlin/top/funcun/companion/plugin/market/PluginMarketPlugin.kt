package top.funcun.companion.plugin.market

import android.util.Log
import top.funcun.companion.sdk.Plugin
import top.funcun.companion.sdk.PluginContext
import top.funcun.companion.sdk.util.PluginId
import top.funcun.companion.sdk.util.SemVer

/**
 * 插件市场插件（占位）。
 *
 * 市场功能（浏览/下载/安装社区插件）尚未实现，
 * 不展示虚构插件列表，待实现后再注册 UI。
 */
class PluginMarketPlugin : Plugin {

    override val id = PluginId("top.funcun.companion.plugin.market")
    override val name = "插件市场"
    override val version = SemVer(1, 0, 0)
    override val description = "浏览和安装社区插件"
    override val icon = 0
    override val dependencies = emptyList<PluginId>()
    override val permissions = emptyList<String>()

    override suspend fun onLoad(context: PluginContext) {
        Log.i(TAG, "PluginMarketPlugin loaded (市场功能未实现，暂无 UI)")
    }

    override suspend fun onEnable() {}
    override suspend fun onDisable() {}
    override suspend fun onUnload() {}

    companion object {
        private const val TAG = "PluginMarketPlugin"
    }
}