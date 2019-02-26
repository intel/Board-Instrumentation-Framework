##############################################################################
#  Copyright (c) 2019 Intel Corporation
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
#    This collector will grab data from an influxDB database.  Collecting from a 
#    database is complicted and takes a lot of parameters. The following is an example
#    <Plugin>
#         <PythonFile>Collectors\InfluxDB.py</PythonFile>
#         <EntryPoint SpawnThread="True">CollectFunction</EntryPoint>
#         <Param>nd-wolfpass-105.jf.intel.com:8086</Param> -- influxDB IP:Port
#         <Param>admin</Param>  -- username
#         <Param>admin</Param>  -- password
#         <Param>collectd</Param> -- name of the DB to connect to
#           Here the next <Params> are any number of named parameters (with unique name) that describes the Measurements to get (influxDB term) must be in { } as below
#         <Param>one=
#                 { 
#                         "measurement": ["*","-upstream_value","-downstream_value","-upstream_tx","-upstream_rx","-downstream_tx","-downstream_rx","-ds_per_cm_value","-turbostat_value"],
#                         "select": "*",
#                         "where": "time > now() - 10s GROUP by type",
#                         "value" : "{value}",
#                         "namespace": "{host}",
#                         "id" : ["{type_instance}","{instance}"],
#                         "separator" : ".",
#                         "name" : "collectd-general",
#                         "csv" : "makelist"
#                 }
#         </Param>
# That will select all measurements '*' from the collecd DB, EXCEPT those listed with a '-' in front of them in "Measurements".
#         <Param>two=
#                 {
#                        "measurement": ["upstream_value","downstream_value","upstream_tx","upstream_rx","downstream_tx","downstream_rx","ds_per_cm_value"],
#                         "select": "*",
#                         "separator" : ".",
#                         "namespace": "{host}",
#                         "id" : ["{host}","{type_instance}","{instance}"],
#                         "value" : "{value}",
#                         "name" : "collectd-general",
#                         "where": "time > now() - 10s GROUP by type"
#                 }
#         </Param>
#     </Plugin>
# </DynamicCollector>   
#
##############################################################################
from collections import Iterable, Sequence 
from influxdb import InfluxDBClient, exceptions
import logging
import ast
import time

VersionStr="19.02.24 Build 1"

logger = object()

# just a wrapper routine for a thread sleeping.  In case needs to be OS specific or something
def Sleep(seconds):
    try:
        time.sleep(seconds)
        
    except BaseException:
        pass

def SleepMs(milliseconds):
    Sleep(float(milliseconds)/1000.0)    

def getCategories(client):
    categories=[]
    for category in client.get_list_measurements():
        categories.append(category['name'])

    return categories

class Entry:
    def __init__(self,key,value):
        self._fixed = True
        self._key = key
        self._value = self._evaluate(value)

    def key(self):
        return self._key

    def value(self):
        return self._value

    def fixed(self):
        return self._fixed

    def get(self):
        return (self._key,self._value,self._fixed)

    def evaluate(self,dataMap):
        if self.fixed():
            return str(self.value())

        if self.key() in dataMap:
            return str(dataMap[self.key()])

        else:
            return None 


    def _evaluate(self,entry) :
        if len(entry) > 3:
            if entry[0] == '{' and entry[-1] == '}' :
                entry = entry[1:-1]
                self._fixed = False
                self._key = entry

        return entry

class Measurement:
    def __init__(self,configMap,name):
        self._MeasurementList = None

        self._Name = name

        if 'separator' in configMap:
            self._separator = configMap['separator']

        else:
            self._separator = "."

        if not 'measurement' in configMap:
            raise ValueError("Measurement must have specified field for identification")

        self._Measurement = []

        for measurement in configMap['measurement']:
            self._Measurement.append(measurement)

        if not 'id' in configMap:
            raise ValueError("Measurement {} did not specify id instructions".format(self._Name))
        
        self._id_keys = []
        for keyEntry in configMap['id']:
            self._id_keys.append(Entry('',keyEntry))

        if not 'value' in configMap:
            raise ValueError("Measurement {} did not specify value".format(self._Name))

        self._value = Entry('value',configMap['value'])

        if not 'select' in configMap:
            raise ValueError("Measurement {} did not select value".format(self._Name))

        self._select = configMap['select']

        if 'namespace' in configMap:
            self._namespace = Entry('namespace',configMap['namespace'])
        
        else:
            self._namespace = None

        if 'csv' in configMap:
            if 'makelist' == configMap['csv']:
                self._makeList = True
                self._makeListOnly = False
                logger.info("Will attempt to make a list of items from influxDB")

            elif 'makelist_only' == configMap['csv']:
                self._makeList = True
                self._makeListOnly = True
                logger.info("Will attempt to make a list of items from influxDB, and exclude the individual datapoints that make up the list")

            else:
                raise ValueError("Measurement {} had invalid csv of {}".format(self._Name,configMap['csv']))

        else:
            self._makeList = False
            self._makeListOnly = False

        if not 'where' in configMap:
            self._where = ""
        
        else:
            self._where = "WHERE " + configMap['where']

    def getCategories(self,client):
        categories=[]
        for category in client.get_list_measurements():
            categories.append(category['name'])

        return categories


    def determineMeasurementList(self,dbClient):
        self._MeasurementList=[]
        localList=[]
        excludeList=[]
        for measurement in self._Measurement:
            if measurement == "*":
                allList = self.getCategories(dbClient)

                for item in allList:
                    if item not in excludeList and item not in localList:
                        localList.append(item)

            elif "-" in measurement and measurement[0] == '-':
                excludeList.append(measurement[1:])
            
            else:
                localList.append(measurement)

        for entry in localList:
            if not entry in excludeList:
                self._MeasurementList.append(entry)
     

    def queryMeasurement(self,measurement,dbClient):
        query = "select {} from {} {}".format(self._select,measurement, self._where)
        #query = "select * from cpu_value where 'instance GROUP BY *".format(self._select,measurement)
        logger.info("Querying with: " + query)
        resultList = []
#        while 0 ==len(resultList):
#            resultSet = dbClient.query(query)    
#            resultList = list(resultSet.get_points())
#            if 0 == len(resultList):
#                SleepMs(100)
        resultSet = dbClient.query(query)    
        resultList = list(resultSet.get_points())

        retMap={}

        if None != self._namespace and len(resultList) > 0:
            NS = self._namespace.evaluate(resultList[0])
        else:
            NS = None

        
        for entry in resultList:
            instanceName = measurement
            ID = measurement
            isInstance = False

            for id_part in self._id_keys:
                idTag = id_part.evaluate(entry)
                if None == idTag:
                    continue

                if self._makeList:
                    try:
                        float(idTag) # if a number, and making a list, then this will not fail
                        isInstance = True
                    except:                        
                        instanceName += self._separator + idTag
                    
                ID += self._separator + idTag

            if self._makeList and isInstance: # generate a list if a bunch of instances
                instanceName += self._separator + "list"
                if not instanceName in retMap: 
                    retMap[instanceName] = ""
                    retMap[instanceName] += self._value.evaluate(entry)

                # if grabbed multiple instances of same data (over a timespan), need to restart the list
                elif instanceName+self._separator +"size" in retMap and int(idTag) < int(retMap[instanceName+self._separator +"size"]): 
                    retMap[instanceName] = ""
                    retMap[instanceName] += self._value.evaluate(entry)
                
                else:
                    retMap[instanceName]+= "," + self._value.evaluate(entry)
                    retMap[instanceName+self._separator +"size"] = idTag # so we can know how many are in list
            
            if (isInstance and not self._makeListOnly) or not isInstance:
                retMap[ID] = self._value.evaluate(entry)
                
        for key in retMap: # in case they specified a Namespace override
            retMap[key] = (retMap[key],NS)

        return retMap

    def performQuery(self,dbClient):
        retMap={}
        if None == self._MeasurementList or 0 == len(self._MeasurementList):
            self.determineMeasurementList(dbClient)

        for measurement in self._MeasurementList:
            retMap.update(self.queryMeasurement(measurement,dbClient))
        
        return retMap

def _ValidateInputFilter(inputStr):
    filter = inputStr.replace('\n', '').replace('\r', '').strip()
    try:
        return ast.literal_eval(filter)
    except Exception as ex:
        raise ValueError("invalid filter specified for InfluxDB collector: " + str(ex))

## Dynamic Collector interface
def CollectFunction(frameworkInterface,target,username,password,database,**filterList):
    global logger
    logger = frameworkInterface.Logger
    logger.info("Starting InfluxDB Collector {0}".format(VersionStr))

    if 0 == len(filterList):
        raise ValueError("No filters specified for InfluxDB collector")

    FilterSet=[]
    for key in filterList:
        filter = filterList[key]
        dbFilter = _ValidateInputFilter(filter)
        FilterSet.append(Measurement(dbFilter,key))

    if not ':' in target:
        raise ValueError("InfluxDB requires 1st Parameter to be IP:Port")

    hostname,port = target.split(":")

    client = InfluxDBClient(hostname, port, username, password, database)

    try:
        while not frameworkInterface.KillThreadSignalled():
            SleepMs(frameworkInterface.Interval)
            dataMap={}

            try:
                for entry in FilterSet:
                    dataMap.update(entry.performQuery(client))

            except exceptions.InfluxDBClientError as Ex:
                logger.error("InfluxDB Collector: {}".format(Ex))
                return

            except exceptions.InfluxDBServerError as Ex:
                logger.error("InfluxDB Collector:  {}".format(Ex))
                return

            except Exception as Ex: #influxDB does NOT have a robust Exception system :-(
                continue

            for ID in dataMap:
                Data, Namespace = dataMap[ID]

                if not frameworkInterface.DoesCollectorExist(ID): # Do we already have this ID?
                    frameworkInterface.AddCollector(ID,Namespace)    # Nope, so go add it, and maybe Custom NS!

                frameworkInterface.SetCollectorValue(ID,Data)

    except Exception as Ex:
        logger.error("Unrecoverable error in InfluxDB Collector plugin: " + str(Ex))


