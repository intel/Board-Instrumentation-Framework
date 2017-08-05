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
#    Setup for a DynamicCollector plugin for the Intel Performance Counter  utility    
##############################################################################

################## Example #####################
#		<DynamicCollector SendOnlyOnChange="True">
#			<Precision>0</Precision>
#			<Plugin>
#				<PythonFile>Collectors/IPC_Linux.py</PythonFile>
#				<EntryPoint SpawnThread="True">PCM_Collector</EntryPoint>
#				<Param>pcm,core,memory,numa,pcie</Param>
#				<Param>1.0</Param>
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
opts = None

#gets current MS since epoch
def GetCurrMS():
    return  int(round(time.time() *1000)) # Gives you float secs since epoch, so make it ms and chop

def __initOptions(timePeriod):
    timePeriod=str(timePeriod)
    try:
        realTime = float(timePeriod)
        if realTime < 1:
            timePeriod='1'
    except:
        timePeriod='1'
    global opts
    ## key, [ program and args], decoderFn
    opts ={'pcm':[['pcm.x','-i=' + timePeriod,'-csv'],decodePCM],
           'reset':[['pcm.x','-i=1','-r'],None],
		   'core': [['pcm-core.x','-i='+ timePeriod,'-csv'],decodeCORE],
		   'memory': [['pcm-memory.x','-i='+ timePeriod,'-csv'],decodeMEMORY],
		   'numa': [['pcm-numa.x','-csv','-- ','date'],decodeNUMA],
		   'pcie': [['pcm-pcie.x','-i='+ timePeriod,'-csv'],decodePCIe]
           }

def __gatherFn(progParams,Verbose=False):
    global Logger
    try:
        if True == Verbose:
            pprint(progParams)
        output = subprocess.check_output(progParams,stderr=subprocess.PIPE)
        retData=output.decode().splitlines() # need to decode and split it up

    except Exception as Ex:
        Logger.error(str(Ex))
        return None

    return retData

def decodePCM(data):
    reader = csv.reader(data,delimiter=';')
    Sections = next(reader)
    Headers = next(reader)
    DataValues  = next(reader)

    RealSections=[] # because as read, is a 'major header' and then a data header and then data
    lastRealSection='Not Used'
    coreCount = 0
    for Section in Sections:
        if len(Section) > 2:	
            lastRealSection = Section
            if '(Socket ' in Section:
                coreCount += 1
        RealSections.append(lastRealSection.strip())

    DataMap={}
    DataMap['pcm.pcm.core_count'] = str(coreCount)
    for index,DataHeader in enumerate(Headers):
        SectionHeader = RealSections[index]
        Value = DataValues[index].strip()
        if len(Value) == 0: # currently it alwasys gives an extra column for some reason
            break
        DataMap['pcm.pcm.' + SectionHeader+'.'+DataHeader]=Value
        
    
    return DataMap

def decodeCORE(data):
    reader = csv.reader(data,delimiter=',') #this one is actual CSV, not separated by ';'

    dummy=next(reader) # several lines we dont' care about
    dummy=next(reader)
    dummy=next(reader)

    Sections = next(reader)
    DataValues  = next(reader)

    coreStr = 'not used'
    DataMap={}
    coreCount=0
    while DataValues != None:
        if len(DataValues) < 9: # seems to have an extra line at end, this filters it out
            break
        for index,item in enumerate(DataValues):
            if len(item) == 0:
                break
            if Sections[index] == 'Core':
                coreStr = item
                if coreStr == "*":
                    coreStr = 'total'
                else:
                    coreCount+=1			
                    coreStr = 'core-' + coreStr

            else:			
                DataMap['pcm.pcm-core.'+coreStr+'.'+Sections[index]] = item

        DataValues  = next(reader)

    DataMap['pcm.pcm-core.core_count'] = str(coreCount)
    return DataMap

def decodePCIe(data):
    reader = csv.reader(data,delimiter=',') #this one is actual CSV, not separated by ';'

    Sections = next(reader)
    DataValues  = next(reader)

    sktStr = 'not used'
    DataMap={}
    socketCount=0
    while DataValues != None:
        if len(DataValues) < 8: # seems to have an extra line at end, this filters it out
            break
        for index,item in enumerate(DataValues):
            if len(item) == 0:
                break
            if Sections[index] == 'Skt':
               sktStr = 'socket-' + item
               socketCount +=1

            else:			
                DataMap['pcm.pcm-pcie.'+sktStr+'.'+Sections[index]] = item
        try:
            DataValues  = next(reader)
        except:
            break

    DataMap['pcm.pcm-pcie.socket_count'] = str(socketCount)
    return DataMap

def decodeMEMORY(data):
    reader = csv.reader(data,delimiter=';') #this one is actual CSV, not separated by ';'

    SocketList = next(reader)
    Sections = next(reader)
    DataValues  = next(reader)

    DataMap={}
    socketCount=0
    while DataValues != None:
        for index,item in enumerate(DataValues):
            if len(item) == 0:
                break
            if len(SocketList[index]) == 0: #date and time
                continue
            
            DataMap['pcm.pcm-memory.' + SocketList[index].strip() +"." + Sections[index].strip()] = item.strip()
        try:
            DataValues  = next(reader)
        except:
            break

    return DataMap

def decodeNUMA(data):
    reader = csv.reader(data,delimiter=',') #this one is actual CSV, not separated by ';'

    DummyDate = next(reader) # in order for numa app to run but once, have to run an external
    DummyDate = next(reader) # program (I don't know why), so I run date, need to 'eat' the output

    Sections = next(reader)
    DataValues  = next(reader)

    DataMap={}
    socketCount=0
    coreCount = 0
    coreStr = 'Not used'
    while DataValues != None:
        for index,item in enumerate(DataValues):
            if len(item) == 0:
                break
            if Sections[index] == 'Core':
                coreStr = item
                coreCount +=1
                if item == '*':
                    coreStr = 'total'

            else:
                DataMap['pcm.pcm-numa.' + coreStr +"." + Sections[index].strip()] = item.strip()
        try:
            DataValues  = next(reader)
        except:
            break
    DataMap['pcm.pcm-numa.core_count'] = str(coreCount)
    return DataMap

def __goCollect(strWhich,Verbose=False):
    if not strWhich in opts:
        return {}
    extProgAndOpts = opts[strWhich][0]    
    retData = __gatherFn(extProgAndOpts,Verbose)
    
    pParseFn= opts[strWhich][1]
    dMap = pParseFn(retData)
    return dMap

def PCM_Collector(frameworkInterface,listOfPrograms,gatherInterval): 
    global Logger
    Logger = frameworkInterface.Logger
    Logger.info("Starting Intel PCM Collector: " +  listOfPrograms)

    __initOptions(gatherInterval)
    gatherItems = listOfPrograms.split(',')
    collectorInterval = float(frameworkInterface.Interval)
    LoopNum = 0
    try:
        while not frameworkInterface.KillThreadSignalled():
            startTime = GetCurrMS()
            for prog in gatherItems:
                try:
                    dataMap = __goCollect(prog)
                except Exception as Ex:
                    Logger.error("Problem doing PCM stuff, resetting PCM")
                    Logger.error(str(Ex))
                    __gatherFn(opts['reset'][0])
                    break

                for entry in dataMap:
                    if LoopNum < 5: # no sense in wasting cycles checking if exists - there are 1000's of them
                        if not frameworkInterface.DoesCollectorExist(entry): # Do we already have this ID?
                            frameworkInterface.AddCollector(entry)    # Nope, so go add it
                        
                    frameworkInterface.SetCollectorValue(entry,dataMap[entry]) 
                time.sleep(.001)

            tDelta = GetCurrMS() - startTime
            if tDelta > collectorInterval: #took longer to collect than collector interval
                sleepTime=100
            else:
                sleepTime = collectorInterval - tDelta

            time.sleep(sleepTime/1000)
            LoopNum += 1
    except Exception as ex:
        Logger.error("Unrecoverable error in Intel PCM Collector plugin: " + str(ex))
        for line in traceback.format_stack():
            Logger.error(line.strip())
    
def TestFn(strWich):
    __initOptions("1")
    dMap = __goCollect(strWich,True)
    return dMap

#import sys
#TestFn(str(sys.argv[1]))
