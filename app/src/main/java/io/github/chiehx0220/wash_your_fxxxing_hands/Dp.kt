package io.github.chiehx0220.wash_your_fxxxing_hands

import android.view.View

fun View.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
