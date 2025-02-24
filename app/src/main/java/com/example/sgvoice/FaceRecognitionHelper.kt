package com.example.sgvoice

import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.framework.image.BitmapImageBuilder
import kotlin.math.sqrt

// 수정된 FaceFeature: 전체 랜드마크 기반 디스크립터와 개별 부위(눈, 코, 입) 디스크립터를 함께 저장
data class FaceFeature(
    val points: List<PointF>,
    val overallDescriptor: FloatArray,
    val partDescriptors: Map<String, FloatArray>
)

// FaceData는 오버레이에 사용할 정보만 포함 (고유 ID, 바운딩 박스, 원본 랜드마크)
data class FaceData(val id: Int, val boundingBox: Rect, val landmarkPoints: List<PointF>, val descriptor: FloatArray)

class FaceRecognitionHelper(
    private val context: Context,
    private val callback: (List<FaceData>) -> Unit
) {
    // MediaPipe FaceLandmarker를 사용해 얼굴 랜드마크를 감지
    private val faceLandmarker: FaceLandmarker =
        FaceLandmarker.createFromFile(context, "face_landmarker.task")

    // 이전에 저장한 얼굴 특징(전체 및 부위별)을 저장 (고유ID -> FaceFeature)
    private val storedFaces = mutableMapOf<Int, FaceFeature>()
    private var nextFaceId = 1
    private val maxFaceId = 6

    // 두 단계 유사도 결합 시 사용할 가중치 및 임계값 (튜닝 필요)
    private val overallWeight = 0.5
    private val partsWeight = 0.5
    private val combinedThreshold = 0.3

    fun processImage(bitmap: Bitmap) {
        try {
            // MediaPipe를 통해 얼굴 랜드마크 감지
            val result = faceLandmarker.detect(BitmapImageBuilder(bitmap).build())
            val detectedFaces = result.faceLandmarks().map { landmarks ->
                // 원본 이미지 크기에 맞춰 랜드마크 좌표 변환
                val points = landmarks.map { PointF(it.x() * bitmap.width, it.y() * bitmap.height) }
                // 얼굴 바운딩 박스 계산
                val boundingBox = calculateBoundingBox(points)
                // 바운딩 박스 기준으로 랜드마크 정규화 (0~1 사이의 값)
                val normalizedPoints = normalizeLandmarks(points, boundingBox)
                // 전체 얼굴 디스크립터 생성 (모든 정규화된 좌표 사용)
                val overallDescriptor = normalizeFeatureVector(extractFeatureVector(normalizedPoints))
                // 부위별 디스크립터 생성 (68점 모델을 가정: 눈, 코, 입)
                val partDescriptors = if (normalizedPoints.size >= 68) getPartDescriptors(normalizedPoints) else emptyMap()
                val faceFeature = FaceFeature(normalizedPoints, overallDescriptor, partDescriptors)
                // 기존 저장된 얼굴과 비교하여, 두 단계 유사도 측정 후 매칭
                val faceId = findMatchingFace(faceFeature) ?: getNextFaceId().also { storedFaces[it] = faceFeature }
                FaceData(faceId, boundingBox, points, overallDescriptor)
            }
            callback(detectedFaces)
        } catch (e: Exception) {
            Log.e("FaceRecognitionHelper", "Error processing image", e)
        }
    }

    // 최대 얼굴 수 초과 시, 가장 오래된 얼굴 제거
    private fun getNextFaceId(): Int {
        if (storedFaces.size >= maxFaceId) {
            val oldestId = storedFaces.keys.minOrNull()
            if (oldestId != null) {
                storedFaces.remove(oldestId)
                return oldestId
            }
        }
        return nextFaceId++.coerceAtMost(maxFaceId)
    }

    // 랜드마크 좌표들을 이어붙여 2차원 특징 벡터(디스크립터) 생성
    private fun extractFeatureVector(points: List<PointF>): FloatArray {
        val vector = FloatArray(points.size * 2)
        points.forEachIndexed { index, point ->
            vector[index * 2] = point.x
            vector[index * 2 + 1] = point.y
        }
        return vector
    }

    // 얼굴 바운딩 박스 기준으로 랜드마크 정규화 (0~1 사이의 값)
    private fun normalizeLandmarks(points: List<PointF>, boundingBox: Rect): List<PointF> {
        val width = boundingBox.width().toFloat().coerceAtLeast(1f)
        val height = boundingBox.height().toFloat().coerceAtLeast(1f)
        return points.map { point ->
            PointF(
                (point.x - boundingBox.left) / width,
                (point.y - boundingBox.top) / height
            )
        }
    }

    // 특징 벡터 정규화 (L2 노름)
    private fun normalizeFeatureVector(vector: FloatArray): FloatArray {
        val norm = sqrt(vector.map { it * it }.sum())
        return if (norm == 0.0f) vector else vector.map { it / norm }.toFloatArray()
    }

    // 부위별 디스크립터 생성 함수
    // 68점 모델 기준으로: 오른쪽 눈(36~41), 왼쪽 눈(42~47), 코(27~35), 입(48~67)
    private fun getPartDescriptors(points: List<PointF>): Map<String, FloatArray> {
        val descriptors = mutableMapOf<String, FloatArray>()
        try {
            // Kotlin의 subList: endIndex는 exclusive
            descriptors["rightEye"] = normalizeFeatureVector(extractFeatureVector(points.subList(36, 42)))
            descriptors["leftEye"] = normalizeFeatureVector(extractFeatureVector(points.subList(42, 48)))
            descriptors["nose"] = normalizeFeatureVector(extractFeatureVector(points.subList(27, 36)))
            descriptors["mouth"] = normalizeFeatureVector(extractFeatureVector(points.subList(48, 68)))
        } catch (e: Exception) {
            Log.e("FaceRecognitionHelper", "Error creating part descriptors", e)
        }
        return descriptors
    }

    // 두 특징 벡터 간 유클리드 거리 계산
    private fun calculateEuclideanDistance(vec1: FloatArray, vec2: FloatArray): Double {
        if (vec1.size != vec2.size) return Double.MAX_VALUE
        var sum = 0.0
        for (i in vec1.indices) {
            val diff = vec1[i] - vec2[i]
            sum += diff * diff
        }
        return sqrt(sum)
    }

    // 저장된 얼굴과 비교하여, 두 단계(전체 + 부위별) 유사도 결합으로 매칭
    private fun findMatchingFace(newFace: FaceFeature): Int? {
        var bestMatchId: Int? = null
        var bestCombinedDistance = Double.MAX_VALUE

        for ((id, storedFace) in storedFaces) {
            // 전체 얼굴 유사도 계산
            val overallDistance = calculateEuclideanDistance(newFace.overallDescriptor, storedFace.overallDescriptor)

            // 부위별 유사도 계산 (동일 부위만 비교)
            val regionDistances = mutableListOf<Double>()
            for ((region, descriptor) in newFace.partDescriptors) {
                if (storedFace.partDescriptors.containsKey(region)) {
                    val distance = calculateEuclideanDistance(descriptor, storedFace.partDescriptors[region]!!)
                    regionDistances.add(distance)
                }
            }
            val avgRegionDistance = if (regionDistances.isNotEmpty()) regionDistances.average() else 0.0

            // 전체와 부위별 유사도를 가중 결합
            val combinedDistance = overallWeight * overallDistance + partsWeight * avgRegionDistance

            Log.d("FaceRecognitionHelper", "Comparing with Face ID: $id, Overall: $overallDistance, Regions Avg: $avgRegionDistance, Combined: $combinedDistance")

            if (combinedDistance < combinedThreshold && combinedDistance < bestCombinedDistance) {
                bestCombinedDistance = combinedDistance
                bestMatchId = id
            }
        }
        return bestMatchId
    }

    // 얼굴 바운딩 박스 계산 (모든 랜드마크 좌표의 최소/최대값 사용)
    private fun calculateBoundingBox(landmarks: List<PointF>): Rect {
        val xs = landmarks.map { it.x }
        val ys = landmarks.map { it.y }
        return Rect(
            xs.minOrNull()?.toInt() ?: 0,
            ys.minOrNull()?.toInt() ?: 0,
            xs.maxOrNull()?.toInt() ?: 0,
            ys.maxOrNull()?.toInt() ?: 0
        )
    }
}
