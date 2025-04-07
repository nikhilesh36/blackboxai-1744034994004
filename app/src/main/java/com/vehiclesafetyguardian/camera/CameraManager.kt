package com.vehiclesafetyguardian.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class CameraManager(private val context: Context) {
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    private val frameBufferQueue = ArrayBlockingQueue<ImageProxy>(3)
    var onFrameProcessed: ((Bitmap) -> Unit)? = null
    private var frameRateMonitor: FrameRateMonitor? = null

    fun enableFrameRateMonitoring(callback: (fps: Int) -> Unit) {
        frameRateMonitor = FrameRateMonitor(callback).apply {
            start()
        }
    }

    fun disableFrameRateMonitoring() {
        frameRateMonitor?.stop()
        frameRateMonitor = null
    }

    suspend fun initialize(): ProcessCameraProvider {
        return suspendCoroutine { continuation ->
            ProcessCameraProvider.getInstance(context).also { future ->
                future.addListener({
                    cameraProvider = future.get()
                    continuation.resume(future.get())
                }, ContextCompat.getMainExecutor(context))
            }
        }
    }

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        analysisUseCase: ImageAnalysis
    ) {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera not initialized")
        cameraProvider.unbindAll()

        val preview = Preview.Builder()
            .build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                analysisUseCase
            )
        } catch (exc: Exception) {
            // Handle camera initialization errors
        }
    }

    fun getImageAnalysisUseCase(
        targetResolution: Size,
        analyzer: ImageAnalysis.Analyzer
    ): ImageAnalysis {
        return ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(targetResolution)
            .build()
            .also { it.setAnalyzer(cameraExecutor, analyzer) }
    }

    fun processFrame(image: ImageProxy) {
        if (frameBufferQueue.remainingCapacity() == 0) {
            frameBufferQueue.poll()?.close()
        }
        frameBufferQueue.add(image)
        
        cameraExecutor.execute {
            try {
                val bitmap = image.toBitmap()
                onFrameProcessed?.invoke(bitmap)
            } finally {
                image.close()
            }
        }
    }

    fun shutdown() {
        cameraExecutor.shutdown()
        frameBufferQueue.clear()
    }
}