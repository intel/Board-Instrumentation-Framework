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
#    Takes a file that is the output of ipmitool and gets a specific item, or    
#    create a new file for the DynamicCollector to use with the other function: GenerateFile
#
##############################################################################

import os
import subprocess
import time
import traceback

#Makes path (if passed) os independent
def convertPath(path):
    separator = os.path.sep

    if separator == '/':
        path = path.replace('\\',os.path.sep)

    elif separator == '\\':
        path = path.replace('/',os.path.sep)

    return path

#gets a specific item from the file, such as rx_packets, or Advertised link
#modes
def GetItem(inpfile,item,instance): 
    Filename = convertPath(inpfile)
    file = open(Filename,'rt')
    if None == file:
        return "File [" + Filename + "] does not exist"

    try:
        instance = int(instance)
    except:
        return "Invalid instance: " + instance + " for IPMItoolParser()"

    retStr = "not found"

    currInst = 0

    for line in file:
        line = line.strip() # get rid of leading/trailing stuff
        parsedItems = line.split("|")
        
        if len(parsedItems) >= 2:
            device = parsedItems[0].strip()
            if device == item: # is case sensitive!
                if currInst == instance:
                    retStr = parsedItems[1].strip()
                    break
                currInst += 1

    return retStr
#Generates a new file for you, if you specify True or "True" for
# showOnlyValid, then it will ignore anything with value of 0x00
# or "Not Readable.  Otherwise it will put everything int the
# specified file
def GenerateFile(inpfile,outputfile,showOnlyValid): 
    Filename = convertPath(inpfile)
    file = open(Filename,'rt')
    if None == file:
        return "File [" + Filename + "] does not exist"

    if str(showOnlyValid).upper() == "TRUE":
        showOnlyValid = True
    elif str(showOnlyValid).upper() == "FALSE":
        showOnlyValid = False
    else:
        return "showOnlyValid is invalid parameter: " + str(showOnlyValid)

    dataStr = ""
    items = {} # generate a hashmap of data points, because there are many that have same
               # name but diff values
    for line in file:
        line = line.strip() # get rid of leading/trailing stuff
        parsedItems = line.split("|")
        
        if len(parsedItems) >= 2:
            device = parsedItems[0].strip()
            value = parsedItems[1].strip()

            if showOnlyValid and value == 'Not Readable':  # skip if not good
                continue

            if showOnlyValid and value == '0x00': #skip if not good
                continue

            if not device in items: # 1st time for this data name
                items[device] = [] # create a new array

            items[device].append(value)
            

    if len(items) > 1:
        for key in items:
            list = items[key]
            if 1 == len(list):
                dataStr += key + "=" + list[0] + os.linesep
            else:
                entryNumber = 0
                for entry in list:
                    dataStr += key + "." + str(entryNumber) + "=" + list[entryNumber] + os.linesep
                    entryNumber+=1

    file = open(outputfile,"wt")
    file.write(dataStr)
    file.close()
    return "HelenKeller" # don't want to send anything

def __readIPMI_Data(prefix=""):
    ## this will throw an exception if ipmitool not installed - I catch it elsewhere now
    data = subprocess.check_output(['ipmitool','sdr']).splitlines()

    items = {}
    dataStr = ""
    for line in data:
        line = line.decode('utf-8')
        line = line.strip() # get rid of leading/trailing stuff

        parsedItems = line.split("|")
        
        if len(parsedItems) >= 2:
            device = prefix + parsedItems[0].strip().replace(' ','_')
            value = parsedItems[1].strip()

            if value == 'Not Readable':  # skip if not good
                continue

            if value == '0x00': #skip if not good
                continue
            
            parts = value.split()
            if len(parts) > 1:  # strip off any words like 'celcius' 
                value = parts[0]

            items[device] = value

    if len(items) > 1:
        for key in items:
            list = items[key]
            if 1 == len(list):
                dataStr += key + "=" + list[0] + os.linesep
            else:
                entryNumber = 0
                for entry in list:
                    dataStr += key + "." + str(entryNumber) + "=" + list[entryNumber] + os.linesep
                    entryNumber+=1

    return items

def GetInitialInfoForSetup():
    return __readIPMI_Data()

def CollectIPMI_Info(frameworkInterface):
    global Logger
    Logger = frameworkInterface.Logger
    Logger.info("Starting IPMI Collector")
    try:
        while not frameworkInterface.KillThreadSignalled():
            dataMap = __readIPMI_Data('ipmi.')
            for entry in dataMap:
                if not frameworkInterface.DoesCollectorExist(entry): # Do we already have this ID?
                    frameworkInterface.AddCollector(entry)    # Nope, so go add it
                        
                frameworkInterface.SetCollectorValue(entry,dataMap[entry]) 
            
            time.sleep(float(frameworkInterface.Interval)/1000.0)

    except Exception as ex:
        Logger.error("Unrecoverable error in CollectIPMI_Info Collector plugin: " + str(ex))
        for line in traceback.format_stack():
            Logger.error(line.strip())


#print(GetItem("ipmitool_results.txt","VCORE PG",0))
#print(GetItem("ipmitool_results.txt","VCORE PG",1))
#GenerateFile("ipmitool_results.txt","ipmiout.txt","True")

