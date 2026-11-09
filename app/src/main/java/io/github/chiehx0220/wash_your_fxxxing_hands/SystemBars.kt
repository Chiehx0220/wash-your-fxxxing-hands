package io.github.chiehx0220.wash_your_fxxxing_hands

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

// API 30+: edge-to-edge, root padded by bars + cutout + IME; older: solid bars, adjustResize
object SystemBars {

    @Suppress("DEPRECATION")
    fun apply(activity: Activity, root: View, surface: Int, lightTheme: Boolean) {
        val window = activity.window
        if (Build.VERSION.SDK_INT >= 30) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
            window.isNavigationBarContrastEnforced = false
            ViewCompat.setOnApplyWindowInsetsListener(root, ::padToInsets)
        } else {
            window.statusBarColor = surface
            window.navigationBarColor = surface
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = lightTheme
            isAppearanceLightNavigationBars = lightTheme
        }
    }

    private fun padToInsets(view: View, insets: WindowInsetsCompat): WindowInsetsCompat {
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
        view.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
        return WindowInsetsCompat.CONSUMED
    }
}
