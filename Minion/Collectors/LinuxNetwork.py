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
#    This attempts to get Netowkr information on a Linux System
#
##############################################################################
import os
import subprocess
import time

lscpiDataMap=None

def ReadFromFile(Filename):
    try:
        file = open(Filename,'rt')
        if None == file:
            return "N/A"
    except Exception:
        return "N/A"

    return file.read()


## Generates a file with info on ALL ethernet devices
def CreatePerfFileAll(outputFile):
    dataStr = ""
    for root, dirs, files in os.walk("/sys/class/net"):
        for dir in dirs:
            nextDir = "/sys/class/net/" + dir + "/statistics"
            for statRoot, statDirs, statFiles in os.walk(nextDir):
                for fname in statFiles:
                    sFileName = nextDir + "/" + fname
                    dataVal = ReadFromFile(sFileName)
                    dataPoint = dir + "." + fname + "=" + dataVal
                    dataStr += dataPoint + os.linesep

    file = open(outputFile,"wt")
    file.write(dataStr)
    file.close()
    return "HelenKeller" # don't want to send anything

## Generates a file with info on a specific ethernet device
def CreatePerfFileDev(ethDev,outputFile):
    dataStr = ""
    nextDir = "/sys/class/net/" + ethDev + "/statistics"
    for statRoot, statDirs, statFiles in os.walk(nextDir):
        for fname in statFiles:
            sFileName = nextDir + "/" + fname
            dataVal = ReadFromFile(sFileName)
            dataPoint = fname + "=" + dataVal
            dataStr += dataPoint + os.linesep

    file = open(outputFile,"wt")
    file.write(dataStr)
    file.close()
    return "HelenKeller" # don't want to send anything

def __GetLSPCIData():
    global lscpiDataMap
    if None != lscpiDataMap:
        return lscpiDataMap
    lscpiDataMap ={}

    checkStr= 'Ethernet controller:'
    checkStrLen = len(checkStr)
    try:
        lspciList = subprocess.check_output('lspci').splitlines()
        for line in lspciList:
            if line[8:8+checkStrLen] == checkStr:
                busID=line[0:7]
                devInfoStr = line[8+checkStrLen+1:]
                lscpiDataMap[busID]=devInfoStr

    except Exception:
        pass

    return lscpiDataMap

def __GetDeviceVendorInfo(ethDev):
    try:
        linkStr=os.readlink("/sys/class/net/" + ethDev + "/device")
        parts = linkStr.split("/")
        busID= parts[-1]
        if busID.split(':')[0] == '0000': # it returns 4 byte bus#
            busID=busID[5:]

        dataMap = __GetLSPCIData()
        if busID in dataMap:
            return dataMap[busID]

    except Exception:
        pass
    return "Unknown Vendor Information"

def __GatherNetworkDeviceInfo(ethDev,retMap,slimDataset):
    nextDir = "/sys/class/net/" + ethDev + "/statistics"
    baseName='netdev.'
    if not slimDataset:
        for statRoot, statDirs, statFiles in os.walk(nextDir):
            for fname in statFiles:
                sFileName = nextDir + "/" + fname
                dataVal = ReadFromFile(sFileName)
                retMap[baseName+ethDev + "." + fname] = dataVal

        verFile = "/sys/class/net/" + ethDev + "/device/driver/module/version"
        retMap[baseName+ethDev + ".version"] = ReadFromFile(verFile)
        numaFile = "/sys/class/net/" + ethDev + "/device/numa_node"
        retMap[baseName+ethDev + ".numa_node"] = ReadFromFile(numaFile)
        retMap[baseName+ethDev + ".vendor_info"] = __GetDeviceVendorInfo(ethDev)
        mtuFile = "/sys/class/net/" + ethDev + "/mtu"
        retMap[baseName+ethDev + ".mtu"] = ReadFromFile(mtuFile)
        operStateFile = "/sys/class/net/" + ethDev + "/operstate"
        retMap[baseName+ethDev + ".state"] = ReadFromFile(operStateFile)

    else: # slim dataset, just to bytes and packets
        for fname in ['rx_bytes','tx_bytes','tx_packets','rx_packets']:
            sFileName = nextDir + "/" + fname
            dataVal = ReadFromFile(sFileName)
            retMap[baseName+ethDev + "." + fname] = dataVal

    return retMap

def __GatherAllNetworkDeviceInfo(slimDataSet):
    tMap={}
    for root, dirs, files in os.walk("/sys/class/net"):
        for dir in dirs:
           tMap= __GatherNetworkDeviceInfo(dir,tMap,slimDataSet)

    return tMap


def CollectAllDevices(frameworkInterface,slimDataSetParam): 
    # frameworkInterface has the following:
    #  frameworkInterface.DoesCollectorExist(ID) # does a collector with ID  already exist
    #  frameworkInterface.AddCollector(ID) # Add a new collectr
    #  frameworkInterface.SetCollectorValue(ID,Value,ElapsedTime) # Assign the  collector a new value, along with how long since last update
    #  frameworkInterface.KillThreadSignalled() # returns True if signalled to end your worker thread, else False
    #  frameworkInterface.LockFileName() # lockfile name for dynamic collector,  if specified
    #  frameworkInterface.Interval() # the frequency from the config file
    
    global Logger
    Logger = frameworkInterface.Logger
    Logger.info("Starting LinuxNetwork Collector, collecting all Devices")
    try:
        if slimDataSetParam.lower() == "true":
            slimDataSet = True
        else:
            slimDataSet = False

        SleepTime = float(frameworkInterface.Interval)/1000.0   

        InitialRun = True
        while not frameworkInterface.KillThreadSignalled():
            dataMap = __GatherAllNetworkDeviceInfo(slimDataSet)
            for entry in dataMap:
                if InitialRun and not frameworkInterface.DoesCollectorExist(entry): # Do we already have this ID?
                    frameworkInterface.AddCollector(entry)    # Nope, so go add it
                        
                frameworkInterface.SetCollectorValue(entry,dataMap[entry]) 

            time.sleep(SleepTime)

            if InitialRun:
                InitialRun = False

    except Exception as ex:
        Logger.error("Unrecoverable error in LinuxNetwork Collector plugin: " + str(ex))

def CollectDevice(frameworkInterface,DeviceName,slimDataSetParam): 
    # frameworkInterface has the following:
    #  frameworkInterface.DoesCollectorExist(ID) # does a collector with ID  already exist
    #  frameworkInterface.AddCollector(ID) # Add a new collectr
    #  frameworkInterface.SetCollectorValue(ID,Value,ElapsedTime) # Assign the  collector a new value, along with how long since last update
    #  frameworkInterface.KillThreadSignalled() # returns True if signalled to end your worker thread, else False
    #  frameworkInterface.LockFileName() # lockfile name for dynamic collector,  if specified
    #  frameworkInterface.Interval() # the frequency from the config file
    
    global Logger
    Logger = frameworkInterface.Logger
    Logger.info("Starting LinuxNetwork Collector, collecting all Devices")
    try:
        if slimDataSetParam.lower() == "true":
            slimDataSet = True
        else:
            slimDataSet = False

        SleepTime = float(frameworkInterface.Interval)/1000.0
        InitialRun = True
        
        while not frameworkInterface.KillThreadSignalled():
            dataMap={}
            __GatherNetworkDeviceInfo(DeviceName,dataMap,slimDataSet)
            for entry in dataMap:
                if InitialRun and not frameworkInterface.DoesCollectorExist(entry): # Do we already have this ID?
                    frameworkInterface.AddCollector(entry)    # Nope, so go add it
                        
                frameworkInterface.SetCollectorValue(entry,dataMap[entry]) 

            time.sleep(SleepTime)

            if InitialRun:
                InitialRun = False

    except Exception as ex:
        Logger.error("Unrecoverable error in LinuxNetwork Collector plugin: " + str(ex))


