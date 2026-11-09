package io.github.chiehx0220.wash_your_fxxxing_hands

import android.net.Uri
import android.webkit.WebView
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

// Back key -> open page layers (menu, sheet): page reports depth, native replies "back"
class BackBridge(private val onChange: () -> Unit) {

    private var depth = 0
    private var reply: JavaScriptReplyProxy? = null

    val hasLayers: Boolean get() = depth > 0

    fun install(webView: WebView) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) return
        WebViewCompat.addWebMessageListener(webView, NAME, ORIGINS) { _, message, _: Uri, _, proxy ->
            onMessage(message, proxy)
        }
    }

    fun reset() {
        depth = 0
        reply = null
        onChange()
    }

    fun consumeBack(): Boolean {
        val proxy = reply ?: return false
        if (depth <= 0) return false
        proxy.postMessage(BACK)
        return true
    }

    private fun onMessage(message: WebMessageCompat, proxy: JavaScriptReplyProxy) {
        reply = proxy
        depth = message.data?.toIntOrNull() ?: 0
        onChange()
    }

    private companion object {
        const val NAME = "qaBridge"
        const val BACK = "back"
        val ORIGINS = setOf("https://*.googleusercontent.com")
    }
}
