package com.handtracker.asl.ml

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

data class ClassificationResult(
    val label: String,
    val confidence: Float
)

class GestureClassifier(context: Context) {

    private var interpreter: Interpreter? = null
    private val labels = mutableListOf<String>()

    init {
        loadModel(context)
        loadLabels(context)
    }

    private fun loadModel(context: Context) {
        try {
            val assetFileDescriptor = context.assets.openFd("gesture_classifier.tflite")
            val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val modelBuffer: ByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(modelBuffer, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadLabels(context: Context) {
        try {
            context.assets.open("labels.txt").use { inputStream ->
                InputStreamReader(inputStream, Charsets.UTF_8).useLines { lines ->
                    lines.forEach { labels.add(it.trim()) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Runs TFLite inference on 42-element float array (normalized landmarks)
     */
    fun classify(features: FloatArray): ClassificationResult? {
        val tflite = interpreter ?: return null
        if (labels.isEmpty() || features.size != 42) return null

        // Input tensor: [1, 42]
        val inputBuffer = ByteBuffer.allocateDirect(42 * 4).apply {
            order(ByteOrder.nativeOrder())
            for (value in features) {
                putFloat(value)
            }
            rewind()
        }

        // Output tensor: [1, num_classes]
        val outputArray = Array(1) { FloatArray(labels.size) }

        tflite.run(inputBuffer, outputArray)

        // Find argmax label
        val probabilities = outputArray[0]
        var maxIdx = 0
        var maxProb = 0f

        for (i in probabilities.indices) {
            if (probabilities[i] > maxProb) {
                maxProb = probabilities[i]
                maxIdx = i
            }
        }

        return ClassificationResult(
            label = labels.getOrElse(maxIdx) { "Unknown" },
            confidence = maxProb
        )
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
