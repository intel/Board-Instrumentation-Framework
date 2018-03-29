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
#    This attempts to get info from an ESXi host.
#
##############################################################################
from __future__ import print_function
from  pprint import pprint
import datetime
import atexit
import time

try:
    import pyVmomi
    from pyVmomi import vim, vmodl
    from pyVim import connect
    from pyVim.connect import SmartConnect, Disconnect
    LoadSuccessful = True
except ImportError:
    LoadSuccessful = False


def GetHostInfo(host):
    summary = host.summary

    #print(host.name)
     
    retMap={}
    retMap['esx.host.hostName'] = host.name
    retMap['esx.host.numCpuCores'] = summary.numCpuCores
    retMap['esx.host.numCpuCores'] = summary.numCpuCores
    retMap['esx.host.numCpuThreads'] = summary.numCpuThreads
    retMap['esx.host.status'] = summary.overallStatus
    
    return retMap

def ListAvailablePerfStats(perfManager):
    for counter in perfManager.perfCounter:
        groupInfo = counter.groupInfo
        nameInfo = counter.nameInfo
        unitInfo = counter.unitInfo
        print("{0} {1} {2} {3}".format(groupInfo.label,nameInfo.key,unitInfo.label,counter.key))
        if groupInfo.label == "CPU":
            pass
            #print(counter)
        if groupInfo.label == "Memory":
           pass
           #return
        
#        print(dir(counter))
        

def GetCPU_Util(computeHost,perfManager):
    startTime = datetime.datetime.now() - datetime.timedelta(days=1)
    endTime = datetime.datetime.now() 

    metricId = vim.PerformanceManager.MetricId(counterId=16, instance="*")
    
    query = vim.PerformanceManager.QuerySpec(maxSample=1,
                             entity=computeHost.host[0],
                             metricId=[metricId],format = "csv",
                             startTime=startTime
                             )#,
                             #endTime=endTime)						
                   
    perfData = perfManager.QueryPerf(querySpec=[query])
    while len(perfData) == 0:
        return None
        
    coreCount = len(perfData[0].value) -1
    coreValues=[]
    total = 0.0
    for x in range(0,coreCount):
        coreValues.append(-99990)
    
    for counter in perfData[0].value:
        if len(counter.id.instance) > 0:
            coreNum = int(counter.id.instance)
            usage = float(counter.value)/100.0
            #print("{0}:{1}".format(coreNum,usage))
            coreValues[coreNum] = usage
            total += usage
    
    retStr=None
    
    for entry in coreValues:
        if None == retStr:
            retStr = str(entry)
        else:
            retStr += ","+str(entry)
        
    total /= coreCount
    retMap={}
    retMap['esx.host.CPU.List'] = retStr
    retMap['esx.host.CPU.Average'] = "{0:.2f}".format(total)
    return retMap

def CollectESX(frameworkInterface,esxHost,esxPort,name,password):
    Logger = frameworkInterface.Logger
    if False == LoadSuccessful:
         Logger.error("esxHost Collector requires the pyvmomi library.  (https://github.com/vmware/pyvmomi) -  pip install pyvmomi")
         return False

    esxPort = int(esxPort)
    try:
        myConnection = connect.ConnectNoSSL(esxHost, esxPort, name, password)
    except Exception as Ex:
         Logger.error("esxHost Collector unable to login to {0} : {1}".format(esxHost,str(Ex)))
    atexit.register(Disconnect, myConnection)
    content = myConnection.RetrieveContent()
    perfManager = content.perfManager

    SleepTime = float(frameworkInterface.Interval)/1000.0

    try:    
        for datacenter in content.rootFolder.childEntity:
            if hasattr(datacenter.vmFolder, 'childEntity'):
                hostFolder = datacenter.hostFolder
                computeResourceList = hostFolder.childEntity
                server = computeResourceList[0]
                break
                #for server in computeResourceList:
        hostInfo = GetHostInfo(server)
        for entry in hostInfo:
            if not frameworkInterface.DoesCollectorExist(entry): # Do we already have this ID?
                frameworkInterface.AddCollector(entry)    # Nope, so go add it
                        
            frameworkInterface.SetCollectorValue(entry,hostInfo[entry]) 


        while not frameworkInterface.KillThreadSignalled():
            cpuUsage = GetCPU_Util(server,perfManager)
            if None != cpuUsage:
                for entry in cpuUsage:
                    if not frameworkInterface.DoesCollectorExist(entry): # Do we already have this ID?
                        frameworkInterface.AddCollector(entry)    # Nope, so go add it
                        
                    frameworkInterface.SetCollectorValue(entry,cpuUsage[entry]) 

            time.sleep(SleepTime)


    except Exception as ex:
        Logger.error("Unrecoverable error in esxHost Collector plugin: " + str(ex))



def GoGather(esxHost, esxPort, name, password):
    try:
        myConnection = connect.ConnectNoSSL(esxHost, esxPort, name, password)
    except Exception as Ex:
         print("esxHost Collector unable to login to {0} : {1}".format(esxHost,str(Ex)))
         return
    
    atexit.register(Disconnect, myConnection)
    content = myConnection.RetrieveContent()
    perfManager = content.perfManager
    #ListAvailablePerfStats(perfManager)
    try:
        for datacenter in content.rootFolder.childEntity:
            if hasattr(datacenter.vmFolder, 'childEntity'):
                hostFolder = datacenter.hostFolder
                computeResourceList = hostFolder.childEntity
                server=computeResourceList[0]

        while True:
            hostInfo = GetHostInfo(server)
            print(hostInfo)
            cpuUsage = GetCPU_Util(server,perfManager)
            print(cpuUsage)

    except Exception as Ex:
        print(str(Ex))    


def main():
    GoGather("10.166.31.99",443,"root","1qazXSW@")

#if __name__ == "__main__":
#    main()
