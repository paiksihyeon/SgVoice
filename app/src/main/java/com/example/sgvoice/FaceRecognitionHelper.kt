package com.example.sgvoice

import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ConcurrentHashMap

class FaceRecognitionHelper(
    private val context: Context,
    private val callback: (List<DetectedFace>) -> Unit
) {
    private val detectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE) // 정확도 우선 모드
        .enableTracking()
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.01f) // 작은 얼굴도 감지 가능하도록 변경
        .build()

    private val faceDetector = FaceDetection.getClient(detectorOptions)
    private val faceIdMap = ConcurrentHashMap<Int, Int>()
    private var nextFaceId = 1

    fun processImage(image: InputImage, imageProxy: ImageProxy) {
        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                val detectedFaces = faces.filter { face ->
                    face.boundingBox.width() > 30 && face.boundingBox.height() > 30 // 더 작은 얼굴도 감지
                }.map { face ->
                    val assignedId = faceIdMap.getOrPut(face.trackingId ?: -1) { nextFaceId++ }
                    DetectedFace(assignedId, face.boundingBox, face.allLandmarks.map { it.position })
                }
                callback(detectedFaces)
            }
            .addOnFailureListener { e ->
                Log.e("FaceRecognition", "얼굴 감지 실패", e)
            }
            .addOnCompleteListener {
                imageProxy.close() // 얼굴 감지 후에 imageProxy 닫기
            }
    }
}

// ✅ ML Kit의 PointF를 명확하게 사용하도록 수정
data class DetectedFace(val id: Int, val boundingBox: Rect, val landmarkPoints: List<PointF>)
