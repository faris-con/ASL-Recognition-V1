# 🤟 ASL Hand Tracker & Translation Engine

> **Real-Time Offline American Sign Language (ASL) Recognition & Text-to-Speech Engine** for Desktop Python and Native Android (Kotlin + Jetpack Compose).

![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?style=flat-square&logo=kotlin&logoColor=white)
![Python](https://img.shields.io/badge/Python-3.11+-3776AB?style=flat-square&logo=python&logoColor=white)
![TensorFlow Lite](https://img.shields.io/badge/TFLite-21.25KB-FF6F00?style=flat-square&logo=tensorflow&logoColor=white)
![MediaPipe](https://img.shields.io/badge/MediaPipe-Tasks_Vision-0097A7?style=flat-square&logo=google&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4?style=flat-square&logo=android&logoColor=white)
![Offline](https://img.shields.io/badge/Offline-100%25_On--Device-00E676?style=flat-square)

---

## 🌟 Key Features

- ⚡ **100% Offline & On-Device Processing**: Zero server latency, 0$ cloud cost, fully functional without internet connection.
- 🎯 **High Precision & Ultra-Lightweight**: Deep Neural Network (MLP) trained on 29 classes, achieving **98.68% accuracy** in a **21.25 KB** TFLite model.
- 🖐️ **21 3D Hand Landmark Skeleton**: Uses MediaPipe Tasks Vision API for smooth 60 FPS joint tracking with scale & shift normalization.
- ⏱️ **Smart Hold Confirmation Filter**: Requires a gesture to be held consistently for 15 frames (~0.5s) to eliminate video jitter and accidental letter spam.
- 🔊 **Offline Text-To-Speech (TTS)**: Instant one-tap voice reading of accumulated sentences.
- 📱 **Modern Android Native App**: Sleek Dark Glassmorphic UI/UX built with Kotlin, CameraX, and Jetpack Compose.

---

## 🏗️ Architecture Overview

```
 ┌─────────────────────────┐
 │ Camera Stream (CameraX) │
 └────────────┬────────────┘
              ▼
 ┌─────────────────────────┐
 │ MediaPipe Tasks Vision  │  (21 3D Hand Landmarks)
 └────────────┬────────────┘
              ▼
 ┌─────────────────────────┐
 │ Feature Normalization   │  Scale & Shift: (x - min_x) / range_x
 └────────────┬────────────┘
              ▼
 ┌─────────────────────────┐
 │ TFLite Classifier (21KB)│  Neural Network (98.68% Accuracy)
 └────────────┬────────────┘
              ▼
 ┌─────────────────────────┐
 │ 15-Frame Hold Filter    │  De-jitter & Sentence Accumulation
 └────────────┬────────────┘
              ▼
 ┌─────────────────────────┐
 │ UI & Offline TTS Engine │  Jetpack Compose + Native Speech
 └─────────────────────────┘
```

---

## 📂 Project Structure

```
.
├── collect_imgs.py           # Collect training images via webcam
├── create_dataset.py         # Extract 42 normalized features into data.pickle
├── train_classifier.py       # Train Scikit-Learn RandomForest classifier
├── train_export_tflite.py    # Train Keras MLP & export gesture_classifier.tflite
├── inference_classifier.py   # Desktop live preview script with OpenCV UI & TTS
├── data.pickle               # Processed landmark dataset
├── model.p                   # Trained Scikit-Learn pickle model
├── hand_landmarker.task      # MediaPipe HandLandmarker asset
├── ASLHandTracker.apk        # Compiled Android APK (Ready to Install)
└── android_app/              # Native Kotlin Android Project
    ├── app/src/main/assets/
    │   ├── hand_landmarker.task
    │   ├── gesture_classifier.tflite
    │   └── labels.txt
    └── java/com/handtracker/asl/
        ├── MainActivity.kt
        ├── ml/               # HandLandmarkerHelper, GestureClassifier, Normalizer
        ├── viewmodel/        # MainViewModel & UI State management
        └── ui/               # Jetpack Compose UI Screens & Components
```

---

## 🚀 Getting Started

### 1️⃣ Prerequisites

- **Python**: 3.10+
- **Android Studio**: Jellyfish or newer (for mobile app customization)
- **JDK**: JDK 17

---

### 2️⃣ Running Python Desktop Version

```bash
# Clone the repository
git clone https://github.com/your-username/Handtracker.git
cd Handtracker

# Create and activate virtual environment
python -m venv .venv
source .venv/bin/activate  # On Windows: .venv\Scripts\activate

# Install dependencies
pip install opencv-python mediapipe tensorflow scikit-learn numpy pyttsx3

# Run the live camera inference preview
python inference_classifier.py
```

#### ⌨️ Desktop Keyboard Controls:
- **`SPACE`**: Read out loud the translated sentence via TTS.
- **`BACKSPACE`**: Delete the last confirmed character.
- **`C`**: Clear the entire sentence.
- **`Q`**: Quit the live camera feed.

---

### 3️⃣ Training & Exporting TFLite Model

To re-train the neural network and export an updated `.tflite` model:

```bash
python train_export_tflite.py
```

This updates `gesture_classifier.tflite` and `labels.txt` directly inside the Android assets folder `android_app/app/src/main/assets/`.

---

### 4️⃣ Building the Android APK

```bash
cd android_app

# Build debug APK using Gradle Wrapper
./gradlew assembleDebug
```

The output APK will be saved at:  
`android_app/app/build/outputs/apk/debug/app-debug.apk`

Or install directly to a connected Android device:
```bash
./gradlew installDebug
```

---

## 📊 Model Specifications & Metrics

| Parameter | Specification |
|---|---|
| **Classes** | 29 (A–Z, Space, Delete, Nothing) |
| **Input Vector** | 42 Normalized Float Features (21 x, y coordinates) |
| **Model Architecture** | MLP: Dense(128) -> BatchNorm -> Dropout -> Dense(64) -> Softmax |
| **Model Size** | **21.25 KB** |
| **Test Accuracy** | **98.68%** |
| **Inference Time** | **< 2ms / frame** |

---

## 📜 License

This project is open-source under the **MIT License**.
