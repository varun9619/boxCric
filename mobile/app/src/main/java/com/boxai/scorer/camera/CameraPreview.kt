package com.boxai.scorer.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraPreview(
    onFaceDetected: (Bitmap) -> Unit,
    modifier: Modifier = Modifier,
    lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    active: Boolean = true
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(cameraProviderFuture) {
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
        }, ContextCompat.getMainExecutor(context))
        onDispose {}
    }

    if (cameraProvider != null) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx)
            },
            update = { previewView ->
                val provider = cameraProvider!!
                if (active) {
                    setupCamera(context, previewView, lifecycleOwner, cameraExecutor, lensFacing, onFaceDetected, provider)
                } else {
                    provider.unbindAll()
                }
            },
            modifier = modifier.fillMaxSize()
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}

private fun setupCamera(
    context: Context,
    previewView: PreviewView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    cameraExecutor: ExecutorService,
    lensFacing: Int,
    onFaceDetected: (Bitmap) -> Unit,
    cameraProvider: ProcessCameraProvider
) {
    try {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, FaceAnalyzer(onFaceDetected))
            }

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner, cameraSelector, preview, imageAnalyzer
        )
    } catch (exc: Exception) {
        Log.e("CameraPreview", "Use case binding failed", exc)
    }
}

private class FaceAnalyzer(private val onFaceDetected: (Bitmap) -> Unit) : ImageAnalysis.Analyzer {
    
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()
    )

    private var isProcessing = false
    private var lastProcessTime = 0L

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        // Limit face detection to 1 frame per second to save battery and reduce network load
        if (isProcessing || currentTime - lastProcessTime < 1000) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            isProcessing = true
            lastProcessTime = currentTime
            try {
                val bitmap = imageProxy.toBitmap()
                val image = InputImage.fromBitmap(bitmap, 0)
                
                detector.process(image)
                    .addOnSuccessListener { faces ->
                        if (faces.isNotEmpty()) {
                            val face = faces.first()
                            val boundingBox = face.boundingBox
                            
                            val padding = 20
                            val left = (boundingBox.left - padding).coerceAtLeast(0)
                            val top = (boundingBox.top - padding).coerceAtLeast(0)
                            val width = (boundingBox.width() + padding * 2).coerceAtMost(bitmap.width - left)
                            val height = (boundingBox.height() + padding * 2).coerceAtMost(bitmap.height - top)
                            
                            if (width > 0 && height > 0) {
                                val croppedBitmap = Bitmap.createBitmap(bitmap, left, top, width, height)
                                onFaceDetected(croppedBitmap)
                            }
                        }
                    }
                    .addOnCompleteListener {
                        isProcessing = false
                        imageProxy.close()
                    }
            } catch (e: Exception) {
                Log.e("FaceAnalyzer", "Failed to process face", e)
                isProcessing = false
                imageProxy.close()
            }
        } else {
            imageProxy.close()
        }
    }
}
