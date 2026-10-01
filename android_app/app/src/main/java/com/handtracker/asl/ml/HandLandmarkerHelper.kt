package com.handtracker.asl.ml

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

class HandLandmarkerHelper(
    val context: Context,
    val minHandDetectionConfidence: Float = 0.3f,
    val minHandPresenceConfidence: Float = 0.3f,
    val landmarkListener: LandmarkListener? = null
) {

    interface LandmarkListener {
        fun onError(error: String)
        fun onResults(result: HandLandmarkerResult, imageWidth: Int, imageHeight: Int)
    }

    private var handLandmarker: HandLandmarker? = null

    init {
        setupHandLandmarker()
    }

    fun setupHandLandmarker() {
        val baseOptionsBuilder = BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task")
            .setDelegate(Delegate.CPU)

        val optionsBuilder = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptionsBuilder.build())
            .setMinHandDetectionConfidence(minHandDetectionConfidence)
            .setMinHandPresenceConfidence(minHandPresenceConfidence)
            .setNumHands(1)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result, inputImage ->
                landmarkListener?.onResults(result, inputImage.width, inputImage.height)
            }
            .setErrorListener { error ->
                landmarkListener?.onError(error.message ?: "Unknown MediaPipe Error")
            }

        try {
            handLandmarker = HandLandmarker.createFromOptions(context, optionsBuilder.build())
        } catch (e: Exception) {
            landmarkListener?.onError("HandLandmarker init failed: ${e.localizedMessage}")
        }
    }

    fun detectLiveStream(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        val frameTime = SystemClock.uptimeMillis()

        // Convert ImageProxy to Bitmap
        val bitmapBuffer = Bitmap.createBitmap(
            imageProxy.width,
            imageProxy.height,
            Bitmap.Config.ARGB_8888
        )
        imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }

        val mpImage = BitmapImageBuilder(bitmapBuffer).build()
        handLandmarker?.detectAsync(mpImage, frameTime)
    }

    fun clear() {
        handLandmarker?.close()
        handLandmarker = null
    }
}
