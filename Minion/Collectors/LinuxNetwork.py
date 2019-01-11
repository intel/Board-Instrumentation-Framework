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
import socket

VersionStr="v19.01.11"
lscpiDataMap=None
netdevInfoDir="/sys/class/net"

def ReadFromFile(Filename):
    try:
        file = open(Filename,'rt')
        if None == file:
            return "N/A"
    except Exception:
        return "N/A"

    return file.read().strip()

# in case we want to run this from a container and mount a dir
def GetBaseDir():
    global netdevInfoDir
    return netdevInfoDir

def SetBaseDir(dirLocation):
    global netdevInfoDir
    netdevInfoDir = dirLocation


## Generates a file with info on ALL ethernet devices
def CreatePerfFileAll(outputFile):
    dataStr = ""
    for root, dirs, files in os.walk(GetBaseDir()):
        for dir in dirs:
            nextDir = GetBaseDir() + "/"  + dir + "/statistics"
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
    nextDir = GetBaseDir() + "/"  + ethDev + "/statistics"
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
            line = line.decode('ascii')
            if checkStr in line:
                busID=line.split(' ')[0]
                devInfoStr = line[len(busID)+checkStrLen+2:]
                lscpiDataMap[busID]=devInfoStr

    except Exception as Ex:
        pass

    return lscpiDataMap

def __GetDeviceVendorInfo(ethDev):
    try:
        linkStr=os.readlink(GetBaseDir() + "/"  + ethDev + "/device")
        parts = linkStr.split("/")
        busID= parts[-1]

        dataMap = __GetLSPCIData()
        if busID in dataMap:
            return dataMap[busID]

    except Exception as ex:
        pass

    return "Unknown Vendor Information"

def __GatherNetworkDeviceInfo(ethDev,retMap,slimDataset):
    nextDir = GetBaseDir() + "/"  + ethDev + "/statistics"
    baseName='netdev.'
    if not slimDataset:
        for statRoot, statDirs, statFiles in os.walk(nextDir):
            for fname in statFiles:
                sFileName = nextDir + "/" + fname
                dataVal = ReadFromFile(sFileName)
                retMap[baseName+ethDev + "." + fname] = dataVal

        verFile = GetBaseDir() + "/"  + ethDev + "/device/driver/module/version"
        retMap[baseName+ethDev + ".version"] = ReadFromFile(verFile)
        numaFile = GetBaseDir() + "/"  + ethDev + "/device/numa_node"
        retMap[baseName+ethDev + ".numa_node"] = ReadFromFile(numaFile)
        retMap[baseName+ethDev + ".vendor_info"] = __GetDeviceVendorInfo(ethDev)
        mtuFile = GetBaseDir() + "/"  + ethDev + "/mtu"
        retMap[baseName+ethDev + ".mtu"] = ReadFromFile(mtuFile)
        operStateFile = GetBaseDir() + "/"  + ethDev + "/operstate"
        retMap[baseName+ethDev + ".state"] = ReadFromFile(operStateFile)
        macAddrFile = GetBaseDir() + "/"  + ethDev + "/address"
        retMap[baseName+ethDev + ".macaddress"] = ReadFromFile(macAddrFile)
        retMap[baseName+ethDev + ".driver"] = __GetDriver(ethDev)
        speedFile = GetBaseDir() + "/"  + ethDev + "/speed"
        try:
            retMap[baseName+ethDev + ".speed"] = ReadFromFile(speedFile)
        except:
            retMap[baseName+ethDev + ".speed"] = "Unknown"
                		

        sckt = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        sckt.connect(("8.8.8.8", 80))
        ipAddr =  sckt.getsockname()[0]
        retMap[baseName+ethDev + ".ipaddress"] = str(ipAddr)

    else: # slim dataset, just to bytes and packets
        for fname in ['rx_bytes','tx_bytes','tx_packets','rx_packets']:
            sFileName = nextDir + "/" + fname
            dataVal = ReadFromFile(sFileName)
            retMap[baseName+ethDev + "." + fname] = dataVal

    if baseName+ethDev+".rx_bytes" in retMap: # should ALWAYS be there
        retMap[baseName+ethDev+".rx_gbps"] = retMap[baseName+ethDev+".rx_bytes"]
        retMap[baseName+ethDev+".tx_gbps"] = retMap[baseName+ethDev+".tx_bytes"]
        retMap[baseName+ethDev+".bx_gbps"] = str(float(retMap[baseName+ethDev+".tx_bytes"]) + float(retMap[baseName+ethDev+".rx_bytes"]))
        retMap[baseName+ethDev+".rx_mbps"] = retMap[baseName+ethDev+".rx_bytes"]
        retMap[baseName+ethDev+".tx_mbps"] = retMap[baseName+ethDev+".tx_bytes"]
        retMap[baseName+ethDev+".bx_mbps"] = str(float(retMap[baseName+ethDev+".tx_bytes"]) + float(retMap[baseName+ethDev+".rx_bytes"]))

    if baseName+ethDev+".tx_packets" in retMap:
        retMap[baseName+ethDev+".tx_pps"] = retMap[baseName+ethDev+".tx_packets"]
        retMap[baseName+ethDev+".rx_pps"] = retMap[baseName+ethDev+".rx_packets"]
        retMap[baseName+ethDev+".bx_pps"] = str(float(retMap[baseName+ethDev+".rx_packets"]) + float(retMap[baseName+ethDev+".tx_packets"]))
        retMap[baseName+ethDev+".tx_mpps"] = retMap[baseName+ethDev+".tx_packets"]
        retMap[baseName+ethDev+".rx_mpps"] = retMap[baseName+ethDev+".rx_packets"]
        retMap[baseName+ethDev+".bx_mpps"] = baseName+ethDev+".bx_pps"
        

    return retMap

def __GetDriver(device):
    link = 	GetBaseDir() + '/' +device + '/device/driver/module/drivers'
    for root, driver, files in os.walk(link):
       driver = driver[0]
       if None != driver[0] and ':' in driver:
          parts = driver.split(':')
          return parts[1]
    return None

def __IsPhysicalDevice(device):
    return None != __GetDriver(device)

def __GatherAllNetworkDeviceInfo(slimDataSet,pyhysicalOnly=True):
    tMap={}
    for root, dirs, files in os.walk(GetBaseDir()):
        for dir in dirs:
           if False == pyhysicalOnly or __IsPhysicalDevice(dir):
              tMap= __GatherNetworkDeviceInfo(dir,tMap,slimDataSet)
           
    return tMap


def CollectAllDevices(frameworkInterface,slimDataSetParam,**kwargs): 
    # frameworkInterface has the following:
    #  frameworkInterface.DoesCollectorExist(ID) # does a collector with ID  already exist
    #  frameworkInterface.AddCollector(ID) # Add a new collectr
    #  frameworkInterface.SetCollectorValue(ID,Value,ElapsedTime) # Assign the  collector a new value, along with how long since last update
    #  frameworkInterface.KillThreadSignalled() # returns True if signalled to end your worker thread, else False
    #  frameworkInterface.LockFileName() # lockfile name for dynamic collector,  if specified
    #  frameworkInterface.Interval() # the frequency from the config file
    #  frameworkInterface.SetNormilization() # 
    #  frameworkInterface.SetScale    
    global Logger
    Logger = frameworkInterface.Logger
    Logger.info("Starting LinuxNetwork Collector {0}, collecting all devices: {1}".format(VersionStr,kwargs))

    try:
        if slimDataSetParam.lower() == "true":
            slimDataSet = True
        else:
            slimDataSet = False
        if 'PhysicalDevicesOnly' in kwargs and kwargs['PhysicalDevicesOnly'].lower() == 'true':
           physicalOnly = True
        else:
           physicalOnly = False		
        SleepTime = float(frameworkInterface.Interval)/1000.0   

        InitialRun = True
        while not frameworkInterface.KillThreadSignalled():
            dataMap = __GatherAllNetworkDeviceInfo(slimDataSet,physicalOnly)
            for entry in dataMap:
                if InitialRun and not frameworkInterface.DoesCollectorExist(entry): # Do we already have this ID?
                    frameworkInterface.AddCollector(entry)    # Nope, so go add it
                    if "_mbps" in entry:
                       frameworkInterface.SetNormilization(entry,"0.000008")					
                    elif "_gbps" in entry:
                       frameworkInterface.SetNormilization(entry,"0.000000008")					
                    elif "_pps" in entry:
                       frameworkInterface.SetNormilization(entry,"1")					
                    elif "_mpps" in entry:
                       frameworkInterface.SetNormilization(entry,".000001")					
                        
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
    #  frameworkInterface.SetNormilization() # 
    #  frameworkInterface.SetScale
    
    global Logger
    Logger = frameworkInterface.Logger
    Logger.info("Starting LinuxNetwork Collector {0}, collecting single Device: {1}".format(VersionStr,DeviceName))
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
                    if "_mbps" in entry:
                       frameworkInterface.SetNormilization(entry,"0.000008")					
                    elif "_gbps" in entry:
                       frameworkInterface.SetNormilization(entry,"0.000000008")					
                    elif "_pps" in entry:
                       frameworkInterface.SetNormilization(entry,"1")					
                    elif "_mpps" in entry:
                       frameworkInterface.SetNormilization(entry,".000001")					
                        
                frameworkInterface.SetCollectorValue(entry,dataMap[entry]) 

            time.sleep(SleepTime)

            if InitialRun:
                InitialRun = False

    except Exception as ex:
        Logger.error("Unrecoverable error in LinuxNetwork Collector plugin: " + str(ex))


