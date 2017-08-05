##############################################################################
#  Copyright (c) 2016 Intel Corporation
# 
# Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
##############################################################################
#    File Abstract: 
#
#    Takes a file that is the output of ehttool Stats (like ethtool -S eth0 > file.txt)    
#    spitting data out to a new file with data=value on each line
#    It will also work with ethtool eth0 > file.txt to get things like link state and such
#    you can even do ethtool eth0 > file.txt  ethtool eth0 >> file.txt to create a big file
#    for parsing
##############################################################################
import os
import ntpath

#Makes path (if passed) os independent
def convertPath(path):
    separator = os.path.sep

    if separator == '/':
        path = path.replace('\\',os.path.sep)

    elif separator == '\\':
        path = path.replace('/',os.path.sep)

    return path

# worker routine to wait for a lock file to not be there
def __WaitForLock(LockFileName):
    iCount = 0
    while True == os.path.isfile(LockFileName) :
        iCount+=1
        Sleep.SleepMs(10)
        if iCount > 100 : # if lock lasts too long, just return something TODO - maybe log something?
            return False
            
    try :
        lockFile = os.open(LockFileName,os.O_CREAT|os.O_EXCL|os.O_RDWR)
        os.write(lockFile,"lock".encode('utf-8'))
        os.close(lockFile)
        return True

    except Exception as ex:
        #give it one more try
       # iCount=0
        while True == os.path.isfile(LockFileName) :
            iCount+=1
            Sleep.SleepMs(10)
            if iCount > 100 : # if lock lasts too long, just return something TODO - maybe log something?
                return False

        try :
            lockFile = os.open(LockFileName,os.O_CREAT|os.O_EXCL|os.O_RDWR)
            os.write(lockFile,"lock".encode('utf-8'))
            os.close(lockFile)

            return True
                
        except Exception as ex:
            #print("Bottom of routine: " + str(ex))
            pass

    return False

#gets a specific item from the file, such as rx_packets, or Advertised link
#modes
def GetItem(inpfile,item): 
    Filename = convertPath(inpfile)
    file = open(Filename,'rt')
    if None == file:
        return "File [" + Filename + "] does not exist"

    _found = False
    retStr = "not found"
    for line in file:
        line = line.strip() # get rid of leading/trailing stuff
        parsedItems = line.split(":")
        #print (parsedItems)
        if len(parsedItems) == 2:
            if True == _found: #is not multi line
                break
            if parsedItems[0] == item: # is case sensitive!
                _found = True
                retStr = parsedItems[1].strip()


        elif _found: # is a mulit-line value, so concat
            line = line.strip()
            if len(line) > 0:
                retStr += " " + line.strip()
            else:
                break

    return retStr


# parses everything
def GenerateFileAll(inpfile,outputfile,LockFileName=None): 
    Filename = convertPath(inpfile)
    if None != LockFileName:
        LockFileName = convertPath(LockFileName)
        lockFile = __WaitForLock(LockFileName)

        if False == lockFile :
            return "HelenKeller" # don't want to send anything

    file = open(Filename,'rt')

    if None == file:
        return "File [" + Filename + "] does not exist"

    strData = file.readlines()
    file.close()
    if None != LockFileName:
        try :
            os.remove(LockFileName)

        except Exception as ex:
            return "HelenKeller" # don't want to send anything

    _found = False
    retStr = ""
    for line in strData:
        line = line.strip() # get rid of leading/trailing stuff
        parsedItems = line.split(":")
        
        if len(parsedItems) == 2:
            if True == _found: #is not multi line
                retStr += os.linesep
                _found = False

            if len(parsedItems[1]) < 1:
                if _found:
                    _found = False

                continue # likely a header, not wanted.

            _found = True
            retStr += parsedItems[0] + "=" + parsedItems[1].strip()


        elif _found: # is a mulit-line value, so concat
            line = line.strip()
            if len(line) > 0:
                retStr += " " + line.strip()
            else:
                retStr += os.linesep
                _found = False


    file = open(outputfile,"wt")
    file.write(retStr)
    file.close()
    return "HelenKeller" # don't want to send anything

#gets just the queues
def GenerateFileQueues(inpfile,outputfile): 
    Filename = convertPath(inpfile)
    file = open(Filename,'rt')
    if None == file:
        return "File [" + Filename + "] does not exist"

    _found = False
    dataStr = ""
    count = 0
    itemsPerQueue = 0

    for line in file:
        line = line.strip() # get rid of leading/trailing stuff
        parsedItems = line.split(":")
        #print (parsedItems)
        if len(parsedItems) == 2:
            if len(parsedItems[1]) < 1:
                continue

            if len(parsedItems[0]) > 4:
                if "x-" in parsedItems[0]:
                    dataStr += parsedItems[0] + "=" + parsedItems[1].strip() + os.linesep
                    _found = True
                    if "-0." in parsedItems[0]:
                        itemsPerQueue += 1
                    count += 1

    if not _found:
        dataStr = "Not Found"
    else:
        dataStr+="QueueCount=" + str((int)(count / itemsPerQueue))

    file = open(outputfile,"wt")
    file.write(dataStr)
    file.close()
    return "HelenKeller" # don't want to send anything

## Routine takes input from a Ethtool -S piped to a file and creates a map/dictionary of the data and value
def __CreateMapFromFile(inpfile):
    inpfile = convertPath(inpfile)
    file = open(inpfile,'rt')

    if None == file:
        print("File [" + inpfile + "] does not exist")
        return None

    dataset={}
    strData = file.readlines()
    file.close()

    _found = False
    retStr = ""
    for line in strData:
        line = line.strip() # get rid of leading/trailing stuff
        parsedItems = line.split(":")
        
        if len(parsedItems) == 2:
            if True == _found: #is not multi line
                retStr += os.linesep
                _found = False

            if len(parsedItems[1]) < 1:
                if _found:
                    _found = False

                continue # likely a header, not wanted.

            _found = True
            item = parsedItems[0]
            data = parsedItems[1].strip()
            dataset[item] = data

    return dataset


def GetFileTimestampMS(strFile):
    try:
        modTime = int(ntpath.getmtime(strFile)*1000) # Convert last modified time in ns to ms
        return modTime 

    except Exception as ex:
        return 0

# This function is designed to take 2 input files that are the ethtool -S data (ethtool -S eth0 > file1.txt)
# it takes the values in the 2nd file, writes it to a DyanamicCollector file format and then it also writes
# the normalized per/second value of the data differences between the 2 files.  It uses the file timestamp
# for the 2 files to determine the normalization period. 
def GenerateDataFromFiles(inpfile1,inpfile2,outputfile,LockFileName=None): 
    startMap = __CreateMapFromFile(inpfile1)
    if None == startMap:
        return "Unable to read " + inpfile1
    endMap = __CreateMapFromFile(inpfile2)

    timeDeltaMS = GetFileTimestampMS(inpfile2) - GetFileTimestampMS(inpfile1)

    if None == endMap:
        return "Unable to read " + inpfile2

    for item in startMap.keys():
        try:
            valueDelta = int(endMap[item]) - int(startMap[item])
            if valueDelta != 0:
                perSecVal = int(float(float(valueDelta) / float(float(timeDeltaMS) / 1000.0)))
            else:
                perSecVal = 0

            newKey = item +".perSecond"
            endMap[newKey] = str(perSecVal)

        except Exception as ex:
            pass

    strReadyData = ""
    for item in endMap.keys():
        entry = item+"="+endMap[item]
        strReadyData += entry + os.linesep

    outputfile = convertPath(outputfile)
    if None != LockFileName:
        LockFileName = convertPath(LockFileName)
        lockFile = __WaitForLock(LockFileName)

        if False == lockFile :
            return "HelenKeller" # don't want to send anything

    file = open(outputfile,"wt")
    file.write(strReadyData)
    file.close()
    if None != LockFileName:
        try :
            os.remove(LockFileName)

        except Exception as ex:
            print(str(ex))
            return None

    return "HelenKeller" # don't want to send anything






#print (GetItem("ethtool_results_p7p1.txt","rx_multicast"))
#print (GetItem("ethtool_results_p7p1.txt","rx_packets"))
#print (GetItem("ethtool_results_p7p1.txt","rx_packets3"))
#GetItem("ethtool_results_p7p1.txt","Supported link modes")
#GenerateFileQueues("ethtool_results_p7p1.txt","ethoutq.txt")
#GenerateFileAll("ethtool_results_p7p1.txt","ethout.txt")