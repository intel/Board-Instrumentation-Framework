##############################################################################
#  Copyright (c) 2020-2023 Intel Corporation
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
#    Gathers networking information vai varios methods under Linux
#
##############################################################################
import os
import subprocess
import time
import socket
import glob
import array
import fcntl  # pylint: disable=import-error 
import struct
from os import path

from pprint import pprint as pprint

VersionStr="v22.04.30"
lscpiDataMap=None
netdevInfoDir="/sys/class/net"

SIOCETHTOOL = 0x8946
ETHTOOL_GSTRINGS = 0x0000001b
ETHTOOL_GSSET_INFO = 0x00000037
ETHTOOL_GSTATS = 0x0000001d
ETH_SS_STATS = 0x01
ETH_SS_PRIV_FLAGS = 0x02
ETH_GSTRING_LEN = 32
ETHTOOL_GDRVINFO = 0x00000003
ETHTOOL_GCOALESCE =	0x0000000e 

ETHTOOL_FWVERS_LEN	= 32
ETHTOOL_BUSINFO_LEN	= 32
ETHTOOL_EROMVERS_LEN = 32

def GetCurrMS():
    return  int(round(time.time() *1000)) # Gives you float secs since epoch, so make it ms and chop


def ReadFromFile(Filename):
    try:
        file = open(Filename,'rt')
        if None == file:
            return "N/A"
    except Exception:
        return "N/A"

    retData = file.read() 
    file.close() #close ASAP
    retData = retData.strip()
    return retData

# in case we want to run this from a container and mount a dir
def GetBaseDir():
    global netdevInfoDir
    return netdevInfoDir

def SetBaseDir(dirLocation):
    global netdevInfoDir
    netdevInfoDir = dirLocation

def ParseListStr(listStr):
    return [5,3,5]


def match(strValue_1, strValue_2): 
  
    # If we reach at the end of both strings, we are done 
    if len(strValue_1) == 0 and len(strValue_2) == 0: 
        return True
  
    # Make sure that the characters after '*' are present 
    # in strValue_2 string. This function assumes that the strValue_1 
    # string will not contain two consecutive '*' 
    if len(strValue_1) > 1 and strValue_1[0] == '*' and  len(strValue_2) == 0: 
        return False
  
    # If the strValue_1 string contains '?', or current characters 
    # of both strings match 
    if (len(strValue_1) > 0 and strValue_1[0] == '?') or (len(strValue_1) != 0
        and len(strValue_2) !=0 and strValue_1[0] == strValue_2[0]): 
        return match(strValue_1[1:],strValue_2[1:]) 
  
    # If there is *, then there are two possibilities 
    # a) We consider current character of strValue_2 string 
    # b) We ignore current character of strValue_2 string. 
    if len(strValue_1) !=0 and strValue_1[0] == '*': 
        return match(strValue_1[1:],strValue_2) or match(strValue_1,strValue_2[1:]) 
  
    return False    

class NetworkInfo:
    def __init__(self,logInst,**kwargs):
        self.__args = kwargs
        self.__LOGGER = logInst
        self._sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, 0) #for ioctl
        self._usingDriverAndSysFS = False

        if False == self.__validateArgs():
            raise ValueError("Invalid Configuration for LinuxNetwork plugin")

    def __validateArgs(self):
        args = self.__args
        LOGGER = self.__LOGGER
        if not 'device' in args:
            self._deviceName = "Not Specified"
        else:
            self._deviceName = args['device']

        if 'slim' in args:
            self._slimDataSet = args['slim'].lower() == 'true' 
        else:
            self._slimDataSet = False

        if 'id' in args:
            self._id = args['id'] 
        else:
            self._id = self._deviceName

        if 'queues' in args:
            self.__queueList= ParseListStr(args['queues'])
            if None == self.__queueList:
                LOGGER.error("Invid queues specified: " + args['queues'])
                return False

        else:
            self.__queueList = None

        if 'vfs' in args:
            self.__vfList= ParseListStr(args['vfs'])
            if None == self.__vfList:
                LOGGER.error("Invid vfs specified: " + args['vfs'])
                return False

        else:
            self.__vfList = None

        if 'source' in args:
            srcStr = args['source'].upper()
            if srcStr == 'SYSFS':
                if None != self.__vfList or None != self.__queueList:
                    if None != self.__vfList:
                        LOGGER.error("source specified as sysfs - cannot get vf info using sysfs, change to driver")
                        return False

                    LOGGER.error("source specified as sysfs - cannot get queue info using sysfs, change to driver")
                    return False

                LOGGER.info("Reading Network info from " + GetBaseDir())
                self.__usingDriver = False

            elif srcStr == 'DRIVER':
                LOGGER.info("Reading Network info from driver" )
                self.__usingDriver = True
                
            elif "|" in srcStr: # could be a combo requested
                driverRequested=False
                sysFsRequested=False
                self.__usingDriver = False
                
                parts=srcStr.strip().split("|")
                
                for part in parts:
                    part= part.lstrip().rstrip()
                    if part.upper() == "DRIVER":
                        driverRequested = True
                        self.__usingDriver = True
                        
                    elif part.upper() == "SYSFS":
                        sysFsRequested = True
                        
                    else:
                        LOGGER.info("Invalid source specified: " + args['source'] )
                        return False
                    
                self._usingDriverAndSysFS = driverRequested and sysFsRequested
                        

            else:
                LOGGER.info("Invalid source specified: " + args['source'] )
                return False

        else:
            if None == self.__vfList and None == self.__queueList:
                LOGGER.info("source not specified, using data from  " + GetBaseDir())
                self.__usingDriver = False

            else:
                LOGGER.info("source not specified, using data from  driver ")
                self.__usingDriver = True

        self.__devList = self.__GetDeviceList()                
        #if 0 == len(self.__devList):
        #    LOGGER.error("No devices matching: " + self._deviceName )
        #    return False

        #LOGGER.info("Collecting for device(s): " + str(self.__devList))

        return True

    def __GetDeviceList(self):
        retList=[]
        #pylint: disable=unused-variable
        for root, dirs, files in os.walk(GetBaseDir()):
            for devName in dirs:
                if match(self._deviceName,devName):
                    retList.append(devName)

        return retList


    def GetStatistics(self):
        retMap={}
        if self.__usingDriver:
            retMap.update(self.__GetStatisticsFromDriver())
        
        if self._usingDriverAndSysFS or False == self.__usingDriver:
            retMap.update(self.__GetStatisticsFromSysFS())
        
        return retMap

    def __GetStatisticsFromSysFS(self):
        retMap = {}
        for ethDev in self.__devList:
            nextDir = GetBaseDir() + "/"  + ethDev + "/statistics"
            baseName='netdev.' + self._id
            #pylint: disable=unused-variable

            if not self._slimDataSet:
                for statRoot, statDirs, statFiles in os.walk(nextDir):
                    for fname in statFiles:
                        sFileName = nextDir + "/" + fname
                        dataVal = ReadFromFile(sFileName)
                        retMap[baseName + "." + fname] = dataVal

            else: # slim dataset, just to bytes and packets
                for fname in ['rx_bytes','tx_bytes','tx_packets','rx_packets']:
                    sFileName = nextDir + "/" + fname
                    dataVal = ReadFromFile(sFileName)
                    retMap[baseName + "." + fname] = dataVal


            if baseName+".rx_bytes" in retMap: # should ALWAYS be there
                retMap[baseName+".rx_gbps"] = retMap[baseName+".rx_bytes"]
                retMap[baseName+".tx_gbps"] = retMap[baseName+".tx_bytes"]
                retMap[baseName+".bx_gbps"] = str(float(retMap[baseName+".tx_bytes"]) + float(retMap[baseName+".rx_bytes"]))
                retMap[baseName+".rx_mbps"] = retMap[baseName+".rx_bytes"]
                retMap[baseName+".tx_mbps"] = retMap[baseName+".tx_bytes"]
                retMap[baseName+".bx_mbps"] = str(float(retMap[baseName+".tx_bytes"]) + float(retMap[baseName+".rx_bytes"]))

            if baseName+".tx_packets" in retMap:
                retMap[baseName+".tx_pps"] = retMap[baseName+".tx_packets"]
                retMap[baseName+".rx_pps"] = retMap[baseName+".rx_packets"]
                retMap[baseName+".bx_pps"] = str(float(retMap[baseName+".rx_packets"]) + float(retMap[baseName+".tx_packets"]))
                retMap[baseName+".tx_mpps"] = retMap[baseName+".tx_packets"]
                retMap[baseName+".rx_mpps"] = retMap[baseName+".rx_packets"]
                retMap[baseName+".bx_mpps"] = str(float(retMap[baseName+".rx_packets"]) + float(retMap[baseName+".tx_packets"]))

        return retMap

    def __GetStatisticsFromDriver(self):
        retMap = {}
        for ethDev in self.__devList:
            # go get the set strings
            baseName='netdev.' + ethDev

            # read the stat strings (differs from dev to dev :-( )
            strings = list(self.__getDriverStringSet(ethDev,ETH_SS_STATS))
            n_stats = len(strings)

            #go get the actual stats
            ethtool_stats_struct = array.array("B", struct.pack("II", ETHTOOL_GSTATS, n_stats))
            ethtool_stats_struct.extend(bytearray(struct.pack('Q', 0) * n_stats))
            self._send_ioctl(ethDev,ethtool_stats_struct)
            for i in range(n_stats):
                offset = 8 + 8 * i
                value = struct.unpack('Q', ethtool_stats_struct[offset:offset+8])[0]
                retMap[baseName + "." + strings[i]] = value

        return retMap

    def _send_ioctl(self, devName,data):
        sendData = struct.pack('16sP', devName.encode("utf-8"), data.buffer_info()[0])
        return fcntl.ioctl(self._sock.fileno(), SIOCETHTOOL, sendData)

    def __getDriverStringSet(self, devName,set_id):
        ## Get how many strings in this set
        ethtool_sset_info_struct = array.array('B', struct.pack("IIQI", ETHTOOL_GSSET_INFO, 0, 1 << set_id, 0))
        self._send_ioctl(devName,ethtool_sset_info_struct)

        set_mask, set_len = struct.unpack("8xQI", ethtool_sset_info_struct)
        if set_mask == 0:
            set_len = 0

        # Go get the strings for this set

        ethtool_gstrings_struct = array.array("B", struct.pack("III", ETHTOOL_GSTRINGS, set_id, set_len))

        #ethtool_gstrings_struct.extend(c'\x00' * int(set_len) * int(ETH_GSTRING_LEN))
        ethtool_gstrings_struct.extend(bytearray(int(set_len) * int(ETH_GSTRING_LEN)))
        self._send_ioctl(devName,ethtool_gstrings_struct)
        
        for index in range(set_len):
            offset = 12 + ETH_GSTRING_LEN * index
            statString = bytearray(ethtool_gstrings_struct[offset:offset+ETH_GSTRING_LEN]).partition(b'\x00')[0].decode("utf-8")
            yield statString
            
    def __GetLSPCIData(self):
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
        #pylint: disable=unused-variable
        except Exception as Ex:
            pass

        return lscpiDataMap

    def GetNetworkDeviceInformation(self,ethDev):
        baseName='netdev.' + ethDev
        retMap={}
        verFile = GetBaseDir() + "/"  + ethDev + "/device/driver/module/version"
        retMap[baseName + ".version"] = ReadFromFile(verFile)
        numaFile = GetBaseDir() + "/"  + ethDev + "/device/numa_node"
        retMap[baseName + ".numa_node"] = ReadFromFile(numaFile)
        retMap[baseName + ".vendor_info"] = self.__GetDeviceVendorInfo(ethDev)
        mtuFile = GetBaseDir() + "/"  + ethDev + "/mtu"
        retMap[baseName + ".mtu"] = ReadFromFile(mtuFile)
        operStateFile = GetBaseDir() + "/"  + ethDev + "/operstate"
        retMap[baseName + ".state"] = ReadFromFile(operStateFile)
        macAddrFile = GetBaseDir() + "/"  + ethDev + "/address"
        retMap[baseName + ".macaddress"] = ReadFromFile(macAddrFile)
        retMap[baseName + ".driver"] = self.__GetDriver(ethDev)
        speedFile = GetBaseDir() + "/"  + ethDev + "/speed"
        try:
            retMap[baseName + ".speed"] = ReadFromFile(speedFile)
        except:
            retMap[baseName + ".speed"] = "Unknown"
                    
    #sckt = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    #sckt.connect(("8.8.8.8", 80))
    #ipAddr =  sckt.getsockname()[0]
    #retMap[baseName + ".ipaddress"] = str(ipAddr)        

        queuesDir = GetBaseDir() + "/" + ethDev + "/queues"
        if os.path.isdir(queuesDir):
            queuCount = len(glob.glob1(queuesDir,"tx*"))
            retMap[baseName + ".queues"] = str(queuCount)                    

        return retMap

    def __GetDeviceVendorInfo(self,ethDev):
        try:
            linkStr=os.readlink(GetBaseDir() + "/"  + ethDev + "/device")
            parts = linkStr.split("/")
            busID= parts[-1]
            
            dataMap = self.__GetLSPCIData()
            if busID in dataMap:
                return dataMap[busID]

            ## some LSPCI dumps an abbreviated bus Id - fun!, so check for that
            for key in dataMap:
                if key in busID:
                    return dataMap[key]
        #pylint: disable=unused-variable
        except Exception as ex:
            pass

        return "Unknown Vendor Information"

    def GatherNetworkDeviceInfo(self,ethDev,retMap,slimDataset):
        nextDir = GetBaseDir() + "/"  + ethDev + "/statistics"
        baseName='netdev.' + ethDev
        if not slimDataset:
            #pylint: disable=unused-variable
            for statRoot, statDirs, statFiles in os.walk(nextDir):
                for fname in statFiles:
                    sFileName = nextDir + "/" + fname
                    dataVal = ReadFromFile(sFileName)
                    retMap[baseName + "." + fname] = dataVal

            #devInfo = self.GetNetworkDeviceInformation(ethDev)
            #retMap.update(devInfo)
            
            # verFile = GetBaseDir() + "/"  + ethDev + "/device/driver/module/version"
            # retMap[baseName + ".version"] = ReadFromFile(verFile)
            # numaFile = GetBaseDir() + "/"  + ethDev + "/device/numa_node"
            # retMap[baseName + ".numa_node"] = ReadFromFile(numaFile)
            # retMap[baseName + ".vendor_info"] = self.__GetDeviceVendorInfo(ethDev)
            # mtuFile = GetBaseDir() + "/"  + ethDev + "/mtu"
            # retMap[baseName + ".mtu"] = ReadFromFile(mtuFile)
            # operStateFile = GetBaseDir() + "/"  + ethDev + "/operstate"
            # retMap[baseName + ".state"] = ReadFromFile(operStateFile)
            # macAddrFile = GetBaseDir() + "/"  + ethDev + "/address"
            # retMap[baseName + ".macaddress"] = ReadFromFile(macAddrFile)
            # retMap[baseName + ".driver"] = self.__GetDriver(ethDev)
            # speedFile = GetBaseDir() + "/"  + ethDev + "/speed"
            # try:
            #     retMap[baseName + ".speed"] = ReadFromFile(speedFile)
            # except:
            #     retMap[baseName + ".speed"] = "Unknown"
                            
            # sckt = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            # sckt.connect(("8.8.8.8", 80))
            # ipAddr =  sckt.getsockname()[0]
            # retMap[baseName + ".ipaddress"] = str(ipAddr)

        else: # slim dataset, just to bytes and packets
            for fname in ['rx_bytes','tx_bytes','tx_packets','rx_packets']:
                sFileName = nextDir + "/" + fname
                dataVal = ReadFromFile(sFileName)
                retMap[baseName + "." + fname] = dataVal

        if baseName+".rx_bytes" in retMap: # should ALWAYS be there
            retMap[baseName+".rx_gbps"] = retMap[baseName+".rx_bytes"]
            retMap[baseName+".tx_gbps"] = retMap[baseName+".tx_bytes"]
            retMap[baseName+".bx_gbps"] = str(float(retMap[baseName+".tx_bytes"]) + float(retMap[baseName+".rx_bytes"]))
            retMap[baseName+".rx_mbps"] = retMap[baseName+".rx_bytes"]
            retMap[baseName+".tx_mbps"] = retMap[baseName+".tx_bytes"]
            retMap[baseName+".bx_mbps"] = str(float(retMap[baseName+".tx_bytes"]) + float(retMap[baseName+".rx_bytes"]))

        if baseName+".tx_packets" in retMap:
            retMap[baseName+".tx_pps"] = retMap[baseName+".tx_packets"]
            retMap[baseName+".rx_pps"] = retMap[baseName+".rx_packets"]
            retMap[baseName+".bx_pps"] = str(float(retMap[baseName+".rx_packets"]) + float(retMap[baseName+".tx_packets"]))
            retMap[baseName+".tx_mpps"] = retMap[baseName+".tx_packets"]
            retMap[baseName+".rx_mpps"] = retMap[baseName+".rx_packets"]
            retMap[baseName+".bx_mpps"] = str(float(retMap[baseName+".rx_packets"]) + float(retMap[baseName+".tx_packets"]))
            

        return retMap

    def __GetDriver(self,device):
        link = 	GetBaseDir() + '/' +device + '/device/driver/module/drivers'
        #pylint: disable=unused-variable
        for root, driver, files in os.walk(link):
            driver = driver[0]
            if None != driver[0] and ':' in driver:
                parts = driver.split(':')
                return parts[1]
            return None

    def __IsPhysicalDevice(self,device):
        return None != self.__GetDriver(device)

    def GatherAllNetworkDeviceInfo(self,slimDataSet,pyhysicalOnly=True):
        tMap={}
        #pylint: disable=unused-variable
        for root, dirs, files in os.walk(GetBaseDir()):
            for dir in dirs:
                if False == pyhysicalOnly or self.__IsPhysicalDevice(dir):
                    tMap= self.GatherNetworkDeviceInfo(dir,tMap,slimDataSet)
            
        return tMap

def CollectInfoForDevice(frameworkInterface,DeviceName):
    global Logger
    Logger = frameworkInterface.Logger
    try:
        objNetInfo = NetworkInfo(Logger,device=DeviceName,source="sysfs")    

        dataMap = objNetInfo.GetNetworkDeviceInformation(DeviceName)
        for entry in dataMap:
            if not frameworkInterface.DoesCollectorExist(entry): # Do we already have this ID?
                frameworkInterface.AddCollector(entry)    # Nope, so go add it
                    
            frameworkInterface.SetCollectorValue(entry,dataMap[entry]) 

    except Exception as ex:
        Logger.error("Unrecoverable error in LinuxNetwork Collector plugin: " + str(ex))


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
        objNetInfo = NetworkInfo(Logger)
        while not frameworkInterface.KillThreadSignalled():
            dataMap = objNetInfo.GatherAllNetworkDeviceInfo(slimDataSet,physicalOnly)
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
        objNetInfo = NetworkInfo(Logger,device=DeviceName,source="sysfs")
        while not frameworkInterface.KillThreadSignalled():
            dataMap={}
            objNetInfo.GatherNetworkDeviceInfo(DeviceName,dataMap,slimDataSet)
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


def CollectDeviceStatistics(frameworkInterface,**kwargs): 

    Logger = frameworkInterface.Logger
    Logger.info("Starting LinuxNetwork CollectDeviceStatistics Collector {0}, collecting single Device: {1}".format(VersionStr,kwargs))

    dataMap ={}
    SleepTime = float(frameworkInterface.Interval)/1000.0
    
    while not frameworkInterface.KillThreadSignalled() and not dataMap:
        # in case driver is NOT loaded
        objNetInfo = NetworkInfo(Logger,**kwargs)

        dataMap = objNetInfo.GetStatistics()
        if not dataMap: #empty dict evaluates to False
            time.sleep(SleepTime)
        
    if frameworkInterface.KillThreadSignalled():
        return "HelenKeller"

    for entry in dataMap:
        if not frameworkInterface.DoesCollectorExist(entry): # Do we already have this ID?
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

    if None == frameworkInterface.KillThreadSignalled: # is not in its own thread
        return "HelenKeller"


    while not frameworkInterface.KillThreadSignalled():
        try:
            time.sleep(SleepTime)
            dataMap = objNetInfo.GetStatistics()

            for entry in dataMap:
                frameworkInterface.SetCollectorValue(entry,dataMap[entry]) 

        except Exception as ex:
            frameworkInterface.Logger.error("Unrecoverable error in LinuxNetwork Collector plugin: " + str(ex))
            


##############################################################################
#  Returns the information down in /proc/net/softnet_stat, in lists
#  Example usage, that normalizes to per ke packets per core:
# <DynamicCollector>
#   <Plugin>
#      <PythonFile>Collectors/LinuxNetwork.py</PythonFile>
#		   <EntryPoint SpawnThread="False">GetPerCorePacketsList</EntryPoint>
#	   </Plugin>
#   <Normalize>0.001</Normalize>
# </DynamicCollector>
def GetPerCorePacketsList(frameworkInterface):
    if not frameworkInterface.DoesCollectorExist("softnet.processed"):
        Logger = frameworkInterface.Logger
        Logger.info("Starting LinuxNetwork GetPerCorePacketsList Collector {0}".format(VersionStr))

    procNetStats = ReadFromFile("/proc/net/softnet_stat")
    statList = procNetStats.split('\n')
    processedList=[]
    droppedList=[]
    time_squeezeList=[]
    cpu_collisionList=[]
    received_rpsList=[]
    flow_limit_countList=[]
    olderKernel = False
    #pylint: disable=unused-variable
    for coreNum,line in enumerate(statList):
        if True == olderKernel:
            processed,dropped,time_squeeze,_,_,_,_,_,cpu_collision,received_rps = line.split(" ")
            flow_limit_count = "0"
            
        else: #older kernels do not have flow_limit_count
            try:
                processed,dropped,time_squeeze,_,_,_,_,_,cpu_collision,received_rps,flow_limit_count = line.split(" ")
                
            except:
                print(line)
                processed,dropped,time_squeeze,_,_,_,_,_,cpu_collision,received_rps = line.split(" ")
                flow_limit_count = "0"
                olderKernel = True
                
        processedList.append(str(int(processed,16)))
        droppedList.append(str(int(dropped,16)))
        time_squeezeList.append(str(int(time_squeeze,16)))
        cpu_collisionList.append(str(int(cpu_collision,16)))
        received_rpsList.append(str(int(received_rps,16)))
        flow_limit_countList.append(str(int(flow_limit_count,16)))
    
    if not frameworkInterface.DoesCollectorExist("softnet.processed"):
        frameworkInterface.AddCollector("softnet.processed")
        frameworkInterface.AddCollector("softnet.dropped")
        frameworkInterface.AddCollector("softnet.time_squeeze")
        frameworkInterface.AddCollector("softnet.cpu_collision")
        frameworkInterface.AddCollector("softnet.received_rps")
        frameworkInterface.AddCollector("softnet.flow_limit_count")
       
    frameworkInterface.SetCollectorValue("softnet.processed",','.join(processedList))
    frameworkInterface.SetCollectorValue("softnet.dropped",','.join(droppedList))
    frameworkInterface.SetCollectorValue("softnet.time_squeeze",','.join(time_squeezeList))
    frameworkInterface.SetCollectorValue("softnet.cpu_collision",','.join(cpu_collisionList))
    frameworkInterface.SetCollectorValue("softnet.received_rps",','.join(received_rpsList))
    frameworkInterface.SetCollectorValue("softnet.flow_limit_count",','.join(flow_limit_countList))
    
def IpRoute2GatherVfStatsForPF(frameworkInterface,pfName):
    cmdAndParams=['ip', '-s', '-s', 'link', 'show', pfName]
    data = subprocess.check_output(cmdAndParams).splitlines()

    currVfNum=None
    nextLine=None
    dataMap={}
    for line in data:
        line = line.decode('utf-8')
        line = line.strip() # get rid of leading/trailing stuff
        if None != currVfNum:
            if nextLine=='TX' or nextLine=='RX':
                rawParts=line.split(' ')
                parts=[]
                for part in rawParts:
                    if part=='':
                        pass
                    else:
                        parts.append(part)
                
                dataMap["{}.vf.{}.{}.bytes".format(pfName,currVfNum,nextLine)] = parts[0]
                dataMap["{}.vf.{}.{}.packets".format(pfName,currVfNum,nextLine)] = parts[1]
                if nextLine=='RX':
                    dataMap["{}.vf.{}.{}.mcast".format(pfName,currVfNum,nextLine)] = parts[2]
                    dataMap["{}.vf.{}.{}.bcast".format(pfName,currVfNum,nextLine)] = parts[3]
                    dataMap["{}.vf.{}.{}.dropped".format(pfName,currVfNum,nextLine)] = parts[4]
                if nextLine=='TX':
                    dataMap["{}.vf.{}.{}.dropped".format(pfName,currVfNum,nextLine)] = parts[2]

            if 'RX: bytes' in line:
                nextLine='RX'
            elif 'TX: bytes' in line:
                nextLine='TX'
            else:
                nextLine=None

        if 'vf ' in line:
            parts=line.split(' ')
            currVfNum=parts[1]
        
        for entry in dataMap:
            if not frameworkInterface.DoesCollectorExist(entry): # Do we already have this ID?
                frameworkInterface.AddCollector(entry)    # Nope, so go add it
                    
            frameworkInterface.SetCollectorValue(entry,dataMap[entry]) 