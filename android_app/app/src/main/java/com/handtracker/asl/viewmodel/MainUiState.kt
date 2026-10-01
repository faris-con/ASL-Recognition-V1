package com.handtracker.asl.viewmodel

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

data class HandLandmarkDrawInfo(
    val landmarks: List<NormalizedLandmark>,
    val boundingBox: FloatArray // [minX, minY, maxX, maxY]
)

data class MainUiState(
    val currentPrediction: String = "",
    val confidence: Float = 0f,
    val confirmedSentence: String = "",
    val confirmationProgress: Float = 0f,
    val isSpeaking: Boolean = false,
    val isHandDetected: Boolean = false,
    val handDrawInfo: HandLandmarkDrawInfo? = null,
    val isFrontCamera: Boolean = true,
    val errorMessage: String? = null
)
