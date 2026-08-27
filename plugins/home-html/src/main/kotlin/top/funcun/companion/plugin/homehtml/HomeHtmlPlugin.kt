package top.funcun.companion.plugin.homehtml

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import top.funcun.companion.sdk.Plugin
import top.funcun.companion.sdk.PluginContext
import top.funcun.companion.sdk.ThemeHostService
import top.funcun.companion.sdk.ThemeHostServiceToken
import top.funcun.companion.sdk.slot.UISlot
import top.funcun.companion.sdk.util.PluginId
import top.funcun.companion.sdk.util.SemVer
import java.io.File

/**
 * 主题插件（WebView 主界面）。
 *
 * 用 WebView 渲染一个「主题包」接管应用全部界面：底栏、专注页、统计页、
 * 插件页、设置页都由主题的 HTML/CSS/JS 决定。
 *
 * 主题包目录（优先级从高到低）：
 * 1. `外部文件目录/themes/current/`（用户自定义主题包，含 index.html）
 * 2. `assets/theme/index.html`（内置默认主题）
 *
 * 主题通过注入的 `TianyiHost` JS 对象访问宿主能力（插件列表、导航项、
 * 配置读写、卸载、插件动作、统计数据等）。
 */
class HomeHtmlPlugin : Plugin {

    override val id = PluginId("top.funcun.companion.plugin.homehtml")
    override val name = "主题（HTML 主界面）"
    override val version = SemVer(1, 0, 0)
    override val description = "用 HTML 主题包接管全部界面，支持第三方主题"
    override val icon = 0
    override val dependencies = emptyList<PluginId>()
    override val permissions = emptyList<String>()

    override val builtin = true
    override val iconEmoji = "🌐"

    /**
     * 主题插件的配置页：自定义 HTML。
     * 主题从 Bridge 拿到 schema 后发现 customHtml 非空，会加载该 HTML 渲染配置界面。
     * 该页面提供「导入主题包」「恢复默认主题」功能。
     */
    override val configSchema = top.funcun.companion.sdk.ConfigSchema(
        customHtml = "config.html",
    )

    private lateinit var ctx: PluginContext
    private var themeHost: ThemeHostService? = null

    override suspend fun onLoad(context: PluginContext) {
        ctx = context
        themeHost = context.getService(ThemeHostServiceToken)
        Log.i(TAG, "HomeHtmlPlugin loaded (themeHost=${themeHost != null})")
    }

    override suspend fun onEnable() {
        // 注册为全屏主界面：宿主把 HOME_TOP 内容全屏渲染
        ctx.registerUI(UISlot.HOME_TOP) {
            ThemeWebView(
                hostContext = ctx.getHostContext(),
                themeHost = themeHost,
            )
        }
    }

    override suspend fun onDisable() {}
    override suspend fun onUnload() {}

    companion object {
        private const val TAG = "HomeHtmlPlugin"
    }
}

/**
 * 解析主题包根路径。
 * 用户主题存在则返回其目录，否则返回 null（使用内置 assets 主题）。
 */
internal fun resolveUserThemeDir(context: Context): File? {
    val dir = File(context.getExternalFilesDir(null), HomeHtmlConstants.USER_THEME_DIR)
    val index = File(dir, "index.html")
    return if (index.exists()) dir else null
}

/**
 * 主题 WebView：渲染主题包并注入 TianyiHost JS 接口。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ThemeWebView(
    hostContext: Context,
    themeHost: ThemeHostService?,
    modifier: Modifier = Modifier,
) {
    var webViewRef: WebView? = null

    // 从主题导入界面返回时重载主题（重新解析用户主题目录）
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                webViewRef?.reload()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webViewRef?.destroy()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                webViewRef = this
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true

                addJavascriptInterface(
                    TianyiHostBridge(themeHost),
                    "TianyiHost",
                )

                val userThemeDir = resolveUserThemeDir(ctx)
                webViewClient = if (userThemeDir != null) {
                    UserThemeWebViewClient(userThemeDir)
                } else {
                    WebViewClient()
                }

                if (userThemeDir != null) {
                    loadUrl(HomeHtmlConstants.USER_THEME_BASE_URL)
                } else {
                    loadUrl("file:///android_asset/theme/index.html")
                }
            }
        },
    )
}

/**
 * 用户主题 WebViewClient：把 https://theme.local/ 下的资源映射到外部主题目录文件。
 * 使用虚拟 https 域名以便 JS 正常工作（file:// 存在同源限制）。
 */
private class UserThemeWebViewClient(private val themeDir: File) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest?,
    ): WebResourceResponse? {
        val url = request?.url ?: return null
        if (url.host != "theme.local") return null

        val relative = url.path?.trimStart('/')?.ifEmpty { "index.html" } ?: "index.html"
        val file = File(themeDir, relative)
        if (!file.exists() || !file.canonicalPath.startsWith(themeDir.canonicalPath)) {
            return null
        }

        val mime = when (file.extension.lowercase()) {
            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js" -> "application/javascript"
            "json" -> "application/json"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "svg" -> "image/svg+xml"
            "woff", "woff2" -> "font/woff2"
            else -> "application/octet-stream"
        }
        return WebResourceResponse(mime, "UTF-8", file.inputStream())
    }
}

/**
 * 注入到主题的 JS 接口：window.TianyiHost。
 *
 * 所有方法返回 JSON 字符串，主题用 JSON.parse 解析。
 */
private class TianyiHostBridge(private val host: ThemeHostService?) {

    @JavascriptInterface
    fun getPlugins(): String = host?.getPluginsJson() ?: """{"plugins":[]}"""

    @JavascriptInterface
    fun getNavItems(): String = host?.getNavItemsJson() ?: """{"items":[]}"""

    @JavascriptInterface
    fun getConfigSchema(pluginId: String): String =
        host?.getConfigSchemaJson(pluginId) ?: "null"

    @JavascriptInterface
    fun readConfig(pluginId: String, key: String, defaultValue: String): String =
        host?.readConfig(pluginId, key, defaultValue) ?: defaultValue

    @JavascriptInterface
    fun writeConfig(pluginId: String, key: String, value: String) {
        host?.writeConfig(pluginId, key, value)
    }

    @JavascriptInterface
    fun uninstallPlugin(pluginId: String): Boolean =
        host?.uninstallPlugin(pluginId) ?: false

    @JavascriptInterface
    fun invokeAction(pluginId: String, actionId: String): String =
        host?.invokeAction(pluginId, actionId) ?: "null"

    @JavascriptInterface
    fun requestNavData(pluginId: String, navId: String): String =
        host?.requestNavData(pluginId, navId) ?: "null"

    @JavascriptInterface
    fun getAppInfo(): String = host?.getAppInfoJson() ?: """{"appName":"依见钟勤"}"""

    @JavascriptInterface
    fun getThemeInfo(): String = host?.getThemeInfoJson() ?: """{"installed":false,"source":"builtin"}"""

    @JavascriptInterface
    fun getCustomConfigHtml(pluginId: String): String =
        host?.getCustomConfigHtml(pluginId) ?: ""

    @JavascriptInterface
    fun importTheme(): Boolean = host?.importTheme() ?: false

    @JavascriptInterface
    fun resetTheme(): Boolean = host?.resetTheme() ?: false
}