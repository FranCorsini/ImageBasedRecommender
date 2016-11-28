import schedule
import time

import os

def main():
    schedule.every(1).hours.do(log_clean)

    while 1:
        schedule.run_pending()
        time.sleep(10)

def log_clean():
    #first cleaning
    file = open('error.log', 'w')
    file.close()
    #file = open('updates_debug.log', 'w')
    #file.close()
    files = os.listdir(os.curdir)
    for f in files:
        #if f.startswith('data.log.'):
        #    os.remove(f)
        if f.startswith('error.log.'):
            os.remove(f)
    print 'cleaning done on: ', time.strftime('%X %x %Z')

    #log top
'''
    with open('top.log','a') as log:   
        log.write(time.strftime('%X %x %Z') + '\n')
        log.write( '-------------------------------------- \n')
        os.system('top -b -n1 >> top.log')
'''        
    


if __name__ == "__main__":
    main()
