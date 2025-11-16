import os
import json
import numpy as np
import tensorflow as tf
from sklearn.preprocessing import normalize
from tensorflow.keras.preprocessing.image import load_img, img_to_array

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

interpreter = tf.lite.Interpreter(model_path=tflite_path)
interpreter.allocate_tensors()

input_details = interpreter.get_input_details()
output_details = interpreter.get_output_details()


# -------------------------------------------
# Helper functions
# -------------------------------------------
def parse_label(label):
    album, artist = label.split("---")
    album = album.replace("_", " ").title()
    artist = artist.replace("_", " ").title()
    return album, artist


def get_embedding(path):
    # Load + preprocess
    img = load_img(path, target_size=(224, 224))
    arr = img_to_array(img)
    arr = tf.keras.applications.efficientnet.preprocess_input(arr)
    arr = np.expand_dims(arr, 0).astype(np.float32)

    # Run TFLite inference
    interpreter.set_tensor(input_details[0]["index"], arr)
    interpreter.invoke()
    vec = interpreter.get_tensor(output_details[0]["index"])[0]

    # Normalize like original code
    return normalize(vec.reshape(1, -1))[0]


def predict(img_path, return_values=False):
    query_vec = get_embedding(img_path)

    scores = np.dot(embeddings, query_vec)
    idx = np.argmax(scores)
    best_score = scores[idx]
    label = labels[idx]

    album, artist = parse_label(label)

    if return_values:
        return album, artist

    print("🎵 Prediction:")
    print("Album :", album)
    print("Artist:", artist)
    print(f"Similarity: {best_score:.4f}")


if __name__ == "__main__":
    test_img = os.path.join(base_path, "test2.jpg")
    predict(test_img)
