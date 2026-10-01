import os
import pickle

import mediapipe as mp
import cv2

# --- New MediaPipe Tasks API ---
BaseOptions = mp.tasks.BaseOptions
HandLandmarker = mp.tasks.vision.HandLandmarker
HandLandmarkerOptions = mp.tasks.vision.HandLandmarkerOptions
VisionRunningMode = mp.tasks.vision.RunningMode

options = HandLandmarkerOptions(
    base_options=BaseOptions(model_asset_path='hand_landmarker.task'),
    running_mode=VisionRunningMode.IMAGE,
    num_hands=1,
    min_hand_detection_confidence=0.3,
    min_hand_presence_confidence=0.3
)

DATA_DIR = './asl_alphabet_train/asl_alphabet_train'

data = []
labels = []

with HandLandmarker.create_from_options(options) as landmarker:
    for dir_ in sorted(os.listdir(DATA_DIR)):
        dir_path = os.path.join(DATA_DIR, dir_)
        if not os.path.isdir(dir_path):
            continue

        img_files = os.listdir(dir_path)[:400]  # Take 400 images per class for fast processing
        print(f'Processing class: {dir_} ({len(img_files)} images)')

        for idx, img_path in enumerate(img_files):
            data_aux = []
            x_ = []
            y_ = []

            img = cv2.imread(os.path.join(dir_path, img_path))
            if img is None:
                continue

            img_rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
            mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=img_rgb)

            results = landmarker.detect(mp_image)

            if results.hand_landmarks:
                hand_landmarks = results.hand_landmarks[0]

                for lm in hand_landmarks:
                    x_.append(lm.x)
                    y_.append(lm.y)

                min_x, max_x = min(x_), max(x_)
                min_y, max_y = min(y_), max(y_)

                range_x = (max_x - min_x) if (max_x - min_x) > 0 else 1.0
                range_y = (max_y - min_y) if (max_y - min_y) > 0 else 1.0

                # Scale & Shift Normalization (Scale invariant)
                for lm in hand_landmarks:
                    data_aux.append((lm.x - min_x) / range_x)
                    data_aux.append((lm.y - min_y) / range_y)

                data.append(data_aux)
                labels.append(dir_)

        print(f'  Done. Total samples so far: {len(data)}')

print(f'\nTotal samples: {len(data)}')
print(f'Total classes: {len(set(labels))}')

f = open('data.pickle', 'wb')
pickle.dump({'data': data, 'labels': labels}, f)
f.close()
print('Dataset saved to data.pickle')