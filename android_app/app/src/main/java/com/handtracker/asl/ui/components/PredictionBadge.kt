package com.handtracker.asl.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.handtracker.asl.ui.theme.DarkSlateCard
import com.handtracker.asl.ui.theme.PrimaryCyan
import com.handtracker.asl.ui.theme.PrimaryEmerald
import com.handtracker.asl.ui.theme.TextPrimary
import com.handtracker.asl.ui.theme.TextSecondary

@Composable
fun PredictionBadge(
    currentPrediction: String,
    confidence: Float,
    confirmationProgress: Float,
    isHandDetected: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isHandDetected && currentPrediction.isNotEmpty(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSlateCard)
                .border(1.dp, PrimaryEmerald.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentPrediction,
                    color = PrimaryEmerald,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                if (confidence > 0f) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${(confidence * 100).toInt()}%",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (confirmationProgress > 0f) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(0.5f)
                ) {
                    Text(
                        text = "Holding...",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    LinearProgressIndicator(
                        progress = { confirmationProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = if (confirmationProgress >= 1.0f) PrimaryEmerald else PrimaryCyan,
                        trackColor = Color.White.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
}
