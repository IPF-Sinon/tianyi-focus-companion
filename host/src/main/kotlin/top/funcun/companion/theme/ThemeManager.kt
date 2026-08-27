package top.funcun.companion.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File

/**
 * 主题管理器。
 *
 * 负责当前主题配置的加载/保存，以及从用户主题目录读取导入的主题。
 * 单例，进程内共享。
 */
object ThemeManager {

    private const val PREFS = "theme_prefs"
    private const val KEY_CONFIG = "theme_config_json"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** 当前主题配置（Compose 可观察） */
    var config by mutableStateOf(ThemeConfig.DEFAULT)
        private set

    /** 初始化：优先读用户导入主题目录，其次读已保存配置，否则默认 */
    fun init(context: Context) {
        val imported = ThemeIO.loadConfig(ThemeIO.themeDir(context))
        config = imported ?: loadSaved(context) ?: ThemeConfig.DEFAULT
    }

    /** 应用一份配置并持久化 */
    fun apply(context: Context, newConfig: ThemeConfig) {
        config = newConfig
        save(context, newConfig)
    }

    /** 局部更新（拷贝修改） */
    fun update(context: Context, transform: (ThemeConfig) -> ThemeConfig) {
        apply(context, transform(config))
    }

    /** 恢复默认主题（并删除用户主题目录） */
    fun reset(context: Context) {
        ThemeIO.reset(context)
        config = ThemeConfig.DEFAULT
        save(context, config)
    }

    /** 背景图文件（用户主题目录内），不存在返回 null */
    fun backgroundFile(context: Context): File? {
        val f = File(ThemeIO.themeDir(context), ThemeConfig.BACKGROUND_FILE)
        return if (config.isBackgroundEnabled && f.exists()) f else null
    }

    /** 字体文件，不存在返回 null */
    fun fontFile(context: Context): File? {
        val f = File(ThemeIO.themeDir(context), ThemeConfig.FONT_FILE)
        return if (config.isFontEnabled && f.exists()) f else null
    }

    private fun save(context: Context, cfg: ThemeConfig) {
        runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_CONFIG, json.encodeToString(cfg)).apply()
        }
    }

    private fun loadSaved(context: Context): ThemeConfig? {
        val s = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_CONFIG, null) ?: return null
        return runCatching { json.decodeFromString<ThemeConfig>(s) }.getOrNull()
    }
}