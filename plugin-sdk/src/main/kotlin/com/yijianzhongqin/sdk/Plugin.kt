package com.yijianzhongqin.sdk

import com.yijianzhongqin.sdk.util.PluginId
import com.yijianzhongqin.sdk.util.SemVer

/**
 * 插件接口。
 * 所有功能必须以插件形式实现此接口，通过 PluginManager 注册。
 */
interface Plugin {

    /** 唯一标识，如 "com.yijianzhongqin.plugin.focus" */
    val id: PluginId

    /** 显示名称，如 "番茄钟引擎" */
    val name: String

    /** 语义化版本号 */
    val version: SemVer

    /** 简短描述 */
    val description: String

    /** 图标资源 ID */
    val icon: Int

    /** 依赖的其他插件 ID */
    val dependencies: List<PluginId>

    /** 需要的 Android 运行时权限 */
    val permissions: List<String>

    /**
     * 插件被加载时调用。
     * 在此方法中注册事件监听器、UI 插槽、服务等。
     */
    suspend fun onLoad(context: PluginContext)

    /**
     * 插件被启用时调用。
     * 此时插件开始正常工作。
     */
    suspend fun onEnable()

    /**
     * 插件被禁用时调用。
     * 此时插件应停止所有工作，但保留数据。
     */
    suspend fun onDisable()

    /**
     * 插件被卸载时调用。
     * 此时插件应清理所有资源。
     */
    suspend fun onUnload()
}
