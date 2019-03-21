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
from operator import itemgetter


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

            if 'csv_sort_by' in configMap:
                self._sortBy = Entry('sortBy',configMap['csv_sort_by'])

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

    def getKeysForCategory(self,client,strMeasurement):
        query = "show series from {} ".format(strMeasurement)
        resultSet = client.query(query)    
        
        seriesList = list(resultSet.get_points())
        return seriesList
        # retList=[]
        # for keyList in seriesList:
        #     entry = {}
        #     keys = keyList['key'].split(',')
        #     for keyVal in keys[1:]:
        #         key,value = keyVal.split('=')
        #         entry[key] = value

        #     retList.append(entry)

        # return retList


      

    # figures out what measurements user wants us to query
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

        # if making lists, create a map of them for updating
        # Need to do this in case the select statement grabs more than
        # one instance of a data point!
        if self._makeList:
            listMap={}
        
        for entry in resultList:
            instanceName = measurement
            ID = measurement
            isInstance = False
            isInstanceKey = None

            for id_part in self._id_keys:
                idTag = id_part.evaluate(entry)
                if None == idTag:
                    continue

                if self._makeList:
                    try:
                        float(idTag) # if a number, and making a list, then this will not fail
                        isInstance = True
                        isInstanceKey = idTag
                    except:                        
                        instanceName += self._separator + idTag
                    
                ID += self._separator + idTag

            if self._makeList and isInstance: # generate a list if a bunch of instances
                instanceName += self._separator + "list"
                if not instanceName in listMap: 
                    listMap[instanceName] = []

                # if grabbed multiple instances of same data (over a timespan), need to restart the list
                # ASSUMING instances start with 0
                if int(isInstanceKey) >= len(listMap[instanceName]):
                    listMap[instanceName].append(self._value.evaluate(entry))

                else:
                    listMap[instanceName][int(isInstanceKey)] = self._value.evaluate(entry)
            
            if (isInstance and not self._makeListOnly) or not isInstance:
                retMap[ID] = self._value.evaluate(entry)

        if self._makeList: # Made lists, now let's add to the returning data map
            for listName in listMap:
                retMap[listName + self._separator + "size"] = str(len(listMap[listName]))
                retMap[listName] = ",".join(listMap[listName])

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
def PointCollectFunction(frameworkInterface,target,username,password,database,**filterList):
    global logger
    logger = frameworkInterface.Logger
    logger.info("Starting InfluxDB Collector {0}".format(VersionStr))

    #GetDBInfoCollectFunction(frameworkInterface,target,username,password,database)

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

    try:
        while not frameworkInterface.KillThreadSignalled():
            SleepMs(frameworkInterface.Interval)
            client = InfluxDBClient(hostname, port, username, password, database)
            dataMap={}

            try:
                for entry in FilterSet:
                    dataMap.update(entry.performQuery(client))

            except exceptions.InfluxDBClientError as Ex:
                logger.error("InfluxDB Collector: {}".format(Ex))
                client.close()
                continue

            except exceptions.InfluxDBServerError as Ex:
                logger.error("InfluxDB Collector:  {}".format(Ex))
                client.close()
                continue

            except Exception as Ex: #influxDB does NOT have a robust Exception system :-(
                logger.info("InfluxDB Collector:  {}".format(Ex))
                client.close()
                continue

            client.close()
            
            for ID in dataMap:
                Data, Namespace = dataMap[ID]

                if not frameworkInterface.DoesCollectorExist(ID,Namespace): # Do we already have this ID?
                    frameworkInterface.AddCollector(ID,Namespace)    # Nope, so go add it, and maybe Custom NS!

                frameworkInterface.SetCollectorValue(ID,Data,None,Namespace)

    except Exception as Ex:
        logger.error("Unrecoverable error in InfluxDB Collector plugin: " + str(Ex))

def GeColumns(frameworkInterface,target,username,password,database,**filterList):
    pass

def generateSortedFetchList(keyList):
    entryMap={}

    for entry in keyList:
        keys = entry['key'].split(',')
        hashKey = keys[0] + "." + entry['key']
        keyMap={}
        sortByKey=None
        for keyVal in keys[1:]:
            key,value = keyVal.split('=')

            keyMap[key] = value
            try:
                fvalue = float(value)
                sortByKey = key
            except:
                hashKey += value

            
        if None != sortByKey:
            hashKey = hashKey.replace(sortByKey + '=' + keyMap[sortByKey],'')

        if not hashKey in entryMap:
            entryMap[hashKey] = []

        else:
            x = hashKey
            pass

        entryMap[hashKey].append((sortByKey,keyMap))

    for key in entryMap:
        if not isinstance(entryMap[key],(list,)):
            continue

        sortBy, keyMap = entryMap[key][0]

        #sortedL = sorted(entryMap[key],key=itemgetter(sortBy))
        pass


    return entryMap


def GetDBInfoCollectFunction(frameworkInterface,target,username,password,database):
    global logger
    logger = frameworkInterface.Logger
    logger.info("Starting InfluxDB GetDB Info Collector {0}".format(VersionStr))

    hostname,port = target.split(":")

    client = InfluxDBClient(hostname, port, username, password, database)
    for category in client.get_list_measurements():
        query = "show series from {} ".format(category['name'])
        resultSet = client.query(query)    
        seriesList = list(resultSet.get_points())

        ID = category['name'] +".columns"        
        keys = seriesList[0]['key'].split(",")
        columns = None
        for colInfo in keys[1:]:
            colName,junk = colInfo.split("=")
            if None == columns:
                columns = colName
            else:
                columns += ","+colName

        if not frameworkInterface.DoesCollectorExist(ID): # Do we already have this ID?
                    frameworkInterface.AddCollector(ID)    # Nope, so go add it, and maybe Custom NS!
        frameworkInterface.SetCollectorValue(ID,columns)



def HistoryCollectFunction(frameworkInterface,target,username,password,database,**filterList):
    global logger
    logger = frameworkInterface.Logger
    logger.info("Starting InfluxDB History Collector {0}".format(VersionStr))

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
    categories=[]
    for category in client.get_list_measurements():
        categories.append(category['name'])

                                # "select": "MEAN(value)", 
                                # "namespace": "nd-wolfpass-105",
                                # "where": ""WHERE time > now() - 24h  GROUP BY time(1h)",
                                # "value" : "{value}",
                                # "id" : ["{type_instance}","{instance}"],        

    #query = "select '*' from 'cpufreq_value' where 'instance' = '0'"
    try:
        strSelect = "*"
        strMeasurement = "cpufreq_value"
        strWhere = "where time > now() - 30s"
        query = "select {} from {} {}".format(strSelect,strMeasurement, strWhere)
        resultSet = client.query(query)    
        resultList = list(resultSet.get_points())

        strWhere = "where time > now() - 30s AND type_instance = '16'"
        query = "select {} from {} {}".format(strSelect,strMeasurement, strWhere)
        resultSet = client.query(query)    
        resultList = list(resultSet.get_points())

        # query = "select COUNT({}) from {} {}".format(strSelect,strMeasurement, strWhere)
        # resultSet = client.query(query)    
        # resultList = list(resultSet.get_points())
        # count = resultList[0]['count_value']

        for filter in FilterSet:
            for cat in filter.getCategories(client):
                keyList = filter.getKeysForCategory(client,cat)
                generateSortedFetchList(keyList)
                pass


        strMeasurement = "cpu_value"
        strSelect = "*"
        strWhere = "WHERE time > now() - 2m"
        strWhere = "WHERE time > now() - 24h"
        strWhere = ""
        query = "show series from {} ".format(strMeasurement)
        query = "show series from {} {}".format(strMeasurement, strWhere)
        resultSet = client.query(query)    
        
        seriesList = list(resultSet.get_points())
        strMeasurement = "cpu_value"
        strSelect = "MEAN(value)"
        strWhere = "WHERE time > now() - 24h"

        for keyList in seriesList:
            query = "select {} from {} {}".format(strSelect,strMeasurement, strWhere)

            keys = keyList['key'].split(',')
            for keyVal in keys[1:]:
                key,value = keyVal.split('=')
                query += " AND {}='{}'".format(key,value)

            query +=  " GROUP BY time(1h) Order by instance"
            resultSet = client.query(query)    
            resultList2 = list(resultSet.get_points())
            print(resultList2)



        strMeasurement = "cpu_value"
        strSelect = "MEAN(value)"
        strWhere = "WHERE time > now() - 24h GROUP BY time(1h)"
        query = "select {} from {} {}".format(strSelect,strMeasurement, strWhere)
        resultSet = client.query(query)    
        x = dir(resultSet)
        resultList2 = list(resultSet.get_points())
        #print(resultList2[0])


    except Exception as Ex:
        print (query)
        print(str(Ex))
        pass

    pass
    return
    try:
        while not frameworkInterface.KillThreadSignalled():
            client = InfluxDBClient(hostname, port, username, password, database)
            SleepMs(frameworkInterface.Interval)
            dataMap={}

            try:
                for entry in FilterSet:
                    dataMap.update(entry.performQuery(client))

            except exceptions.InfluxDBClientError as Ex:
                logger.error("InfluxDB Collector: {}".format(Ex))
                client.close()
                continue

            except exceptions.InfluxDBServerError as Ex:
                logger.error("InfluxDB Collector:  {}".format(Ex))
                client.close()
                continue

            except Exception as Ex: #influxDB does NOT have a robust Exception system :-(
                logger.info("InfluxDB Collector:  {}".format(Ex))
                client.close()
                continue

            client.close()
            
            for ID in dataMap:
                Data, Namespace = dataMap[ID]

                if not frameworkInterface.DoesCollectorExist(ID,Namespace): # Do we already have this ID?
                    frameworkInterface.AddCollector(ID,Namespace)    # Nope, so go add it, and maybe Custom NS!

                frameworkInterface.SetCollectorValue(ID,Data,None,Namespace)

    except Exception as Ex:
        logger.error("Unrecoverable error in InfluxDB Collector plugin: " + str(Ex))

