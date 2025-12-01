import cv2
import os
import numpy as np

BASE_DIR = os.path.dirname(os.path.abspath(__file__))

def extract_cover(img_path, x1, y1, x2, y2):
    """
    Crop an image using exact coordinates:
    (x1, y1) = upper-left
    (x2, y2) = lower-right
    """

    img = cv2.imread(img_path)
    if img is None:
        return None

    h, w = img.shape[:2]

    # Clamp bounds
    x1 = max(0, min(x1, w - 1))
    y1 = max(0, min(y1, h - 1))
    x2 = max(0, min(x2, w))
    y2 = max(0, min(y2, h))

    if x2 <= x1 or y2 <= y1:
        return None

    crop = img[y1:y2, x1:x2]

    # Validate crop isn't empty
    if crop.size == 0:
        return None

    # Validate crop isn't completely black
    if crop.mean() < 5:
        return None

    return crop


if __name__ == "__main__":
    pass



"""
import cv2
import numpy as np
import os
# Resolve script path

BASE_DIR = os.path.dirname(os.path.abspath(__file__))


def extract_cover(img_path):
    img = cv2.imread(img_path)
    if img is None:
        return None

    h, w = img.shape[:2]

    # Size of square (tweakable: 0.88–0.95 depending on your shots)
    side = int(min(h, w) * 0.92)

    # Base center
    cx = w // 2
    cy = h // 2

    # SHIFT UPWARD by ~15% of the image height
    cy = int(cy - h * 0.15)

    # Compute crop
    x1 = cx - side // 2
    y1 = cy - side // 2
    x2 = x1 + side
    y2 = y1 + side

    # Clamp boundaries
    x1 = max(0, x1)
    y1 = max(0, y1)
    x2 = min(w, x2)
    y2 = min(h, y2)

    crop = img[y1:y2, x1:x2]

    return crop


if __name__ == "__main__":
    TEST_IMG = os.path.join(BASE_DIR, "test3.jpg")
    cover = extract_cover(TEST_IMG)

    if cover is not None:
        out_path = os.path.join(BASE_DIR, "cropped_cover.jpg")
        cv2.imwrite(out_path, cover)
        print("Saved cover to:", out_path)
"""