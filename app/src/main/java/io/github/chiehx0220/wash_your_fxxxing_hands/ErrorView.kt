package io.github.chiehx0220.wash_your_fxxxing_hands

import android.content.Context

// Main-frame load failure; replaces the browser error page
class ErrorView(
    context: Context,
    tokens: Map<String, Int>,
    onRetry: () -> Unit,
    onChangeLink: () -> Unit,
) : EmptyStateView(context, tokens, R.drawable.ic_cloud_off, R.string.error_title, R.string.error_message) {

    init {
        add(pillButton(context, tokens, R.string.error_retry, filled = true, onClick = onRetry), 32, height = dp(48))
        add(pillButton(context, tokens, R.string.error_change_link, filled = false, onClick = onChangeLink), 8, height = dp(48))
    }
}
