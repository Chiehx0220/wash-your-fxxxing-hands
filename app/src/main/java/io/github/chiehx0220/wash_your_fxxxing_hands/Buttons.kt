package io.github.chiehx0220.wash_your_fxxxing_hands

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.widget.Button
import androidx.core.graphics.ColorUtils

// M3 pill button: filled (primary) or text
fun pillButton(context: Context, tokens: Map<String, Int>, text: Int, filled: Boolean, onClick: () -> Unit) =
    Button(context).apply {
        val content = tokens.getValue(if (filled) "onPrimary" else "primary")
        setText(text)
        isAllCaps = false
        stateListAnimator = null
        minWidth = dp(120)
        minimumHeight = dp(48)
        setPadding(dp(24), 0, dp(24), 0)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setTextColor(content)
        val pill = GradientDrawable().apply {
            cornerRadius = dp(24).toFloat()
            setColor(if (filled) tokens.getValue("primary") else 0)
        }
        background = RippleDrawable(ColorStateList.valueOf(ColorUtils.setAlphaComponent(content, 0x33)), pill, null)
        setOnClickListener { onClick() }
    }
