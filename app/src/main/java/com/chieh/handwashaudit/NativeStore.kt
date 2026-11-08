package com.chieh.handwashaudit

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import java.io.IOException

// JS bridge: window.NativeStore
class NativeStore(private val context: Context, private val onReady: () -> Unit) {

    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    // Bridge thread
    @JavascriptInterface
    fun ready() = onReady()

    @JavascriptInterface
    fun themeCss(): String = ThemeCss.build(context)

    // department -> names
    @JavascriptInterface
    fun roster(): String = try {
        context.readAsset(ROSTER_ASSET)
    } catch (e: IOException) {
        "{}"
    }

    // Fixed target: never page-supplied
    @JavascriptInterface
    fun openRepo() {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    @JavascriptInterface
    fun get(key: String): String = if (key in KEYS) prefs.getString(key, "").orEmpty() else ""

    @JavascriptInterface
    fun set(key: String, value: String) {
        if (key in KEYS && value.length <= MAX_VALUE_LENGTH) prefs.edit().putString(key, value).apply()
    }

    private companion object {
        const val PREFS_FILE = "quick_audit"
        const val ROSTER_ASSET = "roster.json"
        const val MAX_VALUE_LENGTH = 2000
        const val REPO_URL = "https://github.com/Chiehx0220/wash-your-fxxxing-hands"
        val KEYS = setOf("prefs")
    }
}
