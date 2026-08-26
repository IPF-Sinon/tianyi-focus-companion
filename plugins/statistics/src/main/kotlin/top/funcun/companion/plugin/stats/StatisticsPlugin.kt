package top.funcun.companion.plugin.stats

import android.util.Log
import top.funcun.companion.sdk.Plugin
import top.funcun.companion.sdk.PluginContext
import top.funcun.companion.sdk.util.PluginId
import top.funcun.companion.sdk.util.SemVer

/**
 * 数据统计插件（占位）。
 *
 * 统计功能尚未实现，不注册任何假数据 UI。
 * 待真实统计（读取专注历史）实现后再注册 STATS_CHART / STATS_CARD。
 */
class StatisticsPlugin : Plugin {

    override val id = PluginId("top.funcun.companion.plugin.statistics")
    override val name = "数据统计"
    override val version = SemVer(1, 0, 0)
    override val description = "专注统计图表与周报"
    override val icon = 0
    override val dependencies = emptyList<PluginId>()
    override val permissions = emptyList<String>()

    override suspend fun onLoad(context: PluginContext) {
        Log.i(TAG, "StatisticsPlugin loaded (统计功能未实现，暂无 UI)")
    }

    override suspend fun onEnable() {}
    override suspend fun onDisable() {}
    override suspend fun onUnload() {}

    companion object {
        private const val TAG = "StatisticsPlugin"
    }
}