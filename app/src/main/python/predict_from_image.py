import os
import uuid
import json
import sys
import cv2

import extract_album_cover
import predict_album


def identify_album(image_path, x1, y1, x2, y2):
    """
    Takes an image + crop rectangle, extracts that region,
    predicts its album, and returns {album, artist}.

    If x2 and y2 are both 0, use the image's full width and height.
    """

    # Load the image to determine dimensions if needed
    img = cv2.imread(image_path)
    if img is None:
        return {"error": "Image unreadable"}

    # Check if image is completely black
    if img.mean() < 5:  # Average pixel value < 5 (out of 255)
        return {"error": "Image is completely black - insufficient lighting"}

    h, w = img.shape[:2]

    # If x2 and y2 are 0, use image width and height
    if x2 == 0 and y2 == 0:
        x2 = w
        y2 = h

    cover = extract_album_cover.extract_cover(image_path, x1, y1, x2, y2)
    if cover is None:
        return {"error": "Invalid crop or image unreadable"}

    # Save temp cropped image
    temp_filename = f"temp_cover_{uuid.uuid4().hex}.jpg"
    temp_path = os.path.join(os.path.dirname(__file__), temp_filename)

    write_success = cv2.imwrite(temp_path, cover)
    if not write_success:
        return {"error": "Failed to write temporary image file"}

    # Verify file was written and has content
    if not os.path.exists(temp_path):
        return {"error": "Temporary image file was not created"}

    file_size = os.path.getsize(temp_path)
    if file_size == 0:
        return {"error": "Temporary image file is empty"}

    # Verify the written image can be read back
    verify_img = cv2.imread(temp_path)
    if verify_img is None:
        os.remove(temp_path)
        return {"error": "Temporary image file is corrupted"}

    # Predict
    album, artist = predict_album.predict(temp_path, return_values=True)

    # Cleanup temp file
    try:
        os.remove(temp_path)
    except:
        pass

    return {"album": album, "artist": artist}


# CLI usage:
# python predict_from_image.py image.jpg x1 y1 x2 y2
if __name__ == "__main__":

    if len(sys.argv) != 6:
        print(json.dumps({"error": "Usage: predict_from_image.py image x1 y1 x2 y2"}))
        exit(1)

    img_path = sys.argv[1]
    x1 = int(sys.argv[2])
    y1 = int(sys.argv[3])
    x2 = int(sys.argv[4])
    y2 = int(sys.argv[5])

    result = identify_album(img_path, x1, y1, x2, y2)
    print(json.dumps(result))
