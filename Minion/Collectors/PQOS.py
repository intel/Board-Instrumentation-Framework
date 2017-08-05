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
#    Setup for a DynamicCollector plugin for the Intel Platform QOS utility    
##############################################################################

################## Example #####################
#		<DynamicCollector SendOnlyOnChange="True">
#			<Precision>2</Precision>
#			<Plugin>
#				<PythonFile>Collectors/PQOS.py</PythonFile>
#				<EntryPoint SpawnThread="True">PQOS_Collector</EntryPoint>
#			</Plugin>
#		</DynamicCollector>
################## Example #####################

import csv
import os
import subprocess
import traceback
from pprint import pprint
import time

Logger=None

#gets current MS since epoch
def GetCurrMS():
    return  int(round(time.time() *1000)) # Gives you float secs since epoch, so make it ms and chop

def __gatherFn(progParams,Verbose=False):
    global Logger
    try:
        if True == Verbose:
            pprint(progParams)
        output = subprocess.check_output(progParams,stderr=subprocess.PIPE)
        retData=output.decode().splitlines() # need to decode and split it up

    except Exception as Ex:
        try:
            Logger.error(str(Ex))
        except:
            pprint(Ex)
        return None

    return retData

def decodePQOS(lines):
    dataMap={}
    # eat off any messages
    for index,line in enumerate(lines):
        if 'Core' in line:
            break

    reader = csv.reader(lines[index:],delimiter=',')
    Headers = next(reader)
    for row in reader:
        if len(row) < 3: # could be message/warning at end 
            continue

        core = row[1]
        for index,item in enumerate(row[2:]): # can skip past time and core #
            headerIndex = index + 2
            strItem = 'pqos.core' + core +'.' + Headers[headerIndex]
            dataMap[strItem] = item

    return dataMap

# worker function for another program
def GatherDataforKPIBlast():
    extProgAndOpts = ['pqos','-r','-t',' 1','-u', 'csv']
    try:
        retData = __gatherFn(extProgAndOpts,False)
        if None == retData:
            return None

    except:
        return None

    aliasMap={}

    for index,line in enumerate(retData): #eat past any messages
        if 'Core' in line:
            break

    reader = csv.reader(retData[index:],delimiter=',')
    Headers = next(reader)

    for index,header in enumerate(Headers[2:]):
        aliasMap['pqos.header' + str(index +1)] = header

    aliasMap['pqos.headercount'] = str(len(Headers)-2)

    coreMap={}
    for row in reader:
        if len(row) < 3: # could be message/warning at end 
            continue
        core = row[1]
        coreMap[core]=core

    aliasMap['pqos.corecount'] = len(coreMap)

    return aliasMap



def __goCollect(Verbose=False):
    extProgAndOpts = ['pqos','-r','-t',' 1','-u', 'csv']
    retData = __gatherFn(extProgAndOpts,Verbose)
    
    dMap = decodePQOS(retData)
    return dMap

def PQOS_Collector(frameworkInterface): 
    global Logger
    Logger = frameworkInterface.Logger
    Logger.info("Starting Intel Platform Quality of Service Collector: ")

    collectorInterval = float(frameworkInterface.Interval)
    LoopNum = 0
    try:
        while not frameworkInterface.KillThreadSignalled():
            startTime = GetCurrMS()
            try:
                dataMap = __goCollect()
            except Exception as Ex:
                Logger.error("Problem doing quos stuff")
                Logger.error(str(Ex))
                for line in traceback.format_stack():
                    Logger.error(line.strip())
                break

            for entry in dataMap:
                if LoopNum < 5: # no sense in wasting cycles checking if exists - there are 1000's of them
                    if not frameworkInterface.DoesCollectorExist(entry): # Do we already have this ID?
                        frameworkInterface.AddCollector(entry)    # Nope, so go add it
                        
                frameworkInterface.SetCollectorValue(entry,dataMap[entry]) 

            tDelta = GetCurrMS() - startTime
            if tDelta > collectorInterval: #took longer to collect than collector interval
                sleepTime=100

            else:
                sleepTime = collectorInterval - tDelta

            time.sleep(sleepTime/1000)
            LoopNum += 1

    except Exception as ex:
        Logger.error("Unrecoverable error in Intel Platform Quality of Service plugin: " + str(ex))
        for line in traceback.format_stack():
            Logger.error(line.strip())

#pprint(GatherDataforKPIBlast())