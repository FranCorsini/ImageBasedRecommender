import numpy as np
import sys
import requests
try:
    import cv2
except Exception:
    pass
import sal_thr
#from PIL import Image
import numpy
from scipy import ndimage

face_cascade = cv2.CascadeClassifier('haarcascade_frontalface_default.xml')
url = sys.argv[1]

def open_image_from_url(url):
        response = requests.get(url)
        img = np.asarray(bytearray(response.content))
        img = cv2.imdecode(img, 0)
        return img


def extractSaliencyFeatures(det):
	#img = Image.fromarray(det)
	#img.show()
	min_size = (det.shape[0] * det.shape[1]) / 80 #mininum size of salient object = 2% of the image
	det = cv2.morphologyEx(det, cv2.MORPH_CLOSE,numpy.ones((2,2),numpy.uint8))
	blobs, number_of_blobs = ndimage.label(det)
	slices = ndimage.find_objects(blobs)
	count = 0	
	for elem in slices:
		if numpy.count_nonzero(blobs[elem]) > min_size:
			count = count + 1;
		else:
			blobs[elem] = blobs[elem] * 0
			det[elem] = det[elem] * 0
	foreground = numpy.count_nonzero(det)
	background = det.shape[0] * det.shape[1] - foreground
	ratio = float(foreground) / float(background)
	cmass = ndimage.measurements.center_of_mass(det, blobs,numpy.unique(blobs)[1:])

	return [ratio,count,cmass]


#get and resize image
img = open_image_from_url(url)
height, width = img.shape[:2]
while height > 150 and width > 250:
	height = height / 2
	width = width / 2
img = cv2.resize(img, (width, height)) 

#get faces
faces = []
faces = face_cascade.detectMultiScale(img, 1.3, 5)

#get saliency values
obj = sal_thr.Saliency(img)
det = obj.get_proto_objects_map()
ratio,count,cmass = extractSaliencyFeatures(det)

#classify saliency into a yes/no
sal = 0
if count < 3:
	if ratio > 0.01:
		temp = 1
		lim_width = width /8
		lim_height = height / 8
		for elem in cmass:
			if elem[0] < lim_width or elem[0] > (width - lim_width):
				temp = 0
			if elem[1] < lim_height or elem[1] > (height - lim_height):
				temp = 0
		if temp == 1:
			sal = 1





print len(faces)
print sal
sys.exit(len(faces))