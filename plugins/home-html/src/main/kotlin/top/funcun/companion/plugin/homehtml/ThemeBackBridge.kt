package top.funcun.companion.plugin.homehtml

import android.webkit.WebView

/**
 * 主题返回键桥。
 *
 * 宿主（UIShell 的 BackHandler）通过它询问主题是否消费返回键：
 * 调用主题的 `window.onTianyiBackPressed()`，返回 true 表示主题已处理
 * （如收起配置面板、切回首页），false 则由宿主退出应用。
 */
object ThemeBackBridge {

    @Volatile
    private var webView: WebView? = null

    /** 由主题 WebView 创建时注册 */
    fun attach(view: WebView) {
        webView = view
    }

    /** 由主题 WebView 销毁时注销 */
    fun detach(view: WebView) {
        if (webView === view) webView = null
    }

    /** 是否存在活跃的主题 WebView */
    fun isAttached(): Boolean = webView != null

    /**
     * 询问主题是否消费返回键。
     *
     * JS 求值是异步的，这里用回调把结果交回宿主。
     */
    fun dispatchBack(onResult: (consumed: Boolean) -> Unit) {
        val view = webView
        if (view == null) {
            onResult(false)
            return
        }
        view.post {
            view.evaluateJavascript(
                "(function(){ try { return window.onTianyiBackPressed ? " +
                    "(window.onTianyiBackPressed() ? 'true' : 'false') : 'false'; } " +
                    "catch(e){ return 'false'; } })()",
            ) { value ->
                val consumed = value?.trim()?.trim('"') == "true"
                onResult(consumed)
            }
        }
    }
}