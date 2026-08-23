package com.yijianzhongqin.plugin.notification

import android.util.Log
import com.yijianzhongqin.sdk.Plugin
import com.yijianzhongqin.sdk.PluginContext
import com.yijianzhongqin.sdk.event.AppEvent
import com.yijianzhongqin.sdk.util.PluginId
import com.yijianzhongqin.sdk.util.SemVer

/**
 * 智能提醒插件。
 * 在专注间隙、长时间未专注等时机发送提醒。
 */
class NotificationPlugin : Plugin {

    override val id = PluginId("com.yijianzhongqin.plugin.notification")
    override val name = "智能提醒"
    override val version = SemVer(1, 0, 0)
    override val description = "专注间隙提醒与推送"
    override val icon = 0
    override val dependencies = emptyList<PluginId>()
    override val permissions = emptyList<String>()

    private lateinit var ctx: PluginContext

    override suspend fun onLoad(context: PluginContext) {
        ctx = context
        Log.i(TAG, "NotificationPlugin loaded")

        // 订阅专注完成事件，休息提醒
        context.eventBus.subscribe<AppEvent.FocusCompleted> { event ->
            Log.i(TAG, "专注完成，休息一下吧！(${event.totalMinutes}分钟)")
        }

        // 订阅好感度变化，低好感时安抚
        context.eventBus.subscribe<AppEvent.AffectionChanged> { event ->
            if (event.newValue < 30) {
                Log.i(TAG, "好感度过低，需要安抚用户")
            }
        }
    }

    override suspend fun onEnable() {}
    override suspend fun onDisable() {}
    override suspend fun onUnload() {}

    companion object {
        private const val TAG = "NotificationPlugin"
    }
}
