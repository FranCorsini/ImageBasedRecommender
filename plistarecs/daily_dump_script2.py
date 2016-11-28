import pymongo
import json
import re
import os
import schedule
import time
import traceback
import shutil
from pprint import pprint
from pymongo import MongoClient



img_url_fake = ['http://www.ksta.de', ' ', '']


def main():
	schedule.every(1).hours.do(daily_dump)



	while 1:
	    schedule.run_pending()
	    time.sleep(10)



def daily_dump():
    global img_url_fake
    n_articles = 0
    n_images = 0
    n_clicks = 0
    article_no_image = []
    collection = connectToMongoDB()
    failedJSON = 0
    successfull = False
    names = []

    for i in os.listdir(os.getcwd()):
        if i.startswith("data.log."): 
            try:
                with open(i) as data_file:   
                    names.append(i)
                    with open('results/daily_dump.log','a') as output: #to be changed
                        for line in data_file:
                            try:
                                if line.find('MESSAGE	item_update') != -1:
                                    n_articles = n_articles + 1
                                    newstring = line.strip('MESSAGE	item_update')
                                    newstring = newstring.rsplit("}",1)[0] + '}'
                                    data = json.loads(newstring)
                                    try:
                                        updateItemInMongo(collection, data)
                                    except Exception as e:
                                        print 'error updating database'
                                        print e
                                    img = data["img"]
                                    if img and img not in img_url_fake:
                                        n_images = n_images +1
                                    else:
                                        article_no_image.append('URL:' + data["url"] + '\n' + 'IMG:' + data["img"] + '\n')
                                elif line.find('MESSAGE	event_notification	{\"type\":\"click\"') != -1:
                                    n_clicks = n_clicks + 1
                                    newstring = line.strip('MESSAGE	event_notification')
                                    newstring = newstring.rsplit("}",1)[0] + '}'
                                    data = json.loads(newstring)
                                    try:
                                        updateClickInMongo(collection, data)
                                    except Exception as e:
                                        print 'error updating database'
                                        print e
                            except Exception as e:
                                failedJSON + failedJSON + 1
                        
                        str_out = 'task completed on ' + time.strftime("%d/%m/%Y") + ' at ' + time.strftime("%H:%M:%S") + ' \n'
                        output.write(str_out)
                        str_out = 'number of articles updates: ' + str(n_articles) + '\n'
                        output.write(str_out)
                        str_out = 'number of images in updates: ' + str(n_images) + '\n'
                        output.write(str_out)
                        str_out = 'number of clicks: : ' + str(n_clicks) + '\n'
                        output.write(str_out)
                        str_out = 'number of failed loads: : ' + str(failedJSON) + '\n'
                        output.write(str_out)
                        #output.write('Links to image missing:\n')
                        #for img_url in article_no_image:
                        #    output.write(img_url)
                        output.write('**************************************\n')
                        successfull = True
                #file = open('data.log', 'w')
                #file.close()
            except Exception as e:
                print 'cannot open file at ', time.strftime('%X %x %Z')
                print e
                traceback.print_exc()

            
    if successfull:
        print 'Done at ', time.strftime('%X %x %Z'), 'for files: ', names 
	for i in os.listdir(os.getcwd()):
            if i in names:
		os.remove(i)
    
def copyFile(src, dest):
    try:
        shutil.copy(src, dest)
    # eg. src and dest are the same file
    except shutil.Error as e:
        print('Error: %s' % e)
    # eg. source or destination doesn't exist
    except IOError as e:
        print('Error: %s' % e.strerror)


def connectToMongoDB(): #to be changed
    client = MongoClient()
    db = client.plistaDB #to be changed
    collection = db.items
    return collection

def updateItemInMongo(collection, item):
    #if article already exist
    
    try:
        cur = collection.find({"domainid":item["domainid"], "id":item["id"]},{"clicks":1, "_id":0})
        if cur:
            for elem in cur:
                clicks = elem["clicks"]
        collection.update({"domainid":item["domainid"], "id":item["id"]},item, upsert=True)
        if clicks:
            collection.find_and_modify(query={"domainid": item["domainid"],"id":item["id"]}, update={"$set": {"clicks": clicks}}, full_response=True)
    #if it doesn't exist
    except Exception as e:
        collection.update({"domainid":item["domainid"], "id":item["id"]},item, upsert=True)
        #print "exception 1"
        #print e
    

def updateClickInMongo(collection, click):
    
    try:
        collection.find_and_modify(query={"domainid": click["context"]["simple"]["27"],"id":click["context"]["simple"]["25"]}, update={"$inc": {"clicks": 1}}, upsert=True, full_response=True)
    except Exception as e:
        pass
        #print "exception, not updated"
        #print e
     


if __name__ == "__main__":
    main()
    
    
