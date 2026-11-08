package com.chieh.handwashaudit

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.RequiresApi
import android.R.color as sys

private typealias RoleTable = Map<String, Pair<Int, Int>>  // role -> (light, dark)

// Material 3 colour roles: dynamic color (API 31+), baseline scheme fallback
object Palette {

    fun isDark(context: Context): Boolean =
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES

    fun tokens(context: Context): Map<String, Int> {
        val dark = isDark(context)
        val table = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamic(context) else BASELINE
        return (table + ERROR).mapValues { (_, pair) -> if (dark) pair.second else pair.first }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun dynamic(c: Context): RoleTable {
        fun role(light: Int, dark: Int) = c.getColor(light) to c.getColor(dark)
        return mapOf(
            "primary" to role(sys.system_accent1_600, sys.system_accent1_200),
            "onPrimary" to role(sys.system_accent1_0, sys.system_accent1_800),
            "primaryContainer" to role(sys.system_accent1_100, sys.system_accent1_700),
            "onPrimaryContainer" to role(sys.system_accent1_900, sys.system_accent1_100),
            "secondaryContainer" to role(sys.system_accent2_100, sys.system_accent2_700),
            "onSecondaryContainer" to role(sys.system_accent2_900, sys.system_accent2_100),
            "tertiaryContainer" to role(sys.system_accent3_100, sys.system_accent3_700),
            "onTertiaryContainer" to role(sys.system_accent3_900, sys.system_accent3_100),
            "surface" to role(sys.system_neutral1_10, sys.system_neutral1_900),
            "onSurface" to role(sys.system_neutral1_900, sys.system_neutral1_100),
            "onSurfaceVariant" to role(sys.system_neutral2_700, sys.system_neutral2_200),
            "outline" to role(sys.system_neutral2_500, sys.system_neutral2_400),
            "outlineVariant" to role(sys.system_neutral2_200, sys.system_neutral2_700),
        )
    }

    private fun argb(light: Long, dark: Long) = light.toInt() to dark.toInt()

    private val BASELINE: RoleTable = mapOf(
        "primary" to argb(0xFF6750A4, 0xFFD0BCFF),
        "onPrimary" to argb(0xFFFFFFFF, 0xFF381E72),
        "primaryContainer" to argb(0xFFEADDFF, 0xFF4F378B),
        "onPrimaryContainer" to argb(0xFF21005D, 0xFFEADDFF),
        "secondaryContainer" to argb(0xFFE8DEF8, 0xFF4A4458),
        "onSecondaryContainer" to argb(0xFF1D192B, 0xFFE8DEF8),
        "tertiaryContainer" to argb(0xFFFFD8E4, 0xFF633B48),
        "onTertiaryContainer" to argb(0xFF31111D, 0xFFFFD8E4),
        "surface" to argb(0xFFFEF7FF, 0xFF141218),
        "onSurface" to argb(0xFF1D1B20, 0xFFE6E0E9),
        "onSurfaceVariant" to argb(0xFF49454F, 0xFFCAC4D0),
        "outline" to argb(0xFF79747E, 0xFF938F99),
        "outlineVariant" to argb(0xFFCAC4D0, 0xFF49454F),
    )

    // Static roles, wallpaper-independent
    private val ERROR: RoleTable = mapOf(
        "error" to argb(0xFFB3261E, 0xFFF2B8B5),
        "errorContainer" to argb(0xFFF9DEDC, 0xFF8C1D18),
        "onErrorContainer" to argb(0xFF410E0B, 0xFFF9DEDC),
    )
}
