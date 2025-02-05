package com.example.sgvoice

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View


class FaceOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var faces: List<DetectedFace> = emptyList()

    private val paint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private val textPaint = Paint().apply {
        color = Color.RED
        textSize = 50f
        strokeWidth = 2f
    }

    private val pointPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
    }

    private val adjustedBox = Rect()

    fun setFaces(faces: List<DetectedFace>) {
        this.faces = faces
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        faces.forEach { face ->
            adjustedBox.set(
                face.boundingBox.left,
                face.boundingBox.top + (face.boundingBox.height() * 0.2).toInt(),
                face.boundingBox.right,
                face.boundingBox.bottom + (face.boundingBox.height() * 0.2).toInt()
            )

            canvas.drawRect(adjustedBox, paint)
            canvas.drawText("ID: ${face.id}", adjustedBox.left.toFloat(), adjustedBox.top.toFloat() - 10f, textPaint)

            face.landmarkPoints.forEach { point: PointF ->
                canvas.drawCircle(point.x, point.y + (face.boundingBox.height() * 0.1).toFloat(), 5f, pointPaint)
            }
        }
    }
}
