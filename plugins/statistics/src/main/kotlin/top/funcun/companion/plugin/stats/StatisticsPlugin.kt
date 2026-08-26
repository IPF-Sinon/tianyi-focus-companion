package top.funcun.companion.plugin.stats

import android.content.Context
import android.util.Log
import top.funcun.companion.sdk.ConfigField
import top.funcun.companion.sdk.ConfigFieldType
import top.funcun.companion.sdk.ConfigOption
import top.funcun.companion.sdk.ConfigSchema
import top.funcun.companion.sdk.ConfigSection
import top.funcun.companion.sdk.NavItem
import top.funcun.companion.sdk.Plugin
import top.funcun.companion.sdk.PluginContext
import top.funcun.companion.sdk.event.AppEvent
import top.funcun.companion.sdk.util.PluginId
import top.funcun.companion.sdk.util.SemVer

/**
 * 数据统计插件。
 *
 * 记录专注会话历史（写入插件私有存储），通过 [getNavData] 向主题提供 JSON 数据。
 * 通过 [configSchema] 声明配置项，主题渲染配置表单。
 */
class StatisticsPlugin : Plugin {

    override val id = PluginId("top.funcun.companion.plugin.statistics")
    override val name = "数据统计"
    override val version = SemVer(1, 0, 0)
    override val description = "记录专注历史并向主题提供统计数据"
    override val icon = 0
    override val dependencies = emptyList<PluginId>()
    override val permissions = emptyList<String>()

    override val builtin = true
    override val iconEmoji = "📊"

    /** 声明导航项：主题据此渲染「统计」页 */
    override val navItems = listOf(
        NavItem(
            id = "stats",
            label = "统计",
            icon = "📊",
            order = 20,
        ),
    )

    /** 配置项声明：主题渲染表单 */
    override val configSchema = ConfigSchema(
        sections = listOf(
            ConfigSection(
                title = "统计偏好",
                fields = listOf(
                    ConfigField(
                        key = "week_start",
                        label = "每周起始日",
                        type = ConfigFieldType.SELECT,
                        defaultValue = "monday",
                        options = listOf(
                            ConfigOption("monday", "周一"),
                            ConfigOption("sunday", "周日"),
                        ),
                    ),
                    ConfigField(
                        key = "keep_days",
                        label = "历史保留天数",
                        type = ConfigFieldType.INT,
                        defaultValue = "90",
                        description = "超过此天数的记录会被清理",
                        min = 7,
                        max = 365,
                    ),
                    ConfigField(
                        key = "show_streak",
                        label = "显示连续天数",
                        type = ConfigFieldType.BOOLEAN,
                        defaultValue = "true",
                    ),
                ),
            ),
        ),
    )

    private lateinit var ctx: PluginContext
    private lateinit var store: StatsStore

    override suspend fun onLoad(context: PluginContext) {
        ctx = context
        store = StatsStore(context.getHostContext())

        // 记录专注完成事件
        context.eventBus.subscribe<AppEvent.FocusCompleted> { event ->
            store.recordSession(event.totalMinutes)
            Log.i(TAG, "记录专注会话: ${event.totalMinutes} 分钟")
        }

        Log.i(TAG, "StatisticsPlugin loaded")
    }

    override suspend fun onEnable() {}
    override suspend fun onDisable() {}
    override suspend fun onUnload() {}

    /** 向主题提供统计数据（JSON） */
    override suspend fun getNavData(navId: String): String? {
        if (navId != "stats") return null
        return store.toJson()
    }

    companion object {
        private const val TAG = "StatisticsPlugin"
    }
}

/**
 * 专注历史存储。
 * 用 SharedPreferences 存 CSV 格式：epochDay:minutes,epochDay:minutes,...
 */
internal class StatsStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 记录一次完成的专注 */
    fun recordSession(minutes: Int) {
        val today = todayEpochDay()
        val current = prefs.getInt(keyDay(today), 0)
        prefs.edit()
            .putInt(keyDay(today), current + minutes)
            .putInt(KEY_TOTAL_SESSIONS, prefs.getInt(KEY_TOTAL_SESSIONS, 0) + 1)
            .putInt(KEY_TOTAL_MINUTES, prefs.getInt(KEY_TOTAL_MINUTES, 0) + minutes)
            .apply()
    }

    /** 输出 JSON：今日/本周/总计 + 最近 7 天曲线 */
    fun toJson(): String {
        val today = todayEpochDay()
        val todayMinutes = prefs.getInt(keyDay(today), 0)

        val last7 = (6 downTo 0).map { offset ->
            val day = today - offset
            day to prefs.getInt(keyDay(day), 0)
        }
        val weekMinutes = last7.sumOf { it.second }
        val totalSessions = prefs.getInt(KEY_TOTAL_SESSIONS, 0)
        val totalMinutes = prefs.getInt(KEY_TOTAL_MINUTES, 0)

        // 连续天数：从今天往前数，直到某天为 0
        var streak = 0
        var cursor = today
        while (prefs.getInt(keyDay(cursor), 0) > 0) {
            streak++
            cursor--
        }

        val daily = last7.joinToString(",") { (day, minutes) ->
            """{"epochDay":$day,"minutes":$minutes}"""
        }

        return """
        {
          "todayMinutes": $todayMinutes,
          "weekMinutes": $weekMinutes,
          "totalSessions": $totalSessions,
          "totalMinutes": $totalMinutes,
          "streakDays": $streak,
          "daily": [$daily]
        }
        """.trimIndent()
    }

    private fun todayEpochDay(): Long = System.currentTimeMillis() / 86_400_000L

    private fun keyDay(epochDay: Long) = "day_$epochDay"

    companion object {
        private const val PREFS_NAME = "plugin_top.funcun.companion.plugin.statistics"
        private const val KEY_TOTAL_SESSIONS = "total_sessions"
        private const val KEY_TOTAL_MINUTES = "total_minutes"
    }
}