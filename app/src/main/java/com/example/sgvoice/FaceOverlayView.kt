package com.example.sgvoice

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class FaceOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var faces: List<FaceData> = emptyList()

    // 얼굴 바운딩 박스를 그리기 위한 페인트 (초록색)
    private val boxPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    // 얼굴 고유 ID 텍스트를 그리기 위한 페인트 (빨간색)
    private val idTextPaint = Paint().apply {
        color = Color.RED
        textSize = 50f
        strokeWidth = 2f
    }

    // 랜드마크 점들을 그리기 위한 페인트 (파란색)
    private val landmarkPaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
    }

    // 외부에서 얼굴 데이터를 업데이트할 때 호출
    fun setFaces(faces: List<FaceData>) {
        this.faces = faces
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        faces.forEach { face ->
            // 얼굴 바운딩 박스 표시
            canvas.drawRect(face.boundingBox, boxPaint)
            // 얼굴 고유 ID 표시 (바운딩 박스 위쪽)
            canvas.drawText("ID: ${face.id}", face.boundingBox.left.toFloat(), face.boundingBox.top.toFloat() - 10f, idTextPaint)
            // 해당 얼굴의 모든 랜드마크 점 표시
            face.landmarkPoints.forEach { point ->
                canvas.drawCircle(point.x, point.y, 4f, landmarkPaint)
            }
        }
    }
}
