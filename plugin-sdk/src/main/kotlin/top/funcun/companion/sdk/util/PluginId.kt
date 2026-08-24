package top.funcun.companion.sdk.util

import kotlinx.serialization.Serializable

/** 插件唯一标识 */
@JvmInline
@Serializable
value class PluginId(val value: String) {
    init {
        require(value.matches(Regex("^[a-z][a-z0-9._-]+$"))) {
            "Invalid PluginId: $value"
        }
    }

    override fun toString(): String = value
}
