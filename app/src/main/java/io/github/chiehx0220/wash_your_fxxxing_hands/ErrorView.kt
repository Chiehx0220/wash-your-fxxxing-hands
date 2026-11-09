package io.github.chiehx0220.wash_your_fxxxing_hands

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

// Main-frame load failure: M3 empty state with retry, replaces the browser error page
class ErrorView(context: Context, tokens: Map<String, Int>, onRetry: () -> Unit) : LinearLayout(context) {

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        isClickable = true  // consume touches
        visibility = GONE
        setPadding(dp(32), dp(32), dp(32), dp(32))
        setBackgroundColor(tokens.getValue("surface"))

        val icon = requireNotNull(context.getDrawable(R.drawable.ic_cloud_off)).mutate()
            .apply { setTint(tokens.getValue("onSecondaryContainer")) }
        val badge = ShapeBadge(context, tokens.getValue("secondaryContainer"), icon, dp(48))

        addView(badge, LayoutParams(dp(112), dp(112)))
        addView(label(R.string.error_title, 22f, tokens.getValue("onSurface"), "sans-serif-medium", 28))
        addView(label(R.string.error_message, 14f, tokens.getValue("onSurfaceVariant"), "sans-serif", 8))
        addView(retryButton(tokens, onRetry), LayoutParams(LayoutParams.WRAP_CONTENT, dp(48)).apply { topMargin = dp(32) })
    }

    fun show() {
        if (visibility == VISIBLE) return
        alpha = 0f
        visibility = VISIBLE
        animate().alpha(1f).setDuration(FADE_MS).start()
    }

    fun hide() {
        animate().cancel()
        visibility = GONE
    }

    private fun label(text: Int, sizeSp: Float, color: Int, family: String, topDp: Int) = TextView(context).apply {
        setText(text)
        gravity = Gravity.CENTER_HORIZONTAL
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(color)
        typeface = Typeface.create(family, Typeface.NORMAL)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { topMargin = dp(topDp) }
    }

    private fun retryButton(tokens: Map<String, Int>, onRetry: () -> Unit) = Button(context).apply {
        setText(R.string.error_retry)
        isAllCaps = false
        stateListAnimator = null
        minWidth = dp(120)
        setPadding(dp(32), 0, dp(32), 0)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setTextColor(tokens.getValue("onPrimary"))
        val pill = GradientDrawable().apply {
            cornerRadius = dp(24).toFloat()
            setColor(tokens.getValue("primary"))
        }
        background = RippleDrawable(ColorStateList.valueOf(tokens.getValue("onPrimary") and 0x00FFFFFF or 0x33000000), pill, null)
        setOnClickListener { onRetry() }
    }

    private companion object {
        const val FADE_MS = 200L
    }
}
