package io.github.chiehx0220.wash_your_fxxxing_hands

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

// Page-requested haptic kinds -> platform constants (honours the system touch-feedback setting)
object Haptics {

    fun perform(view: View, kind: String) {
        val constant = when (kind) {
            "tick" -> if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_TICK else HapticFeedbackConstants.CLOCK_TICK
            "confirm" -> if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
            "reject" -> if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
            else -> return
        }
        view.performHapticFeedback(constant)
    }
}
