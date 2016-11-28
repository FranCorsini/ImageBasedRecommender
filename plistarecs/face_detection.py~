import numpy as np
import sys
import requests
try:
    import cv2
except Exception:
    pass

face_cascade = cv2.CascadeClassifier('haarcascade_frontalface_default.xml')
url = sys.argv[1]

def open_image_from_url(url):
        response = requests.get(url)
        img = np.asarray(bytearray(response.content))
        img = cv2.imdecode(img, 0)
        return img

img = open_image_from_url(url)
faces = []
faces = face_cascade.detectMultiScale(img, 1.3, 5)

print len(faces)
sys.exit(len(faces))
