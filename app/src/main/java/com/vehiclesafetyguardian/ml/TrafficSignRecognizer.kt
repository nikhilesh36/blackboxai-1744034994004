package com.vehiclesafetyguardian.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.support.label.Category
import javax.inject.Inject

class TrafficSignRecognizer @Inject constructor(
    private val context: Context
) {
    private lateinit var interpreter: Interpreter
    private val inputSize = 224 // EfficientNet-Lite input size
    private val signClasses = listOf(
        "speed_20", "speed_30", "speed_50", "speed_60", "speed_70", 
        "speed_80", "speed_100", "speed_120", "stop", "yield",
        "no_entry", "pedestrian_crossing", "school_zone", "construction"
    )

    init {
        initializeModel()
    }

    private fun initializeModel() {
        val modelFile = FileUtil.loadMappedFile(context, "efficientnet_lite.tflite")
        interpreter = Interpreter(modelFile)
    }

    fun recognizeSign(croppedSign: Bitmap): TrafficSignResult {
        // Preprocess image
        val tensorImage = TensorImage.fromBitmap(croppedSign)
            .resize(ResizeOp(inputSize, inputSize))
            .rotate(Rot90Op(0)) // Adjust rotation if needed

        // Run inference
        val outputBuffer = TensorBuffer.createFixedSize(
            intArrayOf(1, signClasses.size),
            tensorImage.dataType
        )
        interpreter.run(tensorImage.buffer, outputBuffer.buffer)

        // Get top result
        val results = outputBuffer.floatArray
            .mapIndexed { index, score -> Category(signClasses[index], score) }
            .sortedByDescending { it.score }

        return when (val topResult = results.first()) {
            else -> TrafficSignResult(
                signType = topResult.label,
                confidence = topResult.score,
                boundingBox = RectF() // Actual coordinates would come from object detector
            )
        }
    }
}

data class TrafficSignResult(
    val signType: String,
    val confidence: Float,
    val boundingBox: RectF
) {
    fun isSpeedLimit(): Boolean = signType.startsWith("speed_")
    fun getSpeedLimit(): Int? = if (isSpeedLimit()) {
        signType.substringAfter("speed_").toIntOrNull()
    } else null
}