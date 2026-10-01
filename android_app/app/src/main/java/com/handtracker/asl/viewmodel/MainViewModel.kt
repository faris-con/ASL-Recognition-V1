package com.handtracker.asl.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.handtracker.asl.ml.GestureClassifier
import com.handtracker.asl.ml.LandmarkNormalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val classifier = GestureClassifier(application.applicationContext)
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    // Smart Confirmation Threshold State
    private var lastRawPrediction = ""
    private var consecutiveCount = 0
    private var lastConfirmedChar = ""
    private val confirmThreshold = 15 // 15 consecutive frames (~0.5s) to confirm letter

    init {
        tts = TextToSpeech(application.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    fun onLandmarksDetected(landmarks: List<NormalizedLandmark>, width: Int, height: Int) {
        if (landmarks.isEmpty()) {
            resetFrameState()
            return
        }

        // 1. Calculate Bounding Box
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE

        for (lm in landmarks) {
            if (lm.x() < minX) minX = lm.x()
            if (lm.x() > maxX) maxX = lm.x()
            if (lm.y() < minY) minY = lm.y()
            if (lm.y() > maxY) maxY = lm.y()
        }

        val drawInfo = HandLandmarkDrawInfo(
            landmarks = landmarks,
            boundingBox = floatArrayOf(minX, minY, maxX, maxY)
        )

        // 2. Normalize features
        val features = LandmarkNormalizer.normalizeLandmarks(landmarks)

        // 3. Classify with TFLite
        val result = classifier.classify(features)
        if (result == null || result.confidence < 0.45f) {
            _uiState.update {
                it.copy(
                    isHandDetected = true,
                    handDrawInfo = drawInfo,
                    currentPrediction = "Analyzing...",
                    confidence = 0f,
                    confirmationProgress = 0f
                )
            }
            return
        }

        val predictedChar = result.label
        val conf = result.confidence

        // 4. Confirmation logic
        if (predictedChar == lastRawPrediction) {
            consecutiveCount++
        } else {
            lastRawPrediction = predictedChar
            consecutiveCount = 1
        }

        val progress = (consecutiveCount.toFloat() / confirmThreshold).coerceAtMost(1.0f)

        if (consecutiveCount == confirmThreshold && predictedChar != lastConfirmedChar) {
            appendToSentence(predictedChar)
            lastConfirmedChar = predictedChar
        }

        _uiState.update {
            it.copy(
                isHandDetected = true,
                handDrawInfo = drawInfo,
                currentPrediction = predictedChar,
                confidence = conf,
                confirmationProgress = progress
            )
        }
    }

    fun resetFrameState() {
        lastRawPrediction = ""
        consecutiveCount = 0
        lastConfirmedChar = ""

        _uiState.update {
            it.copy(
                isHandDetected = false,
                handDrawInfo = null,
                currentPrediction = "",
                confidence = 0f,
                confirmationProgress = 0f
            )
        }
    }

    private fun appendToSentence(char: String) {
        val current = _uiState.value.confirmedSentence
        val newSentence = when (char.lowercase()) {
            "space" -> "$current "
            "del" -> if (current.isNotEmpty()) current.dropLast(1) else current
            "nothing" -> current
            else -> "$current$char"
        }

        _uiState.update { it.copy(confirmedSentence = newSentence) }
    }

    fun speakSentence() {
        val text = _uiState.value.confirmedSentence.trim()
        if (text.isNotEmpty() && isTtsReady) {
            _uiState.update { it.copy(isSpeaking = true) }
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ASL_TTS")
        }
    }

    fun deleteLastChar() {
        val current = _uiState.value.confirmedSentence
        if (current.isNotEmpty()) {
            _uiState.update { it.copy(confirmedSentence = current.dropLast(1)) }
        }
    }

    fun clearSentence() {
        _uiState.update { it.copy(confirmedSentence = "") }
        lastConfirmedChar = ""
    }

    fun toggleCamera() {
        _uiState.update { it.copy(isFrontCamera = !it.isFrontCamera) }
    }

    override fun onCleared() {
        super.onCleared()
        classifier.close()
        tts?.stop()
        tts?.shutdown()
    }
}
