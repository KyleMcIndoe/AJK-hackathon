import os
import uuid
import json

# Import your existing modules
import extract_album_cover
import predict_main


def identify_album(image_path):
    """
    Takes a full image, extracts album cover, runs prediction,
    and returns { "album": ..., "artist": ... }.
    """

    # 1. Extract cropped cover
    cover = extract_album_cover.extract_cover(image_path)
    if cover is None:
        return {"error": "No album cover detected"}

    # 2. Save temp cropped image (Kotlin may call repeatedly → use uuid)
    temp_filename = f"temp_cover_{uuid.uuid4().hex}.jpg"
    temp_path = os.path.join(os.path.dirname(__file__), temp_filename)

    import cv2
    cv2.imwrite(temp_path, cover)

    # 3. Predict the album + artist using your existing predict() logic
    album, artist = predict_main.predict(temp_path, return_values=True)

    # Remove temp file
    try:
        os.remove(temp_path)
    except:
        pass

    # 4. Return structured result
    return {"album": album, "artist": artist}


# Allow Kotlin to call via CLI: python predict_from_image.py /path/to/image.jpg
if __name__ == "__main__":
    import sys

    if len(sys.argv) < 2:
        print(json.dumps({"error": "No input image provided"}))
        exit(1)

    img_path = sys.argv[1]

    result = identify_album(img_path)

    # Kotlin will read from stdout → print JSON
    print(json.dumps(result))
