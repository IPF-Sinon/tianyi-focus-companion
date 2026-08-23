package com.yijianzhongqin.sdk

import kotlinx.coroutines.flow.Flow

/**
 * 插件私有数据存储。
 * 每个插件拥有独立命名空间，互不干扰。
 */
interface PluginDataStore {

    fun put(key: String, value: String)
    fun put(key: String, value: Long)
    fun put(key: String, value: Int)
    fun put(key: String, value: Boolean)
    fun put(key: String, value: Float)

    fun getString(key: String, defaultValue: String = ""): String
    fun getLong(key: String, defaultValue: Long = 0L): Long
    fun getInt(key: String, defaultValue: Int = 0): Int
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
    fun getFloat(key: String, defaultValue: Float = 0f): Float

    fun observeString(key: String, defaultValue: String = ""): Flow<String>
    fun observeInt(key: String, defaultValue: Int = 0): Flow<Int>
    fun observeBoolean(key: String, defaultValue: Boolean = false): Flow<Boolean>

    fun remove(key: String)
    fun clear()
}
