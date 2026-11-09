package io.github.chiehx0220.wash_your_fxxxing_hands

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

// Full-screen M3 empty state: shape badge, headline, supporting text
abstract class EmptyStateView(
    context: Context,
    protected val tokens: Map<String, Int>,
    @DrawableRes icon: Int,
    @StringRes title: Int,
    @StringRes message: Int = 0,
) : ScrollView(context) {

    private val column = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        setPadding(dp(32), dp(32), dp(32), dp(32))
    }

    init {
        isFillViewport = true
        isClickable = true  // consume touches
        visibility = GONE
        setBackgroundColor(tokens.getValue("surface"))
        addView(column, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        val glyph = requireNotNull(context.getDrawable(icon)).mutate()
            .apply { setTint(tokens.getValue("onSecondaryContainer")) }
        add(ShapeBadge(context, tokens.getValue("secondaryContainer"), glyph, dp(48)), 0, dp(112), dp(112))
        add(label(title, 22f, "onSurface", "sans-serif-medium"), 28)
        if (message != 0) add(label(message, 14f, "onSurfaceVariant", "sans-serif"), 8)
    }

    open fun show() {
        if (visibility == VISIBLE) return
        alpha = 0f
        visibility = VISIBLE
        animate().alpha(1f).setDuration(FADE_MS).start()
    }

    open fun hide() {
        animate().cancel()
        visibility = GONE
    }

    protected fun add(view: View, topDp: Int, width: Int = WRAP, height: Int = WRAP) {
        column.addView(view, LinearLayout.LayoutParams(width, height).apply { topMargin = dp(topDp) })
    }

    private fun label(@StringRes text: Int, sizeSp: Float, color: String, family: String) = TextView(context).apply {
        setText(text)
        gravity = Gravity.CENTER_HORIZONTAL
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(tokens.getValue(color))
        typeface = Typeface.create(family, Typeface.NORMAL)
    }

    protected companion object {
        const val MATCH = LinearLayout.LayoutParams.MATCH_PARENT
        const val WRAP = LinearLayout.LayoutParams.WRAP_CONTENT
        private const val FADE_MS = 200L
    }
}
