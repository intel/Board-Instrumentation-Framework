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
#    Canned Collector to get a string from a file and send the contents
#
##############################################################################

import os.path
import os
import time
import re

from Util import Sleep

#Makes path (if passed) os independent
def convertPath(path):
    separator = os.path.sep

    if separator == '/':
        path = path.replace('\\',os.path.sep)

    elif separator =='\\':
        path = path.replace('/',os.path.sep)

    return path


def WriteToFile(Filename,DataValue):
    Filename = convertPath(Filename)
    file = open(Filename,'wt')

    file.write(DataValue)
    file.close()

#Opens the specified file
def ReadFromFile(Filename):
    Filename = convertPath(Filename)
    file = open(Filename,'rt')
    if None == file:
        return "File [" + Filename + "] does not exist"

    return file.read()

# reads from a file, but uses the lockfilename as a semaphore
# this way you could have a separate worker app updating a file
# as needed, using the same semaphore file, while collector
# is reading the contents of the same file
def ReadFromFileWithLock(Filename, LockFileName) :
    Filename = convertPath(Filename)
    LockFileName = convertPath(LockFileName)
    lockFile = __WaitForLock(LockFileName)
    if False == lockFile :
        return "0"

    strRet = ReadFromFile(Filename)

    
    try :
        os.remove(LockFileName)

    except Exception as ex:
        return "0"

    return strRet


# reads from a file and returns a matched string pattern.  For
# example you have a process that pipes Ethtool info to a file:
#     rx_bytes: 1124739465
#     rx_error_bytes: 0
#     tx_bytes: 12467736
#     tx_error_bytes: 0
#     rx_ucast_packets: 198836
#     rx_mcast_packets: 686
#     rx_bcast_packets: 10546993
#     tx_ucast_packets: 130518
#
# To get tx_bytes your Collector looks like:
#    <Collector ID="ParseTest" Frequency="250">
#      <Executable>Collectors\FileCollector.py</Executable>
#      <Param>ParseFile</Param>
#      <Param>Demonstration\ethtool.txt</Param>
#      <Param>tx_bytes</Param>
#      <Param>: </Param>
#      <Param>1</Param>
#    </Collector>
#
# which passes the filename, this function to call, search for tx_bytes
# pattern,
# then break into strings based on tokens of : and a space, and return the 2nd
# string (index starts at 0)
def ParseFile(Filename,Pattern,Tokens,Index):
    Filename = convertPath(Filename)
    try:
        file = open(Filename,'rt')
    except Exception:
        return "File [" + Filename + "] does not exist"

    try:
        lines = file.readlines()
    except Exception as Ex:
        return "File [" + Filename + "] " + str(ex)

    file.close()

    return __ParseFile(lines,Pattern,Tokens,Index)
    

# Same as ParseFile, but with a lock file
def ParseFileWithLock(Filename,LockFileName,Pattern,Tokens,Index):
    Filename = convertPath(Filename)
    LockFileName = convertPath(LockFileName)
    lockFile = __WaitForLock(LockFileName)
    if False == lockFile:
        return "0"

    strRet = ParseFile(Filename,Pattern,Tokens,Index)
    
    try :
        os.remove(LockFileName)

    except Exception as ex:
        print(str(ex))
        return "0"

    return strRet

#real worker for parsing a file
def __ParseFile(lines,Pattern,Tokens,Index):
    Index = int(Index) #ensure it's an int and not a string
    try:
        for line in lines:
            if Pattern in line: #look for the desired pattern
                strRet = line.split(Tokens) #use the tokens to break line up into an array of separate strings
                return strRet[Index].lstrip().rstrip()

    except Exception as ex:
        pass

    return "Error parsing file in File Collector"

def get_tmp_file():
    filename='tmp_%s_%s'%(os.getpid(),time.time())
    open(filename,"wt").close()
    return filename

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


            
