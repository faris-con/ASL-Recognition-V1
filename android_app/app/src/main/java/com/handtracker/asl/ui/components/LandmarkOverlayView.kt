package com.handtracker.asl.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.handtracker.asl.ui.theme.BoundingBoxColor
import com.handtracker.asl.ui.theme.LandmarkConnector
import com.handtracker.asl.ui.theme.LandmarkGreen
import com.handtracker.asl.viewmodel.HandLandmarkDrawInfo

// Hand landmark connections matching MediaPipe hand skeleton (21 points)
private val HAND_CONNECTIONS = listOf(
    // Wrist to Thumb
    0 to 1, 1 to 2, 2 to 3, 3 to 4,
    // Wrist to Index
    0 to 5, 5 to 6, 6 to 7, 7 to 8,
    // Index to Middle
    5 to 9, 9 to 10, 10 to 11, 11 to 12,
    // Middle to Ring
    9 to 13, 13 to 14, 14 to 15, 15 to 16,
    // Ring to Pinky
    13 to 17, 0 to 17, 17 to 18, 18 to 19, 19 to 20
)

@Composable
fun LandmarkOverlayView(
    drawInfo: HandLandmarkDrawInfo?,
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (drawInfo == null || drawInfo.landmarks.isEmpty()) return@Canvas

        val canvasWidth = size.width
        val canvasHeight = size.height
        val landmarks = drawInfo.landmarks

        // Convert normalized (0..1) landmarks to screen pixel coordinates
        val points = landmarks.map { lm ->
            val x = if (isFrontCamera) (1.0f - lm.x()) * canvasWidth else lm.x() * canvasWidth
            val y = lm.y() * canvasHeight
            Offset(x, y)
        }

        // 1. Draw Connecting Bones (Skeleton lines)
        for ((startIdx, endIdx) in HAND_CONNECTIONS) {
            if (startIdx < points.size && endIdx < points.size) {
                drawLine(
                    color = LandmarkConnector,
                    start = points[startIdx],
                    end = points[endIdx],
                    strokeWidth = 5f
                )
            }
        }

        // 2. Draw Joint Dots (Glowing Neon circles)
        for (point in points) {
            // Inner vibrant dot
            drawCircle(
                color = LandmarkGreen,
                radius = 8f,
                center = point
            )
            // Outer subtle glow
            drawCircle(
                color = LandmarkGreen.copy(alpha = 0.35f),
                radius = 16f,
                center = point
            )
        }

        // 3. Draw Bounding Box around Detected Hand
        val bbox = drawInfo.boundingBox
        val minX = if (isFrontCamera) (1.0f - bbox[2]) * canvasWidth else bbox[0] * canvasWidth
        val maxX = if (isFrontCamera) (1.0f - bbox[0]) * canvasWidth else bbox[2] * canvasWidth
        val minY = bbox[1] * canvasHeight
        val maxY = bbox[3] * canvasHeight

        val padding = 30f
        val left = (minX - padding).coerceAtLeast(0f)
        val top = (minY - padding).coerceAtLeast(0f)
        val right = (maxX + padding).coerceAtMost(canvasWidth)
        val bottom = (maxY + padding).coerceAtMost(canvasHeight)

        drawRoundRect(
            color = BoundingBoxColor,
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
            cornerRadius = CornerRadius(16f, 16f),
            style = Stroke(
                width = 4f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
            )
        )
    }
}
