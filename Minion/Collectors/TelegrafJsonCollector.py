##############################################################################
#  Copyright (c) 2020 Intel Corporation
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
#    You can point a Telegraf outputs.socket_writer  in JSON format to this plugin 
##############################################################################

import socket
import json
from pprint import pprint as pprint
Logger = None

def __WalkMap(dataMap, frameworkInterface,useDataNamespace,prefix=""):
    Logger = frameworkInterface.Logger

    try:
        dataSet = dataMap['name']
        dataFields = dataMap['fields']
        hostname =  dataMap['tags']['host']
        instance=""
        if len(dataMap['tags']) > 1: # must be instance
            for key in dataMap['tags'].keys(): # Not much I can do about the order
                if 'host' != key :
                    instance += dataMap['tags'][key] + "."
                    
    except:
        Logger.error("Unexpected JSON format for Telegraf Json Collector: " + dataMap)
        return 

    if True == useDataNamespace:
        Namespace = hostname

    else:
        Namespace = None

    for dataName in dataFields:
        value = dataFields[dataName]

        ID = dataSet + "." + instance + dataName

        if not frameworkInterface.DoesCollectorExist(ID,Namespace): # Do we already have this ID?
            frameworkInterface.AddCollector(ID,Namespace)    # Nope, so go add it        
    
        else:
            frameworkInterface.SetCollectorValue(ID,value,None,Namespace) # update the value with the ID, let the framework handle the interval

# <DynamicCollector>
#     <Plugin>
#         <PythonFile>Collectors\TelegrafJsonCollector.py</PythonFile>
#         <EntryPoint SpawnThread="True">CollectFunction</EntryPoint>
#         <Param>PORT=50002</Param>         REQUIRED, what port to listen on
#         <Param>IP=0.0.0.0</Param>         OPTIONAL, default is 0.0.0.0
#         <Param>USE_DATA_NAMESPACE=True</Param>  OPTIONAL, default is True and will get namespace from the json data, if false, will use Minion defined Namespace
#     </Plugin>
# </DynamicCollector>
# Entrypoint for the collector.
def CollectFunction(frameworkInterface, **kwargs): 
    Logger = frameworkInterface.Logger

    if 'IP' in kwargs:
        IP = kwargs['IP']
    else:
        IP='0.0.0.0'
        Logger.info("Telegraph JSON Collector: no IP specified, binding to all devices")        
    
    if not 'PORT' in kwargs:
        raise ValueError("Telegraph JSON Collector required PORT parameter")

    useDataNamespace=True
    if 'USE_DATA_NAMESPACE' in kwargs:
        if kwargs['USE_DATA_NAMESPACE'].lower() == 'false':
            useDataNamespace=False 

    try:
        PORT = int(kwargs['PORT'])
    except:
        raise ValueError("Telegraph JSON Collector invalid PORT parameter: " + str(PORT))

    averageData = False

    Logger.info("Starting Telegraph JSON Collector:" + IP + " [" + str(PORT) + "] Averaging data: " + str(averageData))
    try:       
        recvSocket = socket.socket(socket.AF_INET,socket.SOCK_DGRAM)
        recvSocket.bind((IP,PORT))
        recvSocket.setblocking(True)
        recvSocket.settimeout(0.01)
    except Exception as Ex:
        Logger.error("Error setting up Telegraph JSON Collector: " + str(Ex))
        return

    while not frameworkInterface.KillThreadSignalled():
        try:
            data, _ = recvSocket.recvfrom(81920)
            data = data.strip().decode("utf-8")
            dataMap = json.loads(data)
            __WalkMap(dataMap,frameworkInterface,useDataNamespace)

        except socket.error:
            continue

        except Exception as Ex:
            Logger.error("Telegraph JSON Collector catastrophic error: " + str(Ex))


            
