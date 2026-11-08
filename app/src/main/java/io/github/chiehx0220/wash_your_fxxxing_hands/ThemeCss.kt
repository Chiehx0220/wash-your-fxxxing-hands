package io.github.chiehx0220.wash_your_fxxxing_hands

import android.content.Context
import android.net.Uri

// Stylesheet = generated :root variables + assets/theme.css
object ThemeCss {

    private const val ASSET = "theme.css"
    private const val SPRING = "cubic-bezier(0.34, 1.56, 0.64, 1)"
    private val TONES = mapOf("low" to 9, "high" to 14, "highest" to 19)  // % primary mixed into surface
    private val UPPERCASE = Regex("[A-Z]")

    fun build(context: Context): String {
        val tokens = Palette.tokens(context)
        val dark = Palette.isDark(context)

        val vars = linkedMapOf<String, String>()
        tokens.forEach { (role, color) -> vars[kebab(role)] = hex(color) }
        TONES.forEach { (name, percent) ->
            vars["container-$name"] = "color-mix(in srgb, var(--m3-primary) $percent%, var(--m3-surface))"
        }
        vars["chevron"] = chevron(hex(tokens.getValue("onSurfaceVariant")))
        vars["close-filter"] = if (dark) "invert(1)" else "none"
        vars["spring"] = SPRING

        val scheme = if (dark) "dark" else "light"
        val root = vars.entries.joinToString("", ":root{color-scheme:$scheme;", "}") { "--m3-${it.key}:${it.value};" }
        return root + "\n" + context.readAsset(ASSET)
    }

    private fun kebab(role: String) = role.replace(UPPERCASE) { "-" + it.value.lowercase() }

    private fun hex(color: Int) = String.format("#%06X", color and 0xFFFFFF)

    private fun chevron(color: String) =
        "url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24'%3E" +
            "%3Cpath fill='${Uri.encode(color)}' d='M7 10l5 5 5-5z'/%3E%3C/svg%3E\")"
}
