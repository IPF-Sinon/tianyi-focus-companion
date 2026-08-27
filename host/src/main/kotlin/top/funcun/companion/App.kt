package top.funcun.companion

import android.app.Application
import top.funcun.companion.shell.PluginManager
import top.funcun.companion.theme.ThemeManager
import dagger.hilt.android.HiltAndroidApp

/**
 * 依见钟勤 - 应用入口
 */
@HiltAndroidApp
class App : Application() {

    lateinit var pluginManager: PluginManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        ThemeManager.init(this)
        pluginManager = PluginManager(this)
        pluginManager.initialize()
    }

    override fun onTerminate() {
        pluginManager.shutdownBlocking()
        super.onTerminate()
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
