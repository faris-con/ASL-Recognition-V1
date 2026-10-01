import pickle
import os
import threading

import cv2
import mediapipe as mp
import numpy as np
import pyttsx3

# --- TTS (runs in background thread) ---
def speak_text(text):
    """Speak text in a background thread so it doesn't block the camera."""
    def _speak():
        engine = pyttsx3.init()  # New engine each time to avoid freeze
        engine.setProperty('rate', 150)
        engine.say(text)
        engine.runAndWait()
        engine.stop()
    thread = threading.Thread(target=_speak, daemon=True)
    thread.start()

# --- New MediaPipe Tasks API ---
BaseOptions = mp.tasks.BaseOptions
HandLandmarker = mp.tasks.vision.HandLandmarker
HandLandmarkerOptions = mp.tasks.vision.HandLandmarkerOptions
VisionRunningMode = mp.tasks.vision.RunningMode

model_dict = pickle.load(open('./model.p', 'rb'))
model = model_dict['model']

cap = cv2.VideoCapture(0)

# Build labels from dataset folder names
DATA_DIR = './asl_alphabet_train/asl_alphabet_train'
class_names = sorted(os.listdir(DATA_DIR))

# --- Letter accumulation & TTS state ---
sentence = ""                # The accumulated sentence
current_char = ""            # Currently detected character
char_confirm_count = 0       # How many consecutive frames this char appeared
CONFIRM_THRESHOLD = 15       # Frames needed to confirm a letter
last_confirmed_char = ""     # Prevent adding the same letter repeatedly

# --- HandLandmarker for VIDEO mode ---
options = HandLandmarkerOptions(
    base_options=BaseOptions(model_asset_path='hand_landmarker.task'),
    running_mode=VisionRunningMode.VIDEO,
    num_hands=1,
    min_hand_detection_confidence=0.3,
    min_hand_presence_confidence=0.3
)

with HandLandmarker.create_from_options(options) as landmarker:
    timestamp_ms = 0
    while True:
        data_aux = []
        x_ = []
        y_ = []

        ret, frame = cap.read()
        if not ret:
            break

        H, W, _ = frame.shape
        
        # Detect landmarks on original frame (unflipped for correct hand orientation detection)
        frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=frame_rgb)

        timestamp_ms += 33  # ~30 FPS
        results = landmarker.detect_for_video(mp_image, timestamp_ms)

        # Now flip display frame for natural mirror preview
        display_frame = cv2.flip(frame, 1)

        predicted_character = ""

        if results.hand_landmarks:
            hand_landmarks = results.hand_landmarks[0]

            for lm in hand_landmarks:
                x_.append(lm.x)
                y_.append(lm.y)

            min_x, max_x = min(x_), max(x_)
            min_y, max_y = min(y_), max(y_)

            range_x = (max_x - min_x) if (max_x - min_x) > 0 else 1.0
            range_y = (max_y - min_y) if (max_y - min_y) > 0 else 1.0

            # Normalized features (matching create_dataset.py)
            for lm in hand_landmarks:
                data_aux.append((lm.x - min_x) / range_x)
                data_aux.append((lm.y - min_y) / range_y)

            # Mirror landmarks drawing on display_frame
            landmark_points = []
            for lm in hand_landmarks:
                cx = int((1.0 - lm.x) * W)  # Mirrored X for preview
                cy = int(lm.y * H)
                landmark_points.append((cx, cy))
                cv2.circle(display_frame, (cx, cy), 5, (0, 255, 0), -1)

            # Draw connections
            connections = mp.tasks.vision.HandLandmarksConnections.HAND_CONNECTIONS
            for conn in connections:
                start = landmark_points[conn.start]
                end = landmark_points[conn.end]
                cv2.line(display_frame, start, end, (255, 255, 255), 2)

            x1 = int((1.0 - max_x) * W) - 10
            y1 = int(min_y * H) - 10
            x2 = int((1.0 - min_x) * W) + 10
            y2 = int(max_y * H) + 10

            prediction = model.predict([np.asarray(data_aux)])
            predicted_character = str(prediction[0])

            cv2.rectangle(display_frame, (x1, y1), (x2, y2), (0, 255, 0), 3)
            cv2.putText(display_frame, predicted_character, (x1, max(y1, 30)),
                        cv2.FONT_HERSHEY_SIMPLEX, 1.3, (0, 255, 0), 3, cv2.LINE_AA)

        # --- Smart letter accumulation ---
        if predicted_character:
            if predicted_character == current_char:
                char_confirm_count += 1
            else:
                current_char = predicted_character
                char_confirm_count = 1

            # If same char held for CONFIRM_THRESHOLD frames → add to sentence
            if char_confirm_count == CONFIRM_THRESHOLD and current_char != last_confirmed_char:
                if current_char.lower() == "space":
                    sentence += " "
                elif current_char.lower() == "del":
                    sentence = sentence[:-1]
                elif current_char.lower() == "nothing":
                    pass  # Ignore "nothing" class
                else:
                    sentence += current_char
                last_confirmed_char = current_char
        else:
            # No hand detected → reset
            current_char = ""
            char_confirm_count = 0
            last_confirmed_char = ""  # Allow re-adding same letter after hand disappears

        # --- Draw sentence on screen ---
        # Background bar for sentence display
        cv2.rectangle(display_frame, (0, H - 60), (W, H), (40, 40, 40), -1)
        cv2.putText(display_frame, f"Sentence: {sentence}", (10, H - 20),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.8, (255, 255, 255), 2, cv2.LINE_AA)

        # Confirmation progress bar
        if char_confirm_count > 0 and current_char:
            progress = min(char_confirm_count / CONFIRM_THRESHOLD, 1.0)
            bar_width = int(200 * progress)
            color = (0, 255, 0) if progress >= 1.0 else (0, 200, 255)
            cv2.rectangle(display_frame, (W - 220, 10), (W - 220 + bar_width, 30), color, -1)
            cv2.rectangle(display_frame, (W - 220, 10), (W - 20, 30), (200, 200, 200), 2)
            cv2.putText(display_frame, f"Confirming: {current_char}", (W - 220, 50),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, (200, 200, 200), 1, cv2.LINE_AA)

        # --- Controls info ---
        cv2.putText(display_frame, "SPACE=Speak | BACKSPACE=Delete | C=Clear", (10, 25),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.5, (180, 180, 180), 1, cv2.LINE_AA)

        cv2.imshow('ASL Hand Tracker', display_frame)

        # --- Keyboard controls ---
        key = cv2.waitKey(1) & 0xFF
        if key == ord('q'):
            break
        elif key == ord(' '):  # Space → Speak the sentence
            if sentence.strip():
                speak_text(sentence.strip())
        elif key == 8:  # Backspace → Delete last character
            sentence = sentence[:-1]
        elif key == ord('c') or key == ord('C'):  # C → Clear sentence
            sentence = ""
            last_confirmed_char = ""

cap.release()
cv2.destroyAllWindows()