package com.example.apptest

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class FruitVegClassifier(context: Context) {
    private var interpreter: Interpreter

    private val labels = listOf(
        "apple_ripe", "apple_unripe", "banana_ripe", "banana_unripe",
        "broccoli_cooked", "broccoli_raw", "carrot_cooked", "carrot_raw",
        "mango_ripe", "mango_unripe"
    )

    init {
        val model = loadModelFile(context, "fruitveg_classifier.tflite")
        interpreter = Interpreter(model)
    }

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun classify(bitmap: Bitmap): ClassificationResult {
        // Preprocess image
        val preprocessedBitmap = ImagePreprocessor.preprocessBitmap(bitmap, 224)
        val input = ImagePreprocessor.convertBitmapToModelInput(preprocessedBitmap)

        // Prepare input buffer
        val inputBuffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3)
        inputBuffer.order(ByteOrder.nativeOrder())
        input.forEach { inputBuffer.putFloat(it) }

        // Prepare output buffer
        val output = Array(1) { FloatArray(labels.size) }

        // Run inference
        interpreter.run(inputBuffer, output)

        // Get results
        val probabilities = output[0]
        val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
        val maxProbability = probabilities[maxIndex]

        return ClassificationResult(
            label = labels[maxIndex],
            confidence = maxProbability,
            allProbabilities = labels.zip(probabilities.toList())
        )
    }

    fun close() {
        interpreter.close()
    }
}

data class ClassificationResult(
    val label: String,
    val confidence: Float,
    val allProbabilities: List<Pair<String, Float>>
)
