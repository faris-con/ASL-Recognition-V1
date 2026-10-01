package com.handtracker.asl.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.handtracker.asl.ui.components.CameraPreview
import com.handtracker.asl.ui.components.LandmarkOverlayView
import com.handtracker.asl.ui.components.PredictionBadge
import com.handtracker.asl.ui.components.SentenceBarCard
import com.handtracker.asl.ui.components.TopHeader
import com.handtracker.asl.ui.theme.DarkObsidian
import com.handtracker.asl.viewmodel.MainViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkObsidian)
    ) {
        // 1. CameraX Live Camera View
        CameraPreview(
            isFrontCamera = uiState.isFrontCamera,
            onLandmarksDetected = { result, width, height ->
                val landmarks = result.landmarks().firstOrNull() ?: emptyList()
                viewModel.onLandmarksDetected(landmarks, width, height)
            },
            onError = { err ->
                viewModel.resetFrameState()
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Real-time Landmark Overlay (Bones, Joints, Bounding Box)
        LandmarkOverlayView(
            drawInfo = uiState.handDrawInfo,
            isFrontCamera = uiState.isFrontCamera,
            modifier = Modifier.fillMaxSize()
        )

        // 3. Top Header Glass Bar
        TopHeader(
            isFrontCamera = uiState.isFrontCamera,
            onToggleCamera = { viewModel.toggleCamera() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        )

        // 4. Center-Bottom Prediction Badge & Sentence Builder Card
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            PredictionBadge(
                currentPrediction = uiState.currentPrediction,
                confidence = uiState.confidence,
                confirmationProgress = uiState.confirmationProgress,
                isHandDetected = uiState.isHandDetected
            )

            Spacer(modifier = Modifier.height(12.dp))

            SentenceBarCard(
                sentence = uiState.confirmedSentence,
                onSpeak = { viewModel.speakSentence() },
                onDelete = { viewModel.deleteLastChar() },
                onClear = { viewModel.clearSentence() }
            )
        }
    }
}
