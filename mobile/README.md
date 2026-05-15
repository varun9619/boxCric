# Box Cricket Mobile App (Android/Kotlin)

This directory contains the Android source code for the Box Cricket Scoring App.

## Getting Started

Since Android Gradle projects are complex and best initialized via the IDE, please follow these steps to scaffold the mobile app:

1. Open **Android Studio**.
2. Select **"New Project"** -> **"Empty Activity"** (Using Jetpack Compose).
3. Set Language to **Kotlin**.
4. Set the project location to `C:\Users\tarun\.gemini\antigravity\scratch\box-cricket-ai\mobile`.

## Required Dependencies

Once initialized, add the following to your `app/build.gradle.kts`:

```kotlin
dependencies {
    // CameraX for dual-camera feed processing
    def camerax_version = "1.3.0-rc01"
    implementation("androidx.camera:camera-core:${camerax_version}")
    implementation("androidx.camera:camera-camera2:${camerax_version}")
    implementation("androidx.camera:camera-lifecycle:${camerax_version}")
    implementation("androidx.camera:camera-view:${camerax_version}")

    // MediaPipe for Pose/Crease detection
    implementation("com.google.mediapipe:tasks-vision:0.10.0")

    // TensorFlow Lite for YOLOv8 Custom Models
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    // Ktor Client for WebSocket syncing with FastAPI
    def ktor_version = "2.3.5"
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-websockets:$ktor_version")
}
```
