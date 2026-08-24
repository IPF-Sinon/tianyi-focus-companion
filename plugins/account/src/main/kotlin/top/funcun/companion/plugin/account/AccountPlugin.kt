package top.funcun.companion.plugin.account

import android.util.Log
import top.funcun.companion.sdk.Plugin
import top.funcun.companion.sdk.PluginContext
import top.funcun.companion.sdk.util.PluginId
import top.funcun.companion.sdk.util.SemVer

/**
 * 账号同步插件（可选）。
 * 支持多设备数据同步，用户可选择是否启用。
 */
class AccountPlugin : Plugin {

    override val id = PluginId("top.funcun.companion.plugin.account")
    override val name = "账号同步"
    override val version = SemVer(1, 0, 0)
    override val description = "多设备数据同步"
    override val icon = 0
    override val dependencies = emptyList<PluginId>()
    override val permissions = emptyList<String>()

    override suspend fun onLoad(context: PluginContext) {
        Log.i(TAG, "AccountPlugin loaded")
    }

    override suspend fun onEnable() {}
    override suspend fun onDisable() {}
    override suspend fun onUnload() {}

    companion object {
        private const val TAG = "AccountPlugin"
    }
}
