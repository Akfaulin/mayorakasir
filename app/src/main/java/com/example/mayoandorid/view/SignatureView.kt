package com.example.mayoandorid.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.mayoandorid.util.ImageUtils

class SignatureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        isAntiAlias = true
        color = Color.parseColor("#1E293B")
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 8.0f
    }

    private val path = Path()
    private var bitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private var _hasSignature = false

    init {
        setBackgroundColor(Color.WHITE)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            canvas = Canvas(bitmap!!).apply {
                drawColor(Color.WHITE)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmap?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        canvas.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                path.moveTo(x, y)
                path.lineTo(x + 0.1f, y + 0.1f)
                _hasSignature = true
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                path.lineTo(x, y)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                canvas?.drawPath(path, paint)
                path.reset()
                parent?.requestDisallowInterceptTouchEvent(false)
                invalidate()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                canvas?.drawPath(path, paint)
                path.reset()
                parent?.requestDisallowInterceptTouchEvent(false)
                invalidate()
                return true
            }
            else -> return super.onTouchEvent(event)
        }
    }

    fun clear() {
        path.reset()
        _hasSignature = false
        bitmap?.eraseColor(Color.WHITE)
        invalidate()
    }

    fun hasSignature(): Boolean = _hasSignature

    fun getSignatureBitmap(): Bitmap? = bitmap

    fun getSignatureBase64(): String? {
        if (!_hasSignature || bitmap == null) return null
        return ImageUtils.bitmapToPngBase64(bitmap)
    }
}