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

def __WalkList(dataList, frameworkInterface,prefix=""):
    for entry in dataList:
        if isinstance(entry,dict):
            __WalkMap(entry,frameworkInterface,prefix)
        else:
            pass #shouldn't get here


def __WalkMap(dataMap, frameworkInterface,prefix=""):
    Logger = frameworkInterface.Logger
    for entryKey in dataMap:
        if isinstance(dataMap[entryKey],dict):
            __WalkMap(dataMap[entryKey],frameworkInterface,prefix + entryKey +".")

        elif isinstance(dataMap[entryKey],list):
            __WalkList(dataMap[entryKey],frameworkInterface,prefix + entryKey +".")

        else:
            ID = prefix + entryKey
            value = dataMap[entryKey]
            if not frameworkInterface.DoesCollectorExist(ID): # Do we already have this ID?
                frameworkInterface.AddCollector(ID)    # Nope, so go add it

            frameworkInterface.SetCollectorValue(ID,value) # update the value with the ID, let the framework handle the interval


def JSON_Network_Collector(frameworkInterface, IP, Port): 
    Logger = frameworkInterface.Logger
    Logger.info("Starting JSON Network Collector:" + IP + " [" + Port + "]")
    try:       

        Port = int(Port)
        recvSocket = socket.socket(socket.AF_INET,socket.SOCK_DGRAM)
        recvSocket.bind((IP,Port))
        recvSocket.setblocking(True)
        recvSocket.settimeout(0.01)
    except Exception as Ex:
        logger.error("Error setting up JSON Network Collector: " + str(Ex))
        return

    while not frameworkInterface.KillThreadSignalled():
        try:
            data, fromAddress = recvSocket.recvfrom(8192)
            data = data.strip().decode("utf-8")
            dataMap = json.loads(data)
            __WalkMap(dataMap,frameworkInterface)

        except socket.error:
            continue

        except Exception as Ex:
            Logger.error("JSON Network Collector catastrophic error: " + str(Ex))


            
