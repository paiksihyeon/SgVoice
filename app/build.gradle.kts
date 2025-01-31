plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.sgvoice"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.sgvoice"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // CameraX 라이브러리
    implementation("androidx.camera:camera-core:1.3.0") // 카메라 기능을 사용하기 위한 API 제공
    implementation("androidx.camera:camera-lifecycle:1.3.0") // 앱의 활동(Activity) 또는 프래그먼트(Fragment)와 카메라 기능을 연결
    implementation("androidx.camera:camera-view:1.3.0") // 카메라 미리보기, 캡처 등을 화면에 쉽게 표시
    implementation("androidx.camera:camera-extensions:1.3.0") // 카메라의 성능을 개선하고, 고급 기능(예: 뷰티 모드, 야간 모드 등)을 활성화

    // MediaPipe 라이브러리
    implementation("com.google.mediapipe:tasks-vision:0.10.0") // 얼굴 감지, 손 동작 인식, 포즈 추적 등 다양한 비전 솔루션 제공

    // ML Kit 라이브러리
    implementation("com.google.mlkit:face-detection:16.1.7") // 카메라를 통해 캡처한 이미지에서 얼굴을 자동으로 감지하고, 얼굴의 특성(예: 위치, 표정 등)을 추출

    // AndroidX 라이브러리
    implementation("androidx.core:core-ktx:1.10.1") // Kotlin 확장 라이브러리
    implementation("androidx.appcompat:appcompat:1.6.1") // ActionBar, Toolbar 등과 같은 UI 컴포넌트를 지원
    implementation("com.google.android.material:material:1.9.0") // UI 요소를 Material Design 스타일로 구현
    implementation("androidx.constraintlayout:constraintlayout:2.1.4") // 다양한 화면 크기에서 적응형 UI 생성
}