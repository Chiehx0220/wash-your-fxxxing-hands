package io.github.chiehx0220.wash_your_fxxxing_hands

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.TextView

// M3 Expressive loading overlay
class SplashView(context: Context, tokens: Map<String, Int>) : LinearLayout(context) {

    private var dismissed = false

    private val badge: View
    private val title: TextView
    private val subtitle: TextView
    private val indicator: View

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        isClickable = true  // consume touches
        setBackgroundColor(tokens.getValue("surface"))

        val icon = requireNotNull(context.getDrawable(R.drawable.ic_droplet)).mutate()
            .apply { setTint(tokens.getValue("onPrimaryContainer")) }

        badge = ShapeBadge(context, tokens.getValue("primaryContainer"), icon, dp(56))
        title = label(context.getString(R.string.app_name), 32f, tokens.getValue("onSurface"), "sans-serif-medium")
        subtitle = label(SUBTITLE, 14f, tokens.getValue("onSurfaceVariant"), "sans-serif")
        indicator = MorphingLoadingIndicator(context, tokens.getValue("primary"), tokens.getValue("secondaryContainer"))

        addView(badge, LayoutParams(dp(128), dp(128)))
        addView(title, centered().apply { topMargin = dp(32) })
        addView(subtitle, centered().apply { topMargin = dp(6) })
        addView(indicator, centered().apply { topMargin = dp(56); width = dp(64); height = dp(64) })

        playEntrance()
    }

    fun dismiss() {
        if (dismissed) return
        dismissed = true
        animate().alpha(0f).scaleX(EXIT_SCALE).scaleY(EXIT_SCALE).setDuration(FADE_MS)
            .withEndAction { (parent as? ViewGroup)?.removeView(this) }
    }

    private fun label(text: String, sizeSp: Float, color: Int, family: String) = TextView(context).apply {
        this.text = text
        gravity = Gravity.CENTER_HORIZONTAL
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(color)
        typeface = Typeface.create(family, Typeface.NORMAL)
    }

    private fun centered() = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

    // Staggered spring entrance
    private fun playEntrance() {
        badge.scaleX = 0.3f
        badge.scaleY = 0.3f
        badge.alpha = 0f
        badge.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(650)
            .setInterpolator(OvershootInterpolator(2.4f)).start()

        listOf(title, subtitle, indicator).forEachIndexed { i, view ->
            view.alpha = 0f
            view.translationY = dp(20).toFloat()
            view.animate().alpha(1f).translationY(0f).setStartDelay(120L + i * 90L).setDuration(450)
                .setInterpolator(DecelerateInterpolator(1.8f)).start()
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val SUBTITLE = "大風吹 吹什麼 吹沒洗手的人"
        const val FADE_MS = 240L
        const val EXIT_SCALE = 1.06f
    }
}
