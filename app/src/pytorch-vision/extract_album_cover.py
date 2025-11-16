import os
import cv2
import numpy as np
from ultralytics import YOLO

# Resolve script path
BASE_DIR = os.path.dirname(os.path.abspath(__file__))

# Load YOLO model trained on synthetic data
MODEL_PATH = os.path.join(BASE_DIR, "yolov8n.pt")
model = YOLO(MODEL_PATH)

def warp_to_square(img, xyxy):
    x1, y1, x2, y2 = map(int, xyxy)
    crop = img[y1:y2, x1:x2]
    crop = cv2.resize(crop, (224, 224))
    return crop

def extract_cover(image_path):
    img = cv2.imread(image_path)
    results = model(img)[0]

    if len(results.boxes) == 0:
        print("No album cover detected.")
        return None

    xyxy = results.boxes[0].xyxy[0].tolist()
    return warp_to_square(img, xyxy)

if __name__ == "__main__":
    TEST_IMG = os.path.join(BASE_DIR, "test2.jpg")
    cover = extract_cover(TEST_IMG)

    if cover is not None:
        out_path = os.path.join(BASE_DIR, "cropped_cover.jpg")
        cv2.imwrite(out_path, cover)
        print("Saved cover to:", out_path)
