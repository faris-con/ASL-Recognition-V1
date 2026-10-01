package com.handtracker.asl.ml

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

object LandmarkNormalizer {

    /**
     * Normalizes 21 hand landmarks using Scale & Shift normalization (exact match to Python create_dataset.py)
     * Output: 42 Float Array [x0_norm, y0_norm, x1_norm, y1_norm, ...]
     */
    fun normalizeLandmarks(landmarks: List<NormalizedLandmark>): FloatArray {
        if (landmarks.isEmpty()) return FloatArray(0)

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

        val rangeX = if ((maxX - minX) > 0f) (maxX - minX) else 1.0f
        val rangeY = if ((maxY - minY) > 0f) (maxY - minY) else 1.0f

        val features = FloatArray(landmarks.size * 2)
        var index = 0
        for (lm in landmarks) {
            features[index++] = (lm.x() - minX) / rangeX
            features[index++] = (lm.y() - minY) / rangeY
        }

        return features
    }
}
