package top.funcun.companion.plugin.peace

import android.util.Log
import top.funcun.companion.sdk.Plugin
import top.funcun.companion.sdk.PluginContext
import top.funcun.companion.sdk.util.PluginId
import top.funcun.companion.sdk.util.SemVer

/**
 * 学习白名单。
 * 在白名单中的 App 不会被拦截（如词典、笔记、浏览器）。
 */
class PeaceZonePlugin : Plugin {

    override val id = PluginId("top.funcun.companion.plugin.peace.zone")
    override val name = "学习白名单"
    override val version = SemVer(1, 0, 0)
    override val description = "学习工具白名单，不被拦截"
    override val icon = 0
    override val dependencies = emptyList<PluginId>()
    override val permissions = emptyList<String>()

    override suspend fun onLoad(context: PluginContext) {
        Log.i(TAG, "PeaceZonePlugin loaded")
    }

    override suspend fun onEnable() {}
    override suspend fun onDisable() {}
    override suspend fun onUnload() {}

    companion object {
        private const val TAG = "PeaceZonePlugin"

        val WHITELIST = listOf(
            "com.android.chrome",              // Chrome
            "org.mozilla.firefox",             // Firefox
            "com.tencent.mtt",                 // QQ 浏览器
            "com.UCMobile",                    // UC 浏览器
            "com.xiaomi.market",               // 小米应用商店
            "com.miui.notes",                  // 小米便签
            "com.miui.calculator",             // 计算器
            "com.miui.weather2",               // 天气
            "com.youdao.dict",                 // 有道词典
            "com.baidu.searchbox",             // 百度
            "com.evernote",                    // 印象笔记
            "com.notion.id",                   // Notion
            "com.tencent.wework",              // 企业微信
            "com.tencent.mobileqq",            // QQ
            "com.tencent.mm",                  // 微信
        )
    }
}
