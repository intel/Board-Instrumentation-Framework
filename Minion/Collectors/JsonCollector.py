##############################################################################
#  Copyright (c) 2018 Intel Corporation
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
##############################################################################

import socket
import json
from pprint import pprint as pprint
Logger = None

__averageMap={}

def __WalkList(dataList, frameworkInterface,averageFlag,prefix=""):
    for entry in dataList:
        if isinstance(entry,dict):
            __WalkMap(entry,frameworkInterface,averageFlag,prefix)
        else:
            pass #shouldn't get here

def __WalkMap(dataMap, frameworkInterface,averageFlag,prefix=""):
    #pylint: disable=unused-variable
    Logger = frameworkInterface.Logger
    global __averageMap
    for entryKey in dataMap:
        if isinstance(dataMap[entryKey],dict):
            __WalkMap(dataMap[entryKey],frameworkInterface,averageFlag,prefix + entryKey + ".")

        elif isinstance(dataMap[entryKey],list):
            __WalkList(dataMap[entryKey],frameworkInterface,averageFlag,prefix + entryKey + ".")

        else:
            ID = prefix + entryKey
            value = dataMap[entryKey]
            if not frameworkInterface.DoesCollectorExist(ID): # Do we already have this ID?
                frameworkInterface.AddCollector(ID)    # Nope, so go add it
                if True == averageFlag:
                    try:
                        numberVal = float(value)
                        __averageMap[ID]=[]
                        __averageMap[ID].append(numberVal)

                    except Exception as Ex:
                        print(ID)					     
                        pass

            if True == averageFlag and ID in __averageMap:
                if len(__averageMap[ID]) > 10:
                    __averageMap[ID].pop(0)

                __averageMap[ID].append(float(value))
                avgVal = sum(__averageMap[ID]) / len(__averageMap[ID])
                frameworkInterface.SetCollectorValue(ID,str(avgVal))

            else:
                frameworkInterface.SetCollectorValue(ID,value) # update the value with the ID, let the framework handle the interval

def kwtest(frameworkInterface,arg1,arg2,**kwargs):
    print(arg1)
    print(arg2)
    print(kwargs)

def JSON_Network_Collector(frameworkInterface, IP, Port, averageDataStr): 
    Logger = frameworkInterface.Logger

    averageDataStr = str(averageDataStr)
    if averageDataStr.upper() == "TRUE":
        averageData = True
    else:
        averageData = False

    Logger.info("Starting JSON Network Collector:" + IP + " [" + Port + "] Averaging data: " + str(averageData))
    try:       
        Port = int(Port)
        recvSocket = socket.socket(socket.AF_INET,socket.SOCK_DGRAM)
        recvSocket.bind((IP,Port))
        recvSocket.setblocking(True)
        recvSocket.settimeout(0.01)
    except Exception as Ex:
        Logger.error("Error setting up JSON Network Collector: " + str(Ex))
        return

    while not frameworkInterface.KillThreadSignalled():
        try:
            #pylint: disable=unused-variable
            data, fromAddress = recvSocket.recvfrom(8192)
            data = data.strip().decode("utf-8")
            dataMap = json.loads(data)
            __WalkMap(dataMap,frameworkInterface,averageData)

        except socket.error:
            continue

        except Exception as Ex:
            Logger.error("JSON Network Collector catastrophic error: " + str(Ex))


            
