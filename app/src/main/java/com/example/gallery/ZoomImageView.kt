package com.example.gallery

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

class ZoomImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val baseMatrix = Matrix()
    private val drawMatrix = Matrix()
    private var currentScale = 1f
    private val maxScale = 5f

    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val target = (currentScale * detector.scaleFactor).coerceIn(1f, maxScale)
                val real = target / currentScale
                currentScale = target
                drawMatrix.postScale(real, real, detector.focusX, detector.focusY)
                clamp()
                imageMatrix = drawMatrix
                return true
            }
        }
    )

    private val gestureDetector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (currentScale > 1.05f) resetZoom() else zoomTo(2.6f, e.x, e.y)
                return true
            }
        }
    )

    init {
        scaleType = ScaleType.MATRIX
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        post { initBase() }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        initBase()
    }

    private fun initBase() {
        val d = drawable ?: return
        val vw = width.toFloat()
        val vh = height.toFloat()
        val dw = d.intrinsicWidth.toFloat()
        val dh = d.intrinsicHeight.toFloat()
        if (vw <= 0f || vh <= 0f || dw <= 0f || dh <= 0f) return
        val scale = minOf(vw / dw, vh / dh)
        val dx = (vw - dw * scale) / 2f
        val dy = (vh - dh * scale) / 2f
        baseMatrix.reset()
        baseMatrix.postScale(scale, scale)
        baseMatrix.postTranslate(dx, dy)
        drawMatrix.set(baseMatrix)
        currentScale = 1f
        imageMatrix = drawMatrix
    }

    private fun zoomTo(scale: Float, fx: Float, fy: Float) {
        val real = scale / currentScale
        currentScale = scale
        drawMatrix.postScale(real, real, fx, fy)
        clamp()
        imageMatrix = drawMatrix
    }

    private fun resetZoom() {
        drawMatrix.set(baseMatrix)
        currentScale = 1f
        imageMatrix = drawMatrix
    }

    private fun clamp() {
        val d = drawable ?: return
        val rect = RectF(0f, 0f, d.intrinsicWidth.toFloat(), d.intrinsicHeight.toFloat())
        drawMatrix.mapRect(rect)
        val vw = width.toFloat()
        val vh = height.toFloat()
        var dx = 0f
        var dy = 0f
        if (rect.width() <= vw) dx = (vw - rect.width()) / 2f - rect.left
        else {
            if (rect.left > 0f) dx = -rect.left
            if (rect.right < vw) dx = vw - rect.right
        }
        if (rect.height() <= vh) dy = (vh - rect.height()) / 2f - rect.top
        else {
            if (rect.top > 0f) dy = -rect.top
            if (rect.bottom < vh) dy = vh - rect.bottom
        }
        drawMatrix.postTranslate(dx, dy)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x; lastY = event.y; dragging = true
            }
            MotionEvent.ACTION_MOVE -> {
                if (dragging && currentScale > 1.01f && !scaleDetector.isInProgress &&
                    event.pointerCount == 1
                ) {
                    drawMatrix.postTranslate(event.x - lastX, event.y - lastY)
                    clamp()
                    imageMatrix = drawMatrix
                }
                lastX = event.x; lastY = event.y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> dragging = false
        }
        parent?.requestDisallowInterceptTouchEvent(
            currentScale > 1.01f || scaleDetector.isInProgress
        )
        return true
    }
}
