package com.example.sgvoice

import android.content.Context
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceDetection

class FaceRecognitionHelper(private val context: Context, private val callback: (List<Face>) -> Unit) {
    private val detectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()

    private val faceDetector = FaceDetection.getClient(detectorOptions)

    fun processImage(image: InputImage) {
        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                Log.d("FaceRecognition", "Detected ${faces.size} face(s)")
                callback(faces)
            }
            .addOnFailureListener { e ->
                Log.e("FaceRecognition", "Face detection failed", e)
            }
    }
}
