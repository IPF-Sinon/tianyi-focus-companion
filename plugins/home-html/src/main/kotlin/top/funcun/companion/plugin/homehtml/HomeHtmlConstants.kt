package top.funcun.companion.plugin.homehtml

/**
 * 主题插件共享常量。
 * 宿主（host）也会引用这些值，用于定位用户主题目录。
 */
object HomeHtmlConstants {

    /** 用户自定义主题包目录（相对应用外部文件目录） */
    const val USER_THEME_DIR = "themes/current"

    /** 内置主题资源路径（相对 assets） */
    const val BUILTIN_THEME_ASSET = "theme/index.html"

    /** 虚拟域名（用户主题通过它加载，避免 file:// 同源限制） */
    const val USER_THEME_BASE_URL = "https://theme.local/index.html"
}