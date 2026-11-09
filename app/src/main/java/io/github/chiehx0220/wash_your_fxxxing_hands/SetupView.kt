package io.github.chiehx0220.wash_your_fxxxing_hands

import android.content.ClipboardManager
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.text.InputType
import android.util.TypedValue
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.core.graphics.ColorUtils

// Deployment URL entry; onSubmit returns false when rejected
class SetupView(
    context: Context,
    tokens: Map<String, Int>,
    private val onSubmit: (String) -> Boolean,
) : EmptyStateView(context, tokens, R.drawable.ic_link, R.string.setup_title) {

    private val input = urlField()
    private val error = errorLabel()

    init {
        add(input, 28, MATCH)
        add(error, 8, MATCH)
        add(pillButton(context, tokens, R.string.setup_confirm, filled = true, onClick = ::submit), 24, height = dp(48))
        add(pillButton(context, tokens, R.string.setup_paste, filled = false, onClick = ::paste), 8, height = dp(48))
    }

    fun show(current: String?) {
        input.setText(current.orEmpty())
        error.visibility = INVISIBLE
        show()
    }

    override fun hide() {
        input.clearFocus()
        context.getSystemService(InputMethodManager::class.java).hideSoftInputFromWindow(windowToken, 0)
        super.hide()
    }

    private fun submit() {
        error.visibility = if (onSubmit(input.text.toString())) INVISIBLE else VISIBLE
    }

    private fun paste() {
        val clip = context.getSystemService(ClipboardManager::class.java).primaryClip
        val text = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)
        input.setText(text?.toString()?.trim().orEmpty())
        input.setSelection(input.text.length)
    }

    private fun urlField() = EditText(context).apply {
        setHint(R.string.setup_hint)
        setHintTextColor(ColorUtils.setAlphaComponent(tokens.getValue("onSurfaceVariant"), 0x99))
        setTextColor(tokens.getValue("onSurface"))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        setRawInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)  // wraps; Enter stays "go"
        imeOptions = EditorInfo.IME_ACTION_GO
        maxLines = 4
        setPadding(dp(16), dp(14), dp(16), dp(14))
        background = fieldBackground()
        setOnEditorActionListener { _, action, _ ->
            (action == EditorInfo.IME_ACTION_GO).also { if (it) submit() }
        }
        // Hardware Enter
        setOnKeyListener { _, keyCode, event ->
            (keyCode == KeyEvent.KEYCODE_ENTER).also { if (it && event.action == KeyEvent.ACTION_UP) submit() }
        }
    }

    // Tonal fill; outline becomes primary on focus
    private fun fieldBackground(): StateListDrawable {
        fun box(strokeDp: Int, stroke: Int) = GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            setColor(ColorUtils.setAlphaComponent(tokens.getValue("onSurface"), 0x0F))
            setStroke(dp(strokeDp), stroke)
        }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_focused), box(2, tokens.getValue("primary")))
            addState(intArrayOf(), box(1, tokens.getValue("outlineVariant")))
        }
    }

    private fun errorLabel() = TextView(context).apply {
        setText(R.string.setup_invalid)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTextColor(tokens.getValue("error"))
        visibility = INVISIBLE
    }
}
