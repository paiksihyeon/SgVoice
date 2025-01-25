pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()  // Google 저장소 추가
        mavenCentral()  // Maven Central 추가
    }
}

rootProject.name = "sgvoice"
include(":app")