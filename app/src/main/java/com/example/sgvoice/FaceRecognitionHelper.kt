package com.example.sgvoice

import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.framework.image.BitmapImageBuilder
import kotlin.math.sqrt

class FaceRecognitionHelper(
    context: Context,
    private val callback: (List<FaceData>) -> Unit
) {
    private val faceLandmarker: FaceLandmarker =
        FaceLandmarker.createFromFile(context, "face_landmarker.task")

    private val storedFaces = mutableMapOf<Int, MutableList<FloatArray>>()
    private val faceTrackingMap = mutableMapOf<Int, FaceData>() // 얼굴 ID 추적용 맵
    private var nextFaceId = 1
    private val maxFaceId = 6

    fun processImage(bitmap: Bitmap) {
        try {
            val result = faceLandmarker.detect(BitmapImageBuilder(bitmap).build())
            val detectedFaces = mutableListOf<FaceData>()

            result.faceLandmarks().forEach { landmarks ->
                var points = landmarks.map { PointF(it.x() * bitmap.width, it.y() * bitmap.height) }

                // 얼굴 전체를 기준으로 정렬 (턱선, 이마 포함)
                val alignedPoints = alignFaceWithFullStructure(points)
                val boundingBox = calculateBoundingBox(alignedPoints)
                val normalizedPoints = normalizeFaceSize(alignedPoints, boundingBox)
                val overallDescriptor = normalizeFeatureVector(extractFeatureVector(normalizedPoints))

                // 기존 얼굴과 비교하여 가장 유사한 ID 유지
                val faceId = findMatchingFace(overallDescriptor, boundingBox) ?: getNextFaceId().also { storeFace(it, overallDescriptor) }

                // 현재 프레임에서 감지된 얼굴을 추적 맵에 저장
                faceTrackingMap[faceId] = FaceData(faceId, boundingBox, alignedPoints, overallDescriptor)
                detectedFaces.add(FaceData(faceId, boundingBox, alignedPoints, overallDescriptor))
            }

            callback(detectedFaces)

        } catch (e: Exception) {
            Log.e("FaceRecognitionHelper", "Error processing image", e)
        }
    }

    private fun getNextFaceId() =
        storedFaces.keys.minOrNull()?.takeIf { storedFaces.size >= maxFaceId }?.also { storedFaces.remove(it) }
            ?: nextFaceId++

    private fun storeFace(id: Int, descriptor: FloatArray) {
        storedFaces.getOrPut(id) { mutableListOf() }.add(descriptor)
    }

    private fun extractFeatureVector(points: List<PointF>) = FloatArray(points.size * 2).apply {
        points.forEachIndexed { index, point ->
            this[index * 2] = point.x
            this[index * 2 + 1] = point.y
        }
    }

    private fun normalizeFaceSize(points: List<PointF>, boundingBox: Rect) =
        points.map { point ->
            PointF(
                (point.x - boundingBox.left) / boundingBox.width().coerceAtLeast(1).toFloat(),
                (point.y - boundingBox.top) / boundingBox.height().coerceAtLeast(1).toFloat()
            )
        }

    private fun normalizeFeatureVector(vector: FloatArray): FloatArray {
        val norm = sqrt(vector.map { it * it }.sum().toDouble()).toFloat()
        return if (norm == 0.0f) vector else vector.map { it / norm }.toFloatArray()
    }

    private fun findMatchingFace(newDescriptor: FloatArray, newBoundingBox: Rect): Int? {
        var bestMatchId: Int? = null
        var bestSimilarity = -1.0
        var bestDistance = Double.MAX_VALUE

        faceTrackingMap.forEach { (id, previousFace) ->
            val similarity = calculateCosineSimilarity(newDescriptor, previousFace.descriptor)
            val distance = calculateL2Distance(newBoundingBox, previousFace.boundingBox)

            if (similarity > bestSimilarity && distance < 0.2) {
                bestSimilarity = similarity
                bestMatchId = id
            }
        }
        return bestMatchId
    }

    private fun calculateL2Distance(bbox1: Rect, bbox2: Rect): Double {
        val dx = (bbox1.centerX() - bbox2.centerX()).toDouble()
        val dy = (bbox1.centerY() - bbox2.centerY()).toDouble()
        return sqrt(dx * dx + dy * dy)
    }

    private fun calculateBoundingBox(points: List<PointF>): Rect {
        val minX = points.minOf { it.x }.toInt()
        val minY = points.minOf { it.y }.toInt()
        val maxX = points.maxOf { it.x }.toInt()
        val maxY = points.maxOf { it.y }.toInt()
        return Rect(minX, minY, maxX, maxY)
    }

    private fun alignFaceWithFullStructure(points: List<PointF>): List<PointF> {
        val leftEyeCenter = getCenter(points.subList(36, 42))
        val rightEyeCenter = getCenter(points.subList(42, 48))
        val chinCenter = getCenter(points.subList(0, 17)) // 턱 중심
        val foreheadCenter = getCenter(points.subList(17, 27)) // 이마 중심

        val faceCenter = PointF(
            (leftEyeCenter.x + rightEyeCenter.x + chinCenter.x + foreheadCenter.x) / 4,
            (leftEyeCenter.y + rightEyeCenter.y + chinCenter.y + foreheadCenter.y) / 4
        )

        val deltaY = rightEyeCenter.y - leftEyeCenter.y
        val deltaX = rightEyeCenter.x - leftEyeCenter.x
        var angle = Math.toDegrees(Math.atan2(deltaY.toDouble(), deltaX.toDouble())).toFloat()

        if (isPortraitMode(points)) angle += 90f // 📌 세로 모드일 때 추가 보정

        return points.map { rotatePoint(it, faceCenter, angle) }
    }

    private fun isPortraitMode(points: List<PointF>): Boolean {
        val boundingBox = calculateBoundingBox(points)
        return boundingBox.height() > boundingBox.width() // 높이가 너비보다 크면 세로 모드로 간주
    }

    private fun rotatePoint(point: PointF, center: PointF, angle: Float): PointF {
        val radian = Math.toRadians(angle.toDouble())
        val cosA = Math.cos(radian)
        val sinA = Math.sin(radian)

        val translatedX = point.x - center.x
        val translatedY = point.y - center.y

        return PointF(
            (cosA * translatedX + sinA * translatedY + center.x).toFloat(),
            (-sinA * translatedX + cosA * translatedY + center.y).toFloat()
        )
    }

    private fun getCenter(points: List<PointF>) = PointF(
        points.sumOf { it.x.toDouble() }.toFloat() / points.size,
        points.sumOf { it.y.toDouble() }.toFloat() / points.size
    )

    private fun calculateCosineSimilarity(vec1: FloatArray, vec2: FloatArray): Double {
        val dotProduct = vec1.zip(vec2).map { (v1, v2) -> v1 * v2 }.sum()
        val normVec1 = sqrt(vec1.map { it * it }.sum().toDouble())
        val normVec2 = sqrt(vec2.map { it * it }.sum().toDouble())
        return dotProduct / (normVec1 * normVec2)
    }
}
