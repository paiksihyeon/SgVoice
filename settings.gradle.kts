pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.google.com") }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://maven.google.com") }
    }
}

rootProject.name = "SgVoice"
include(":app")
