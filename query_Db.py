import pymongo
import json
from pymongo import MongoClient

img_url_fake = ['http://www.ksta.de',' ','','\t']

def connectToMongoDB():
    client = MongoClient()
    db = client.daily_dump
    collection = db.items
    return collection
real = 0
fake = 0

collection = connectToMongoDB()
with open('real_image_dump.log','w') as image_output:
    with open('fake_image_dump.log','w') as fake_output:
        
        total = collection.count()
        cursor = collection.find({"img":{"$exists": True}})


        for elem in cursor:
            if str(elem["img"]) not in img_url_fake and (str(elem["img"]).find('jpg') != -1 or str(elem["img"]).find('JPG') != -1 or str(elem["img"]).find('jpeg') != -1 or str(elem["img"]).find('png') or str(elem["img"]).find('imagix')) :
                image_output.write(str(elem["img"]) + '\n')
                real = real + 1
            else:
                fake_output.write(str(elem["img"]) + '\n')
                fake = fake + 1

no = total - (real + fake)
print 'images: ', real
print 'fake images: ', fake
print 'no images: ', no