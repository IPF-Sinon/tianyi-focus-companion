package com.yijianzhongqin.sdk.util

/** 服务注册/发现 Token */
class ServiceToken<T>(val name: String) {
    companion object {
        fun <T> create(name: String): ServiceToken<T> = ServiceToken(name)
    }
}
