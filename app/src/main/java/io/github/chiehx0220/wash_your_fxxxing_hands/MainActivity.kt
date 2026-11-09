package io.github.chiehx0220.wash_your_fxxxing_hands

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

private const val APP_URL =
    "https://script.google.com/macros/s/DEPLOYMENT_ID/exec"

private const val SIGN_IN_HOST = "accounts.google.com"
private val ALLOWED_HOSTS = listOf("script.google.com", "googleusercontent.com", SIGN_IN_HOST)

// asset -> origin rule
private val INJECTIONS = listOf(
    "inject.js" to "https://*.googleusercontent.com",  // sandbox frame
    "hide_banner.js" to "https://script.google.com",   // wrapper frame
)

private const val SPLASH_TIMEOUT_MS = 15_000L
private const val STALE_AFTER_MS = 2 * 60 * 60 * 1000L  // background time before reload

private fun hostOf(url: String?): String? = url?.let { Uri.parse(it).host }

private fun isAllowed(host: String) = ALLOWED_HOSTS.any { host == it || host.endsWith(".$it") }

class MainActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var tokens: Map<String, Int>
    private lateinit var root: FrameLayout
    private lateinit var webView: WebView
    private lateinit var backBridge: BackBridge
    private lateinit var splash: SplashView
    private lateinit var errorView: ErrorView
    private var stoppedAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tokens = Palette.tokens(this)
        val surface = tokens.getValue("surface")

        webView = WebView(this).apply { setBackgroundColor(surface) }
        backBridge = BackBridge(this, webView)
        errorView = ErrorView(this, tokens, ::load)
        root = FrameLayout(this).apply {
            setBackgroundColor(surface)
            addView(webView)
            addView(errorView)
        }
        setContentView(root)
        showSplash()

        SystemBars.apply(this, root, surface, lightTheme = !Palette.isDark(this))
        configureWebView()
        installInjections()

        if (savedInstanceState == null) webView.loadUrl(APP_URL) else webView.restoreState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onStop() {
        super.onStop()
        stoppedAt = SystemClock.elapsedRealtime()
    }

    // Stale page: reload instead of submitting from it
    override fun onStart() {
        super.onStart()
        val stale = stoppedAt != 0L && SystemClock.elapsedRealtime() - stoppedAt > STALE_AFTER_MS
        if (stale && hostOf(webView.url) != SIGN_IN_HOST) load()
    }

    // API < 33; later, BackBridge's dispatcher callback
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!backBridge.handle()) super.onBackPressed()
    }

    private fun load() {
        errorView.hide()
        showSplash()
        webView.loadUrl(APP_URL)
    }

    private fun showSplash() {
        val shown = SplashView(this, tokens)
        splash = shown
        root.addView(shown)
        handler.postDelayed({ shown.dismiss() }, SPLASH_TIMEOUT_MS)  // never hide failures
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    private fun configureWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportZoom(false)
            builtInZoomControls = false
        }
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)

        val store = NativeStore(
            this,
            onReady = { runOnUiThread { splash.dismiss() } },
            onHaptic = { kind -> runOnUiThread { Haptics.perform(webView, kind) } },
        )
        webView.addJavascriptInterface(store, "NativeStore")
        backBridge.install()
        webView.webViewClient = PageClient()
    }

    private fun installInjections() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            Toast.makeText(this, R.string.webview_outdated, Toast.LENGTH_LONG).show()
            splash.dismiss()
            return
        }
        INJECTIONS.forEach { (asset, origin) ->
            WebViewCompat.addDocumentStartJavaScript(webView, readAsset(asset), setOf(origin))
        }
    }

    private inner class PageClient : WebViewClient() {

        // Host allowlist; others via ACTION_VIEW
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (isAllowed(request.url.host.orEmpty())) return false
            startActivity(Intent(Intent.ACTION_VIEW, request.url))
            return true
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) = backBridge.reset()

        override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) = backBridge.sync()

        // Sign-in: no injection, reveal page
        override fun onPageFinished(view: WebView, url: String) {
            if (hostOf(url) == SIGN_IN_HOST) splash.dismiss()
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (!request.isForMainFrame) return
            splash.dismiss()
            errorView.show()
        }
    }
}
