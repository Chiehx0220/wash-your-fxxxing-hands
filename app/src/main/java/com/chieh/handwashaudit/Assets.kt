package com.chieh.handwashaudit

import android.content.Context

fun Context.readAsset(name: String): String = assets.open(name).bufferedReader().use { it.readText() }
