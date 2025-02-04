package com.example.sgvoice

import android.content.Context
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*

class FaceRecognitionHelper(
    private val context: Context,
    private val callback: (List<DetectedFace>, Int) -> Unit
) {
    private val detectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST) //  속도를 빠르게 변경
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE) //  랜드마크 감지를 끔
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE) //  표정 감지를 끔
        .enableTracking() //  얼굴 추적 활성화 (같은 얼굴을 동일하게 인식)
        .build()

    private val faceDetector = FaceDetection.getClient(detectorOptions)

    fun processImage(image: InputImage) {
        val startTime = System.currentTimeMillis() //  시작 시간 기록
        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                val processingTime = System.currentTimeMillis() - startTime //  감지 속도 계산
                Log.d("FaceRecognition", "Processing Time: ${processingTime}ms") //  로그 출력

                val detectedFaces = faces.map { face ->
                    val faceId = face.trackingId ?: -1
                    DetectedFace(faceId)
                }
                callback(detectedFaces, detectedFaces.size)
            }
            .addOnFailureListener { e ->
                Log.e("FaceRecognition", "Face detection failed", e)
            }
    }
}

data class DetectedFace(val id: Int)
