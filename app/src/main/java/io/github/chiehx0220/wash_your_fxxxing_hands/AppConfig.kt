package io.github.chiehx0220.wash_your_fxxxing_hands

import android.content.Context
import java.net.URI

// Deployment URL: user-entered, never in source
object AppConfig {

    private const val PREFS_FILE = "config"
    private const val KEY_URL = "app_url"
    private const val HOST = "script.google.com"
    private val EXEC_PATH = Regex("^/(?:a/macros/[^/]+|macros)/s/[A-Za-z0-9_-]+/exec$")

    fun url(context: Context): String? =
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE).getString(KEY_URL, null)

    fun save(context: Context, url: String) {
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE).edit().putString(KEY_URL, url).apply()
    }

    // Normalized /exec URL, or null
    fun parse(input: String): String? {
        val uri = try { URI(input.trim()) } catch (e: Exception) { return null }
        if (uri.scheme != "https" || uri.host != HOST || !EXEC_PATH.matches(uri.rawPath.orEmpty())) return null
        return "https://$HOST${uri.rawPath}"
    }
}
