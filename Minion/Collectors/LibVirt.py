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
#    Gathers some data from Libvirt, as a Minion plugin
#
##############################################################################
from __future__ import print_function
import sys
from pprint import pprint
import time
import libvirt

_connection=None
lvConnectionPoint='qemu:///system'

DomainMap={}
Logger=None

def getConnection():
	global _connection,lvConnectionPoint

	if None == _connection:
		_connection = libvirt.open(lvConnectionPoint)
	
	return _connection
	
## some nice code I found online for this fn, however I lost the link where I found it
## if I find it again, sill site source
def libvirt_error_handler(ctx, err):
    global Logger

    if None == Logger:
       return
    Logger.error('Error code:    '+str(err[0]), file=sys.stderr)
    Logger.error('Error domain:  '+str(err[1]), file=sys.stderr)
    Logger.error('Error message: '+err[2], file=sys.stderr)
    Logger.error('Error level:   '+str(err[3]), file=sys.stderr)
    if err[4] != None:
        Logger.error('Error string1: '+err[4], file=sys.stderr)
    else:
        Logger.error('Error string1:', file=sys.stderr)
    if err[5] != None:
        Logger.error('Error string2: '+err[5], file=sys.stderr)
    else:
        Logger.error('Error string2:', file=sys.stderr)
    if err[6] != None:
        Logger.error('Error string3: '+err[6], file=sys.stderr)
    else:
        Logger.error('Error string3:', file=sys.stderr)
    Logger.error('Error int1:    '+str(err[7]), file=sys.stderr)
    Logger.error('Error int2:    '+str(err[8]), file=sys.stderr)
    exit(1)	
    

def getPlatformInfo():
    conn = getConnection()
    nodeinfo = conn.getInfo()
    SystemMap={}

    SystemMap["libvirt.system.cpu_model"] = str(nodeinfo[0])
    SystemMap["libvirt.system.memory_size"] = str(nodeinfo[1])
    SystemMap["libvirt.system.cpu_count"] = str(nodeinfo[2])
    SystemMap["libvirt.system.cpu_speed"] = str(nodeinfo[3])
    SystemMap["libvirt.system.numa_nodes"] = str(nodeinfo[4])
    SystemMap["libvirt.system.cpu_sockets"] = str(nodeinfo[5])
    SystemMap["libvirt.system.cpu_cores_per_socket"] = str(nodeinfo[6])
    SystemMap["libvirt.system.cpu_threads_per_core"] = str(nodeinfo[7])

    return SystemMap

def getCPUInfo():
	conn = getConnection()
	stats = conn.getCPUStats(0)

	print("kernel: " + str(stats['kernel']))
	print("idle:   " + str(stats['idle']))
	print("user:   " + str(stats['user']))
	print("iowait: " + str(stats['iowait']))
	
	#stats = conn.getCPUStats(VIR_NODE_CPU_STATS_ALL_CPUS)
	#print(stats)
	
def dumpObject(obj):
		print(dir(obj))
		
def dumpMap(map):
	for key in sorted(map):
		print(key +": " + str(map[key]))
	
#info on specific domain (VM)
def getDomainInfo(domain):
    conn = getConnection()
    infoMap={}
    info = domain.info()
    name = "libvirt.domain."+ domain.name()+"." 
    if 1 == info[0]:
        ActiveStr = "Active"
    else:
        ActiveStr = "Inactive"

    infoMap[name + "domain_id"]=domain.ID()
    infoMap[name + "domain_uuid"]=domain.UUIDString()
    infoMap[name + "state"]=ActiveStr
    infoMap[name + "max_memory"]=str(info[1]/1024)
    infoMap[name + "vcpu_count"]=str(info[3])
    try:
        if "getTime" in dir(domain):
            timeStruct = domain.getTime()
            if None != timeStruct:
                timestamp = time.ctime(float(timeStruct['seconds']))
                infoMap[name + "current_time"]=str(timestamp)
    except Exception:
        pass # python may support the getTime fn, but internally blow up I found

    try:
        infoMap[name + "hostname"]=domain.hostname()
    except Exception:
        pass # python libvirt may not support the hostname fn

    return infoMap

def updateDomainCPU_Pinning(domain):
    global DomainMap
    pinStr=""
    for core in domain.vcpus()[0]:
        if len(pinStr) < 1:
            pinStr = str(core[3])
        else:
            pinStr += "," + str(core[3])

    DomainMap[domain.name()]["libvirt.domain."+domain.name() + ".cpu_pin"]=pinStr		


def getDomainsInfo():
    global DomainMap
    conn = getConnection()

    domainIDs = conn.listAllDomains()
    if domainIDs == None:
        return False

    if len(domainIDs) == 0:
        return False
    else:
        for domain in domainIDs:
            DomainMap[domain.name()]=getDomainInfo(domain)
            if domain.isActive():  #maybe just do all...
                updateDomainCPU_Pinning(domain)

def getCoreTimes(domainID):
    conn = getConnection()
    try:
        domain = conn.lookupByName(domainID)
    except Exception:
        return None
    
    if not domain.isActive():
        return None

    retList=[]
    retList.append(int(round(time.time() * 1000)))
    cpu_stats = domain.vcpus()
    info = domain.info()
    cpuTimes=[]
    for entry in cpu_stats[0]:
        cpuTimes.append(entry[2]) #individual cores

    retList.append(cpuTimes)
    retList.append(info[4]/info[3]) #overall CPU Utilization

    return retList

def getCPU_Utilization(timePeriod):
    mapStart={}
    global DomainMap
    for domainID in DomainMap:
        mapStart[domainID] = getCoreTimes(domainID)

    time.sleep(timePeriod)

    for domainID in DomainMap:
        if mapStart[domainID] == None:  # means it wasn't active
            continue
        endTime,endCPU,endAvg = getCoreTimes(domainID)
        startTime,startCPU,startAvg = mapStart[domainID]

        timeDelta = endTime - startTime
        utilList=""
        for index,cpu in enumerate(startCPU):
            cpuDelta = endCPU[index] - cpu
            util = 100 * cpuDelta/(timeDelta *1000000)
            if len(utilList)<1:
                utilList = str(util)
            else:
                utilList += ","+str(util)

        DomainMap[domainID]["libvirt.domain."+domainID+".cpu_util_list"] = utilList
        cpuDelta = endAvg - startAvg
        util = 100 * cpuDelta/(timeDelta *1000000)		
        DomainMap[domainID]["libvirt.domain."+domainID+".cpu_util"] = str(util)
	    #dumpMap(DomainMap[domainID])

def GatherSetupInfo(ConnectPoint):
    global lvConnectionPoint
    lvConnectionPoint = ConnectPoint
    try:
        AliasMap={}
        ctx = 'dummy data, not used'
        libvirt.registerErrorHandler(libvirt_error_handler, ctx)			

        SystemMap = getPlatformInfo() # does not change, no need to do in a loop
        getDomainsInfo()                          
        AliasMap['libvirt.system.cpu_count'] = SystemMap["libvirt.system.cpu_count"]
        for index,DomainID in enumerate(sorted(DomainMap)):
            AliasMap['libvirt.domain.'+DomainID+".vcpu_count"] = DomainMap[DomainID]['libvirt.domain.'+DomainID+".vcpu_count"]
            AliasMap['libvirt-domain-' + str(index+1)]=DomainID

        AliasMap['libvirt.domain_count'] = str(len(DomainMap))
        return AliasMap
    
    except Exception as Ex:
        raise Exception("LibVirt GatherSetupInfo encountered problem: " + str(Ex))



# this is the entry point.  Makes JSON calls to OVSDB, makes collectors
# out of the responses
def LibVirt_Collector(frameworkInterface, ConnectPoint): 
    # frameworkInterface has the following:
    #  frameworkInterface.DoesCollectorExist(ID) # does a collector with ID  already exist
    #  frameworkInterface.AddCollector(ID) # Add a new collectr
    #  frameworkInterface.SetCollectorValue(ID,Value,ElapsedTime) # Assign the  collector a new value, along with how long since last update
    #  frameworkInterface.KillThreadSignalled() # returns True if signalled to end your worker thread, else False
    #  frameworkInterface.LockFileName() # lockfile name for dynamic collector,  if specified
    #  frameworkInterface.Interval() # the frequency from the config file
    
    global Logger,lvConnectionPoint,DomainMap
    Logger = frameworkInterface.Logger
    lvConnectionPoint = ConnectPoint
    try:
        Logger.info("Starting LIBVIRT Collector [" + lvConnectionPoint + "]")
        ctx = 'dummy data, not used'
        libvirt.registerErrorHandler(libvirt_error_handler, ctx)			
        Interval = float(frameworkInterface.Interval)/1000.0

        SystemMap = getPlatformInfo() # does not change, no need to do in a loop
        
        for entry in SystemMap:
            frameworkInterface.AddCollector(entry)  

        while not frameworkInterface.KillThreadSignalled():
            getDomainsInfo()
            getCPU_Utilization(Interval/3)
            
            for entry in SystemMap: # need 2 do this every loop to handle 'refresh' messages, even though static data
                frameworkInterface.SetCollectorValue(entry,SystemMap[entry])

            for Domain in DomainMap:
                for entry in DomainMap[Domain]:
                    if not frameworkInterface.DoesCollectorExist(entry): # Do we already have this ID?
                        frameworkInterface.AddCollector(entry)    # Nope, so go add it
                        
                    frameworkInterface.SetCollectorValue(entry,DomainMap[Domain][entry]) 

            time.sleep(Interval)

    except Exception as Ex:
        Logger.error("Uncaught error in LibVirt plugin: " + str(Ex))

#pprint(GatherSetupInfo(lvConnectionPoint))
