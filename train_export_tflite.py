import os
import pickle
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
import tensorflow as tf

print(f"TensorFlow version: {tf.__version__}")

# 1. Load dataset
data_path = './data.pickle'
if not os.path.exists(data_path):
    raise FileNotFoundError("data.pickle not found! Run create_dataset.py first.")

with open(data_path, 'rb') as f:
    data_dict = pickle.load(f)

data = np.asarray(data_dict['data'], dtype=np.float32)
labels = np.asarray(data_dict['labels'])

print(f"Original dataset size: {len(data)} samples, {len(np.unique(labels))} classes")

# Filter low-count classes if any
unique_labels, counts = np.unique(labels, return_counts=True)
valid_classes = unique_labels[counts >= 5]
valid_mask = np.isin(labels, valid_classes)

data = data[valid_mask]
labels = labels[valid_mask]

# 2. Encode Labels
label_encoder = LabelEncoder()
encoded_labels = label_encoder.fit_transform(labels)
num_classes = len(label_encoder.classes_)

print(f"Filtered dataset: {len(data)} samples, {num_classes} active classes:")
print(list(label_encoder.classes_))

# Save labels.txt for Android app assets
output_dir = './android_app/app/src/main/assets'
os.makedirs(output_dir, exist_ok=True)

labels_file = os.path.join(output_dir, 'labels.txt')
with open(labels_file, 'w', encoding='utf-8') as f:
    for cls in label_encoder.classes_:
        f.write(f"{cls}\n")
print(f"Saved classes to {labels_file}")

# 3. Train-Test Split
X_train, X_test, y_train, y_test = train_test_split(
    data, encoded_labels, test_size=0.2, random_state=42, stratify=encoded_labels
)

# 4. Build Keras Model (Lightweight MLP for TFLite)
model = tf.keras.Sequential([
    tf.keras.layers.Input(shape=(42,)),
    tf.keras.layers.Dense(128, activation='relu'),
    tf.keras.layers.BatchNormalization(),
    tf.keras.layers.Dropout(0.2),
    tf.keras.layers.Dense(64, activation='relu'),
    tf.keras.layers.BatchNormalization(),
    tf.keras.layers.Dropout(0.2),
    tf.keras.layers.Dense(num_classes, activation='softmax')
])

model.compile(
    optimizer=tf.keras.optimizers.Adam(learning_rate=0.001),
    loss='sparse_categorical_crossentropy',
    metrics=['accuracy']
)

model.summary()

# 5. Train Model
print("\n--- Training Keras Model ---")
history = model.fit(
    X_train, y_train,
    epochs=50,
    batch_size=32,
    validation_data=(X_test, y_test),
    verbose=1
)

loss, accuracy = model.evaluate(X_test, y_test)
print(f"\nFinal Test Accuracy: {accuracy * 100:.2f}%")

# 6. Convert to TensorFlow Lite (.tflite)
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]  # Dynamic range quantization for speed & micro size
tflite_model = converter.convert()

tflite_file = os.path.join(output_dir, 'gesture_classifier.tflite')
with open(tflite_file, 'wb') as f:
    f.write(tflite_model)

print(f"Successfully converted and saved TFLite model to: {tflite_file}")
print(f"Model File Size: {os.path.getsize(tflite_file) / 1024:.2f} KB")

# Also copy hand_landmarker.task to assets if available
landmarker_src = './hand_landmarker.task'
if os.path.exists(landmarker_src):
    import shutil
    shutil.copy(landmarker_src, os.path.join(output_dir, 'hand_landmarker.task'))
    print(f"Copied hand_landmarker.task to {output_dir}")
