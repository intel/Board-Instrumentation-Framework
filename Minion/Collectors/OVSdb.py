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
#       Connects to ovsb via JSON to get interface info
##############################################################################

################## Example #####################
#		<DynamicCollector>
#			<Plugin>
#				<PythonFile>Collectors/OVSdb.py</PythonFile>
#				<EntryPoint SpawnThread="True">OVSDB_Collector</EntryPoint>
#				<Param>ip or dns name</Param>
#				<Param>6640</Param>
#				<Param>False</Param>
#			</Plugin>
#		</DynamicCollector>
################## Example #####################

import sys
try:
    import queue # pyhton 3
except:
    import Queue as queue # python 2
import time
import socket
import json
from pprint import pprint

DEFAULT_DB = 'Open_vSwitch'
BUFFER_SIZE = 4096

InterfaceMap = {}
Logger = None

def __read_json_reply(socket,prevData=""):
    global BUFFER_SIZE
    packet = ""
    while "error" not in packet or "id" not in packet or "result" not in packet:
        reply = socket.recv(BUFFER_SIZE).decode('utf-8')
        
        if len(prevData) > 0:
            packet = prevData
            prevData = ""
        packet += reply
        
    try:
       
       data = json.loads(packet)
    except Exception as Ex:
       
        BUFFER_SIZE = BUFFER_SIZE + 2048 # buffer too small, so make larger
        if len(packet) > 16 * 1024:  # was toooo large
           return None
        data = __read_json_reply(socket,packet) # is a stream, and didn't get all the data, so go read more
       
    return data

def LogError(strError):
	global Logger
	if None == Logger:
		pprint(strError)
	else:
		Logger.error(str(Ex))

def monitorMsg(columns, monitor_id=None, db=DEFAULT_DB):
    msg = {"method":"monitor", "params":[db, monitor_id, columns], "id":0}
    return json.dumps(msg)

def Send_Python3(sock,data):
	sock.send(bytes(data,'UTF-8'))

def Send_Python2(sock,data):
	sock.send(bytes(data))

def __IsList(objTest):
	return type(objTest) is type([]) 
	
def __IsMap(objTest):
	return type(objTest) is type({}) 
		
def MakeEntry(name,objData,InterfaceMap,slimDataSet):
    if __IsList(objData):
	    #print(name +" Is a list")
        if len(objData) < 2:
            return

        item = objData[0]
        data = objData[1]
        if __IsMap(data):
            newName = name + "." #+ item
            MakeEntry(newName + ".",data,slimDataSet)

        elif __IsList(data):
            for item in data:
                MakeEntry(name,item,InterfaceMap,slimDataSet)
        else:
            data = str(data)
            if len(data) > 0:
			    #print(name + "." + str(item) + "=" + str(data))
                key = name + "." + str(item)

                if not slimDataSet:
                    InterfaceMap[key] = str(data)
                else:
                    if item == 'rx_bytes' or item == 'tx_bytes' or item == 'rx_packets' or item == 'tx_packets':
                        InterfaceMap[key] = str(data)

    #		for item in objData:
    #			MakeEntry(name,item)

    elif __IsMap(objData):
	    #print(name +" Is a dict")

        for itemKey in objData:
            item = objData[itemKey]
            if type(item) is type(u"unicodestr"):  # python2 difference :-(
               item = str(item)
            if isinstance(item,str):
                if len(item) > 0:
				    #print(name + "." + itemKey + "=" + item)
                    key = name + "." + itemKey
                    if not slimDataSet:
                        InterfaceMap[key] = str(item)
                    else:
                        if item == 'rx_bytes' or item == 'tx_bytes' or item == 'rx_packets' or item == 'tx_packets':
                            InterfaceMap[key] = str(item)

                    #InterfaceMap[key] = str(item)

            else:
                newName = name + "." + itemKey
                MakeEntry(newName,item,InterfaceMap,slimDataSet)

    else:
        LogError("unknown ovsdb item: " + str(objData) + " : " + str(type(objData)))


def HandleInterfaceInfo(iInfo,slimDataSet):
	if len(iInfo) < 1:
		return
	
	#exit()
	#try:
	#error = iInfo['error']
	id = iInfo['id']
	if 'result' in iInfo:
		result = iInfo['result']
	else:
		return False
		
	if 'Interface' in result:
		interface = result['Interface']
	else:
		return False
			
	#except Exception as Ex:
	#	print(str(Ex))
	#	return
	try:
		if len(InterfaceMap) < 1: # go initialize and sort
			for uuid in interface:
				infoState = interface[uuid]
				for infoStateKey in infoState:
					interfaceData = infoState[infoStateKey]
					name = interfaceData['name']			
					InterfaceMap[name] = {}
					
			interfaceIndex = 0
			for ovsName in sorted(InterfaceMap):
				interfaceIndex += 1
				devName = "ovs-interface." + str(interfaceIndex)
				#InterfaceMap[ovsName][devName] = ovsName
			
		#pprint(interface)
		for uuid in interface:
			infoState = interface[uuid]
#			pprint(infoState)
			
			for infoStateKey in infoState:
				interfaceData = infoState[infoStateKey]
				name = interfaceData['name']
				MakeEntry('ovsdb.' + name,interfaceData,InterfaceMap[name],slimDataSet)
				
					
	except Exception as Ex:
		LogError(Ex)
		return False
		
	return True
	

def DumpEntries():
	itemCount = 0
	
	for entry in sorted(InterfaceMap):
		print(entry)
		for key in sorted(InterfaceMap[entry]):
			value = InterfaceMap[entry][key]
			print("  " + key + "=" + value)
			itemCount +=1
			
	print("Interface Count: " + str(len(InterfaceMap)) + ". Item Count: " + str(itemCount))
		
# this is the Setup script to call to go collect data from the DB
# returns a map of aliases to be used by setup script
def GatherSetupInfo(IP, Port):
    Port = int(Port)
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    #columns =
    #{"Interface":{"columns":["name","link_state","status","type","mac_in_use","other_config","statistics","external_ids"]}}
    #columns =
    #{"Interface":{"columns":["name","link_state","status","type","mac_in_use","other_config","statistics"]}}
    columns = {"Interface":{"columns":["name","link_state"]}}
    try:       
        #print("Connecting")
        sock.connect((IP, Port))		
    except Exception as Ex:
        raise Exception("OVSDB - cannot connect to: " + IP + "[" + str(Port) + "]")

    response = ""
    counter = 0
    if sys.version_info < (3, 0):
        Send = Send_Python2
    else:
        Send = Send_Python3

    while len(response) < 1 and counter < 100:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.connect((IP, Port))				
        Send(sock,monitorMsg(columns, 1))
        response = __read_json_reply(sock)
        counter += 1
        sock.close()
        if len(response) < 1:		
            time.sleep(.1)
        
    if len(response) > 0:
        HandleInterfaceInfo(response,False)
        #DumpEntries()
        AliasMap = {}
        
        for index,entry in enumerate(sorted(InterfaceMap)):
            devIndex = index + 1
            id = "ovsdb.interface-" + str(devIndex)
            AliasMap[id] = str(entry)

        #pprint(AliasMap)
        return AliasMap

    raise Exception("Unable to gather data from OVSDB")
    

# this is the entry point.  Makes JSON calls to OVSDB, makes collectors
# out of the responses
def OVSDB_Collector(frameworkInterface, IP, Port, slimDataset): 
    # frameworkInterface has the following:
    #  frameworkInterface.DoesCollectorExist(ID) # does a collector with ID
    #  already exist
    #  frameworkInterface.AddCollector(ID) # Add a new collectr
    #  frameworkInterface.SetCollectorValue(ID,Value,ElapsedTime) # Assign the
    #  collector a new value, along with how long since last update
    #  frameworkInterface.KillThreadSignalled() # returns True if signalled to
    #  end your worker thread, else False
    #  frameworkInterface.LockFileName() # lockfile name for dynamic collector,
    #  if specified
    #  frameworkInterface.Interval() # the frequency from the config file
    # ovs-vsctl set-manager ptcp:6634
    global Logger
    Logger = frameworkInterface.Logger
    Logger.info("Starting OVSDB JSON Collector:" + IP + " [" + Port + "]")

    if sys.version_info < (3, 0):
        Send = Send_Python2
    else:
        Send = Send_Python3

    try:
        Port = int(Port)
        if slimDataset.lower() == "true":
            slimDataset = True
        else:
            slimDataset = False

        #### Columns to fetch ###
        #columns =
        #{"Interface":{"columns":["name","link_state","status","type","mac_in_use","other_config","statistics","external_ids"]}}
        if not slimDataset:
            columns = {"Interface":{"columns":["name","link_state","status","type","mac_in_use","other_config","statistics","external_ids"]}}
        else:
            columns = {"Interface":{"columns":["name","statistics"]}}

        #columns = {"Interface":{"columns":["name","link_state"]}}
        
        while not frameworkInterface.KillThreadSignalled():
            try:       
                sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                sock.connect((IP, Port))
            except Exception as Ex:
                Logger.error("OVSDB Collector - cannot connect to: " + IP + "[" + str(Port) + "]")
                Logger.error(str(Ex))
                return

            Send(sock,monitorMsg(columns, 1))
            response = __read_json_reply(sock)

            if len(response) > 0:
                HandleInterfaceInfo(response,slimDataset)
                
                for Interface in InterfaceMap:
                    for entry in InterfaceMap[Interface]:
                        if not frameworkInterface.DoesCollectorExist(entry): # Do we already have this ID?
                            frameworkInterface.AddCollector(entry)    # Nope, so go add it
                        
                        frameworkInterface.SetCollectorValue(entry,InterfaceMap[Interface][entry]) 

            time.sleep(float(frameworkInterface.Interval) / 1000.0)
            sock.close()

    except Exception as ex:
        Logger.error("Unrecoverable error in OVSDB plugin: " + str(ex))

