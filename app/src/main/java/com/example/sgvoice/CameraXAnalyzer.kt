package com.example.sgvoice

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetector

class CameraXAnalyzer(
    private val faceDetector: FaceDetector,
    private val onFacesDetected: (List<com.google.mlkit.vision.face.Face>) -> Unit
) : ImageAnalysis.Analyzer {

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    onFacesDetected(faces)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
