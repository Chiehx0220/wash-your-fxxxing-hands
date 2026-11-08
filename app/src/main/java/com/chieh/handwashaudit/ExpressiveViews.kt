package com.chieh.handwashaudit

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.Drawable
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

// Unit-radius polygons, origin-centered
private object Shapes {
    val cookie9 = RoundedPolygon.star(9, 1f, 0.8f, CornerRounding(0.4f))

    val loadingSequence = listOf(
        RoundedPolygon.star(10, 1f, 0.84f, CornerRounding(0.22f), CornerRounding(0.12f)),  // soft burst
        cookie9,
        RoundedPolygon(5, 1f, rounding = CornerRounding(0.3f)),                            // pentagon
        RoundedPolygon.rectangle(1.9f, 1.1f, CornerRounding(0.55f)),                       // pill
        RoundedPolygon.star(8, 1f, 0.8f, CornerRounding(0.16f)),                           // sunny
        RoundedPolygon.star(4, 1f, 0.72f, CornerRounding(0.5f)),                           // cookie 4
        RoundedPolygon.rectangle(1.9f, 1.4f, CornerRounding(0.7f)),                        // oval
    )
}

// Linear phase 0..cycle, repeating while attached
abstract class LoopingView(context: Context, cycleMs: Long, cycle: Float) : View(context) {

    protected var phase = 0f
        private set

    private val viewMatrix = Matrix()
    private val animator = ValueAnimator.ofFloat(0f, cycle).apply {
        duration = cycleMs
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
    }

    protected val radius get() = minOf(width, height) / 2f

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    // Unit path -> view space: scale, rotate, center
    protected fun toViewSpace(source: Path, target: Path, scale: Float, degrees: Float) {
        viewMatrix.reset()
        viewMatrix.postScale(scale, scale)
        viewMatrix.postRotate(degrees)
        viewMatrix.postTranslate(width / 2f, height / 2f)
        source.transform(viewMatrix, target)
    }
}

// M3 Expressive loading indicator: shape morph + rotation on tonal container
class MorphingLoadingIndicator(
    context: Context,
    private val indicatorColor: Int,
    private val containerColor: Int,
) : LoopingView(context, STEP_MS * Shapes.loadingSequence.size, Shapes.loadingSequence.size.toFloat()) {

    private val morphs = Shapes.loadingSequence.mapIndexed { i, shape ->
        Morph(shape, Shapes.loadingSequence[(i + 1) % Shapes.loadingSequence.size])
    }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val unitPath = Path()
    private val viewPath = Path()
    private val overshoot = OvershootInterpolator(1.6f)

    override fun onDraw(canvas: Canvas) {
        paint.color = containerColor
        canvas.drawCircle(width / 2f, height / 2f, radius, paint)

        val step = phase.toInt().coerceAtMost(morphs.size - 1)
        val local = (phase - step) / MORPH_FRACTION
        val progress = if (local >= 1f) 1f else overshoot.getInterpolation(local)
        morphs[step].toPath(progress, unitPath)
        toViewSpace(unitPath, viewPath, radius * INDICATOR_RATIO, phase * DEGREES_PER_STEP)

        paint.color = indicatorColor
        canvas.drawPath(viewPath, paint)
    }

    private companion object {
        const val STEP_MS = 700L
        const val MORPH_FRACTION = 0.7f  // remainder holds
        const val INDICATOR_RATIO = 0.8f
        const val DEGREES_PER_STEP = 55f
    }
}

// Slowly rotating cookie behind a static icon
class ShapeBadge(
    context: Context,
    shapeColor: Int,
    private val icon: Drawable,
    private val iconSizePx: Int,
) : LoopingView(context, ROTATION_MS, 360f) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = shapeColor }
    private val unitPath = Shapes.cookie9.toPath()
    private val viewPath = Path()

    override fun onDraw(canvas: Canvas) {
        toViewSpace(unitPath, viewPath, radius, phase)
        canvas.drawPath(viewPath, paint)

        val cx = width / 2
        val cy = height / 2
        val half = iconSizePx / 2
        icon.setBounds(cx - half, cy - half, cx + half, cy + half)
        icon.draw(canvas)
    }

    private companion object {
        const val ROTATION_MS = 24_000L
    }
}
