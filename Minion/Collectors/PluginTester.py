##############################################################################
#  Copyright (c) 2017 Intel Corporation
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
#       Example plugin collector
##############################################################################
import random
import time

__Instance=0

def CreateList(numberToCreate):
    global __Instance
    __Instance+=1
    thisInst = __Instance
    retMap={}
    for index in range(1,numberToCreate+1):
        idName="PluginTestData."+str(thisInst)+"-" + str(index)
        val = random.randrange(int(1),int(5000))
        retMap[idName] = str(val)

    return retMap

def UpdateList(dataMap):
    updatedCount=0    
    for key in dataMap:
        val = random.randrange(int(0),int(100))
        if val > 95: # only update 5% of the time
            val = random.randrange(int(1),int(5000))
            dataMap[key] = str(val)
            updatedCount += 1

    return updatedCount

## Entry Point ##
def CollectFunction(frameworkInterface,numOfItems): 
    global Logger
    Logger = frameworkInterface.Logger
    Logger.info("Starting Plugin TesterCollector ")
    try:
        numOfItems = int(numOfItems)
    except:
        Logger.error("Invalid Num of Items: " + numOfItems)
        return

    sleepTime = float(frameworkInterface.Interval)
    dataMap=CreateList(numOfItems)

    for collectorID in sorted(dataMap):
        frameworkInterface.AddCollector(collectorID)    
        frameworkInterface.SetCollectorValue(collectorID,dataMap[collectorID]) 

    while not frameworkInterface.KillThreadSignalled():
        updatedCount = UpdateList(dataMap)
        if updatedCount > 0:
            for collectorID in dataMap:
                frameworkInterface.SetCollectorValue(collectorID,dataMap[collectorID]) 

        time.sleep(sleepTime/1000)


