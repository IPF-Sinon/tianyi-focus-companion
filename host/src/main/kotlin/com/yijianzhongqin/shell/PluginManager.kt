package com.yijianzhongqin.shell

import android.content.Context
import androidx.compose.runtime.Composable
import android.util.Log
import com.yijianzhongqin.sdk.Plugin
import com.yijianzhongqin.sdk.PluginContext
import com.yijianzhongqin.sdk.event.EventBus
import com.yijianzhongqin.sdk.util.PluginId
import dalvik.system.DexClassLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * 插件管理器。
 * 负责插件生命周期管理。
 * Phase 1 采用编译内置插件方式，后续支持外部 .tianyi-plugin 动态加载。
 */
class PluginManager(private val context: Context) {

    companion object {
        private const val TAG = "PluginManager"

        // 内置插件入口类列表
        private val BUILTIN_PLUGINS = listOf(
            "com.yijianzhongqin.plugin.onboarding.OnboardingPlugin",
            "com.yijianzhongqin.plugin.focus.FocusEnginePlugin",
            "com.yijianzhongqin.plugin.affection.AffectionPlugin",
            "com.yijianzhongqin.plugin.character.TianyiCharacterPlugin",
            "com.yijianzhongqin.plugin.voice.VoiceConversationPlugin",
            "com.yijianzhongqin.plugin.patrol.PatrolVlmPlugin",
            "com.yijianzhongqin.plugin.enforce.EnforceBlockPlugin",
            "com.yijianzhongqin.plugin.enforce.lock.EnforceLockPlugin",
            "com.yijianzhongqin.plugin.honor.HonorPlugin",
            "com.yijianzhongqin.plugin.stats.StatisticsPlugin",
            "com.yijianzhongqin.plugin.soundscape.SoundscapePlugin",
            "com.yijianzhongqin.plugin.peace.PeaceZonePlugin",
            "com.yijianzhongqin.plugin.notification.NotificationPlugin",
            "com.yijianzhongqin.plugin.account.AccountPlugin",
            "com.yijianzhongqin.plugin.market.PluginMarketPlugin",
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val eventBus = EventBus()
    private val pluginContexts = mutableMapOf<PluginId, PluginContext>()
    private val plugins = mutableMapOf<PluginId, Plugin>()
    private val pluginStates = mutableMapOf<PluginId, PluginState>()
    private val dexLoaders = mutableMapOf<PluginId, DexClassLoader>()
    private val slotContents = mutableMapOf<com.yijianzhongqin.sdk.slot.UISlot, MutableList<@Composable () -> Unit>>()

    /** 获取事件总线实例 */
    fun getEventBus(): EventBus = eventBus

    /** 初始化：加载内置插件 */
    fun initialize() {
        Log.i(TAG, "PluginManager initializing...")

        BUILTIN_PLUGINS.forEach { className ->
            runCatching {
                val clazz = Class.forName(className)
                val plugin = clazz.getDeclaredConstructor().newInstance() as Plugin
                plugins[plugin.id] = plugin

                // 创建 PluginContext
                val pluginContext = createPluginContext(plugin.id)
                pluginContexts[plugin.id] = pluginContext

                // 加载插件
                runBlocking {
                    plugin.onLoad(pluginContext)
                    plugin.onEnable()
                }
                pluginStates[plugin.id] = PluginState.ENABLED
                Log.i(TAG, "Plugin loaded: ${plugin.id} v${plugin.version}")
            }.onFailure { e ->
                Log.e(TAG, "Failed to load plugin: $className", e)
                // 遇到不存在的 demo 插件也继续
            }
        }

        Log.i(TAG, "PluginManager initialized. Loaded ${plugins.size} plugins.")
    }

    private fun createPluginContext(pluginId: PluginId): PluginContext {
        return object : PluginContext {
            override val eventBus: EventBus = this@PluginManager.eventBus

            override val dataStore: com.yijianzhongqin.sdk.PluginDataStore =
                object : com.yijianzhongqin.sdk.PluginDataStore {
                    private val prefs = context.getSharedPreferences(
                        "plugin_${pluginId.value}",
                        Context.MODE_PRIVATE,
                    )
                    override fun put(key: String, value: String) { prefs.edit().putString(key, value).apply() }
                    override fun put(key: String, value: Long) { prefs.edit().putLong(key, value).apply() }
                    override fun put(key: String, value: Int) { prefs.edit().putInt(key, value).apply() }
                    override fun put(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
                    override fun put(key: String, value: Float) { prefs.edit().putFloat(key, value).apply() }
                    override fun getString(key: String, defaultValue: String): String = prefs.getString(key, defaultValue) ?: defaultValue
                    override fun getLong(key: String, defaultValue: Long): Long = prefs.getLong(key, defaultValue)
                    override fun getInt(key: String, defaultValue: Int): Int = prefs.getInt(key, defaultValue)
                    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = prefs.getBoolean(key, defaultValue)
                    override fun getFloat(key: String, defaultValue: Float): Float = prefs.getFloat(key, defaultValue)
                    override fun observeString(key: String, defaultValue: String): kotlinx.coroutines.flow.Flow<String> = kotlinx.coroutines.flow.flowOf(getString(key, defaultValue))
                    override fun observeInt(key: String, defaultValue: Int): kotlinx.coroutines.flow.Flow<Int> = kotlinx.coroutines.flow.flowOf(getInt(key, defaultValue))
                    override fun observeBoolean(key: String, defaultValue: Boolean): kotlinx.coroutines.flow.Flow<Boolean> = kotlinx.coroutines.flow.flowOf(getBoolean(key, defaultValue))
                    override fun remove(key: String) { prefs.edit().remove(key).apply() }
                    override fun clear() { prefs.edit().clear().apply() }
                }

            override val database: com.yijianzhongqin.sdk.PluginDatabase =
                object : com.yijianzhongqin.sdk.PluginDatabase {
                    override fun <T : androidx.room.RoomDatabase> getDatabase(dbClass: Class<T>): T {
                        // Phase 1 简化：未提供数据库实例
                        throw UnsupportedOperationException("Database not yet supported in Phase 1")
                    }
                    override fun close() {}
                }

            override val resourceProvider: com.yijianzhongqin.sdk.ResourceProvider =
                object : com.yijianzhongqin.sdk.ResourceProvider {
                    override fun getString(resId: Int): String = context.getString(resId)
                    override fun getDrawable(resId: Int): Any? = context.getDrawable(resId)
                    override fun getColor(resId: Int): Int = context.getColor(resId)
                }

            override fun <T> registerService(token: com.yijianzhongqin.sdk.util.ServiceToken<T>, impl: T) {
                // Phase 1 简化：直接存内存
                services[token.name] = impl
            }

            override fun <T> getService(token: com.yijianzhongqin.sdk.util.ServiceToken<T>): T? {
                @Suppress("UNCHECKED_CAST")
                return services[token.name] as? T
            }

            override fun registerUI(slot: com.yijianzhongqin.sdk.slot.UISlot, composable: @Composable () -> Unit) {
                slotContents.getOrPut(slot) { mutableListOf() }.add(composable)
            }

            override fun unregisterUI(slot: com.yijianzhongqin.sdk.slot.UISlot) {
                slotContents.remove(slot)
            }

            override fun getHostContext(): Context = this@PluginManager.context
        }
    }

    private val services = mutableMapOf<String, Any?>()

    /** 获取指定插槽的所有 UI 组件 */
    fun getSlotContents(slot: com.yijianzhongqin.sdk.slot.UISlot): List<@Composable () -> Unit> {
        return slotContents[slot] ?: emptyList()
    }

    /** 加载一个外部插件包 */
    fun loadExternal(pluginDir: File): Result<Plugin> = runCatching {
        val manifestFile = pluginDir.resolve("plugin.toml")
        require(manifestFile.exists()) { "Missing plugin.toml in $pluginDir" }

        val manifest = parseManifest(manifestFile)
        val pluginId = PluginId(manifest["id"] ?: error("Missing plugin id"))
        val entryClass = manifest["entry"] ?: error("Missing entry class")

        val dexFile = pluginDir.resolve("classes.dex")
        require(dexFile.exists()) { "Missing classes.dex in $pluginDir" }

        val optimizedDir = File(context.cacheDir, "dex/${pluginId.value}")
        optimizedDir.mkdirs()

        val loader = DexClassLoader(
            dexFile.absolutePath,
            optimizedDir.absolutePath,
            null,
            context.classLoader
        )

        val pluginClass = loader.loadClass(entryClass)
        val plugin = pluginClass.getDeclaredConstructor().newInstance() as Plugin

        dexLoaders[pluginId] = loader
        plugins[pluginId] = plugin
        pluginStates[pluginId] = PluginState.LOADED

        Log.i(TAG, "Plugin loaded: ${plugin.id} v${plugin.version}")
        plugin
    }

    /** 启用插件 */
    suspend fun enablePlugin(pluginId: PluginId): Result<Unit> = runCatching {
        val plugin = plugins[pluginId] ?: error("Plugin not loaded: $pluginId")
        val ctx = pluginContexts[pluginId] ?: error("Plugin context not created")
        plugin.onEnable()
        pluginStates[pluginId] = PluginState.ENABLED
        Log.i(TAG, "Plugin enabled: $pluginId")
    }

    /** 禁用插件 */
    suspend fun disablePlugin(pluginId: PluginId): Result<Unit> = runCatching {
        val plugin = plugins[pluginId] ?: error("Plugin not loaded: $pluginId")
        plugin.onDisable()
        pluginStates[pluginId] = PluginState.DISABLED
        Log.i(TAG, "Plugin disabled: $pluginId")
    }

    /** 卸载插件 */
    suspend fun unloadPlugin(pluginId: PluginId): Result<Unit> = runCatching {
        val plugin = plugins[pluginId] ?: error("Plugin not loaded: $pluginId")
        plugin.onUnload()
        plugins.remove(pluginId)
        pluginContexts.remove(pluginId)
        dexLoaders.remove(pluginId)
        pluginStates.remove(pluginId)
        Log.i(TAG, "Plugin unloaded: $pluginId")
    }

    /** 获取插件状态 */
    fun getPluginState(pluginId: PluginId): PluginState =
        pluginStates[pluginId] ?: PluginState.NOT_LOADED

    /** 获取所有已加载的插件 */
    fun getLoadedPlugins(): Map<PluginId, Plugin> = plugins.toMap()

    /** 异步关闭所有插件 */
    suspend fun shutdown() {
        plugins.keys.toList().forEach { pluginId ->
            runCatching { unloadPlugin(pluginId) }
        }
        Log.i(TAG, "PluginManager shutdown complete")
    }

    /** 同步关闭（用于 Application.onTerminate） */
    fun shutdownBlocking() {
        runBlocking { shutdown() }
    }

    private fun parseManifest(file: File): Map<String, String> {
        val result = mutableMapOf<String, String>()
        file.readLines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("[")) {
                val parts = trimmed.split("=", limit = 2)
                if (parts.size == 2) {
                    result[parts[0].trim()] = parts[1].trim().trim('"')
                }
            }
        }
        return result
    }
}

enum class PluginState {
    NOT_LOADED,
    LOADED,
    ENABLED,
    DISABLED,
    ERROR
}
