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
#    This attempts to get CPU utilization information on a Linux System
#
##############################################################################

from time import sleep
import sys
import os

procInfoDir="/sys/devices/system/cpu"
desiredFreqStats=["cpuinfo_min_freq","scaling_driver","energy_performance_preference","cpuinfo_max_freq","cpuinfo_cur_freq","scaling_cur_freq","scaling_governor","scaling_available_governors"]

def GetBaseDir():
    global procInfoDir
    return procInfoDir


def ReadFromFile_Legacy(Filename):
    try:
        file = open(Filename,'rt')
        if None == file:
            return "N/A"
    except Exception:
        return "N/A"

    return file.read().strip()

def ReadFromFile(Filename):
    file = open(Filename,'rt')
    if None == file:
        return "File [" + Filename + "] does not exist"

    lines = [line.rstrip('\n') for line in open(Filename)]
    return lines

#Linux 3.10.0-229.el7.x86_64 (csx-61) 09/07/2015 _x86_64_ (16 CPU)

#10:26:21 AM CPU %usr %nice %sys %iowait %irq %soft %steal %guest %gnice %idle
#10:26:21 AM all 1.98 0.00 0.24 0.02 0.00 0.01 0.00 0.00 0.00 97.74
#10:26:21 AM 0 0.35 0.00 0.22 0.22 0.00 0.01 0.00 0.00 0.00 99.19
#10:26:21 AM 1 0.77 0.01 0.10 0.00 0.00 0.00 0.00 0.00 0.00 99.12
#10:26:21 AM 2 0.46 0.01 0.08 0.01 0.00 0.00 0.00 0.00 0.00 99.44
#10:26:21 AM 3 0.74 0.01 0.09 0.00 0.00 0.00 0.00 0.00 0.00 99.16
#10:26:21 AM 4 0.49 0.01 0.08 0.01 0.00 0.00 0.00 0.00 0.00 99.41
#10:26:21 AM 5 0.94 0.01 0.10 0.01 0.00 0.00 0.00 0.00 0.00 98.94
#10:26:21 AM 6 0.49 0.01 0.09 0.01 0.00 0.00 0.00 0.00 0.00 99.40
#10:26:21 AM 7 1.31 0.01 0.11 0.01 0.00 0.00 0.00 0.00 0.00 98.56
#10:26:21 AM 8 0.31 0.00 0.05 0.00 0.00 0.00 0.00 0.00 0.00 99.64
#10:26:21 AM 9 0.38 0.00 0.05 0.00 0.00 0.00 0.00 0.00 0.00 99.56
#10:26:21 AM 10 0.29 0.01 0.12 0.00 0.00 0.00 0.00 0.00 0.00 99.58
#10:26:21 AM 11 0.32 0.01 0.13 0.00 0.00 0.00 0.00 0.00 0.00 99.53
#10:26:21 AM 12 0.26 0.00 0.15 0.00 0.00 0.00 0.00 0.00 0.00 99.58
#10:26:21 AM 13 24.13 0.00 2.45 0.01 0.00 0.11 0.00 0.00 0.00 73.31
#10:26:21 AM 14 0.21 0.00 0.05 0.00 0.00 0.00 0.00 0.00 0.00 99.73
#10:26:21 AM 15 0.31 0.00 0.05 0.00 0.00 0.00 0.00 0.00 0.00 99.64
def TokenizeRow(row):
    columnsRaw = row.split()
    retList = columnsRaw[2:len(columnsRaw)]
    return retList
    
def CreatePerfFileFromMPStatFile(inputFile,outputFile):
    rawData = ReadFromFile(inputFile)
    columnHeaders = []
    startFound = False

    writeData = ""

    for row in rawData:
        if not startFound:
            if row.find("usr") > 0:
                print(row)
                startFound = True
                columnHeaders = TokenizeRow(row)
        else:
            columns = TokenizeRow(row)
            
            cpuID = "CPU." + columns[0]
            for loop in range(1,len(columns)):
                writeData += cpuID + "." + columnHeaders[loop] + columns[loop] + os.linesep
                pass


    file = open(outputFile,"wt")
    file.write(writeData)
    file.close()
    return "HelenKeller" # don't want to send anything

#Worker class to get cpu load info
class CPULoad(object):
    def __init__(self, period = 1):
        self._period = period #how long to wait when getting 2nd reading

    def calcCPU_Time(self):

        cpu_infoMap = {} 
        with open('/proc/stat','r') as fpStats:
            lines = [line.split(' ') for content in fpStats.readlines() for line in content.split('\n') if line.startswith('cpu')]

            for cpu_line_list in lines:
                if '' in cpu_line_list: cpu_line_list.remove('')
                cpu_line_list = [cpu_line_list[0]]+[float(i) for i in cpu_line_list[1:]]
                #pylint: disable=unused-variable
                cpu_id,user,nice,system,idle,iowait,irq,softrig,steal,guest,guest_nice = cpu_line_list

                Idle=idle+iowait
                NonIdle=user+nice+system+irq+softrig+steal

                Total=Idle+NonIdle
                
                cpu_infoMap.update({cpu_id:{'total':Total,'idle':Idle}})
            return cpu_infoMap

    def getcpuload(self):
        # Remebmer that Percentage=((Total-PrevTotal)-(Idle-PrevIdle))/(Total-PrevTotal)
        # read 1
        start = self.calcCPU_Time()
        #snooze a bit
        sleep(self._period)
        #read 2
        stop = self.calcCPU_Time()

        cpu_load_List = {}

        for cpu in start:
            Total = stop[cpu]['total']
            PrevTotal = start[cpu]['total']

            Idle = stop[cpu]['idle']
            PrevIdle = start[cpu]['idle']
            CPU_Percentage=((Total-PrevTotal)-(Idle-PrevIdle))/(Total-PrevTotal)*100
            cpu_load_List.update({cpu: CPU_Percentage})
        return cpu_load_List

def ReadProcStats():
    dataMap = {} #collect here the information
    with open('/proc/stat','r') as f_stat:
        lines = [line.split(' ') for content in f_stat.readlines() for line in content.split('\n') if line.startswith('cpu')]

        #compute for every cpu
        for cpu_line in lines:
            if '' in cpu_line: cpu_line.remove('')#remove empty elements
            cpu_id,user,nice,system,idle,iowait,irq,softriq,steal,guest,guest_nice = cpu_line
            if cpu_id == 'cpu':
                cpu_id = 'total'
            dataMap['cpu.'+cpu_id+'.user'] = user
            dataMap['cpu.'+cpu_id+'.nice'] = nice
            dataMap['cpu.'+cpu_id+'.system'] = system
            dataMap['cpu.'+cpu_id+'.idle'] = idle
            dataMap['cpu.'+cpu_id+'.iowait'] = iowait
            dataMap['cpu.'+cpu_id+'.irq'] = irq
            dataMap['cpu.'+cpu_id+'.softirq'] = softriq
            dataMap['cpu.'+cpu_id+'.steal'] = steal
            dataMap['cpu.'+cpu_id+'.guest'] = guest
            dataMap['cpu.'+cpu_id+'.guest_nice'] = guest_nice

    return dataMap

# uses the fact that /proc/stat has the desired information.  pass target file to put
# the desired data, the period to sample stats over and precision level if desired
def CreateCPUUtilFileFromProcStats(targetFile,interval=.1,precision=2):
    interval=float(interval)
    precision=float(precision)

    x = CPULoad(interval)  # use this cool script I found online
    strPrecision = '.' + str(int(precision)) + 'f'
    data = x.getcpuload()
    writeData = "CPU_COUNT=" + str(len(data) -1 ) + os.linesep # also has overall CPU in there
    for proc in data:
        writeData+= proc +"="+str(format(data[proc],strPrecision)) + os.linesep

    file = open(targetFile,"wt")
    file.write(writeData)
    file.close()
    return "HelenKeller" # don't want to send anything

# create a comma separated list with all cores and nothign else
# returnTuple will return the raw cpu load data too
def CreateUtilizationList(interval=.1,precision=2,returnTuple=False):
    interval=float(interval)
    precision=float(precision)

    cpuInfo = CPULoad(interval)  # use this cool script I found online
    strPrecision = '.' + str(int(precision)) + 'f'
    data = cpuInfo.getcpuload()
    coreCount = len(data) -1
    first = True
    
	# data is stored in has, with key being cpu#
    for index in range(0,coreCount):
        key = "cpu" + str(index)
        cpuVal = str(format(data[key],strPrecision))
        if True == first:
            writeData = cpuVal
            first = False
        else:
            writeData+= "," + cpuVal

    if True == returnTuple:
        return (writeData,data)

    return writeData
    
def getFrequencyInfo(prefix=""):
    retMap={}
    coreCount=0
    for cpuDir in os.listdir(GetBaseDir()):
            if not 'cpu' in cpuDir:
                continue
            
            if cpuDir in ['cpufreq','cpuidle']: #don't want these directories
                continue
                
            coreCount+=1

            nextDir = GetBaseDir() + "/"  + cpuDir + "/cpufreq"
            #pylint: disable=unused-variable
            for statRoot, statDirs, statFiles in os.walk(nextDir):
                for file in statFiles:
                    if file in desiredFreqStats:
                        readFile = GetBaseDir() + "/"  + cpuDir + "/cpufreq/" + file
                        key = "{0}.{1}".format(cpuDir,file)
                        
                        retMap[prefix+key] = ReadFromFile_Legacy(readFile)
                        

    freqList=None
    # create a comma separated list for graphing
    if "cpu0.cpuinfo_cur_freq" in retMap:
        freqKey = "cpuinfo_cur_freq"

    else:
        freqKey = "scaling_cur_freq"


    for coreNum in range(0,coreCount): # to sort in right order -dir walk not going to do it
        key = "cpu{0}.{1}".format(coreNum,freqKey)

        if None == freqList: #1st one
            try:
                freqList=retMap[prefix+key]
            except: # nothing there, so likely in a VM, so spoof it
                freqList = "0"
                for cn in range(1,coreCount):
                    freqList += ",0"
                break

        else:
            freqList += ","+retMap[prefix+key]
            
    #retMap[prefix+"cpu_frequency_list"] = freqList
    
    return freqList
    

def GetSystemAverageCPU(interval=.1,precision=2):
    interval=float(interval)
    precision=float(precision)

    cpuInfo = CPULoad(interval)  # use this cool script I found online
    strPrecision = '.' + str(int(precision)) + 'f'
    data = cpuInfo.getcpuload()
    coreCount = len(data) -1

    total = 0.0
    for index in range(0,coreCount):
        key = "cpu" + str(index)
        cpuVal = data[key]
        
        total += cpuVal
    
    total = total/coreCount
    
    return str(format(total,strPrecision))

def GetCoreCount():
    coreCount = 0
    for cpuDir in os.listdir(GetBaseDir()):
            if not 'cpu' in cpuDir:
                continue
            
            if cpuDir in ['cpufreq','cpuidle']: #don't want these directories
                continue
                
            coreCount+=1
            
    return coreCount

## Dynamic Collector interface, gets all raw stats
def CollectStatsFunction(frameworkInterface):
    dataMap = ReadProcStats()

    for collectorID in dataMap:
        if not frameworkInterface.DoesCollectorExist(collectorID):
            frameworkInterface.AddCollector(collectorID)    

        frameworkInterface.SetCollectorValue(collectorID,dataMap[collectorID]) 

#if __name__=='__main__':
#    CreateCPUUtilFileFromProcStats("foo.txt",.1,4)

