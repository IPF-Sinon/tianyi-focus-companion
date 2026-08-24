package top.funcun.companion.sdk

import android.content.Context
import androidx.compose.runtime.Composable
import top.funcun.companion.sdk.event.EventBus
import top.funcun.companion.sdk.slot.UISlot
import top.funcun.companion.sdk.util.ServiceToken

/**
 * 插件上下文。
 * 插件通过此接口与宿主交互。
 */
interface PluginContext {

    /** 事件总线，用于插件间通信 */
    val eventBus: EventBus

    /** 插件私有 KV 存储 */
    val dataStore: PluginDataStore

    /** 插件私有数据库 */
    val database: PluginDatabase

    /** 资源提供器 */
    val resourceProvider: ResourceProvider

    /**
     * 注册一个服务，供其他插件发现和使用。
     */
    fun <T> registerService(token: ServiceToken<T>, impl: T)

    /**
     * 获取其他插件注册的服务。
     */
    fun <T> getService(token: ServiceToken<T>): T?

    /**
     * 注册一个 UI 组件到指定插槽。
     * 插件可以注册多个组件到同一插槽，宿主按注册顺序渲染。
     */
    fun registerUI(slot: UISlot, composable: @Composable () -> Unit)

    /**
     * 从指定插槽移除之前注册的 UI 组件。
     */
    fun unregisterUI(slot: UISlot)

    /** 获取宿主 Android Context */
    fun getHostContext(): Context
}

/**
 * 资源提供器接口。
 * 插件通过此接口访问宿主提供的资源。
 */
interface ResourceProvider {
    fun getString(resId: Int): String
    fun getDrawable(resId: Int): Any?
    fun getColor(resId: Int): Int
}
