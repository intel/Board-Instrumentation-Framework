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
#       Gets some system information
##############################################################################

################## Example #####################
#		<DynamicCollector SendOnlyOnChange="True">
#			<Plugin>
#				<PythonFile>Collectors/SystemInfo_Linux.py</PythonFile>
#				<EntryPoint SpawnThread="True">CollectSystemInfo_Linux</EntryPoint>
#			</Plugin>
#		</DynamicCollector>
################## Example #####################

import platform
import os
import csv
import time
from collections import namedtuple
import multiprocessing
import subprocess
import traceback
from Collectors import Linux_CPU
from pprint import pprint

DMI_Data=None
VersionStr="v19.01.15"


def ReadFromFile(Filename):
    try:
        file = open(Filename,'rt')
        if None == file:
            return "File [" + Filename + "] does not exist"
    except Exception:
        return "File [" + Filename + "] does not exist"

    return file.read()


def GetPlatform():
    return platform.uname()[0]

def GetRelease():
    return platform.release()

def GetLinuxDistro():
    try: # was removed in python 3.8 :-(
        return str(platform.linux_distribution()[0] + " " + platform.linux_distribution()[1])
    except Exception:
        RELEASE_INFO = {}
        with open("/etc/os-release") as fp:
            fReader = csv.reader(fp, delimiter="=")
            for line in fReader:
                if fReader:
                    RELEASE_INFO[line[0]] = line[1]
                    
        if RELEASE_INFO["ID"] in ["debian", "raspbian"]:
            with open("/etc/debian_version") as fp:
                DEBIAN_VERSION = fp.readline().strip()
                
            major_version = DEBIAN_VERSION.split(".")[0]
            version_split = RELEASE_INFO["VERSION"].split(" ", maxsplit=1)
            if version_split[0] == major_version:
                RELEASE_INFO["VERSION"] = " ".join([DEBIAN_VERSION] + version_split[1:])
                            
        return RELEASE_INFO["NAME"] + " " + RELEASE_INFO["VERSION_ID"]         
                          
def GetCPUInfo_Model_Linux():
    with open('/proc/cpuinfo') as f:
        for line in f:
            # Ignore the blank line separating the information between
            # details about two processing units
            if line.strip():
                if line.rstrip('\n').startswith('model name'):
                    model_name = line.rstrip('\n').split(':')[1]
                    return model_name.strip()
    return "Unable to determing CPU Model"

def GetCoreCount():
    return str(multiprocessing.cpu_count())

def GetMemoryInfo_Linux(item):
    with open('/proc/meminfo') as f:
        for line in f:
            data = line.split(':')
            if item == data[0]:
                return  data[1].strip()
    return item + "=Invalid Item"

def NumaNodeExists(nodeNum):
    return os.path.exists('/sys/devices/system/node/node'+str(nodeNum))

def GetNumaInfo(nodeNum):
    retMap={}
    retMap['system.numa.node' + str(nodeNum) +'.core_list'] = ReadFromFile('/sys/devices/system/node/node'+str(nodeNum)+'/cpulist')
    return retMap

def __GetNumaStats(nodeCount):
    retMap={}
    for node in range(0,nodeCount):
        lines = ReadFromFile("/sys/devices/system/node/node" + str(node) + "/numastat").splitlines()
        for entry in lines:
            id,val = entry.strip().split(' ')
            retMap['system.numa.node' + str(node) + "." + id] = val

    return retMap

def IsHyperthreadingEnabled():
    sibs = ReadFromFile('/sys/devices/system/node/node0/cpu0/topology/thread_siblings_list').strip().split(',')
    return len(sibs) > 1 # otherwise HT not enabled

def __CreateNumaMap():
    socketNum=0
    SibCoreMap={}
    CoreMap={}
    while NumaNodeExists(socketNum):
        # gets list of cores on this Socket, could be HT or not
        # some platforms look like: 1,3,5,7,9,11,13,15,17,19,21,23,25,27,29,31,33,35,37,39,41,43,45,47,49,51,53,55
        # others look like: 0-13,28-41

        coreList = ReadFromFile('/sys/devices/system/node/node'+str(socketNum)+'/cpulist').strip().split(',')
        
        if '-' in coreList[0]: 
            altList=[]
            for cList in coreList: #is the 0-13,28-41 form
                parts=cList.split('-')
                for index in range(int(parts[0]),int(parts[1])+1):
                    altList.append(str(index))
            
            coreList=altList
        
        for coreNum,core in enumerate(coreList):
            core = core.strip()
            sibs = ReadFromFile('/sys/devices/system/node/node' + str(socketNum) + '/cpu' + core + '/topology/thread_siblings_list').strip().split(',')
            
            if sibs[0] in SibCoreMap:
                if sibs[1] != SibCoreMap[sibs[0]]:
                    #print("Unexpected situation!")
                    pass
            else:
                SibCoreMap[sibs[0]] = sibs[1]
                SibCoreMap[sibs[1]] = sibs[0]
                CoreMap[sibs[0]]={}
                CoreMap[sibs[0]]['util'] = [0,0]
                CoreMap[sibs[0]]['socket'] = str(socketNum)
                CoreMap[sibs[0]]['corenum'] = str(coreNum)
                CoreMap[sibs[0]]['sibling'] = sibs[1]

        socketNum += 1
        #pprint(coreList)            

    return (SibCoreMap,CoreMap)

def __ProcessHyperthreadCPUInfo(CoreMap,SiblingMap,loadData):
    if not IsHyperthreadingEnabled():
        return None

    for core in loadData:
        if len(core) < 4:
            continue
        val = loadData[core]
        val = str(int(val))
        if len(core)==4:
            core=core[-1:]
        else:
            core=core[-2:]
            
        if core in CoreMap:
            CoreMap[core]['util'][0] = val
        else:
            CoreMap[SiblingMap[core]]['util'][1] = val

    retMap={}

    for pCPU in CoreMap:
        vCPU0 = CoreMap[pCPU]['util'][0]
        vCPU1 = CoreMap[pCPU]['util'][1]
        socket = CoreMap[pCPU]['socket']
        socketCore = CoreMap[pCPU]['corenum']
        total = int(vCPU0) + int(vCPU1)
        if total > 100:
            total = 100
        Id = "system.numa.ht.socket" + socket + '.core' + socketCore + ".util"
        retMap[Id]=str(total)

        Id = "system.numa.ht.socket" + socket + '.core' + socketCore + ".vcpu0.util"
        retMap[Id]=str(vCPU0)

        Id = "system.numa.ht.socket" + socket + '.core' + socketCore + ".vcpu1.util"
        retMap[Id]=str(vCPU1)

    return retMap
    
             
def GetSystemInfo_Linux(outputFile):
    data = "Platform="+GetPlatform() + os.linesep
    data += "Distro=" + GetLinuxDistro() + os.linesep
    data += "CPU.Model=" + GetCPUInfo_Model_Linux()
    data += "Memory.Total=" + GetMemoryInfo_Linux("MemTotal") + os.linesep
    data += "Memory.Free=" + GetMemoryInfo_Linux("MemFree") + os.linesep
    data += "CoreCount=" + GetCoreCount() + os.linesep
    

    file = open(outputFile,"wt")
    file.write(data)
    file.close()
    return "HelenKeller" # don't want to send anything

def __GetDMI_Data():
    global DMI_Data
    if None == DMI_Data:
        try:
            tDMI_Data = subprocess.check_output('dmidecode').splitlines()
            DMI_Data=[]
            for line in tDMI_Data:
                DMI_Data.append(line.decode('utf-8'))
        #pylint: disable=unused-variable
        except Exception as Ex:
            Logger.error("dmidecode not installed - unable to read all information")
            None

    return DMI_Data

def __GetDMI_Info(section,instance,item):
    sectionFound = False
    currInst = 0
    
    for line in __GetDMI_Data():
        if len(line) < 2:
            continue

        if not sectionFound:
            if line[0] == ' ': #sections start @ left, with no spaces
                continue
            if line[0] == '\t': #sections start @ left, with no spaces
                continue

            if line.strip() == section:
                if currInst == instance:
                    sectionFound = True
                else:
                    currInst += 1
        else:
            line = line.strip() # get rid of leading spaces
            parts = line.split(':')
            
            if len(parts) > 1:
                
                if parts[0].strip() == item:
                    return parts[1].strip()

    return "not found"

def __GetStaticInfo():
    retMap={}
    retMap['system.platform'] = GetPlatform()
    retMap['system.distro'] = GetLinuxDistro()
    retMap['system.cpu_model'] =  GetCPUInfo_Model_Linux()
    retMap['system.mem_total'] = GetMemoryInfo_Linux("MemTotal")
    retMap['system.core_count'] = GetCoreCount()

    iCount=0
    while NumaNodeExists(iCount):
        retMap.update(GetNumaInfo(iCount))
        iCount += 1
        
    retMap['system.numa_count'] = str(iCount)

    if None != __GetDMI_Data():
        try:
            retMap['system.bios.vendor'] = __GetDMI_Info("BIOS Information",0,"Vendor") 

            retMap['system.bios.version'] = __GetDMI_Info("BIOS Information",0,"Version") 
            retMap['system.bios.releaseDate'] = __GetDMI_Info("BIOS Information",0,"Release Date") 
            retMap['system.bios.revision'] = __GetDMI_Info("BIOS Information",0,"BIOS Revision") 

            retMap['system.manufacturer'] = __GetDMI_Info("System Information",0,"Manufacturer") 
            retMap['system.product_name'] = __GetDMI_Info("System Information",0,"Product Name") 
            retMap['system.serial_number'] = __GetDMI_Info("System Information",0,"Serial Number") 
            retMap['system.family'] = __GetDMI_Info("System Information",0,"Family") 

            if IsHyperthreadingEnabled():
                retMap['system.hyperthreading_enabled'] = 'yes'
            else:
                retMap['system.hyperthreading_enabled'] = 'no'
        except Exception as Ex:
            Logger.error("Unexpected problem in __GetStaticInfo() " + str(Ex))

    else: #DMI decode not around, so look another way
        answer='yes'
        if not IsHyperthreadingEnabled():
            answer = 'no'
        retMap['system.hyperthreading_enabled'] = answer

    return retMap

def SystemUptimeShort():
    parts = ReadFromFile('/proc/uptime').strip().split(' ')
    seconds = float(parts[0])
    strTime=""
    for scale in 86400, 3600, 60:
        result, seconds = divmod(seconds, scale)
        result = (int)(result)
        seconds = (int) (seconds)
        if strTime != '' or result > 0:
            strTime += '{0:02d}:'.format(result)
    strTime += '{0:02d}'.format(seconds)
    return strTime    

def SystemUptimeLong():
    parts = ReadFromFile('/proc/uptime').strip().split(' ')
    seconds = float(parts[0])
    days,seconds = divmod(seconds,86400)
    hours,seconds = divmod(int(seconds),3600)
    minutes,seconds = divmod(int(seconds),60)
    
    strTime="{0} days,  {1}:{2}".format(int(days),hours,minutes)

    return strTime    

def CollectSystemInfo_Linux(frameworkInterface,showHyperthreadingCoreDetails=False,**kwargs):
    global Logger
    Logger = frameworkInterface.Logger
    Logger.info("Starting CollectSystemInfo_Linux Collector {0} {1}".format(VersionStr,kwargs))
    updatedCount = 0
    showHyperthreadingCoreDetails = str(showHyperthreadingCoreDetails).lower()
    if showHyperthreadingCoreDetails == 'true':
        showHyperthreadingCoreDetails = True
    else:
        showHyperthreadingCoreDetails = False

    if 'ShowFrequencyInfo' in kwargs and kwargs['ShowFrequencyInfo'].lower() == 'true':
        showFreq = True
    else:
        showFreq = False		


    if showHyperthreadingCoreDetails:
        showHyperthreadingCoreDetails = IsHyperthreadingEnabled() #don't bother doing if HT is not enabled

    try:

        dataMap = __GetStaticInfo()
        corecount = float(dataMap['system.core_count'])
        numaCount = int(dataMap['system.numa_count'])
        if True == showHyperthreadingCoreDetails:
            SiblingMap,CoreMap = __CreateNumaMap()

        while not frameworkInterface.KillThreadSignalled():
            dataMap["system.mem_available"] = GetMemoryInfo_Linux('MemAvailable')
            dataMap["system.hugepages_total"] = GetMemoryInfo_Linux('HugePages_Total')
            dataMap["system.hugepages_free"] = GetMemoryInfo_Linux('HugePages_Free')
            dataMap["system.hugepages_size"] = GetMemoryInfo_Linux('Hugepagesize')

            if showHyperthreadingCoreDetails:
                dataMap.update(__GetNumaStats(numaCount))

            try:
                dataMap["system.cpu_util_list"],utilData = Linux_CPU.CreateUtilizationList(.1,2,True)
                if showFreq:
                     dataMap.update(Linux_CPU.getFrequencyInfo("system."))

                if showHyperthreadingCoreDetails:
                    dataMap.update(__ProcessHyperthreadCPUInfo(CoreMap,SiblingMap,utilData))

                total = 0.0
                for val in dataMap["system.cpu_util_list"].split(","):
                    total += float(val)
                dataMap["system.cpu_util"] = str(total/corecount)

                if True == showHyperthreadingCoreDetails:
                    pass
            #pylint: disable=unused-variable
            except Exception as Ex:
                pass

            dataMap["system.uptime.short"] = SystemUptimeShort()
            dataMap["system.uptime.long"] = SystemUptimeLong()

            for entry in dataMap:
                if not frameworkInterface.DoesCollectorExist(entry): # Do we already have this ID?
                    frameworkInterface.AddCollector(entry)    # Nope, so go add it

                    if ".core_list" in entry: # list of cores, so no need to precision
                        frameworkInterface.SetPrecision(entry,0)
                        
                frameworkInterface.SetCollectorValue(entry,dataMap[entry]) 

            if updatedCount < 5: # save some cycles
                updatedCount += 1
            
            time.sleep(float(frameworkInterface.Interval)/1000.0)

    except Exception as ex:
        Logger.error("Unrecoverable error in CollectSystemInfo_Linux Collector plugin: " + str(ex))
        for line in traceback.format_stack():
            Logger.error(line.strip())


