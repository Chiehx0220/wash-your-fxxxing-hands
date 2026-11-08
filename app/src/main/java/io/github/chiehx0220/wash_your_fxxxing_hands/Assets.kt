package io.github.chiehx0220.wash_your_fxxxing_hands

import android.content.Context

fun Context.readAsset(name: String): String = assets.open(name).bufferedReader().use { it.readText() }
