package com.vehiclesafetyguardian.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import androidx.camera.core.ImageProxy
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import javax.inject.Inject

class ObjectDetector @Inject constructor(
    private val context: Context
) {
    private lateinit var interpreter: Interpreter
    private val inputSize = 416 // YOLOv4-tiny input size
    private val outputSize = 2535 // (13x13 + 26x26 + 52x52) * 3 anchors
    private val numClasses = 80 // COCO dataset classes

    private val imageProcessor = ImageProcessor.Builder()
        .add(ResizeWithCropOrPadOp(inputSize, inputSize))
        .add(ResizeOp(inputSize, inputSize))
        .add(NormalizeOp(0f, 255f)) // Normalize to [0,1]
        .build()

    init {
        initializeModel()
    }

    private fun initializeModel() {
        val modelFile = FileUtil.loadMappedFile(context, "yolov4_tiny.tflite")
        interpreter = Interpreter(modelFile)
    }

    fun getAnalyzer(): ImageAnalysis.Analyzer {
        return ImageAnalysis.Analyzer { imageProxy ->
            val bitmap = imageProxy.toBitmap()
            val results = detectObjects(bitmap)
            // Process results and send to ViewModel
            imageProxy.close()
        }
    }

    fun detectObjects(bitmap: Bitmap): List<DetectionResult> {
        // Preprocess image
        var tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        // Prepare output buffers
        val outputLocations = TensorBuffer.createFixedSize(
            intArrayOf(1, outputSize, 4),
            DataType.FLOAT32
        )
        val outputClasses = TensorBuffer.createFixedSize(
            intArrayOf(1, outputSize, numClasses),
            DataType.FLOAT32
        )
        val outputScores = TensorBuffer.createFixedSize(
            intArrayOf(1, outputSize),
            DataType.FLOAT32
        )
        val numDetections = TensorBuffer.createFixedSize(
            intArrayOf(1),
            DataType.FLOAT32
        )

        // Run inference
        val inputs = mapOf(0 to tensorImage.buffer)
        val outputs = mapOf(
            0 to outputLocations.buffer,
            1 to outputClasses.buffer,
            2 to outputScores.buffer,
            3 to numDetections.buffer
        )
        interpreter.runForMultipleInputsOutputs(inputs, outputs)

        // Post-process results
        return processOutputs(
            outputLocations.floatArray,
            outputClasses.floatArray,
            outputScores.floatArray,
            numDetections.floatArray[0].toInt()
        )
    }

    private fun processOutputs(
        locations: FloatArray,
        classes: FloatArray,
        scores: FloatArray,
        numDetections: Int
    ): List<DetectionResult> {
        val results = mutableListOf<DetectionResult>()
        
        for (i in 0 until numDetections) {
            val score = scores[i]
            if (score < 0.5f) continue // Confidence threshold
            
            val box = RectF(
                locations[i * 4 + 1] * inputSize,
                locations[i * 4] * inputSize,
                locations[i * 4 + 3] * inputSize,
                locations[i * 4 + 2] * inputSize
            )
            
            val classId = classes[i * numClasses].toInt()
            when (classId) {
                2, 5, 7 -> results.add(DetectionResult.Vehicle(box, score, classId))
                0 -> results.add(DetectionResult.Person(box, score))
                // Add more class mappings as needed
            }
        }
        
        return results
    }
}

sealed class DetectionResult {
    data class Vehicle(
        val boundingBox: RectF,
        val confidence: Float,
        val classId: Int,
        var closingSpeed: Float = 0f
    ) : DetectionResult()

    data class Person(
        val boundingBox: RectF,
        val confidence: Float
    ) : DetectionResult()

    // Add more detection types as needed
}