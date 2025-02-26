package com.example.sgvoice

import android.graphics.PointF
import android.graphics.Rect

// 얼굴의 ID, 바운딩 박스, 랜드마크 좌표, 디스크립터 정보를 저장하는 데이터 클래스
data class FaceData(
    val id: Int,
    val boundingBox: Rect,
    val landmarkPoints: List<PointF>,
    val descriptor: FloatArray
)
