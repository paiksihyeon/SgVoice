package com.example.sgvoice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val cameraPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            startCamera()
        } else {
            showPermissionDeniedMessage()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(res.layout.layout)

        // 카메라 권한 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            cameraPermissionRequest.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val previewView = findViewById<PreviewView>(res.id.previewView)

        // CameraProvider 인스턴스 가져오기
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // 프리뷰 빌드
            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

            // 카메라 설정 (후면 카메라)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // 카메라 시작
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun showPermissionDeniedMessage() {
        // 권한 거부 메시지 처리
        Toast.makeText(this, "카메라 권한이 필요합니다. 권한을 허용해주세요.", Toast.LENGTH_LONG).show()
    }
}
