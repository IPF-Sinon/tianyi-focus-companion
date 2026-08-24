package top.funcun.companion.sdk

import androidx.room.RoomDatabase

/**
 * 插件私有数据库。
 * 每个插件通过此接口获取自己的 Room 数据库实例。
 * 插件不直接创建 Room 数据库，而是通过宿主统一管理。
 */
interface PluginDatabase {
    fun <T : RoomDatabase> getDatabase(dbClass: Class<T>): T
    fun close()
}
