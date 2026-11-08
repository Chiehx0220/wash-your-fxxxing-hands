package io.github.chiehx0220.wash_your_fxxxing_hands

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

private const val APP_URL =
    "https://script.google.com/macros/s/DEPLOYMENT_ID/exec"

// (asset, origin rule)
private val INJECTIONS = listOf(
    "inject.js" to "https://*.googleusercontent.com",  // sandbox frame
    "hide_banner.js" to "https://script.google.com",   // wrapper frame
)

private const val SIGN_IN_HOST = "accounts.google.com"
private val ALLOWED_HOSTS = listOf("script.google.com", "googleusercontent.com", SIGN_IN_HOST)
private const val SPLASH_TIMEOUT_MS = 15_000L

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var splash: SplashView
    private val backBridge = BackBridge()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tokens = Palette.tokens(this)
        val surface = tokens.getValue("surface")

        webView = WebView(this).apply { setBackgroundColor(surface) }
        splash = SplashView(this, tokens)
        setContentView(FrameLayout(this).apply {
            addView(webView)
            addView(splash)
        })

        applySystemBars(surface, light = !Palette.isDark(this))
        configureWebView()
        installInjections()

        // Timeout: never hide failures behind the overlay
        Handler(Looper.getMainLooper()).postDelayed({ splash.dismiss() }, SPLASH_TIMEOUT_MS)

        if (savedInstanceState == null) webView.loadUrl(APP_URL) else webView.restoreState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            backBridge.consumeBack() -> Unit
            webView.canGoBack() -> webView.goBack()
            else -> super.onBackPressed()
        }
    }

    private fun applySystemBars(surface: Int, light: Boolean) {
        window.statusBarColor = surface
        window.navigationBarColor = surface
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = light
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    private fun configureWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)

        webView.addJavascriptInterface(NativeStore(this) { runOnUiThread { splash.dismiss() } }, "NativeStore")
        backBridge.install(webView)
        webView.webViewClient = object : WebViewClient() {
            // Host allowlist; others via ACTION_VIEW
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val host = request.url.host.orEmpty()
                if (ALLOWED_HOSTS.any { host == it || host.endsWith(".$it") }) return false
                startActivity(Intent(Intent.ACTION_VIEW, request.url))
                return true
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) = backBridge.reset()

            // Sign-in: no injection, reveal page
            override fun onPageFinished(view: WebView, url: String) {
                if (Uri.parse(url).host == SIGN_IN_HOST) splash.dismiss()
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) splash.dismiss()
            }
        }
    }

    private fun installInjections() {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            Toast.makeText(this, "WebView 版本過舊，快速填寫功能無法使用", Toast.LENGTH_LONG).show()
            splash.dismiss()
            return
        }
        INJECTIONS.forEach { (asset, origin) ->
            WebViewCompat.addDocumentStartJavaScript(webView, readAsset(asset), setOf(origin))
        }
    }
}
