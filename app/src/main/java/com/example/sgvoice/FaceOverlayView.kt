package com.example.sgvoice

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View

class FaceOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var faces: List<FaceData> = emptyList()

    private val boxPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    private val idTextPaint = Paint().apply {
        color = Color.RED
        textSize = 50f
        strokeWidth = 2f
    }

    private val landmarkPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
    }

    fun setFaces(faces: List<FaceData>) {
        this.faces = faces
        invalidate() // UI 강제 업데이트
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        faces.forEach { face ->
            canvas.drawRect(face.boundingBox, boxPaint)
            canvas.drawText("ID: ${face.id}", face.boundingBox.left.toFloat(), face.boundingBox.top.toFloat() - 10f, idTextPaint)

            // 모든 얼굴의 랜드마크를 표시
            face.landmarkPoints.forEach { point ->
                canvas.drawCircle(point.x, point.y, 4f, landmarkPaint)
            }
        }
    }
}
