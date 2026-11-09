package io.github.chiehx0220.wash_your_fxxxing_hands

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.webkit.WebView
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

// Back routing: page layers (qaBridge: depth in, "back" out) -> WebView history -> system
class BackBridge(private val activity: Activity, private val webView: WebView) {

    private var depth = 0
    private var reply: JavaScriptReplyProxy? = null
    private var callback: OnBackInvokedCallback? = null

    fun install() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) return
        WebViewCompat.addWebMessageListener(webView, NAME, ORIGINS) { _, message, _: Uri, _, proxy ->
            reply = proxy
            depth = message.data?.toIntOrNull() ?: 0
            sync()
        }
    }

    fun reset() {
        depth = 0
        reply = null
        sync()
    }

    // false: nothing to consume
    fun handle(): Boolean {
        val proxy = reply
        return when {
            proxy != null && depth > 0 -> true.also { proxy.postMessage(BACK) }
            webView.canGoBack() -> true.also { webView.goBack() }
            else -> false
        }
    }

    // API 33+: registered only while consumable, else system back-to-home animation
    fun sync() {
        if (Build.VERSION.SDK_INT < 33) return
        val needed = depth > 0 || webView.canGoBack()
        val dispatcher = activity.onBackInvokedDispatcher
        val current = callback
        when {
            needed && current == null -> {
                callback = OnBackInvokedCallback { handle() }.also {
                    dispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, it)
                }
            }
            !needed && current != null -> {
                dispatcher.unregisterOnBackInvokedCallback(current)
                callback = null
            }
        }
    }

    private companion object {
        const val NAME = "qaBridge"
        const val BACK = "back"
        val ORIGINS = setOf("https://*.googleusercontent.com")
    }
}
