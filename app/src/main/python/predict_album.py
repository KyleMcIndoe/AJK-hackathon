import os
import json
import numpy as np
from PIL import Image
from sklearn.preprocessing import normalize
import tflite_runtime.interpreter as tflite


base_path = os.path.dirname(os.path.abspath(__file__))

# -------------------------------------------
# Load embeddings + labels
# -------------------------------------------
embeddings = np.load(os.path.join(base_path, "embeddings.npy"))
with open(os.path.join(base_path, "labels.json"), "r", encoding="utf-8") as f:
    labels = json.load(f)

# -------------------------------------------
# Load TensorFlow Lite model
# -------------------------------------------
tflite_path = os.path.join(base_path, "efficientnetb0_embed.tflite")

interpreter = tflite.Interpreter(model_path=tflite_path)
interpreter.allocate_tensors()

input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()


# -------------------------------------------
# Image preprocessing (PIL version)
# -------------------------------------------
def load_and_preprocess(path):
    # Load with Pillow
    img = Image.open(path).convert("RGB")
    img = img.resize((224, 224), Image.BILINEAR)

    # Convert to float32 numpy array
    arr = np.array(img, dtype=np.float32)

    # EfficientNet-like normalization (same as tf.keras preprocessing)
    arr = arr / 127.5 - 1.0     # Match tf.keras.applications.efficientnet.preprocess_input

    # Add batch dimension
    return np.expand_dims(arr, axis=0)


# -------------------------------------------
# Helper to run the model
# -------------------------------------------
def get_embedding(path):
    arr = load_and_preprocess(path)

    interpreter.set_tensor(input_details[0]["index"], arr)
    interpreter.invoke()

    vec = interpreter.get_tensor(output_details[0]["index"])[0]

    return normalize(vec.reshape(1, -1))[0]


def parse_label(label):
    album, artist = label.split("---")
    album = album.replace("_", " ").title()
    artist = artist.replace("_", " ").title()
    return album, artist


# -------------------------------------------
# Prediction function
# -------------------------------------------
def predict(img_path, return_values=False):
    query_vec = get_embedding(img_path)

    scores = np.dot(embeddings, query_vec)
    idx = np.argmax(scores)
    best_score = scores[idx]

    album, artist = parse_label(labels[idx])

    if return_values:
        return album, artist

    print("🎵 Prediction:")
    print("Album :", album)
    print("Artist:", artist)
    print(f"Similarity: {best_score:.4f}")


if __name__ == "__main__":
    test_img = os.path.join(base_path, "test2.jpg")
    predict(test_img)
