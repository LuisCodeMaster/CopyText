package com.luiscode.copytext.capture

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max
import kotlin.math.min

class SelectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val overlayPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = 0x99000000.toInt()
        }

    private val borderPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = 0xFFFFFFFF.toInt()
        }

    private val handlePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = 0xFFFFFFFF.toInt()
        }

    private var startX = 0f
    private var startY = 0f
    private var endX = 0f
    private var endY = 0f

    private var selecting = false
    private var resizing = false

    private var resizeMode = ResizeMode.NONE

    private val handleRadius = 22f
    private val minimumSize = 40f

    var selectionRect: RectF? = null
        private set

    private enum class ResizeMode {
        NONE,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val rect = selectionRect ?: return

        val left = rect.left
        val top = rect.top
        val right = rect.right
        val bottom = rect.bottom

        // Área acima da seleção
        canvas.drawRect(
            0f,
            0f,
            width.toFloat(),
            top,
            overlayPaint
        )

        // Área abaixo da seleção
        canvas.drawRect(
            0f,
            bottom,
            width.toFloat(),
            height.toFloat(),
            overlayPaint
        )

        // Área à esquerda da seleção
        canvas.drawRect(
            0f,
            top,
            left,
            bottom,
            overlayPaint
        )

        // Área à direita da seleção
        canvas.drawRect(
            right,
            top,
            width.toFloat(),
            bottom,
            overlayPaint
        )

        // Borda da seleção
        canvas.drawRect(
            rect,
            borderPaint
        )

        // Alças
        drawHandle(
            canvas,
            left,
            top
        )

        drawHandle(
            canvas,
            right,
            top
        )

        drawHandle(
            canvas,
            left,
            bottom
        )

        drawHandle(
            canvas,
            right,
            bottom
        )
    }

    private fun drawHandle(
        canvas: Canvas,
        x: Float,
        y: Float
    ) {
        canvas.drawCircle(
            x,
            y,
            handleRadius,
            handlePaint
        )
    }

    override fun onTouchEvent(
        event: MotionEvent
    ): Boolean {

        when (event.actionMasked) {

            MotionEvent.ACTION_DOWN -> {

                val rect = selectionRect

                if (
                    rect != null &&
                    isNear(
                        event.x,
                        event.y,
                        rect.left,
                        rect.top
                    )
                ) {
                    resizeMode =
                        ResizeMode.TOP_LEFT

                    resizing = true

                    return true
                }

                if (
                    rect != null &&
                    isNear(
                        event.x,
                        event.y,
                        rect.right,
                        rect.top
                    )
                ) {
                    resizeMode =
                        ResizeMode.TOP_RIGHT

                    resizing = true

                    return true
                }

                if (
                    rect != null &&
                    isNear(
                        event.x,
                        event.y,
                        rect.left,
                        rect.bottom
                    )
                ) {
                    resizeMode =
                        ResizeMode.BOTTOM_LEFT

                    resizing = true

                    return true
                }

                if (
                    rect != null &&
                    isNear(
                        event.x,
                        event.y,
                        rect.right,
                        rect.bottom
                    )
                ) {
                    resizeMode =
                        ResizeMode.BOTTOM_RIGHT

                    resizing = true

                    return true
                }

                startX = event.x
                startY = event.y

                endX = event.x
                endY = event.y

                selecting = true
                resizing = false

                selectionRect =
                    RectF(
                        startX,
                        startY,
                        endX,
                        endY
                    )

                invalidate()

                return true
            }

            MotionEvent.ACTION_MOVE -> {

                if (selecting) {

                    endX = event.x
                    endY = event.y

                    selectionRect =
                        RectF(
                            min(startX, endX),
                            min(startY, endY),
                            max(startX, endX),
                            max(startY, endY)
                        )

                    invalidate()

                    return true
                }

                if (resizing) {

                    resizeSelection(
                        event.x,
                        event.y
                    )

                    invalidate()

                    return true
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {

                selecting = false
                resizing = false

                resizeMode =
                    ResizeMode.NONE

                invalidate()

                return true
            }
        }

        return true
    }

    private fun resizeSelection(
        x: Float,
        y: Float
    ) {

        val rect =
            selectionRect
                ?: return

        var left = rect.left
        var top = rect.top
        var right = rect.right
        var bottom = rect.bottom

        when (resizeMode) {

            ResizeMode.TOP_LEFT -> {
                left = x
                top = y
            }

            ResizeMode.TOP_RIGHT -> {
                right = x
                top = y
            }

            ResizeMode.BOTTOM_LEFT -> {
                left = x
                bottom = y
            }

            ResizeMode.BOTTOM_RIGHT -> {
                right = x
                bottom = y
            }

            ResizeMode.NONE -> {
                return
            }
        }

        if (right - left < minimumSize) {

            when (resizeMode) {

                ResizeMode.TOP_LEFT,
                ResizeMode.BOTTOM_LEFT -> {
                    left =
                        right - minimumSize
                }

                ResizeMode.TOP_RIGHT,
                ResizeMode.BOTTOM_RIGHT -> {
                    right =
                        left + minimumSize
                }

                ResizeMode.NONE -> {}
            }
        }

        if (bottom - top < minimumSize) {

            when (resizeMode) {

                ResizeMode.TOP_LEFT,
                ResizeMode.TOP_RIGHT -> {
                    top =
                        bottom - minimumSize
                }

                ResizeMode.BOTTOM_LEFT,
                ResizeMode.BOTTOM_RIGHT -> {
                    bottom =
                        top + minimumSize
                }

                ResizeMode.NONE -> {}
            }
        }

        left =
            left.coerceIn(
                0f,
                width.toFloat() - minimumSize
            )

        right =
            right.coerceIn(
                minimumSize,
                width.toFloat()
            )

        top =
            top.coerceIn(
                0f,
                height.toFloat() - minimumSize
            )

        bottom =
            bottom.coerceIn(
                minimumSize,
                height.toFloat()
            )

        selectionRect =
            RectF(
                min(left, right),
                min(top, bottom),
                max(left, right),
                max(top, bottom)
            )
    }

    private fun isNear(
        x: Float,
        y: Float,
        targetX: Float,
        targetY: Float
    ): Boolean {

        val dx =
            x - targetX

        val dy =
            y - targetY

        val radius =
            handleRadius * 2

        return (
            dx * dx +
            dy * dy
        ) <= (
            radius * radius
        )
    }

    fun clearSelection() {

        selectionRect = null

        selecting = false
        resizing = false

        startX = 0f
        startY = 0f
        endX = 0f
        endY = 0f

        resizeMode =
            ResizeMode.NONE

        invalidate()
    }
}
