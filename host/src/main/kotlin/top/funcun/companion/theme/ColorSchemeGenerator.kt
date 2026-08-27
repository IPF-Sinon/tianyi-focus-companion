package top.funcun.companion.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

/**
 * 从种子色生成 Material3 配色（基于 MaterialKolor）。
 */
object ColorSchemeGenerator {

    /**
     * @param seedHex 种子色，如 "#4A90E2"
     * @param isDark 是否暗色
     * @param styleName 配色风格（对齐 FolkPatch colorStyle）
     */
    fun generate(seedHex: String, isDark: Boolean, styleName: String): ColorScheme {
        val seed = parseColor(seedHex) ?: Color(0xFF4A90E2)
        val style = paletteStyleOf(styleName)
        return dynamicColorScheme(
            seedColor = seed,
            isDark = isDark,
            style = style,
        )
    }

    private fun paletteStyleOf(name: String): PaletteStyle = when (name.uppercase()) {
        "TONAL_SPOT" -> PaletteStyle.TonalSpot
        "VIBRANT" -> PaletteStyle.Vibrant
        "EXPRESSIVE" -> PaletteStyle.Expressive
        "SPRITZ", "NEUTRAL" -> PaletteStyle.Neutral
        "MONOCHROME" -> PaletteStyle.Monochrome
        "FIDELITY" -> PaletteStyle.Fidelity
        "CONTENT" -> PaletteStyle.Content
        "RAINBOW" -> PaletteStyle.Rainbow
        "FRUIT_SALAD" -> PaletteStyle.FruitSalad
        else -> PaletteStyle.TonalSpot
    }

    /** 解析 #RRGGBB / #AARRGGBB */
    fun parseColor(hex: String): Color? {
        return try {
            val s = hex.trim().removePrefix("#")
            val v = when (s.length) {
                6 -> 0xFF000000L or s.toLong(16)
                8 -> s.toLong(16)
                else -> return null
            }
            Color(v.toInt())
        } catch (e: Exception) {
            null
        }
    }
}