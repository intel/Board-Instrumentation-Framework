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
from pprint import pprint as pprint


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
        self._listMap={}  # Keep it around, as member in case I get partial data

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

        if 'other' in configMap:
            self._other = configMap['other']
        
        else:
            self._other = None

        if 'instance' in configMap:
            self._instance = configMap['instance']
        
        else:
            self._instance = None

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

            self._csvSizeDetermined = False

        else:
            self._makeList = False
            self._makeListOnly = False

        if not 'where' in configMap:
            self._where = ""
        
        else:
            self._where = "WHERE " + configMap['where']

    def getMeasurementList(self):
        return self._MeasurementList

    def __getCategories(self,client):
        categories=[]
        for category in client.get_list_measurements():
            categories.append(category['name'])

        return categories

    # gets all the different possible different datapoint ID's for a category
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
        self.__getCategories(dbClient)
        for measurement in self._Measurement:
            if measurement == "*":
                allList = self.__getCategories(dbClient)

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

        if self._makeList:
            self._seriesSizes = self.getMeasurmentListSize(dbClient)

    def createColumnStr(self, keyStr):
        keys = keyStr.split(",")
        retStr = keys[0]
        for colInfo in keys[1:]:
            colName, valueStr = colInfo.split("=")
            try:
                float(valueStr)

            except Exception:
                retStr += colInfo

        return retStr

    def getMeasurmentListSize(self,dbClient):
        seriesMap={}
        for measurement in self._MeasurementList:
            query = "show series from {} ".format(measurement)
            resultSet = dbClient.query(query)    
            #pprint(resultSet)
            seriesList = list(resultSet.get_points())
            for series in seriesList:
                columnsStr = self.createColumnStr(series['key'])

                if not columnsStr in seriesMap: # make a map of unique columns sets, minus the 'instance'
                    seriesMap[columnsStr] = 0

                seriesMap[columnsStr] += 1

        
        return seriesMap


    def queryHistoricalMeasurement(self,filter,measurement,dbClient):
        retMap={}

        if self._makeList:
            if None == self._instance:
                logger.error("Historical Query specified to make csv, but did not provide info on what instance is")
                return (None,None)

        NS=""
        try:
            query = "show series from {} {}".format(measurement,self._where)
            #query = "show series from {}".format(measurement)
            resultSet = dbClient.query(query)    
            resultList = list(resultSet.get_points()) # this should now be a list with length = to the # of instances for this measurement

#            if not self._instance :
#                logger.error("Historical Query specified instance to make csv, but the instance of {} does not exist in measurement {}".format(self._instance,measurement))
#                return (None,None)

            if len(resultList) == 0:
                logger.error("Historical Query specified resulted in no data")
                return (None,None)

            # so now have the instance col name and the # of instances, go to work
            instanceCount = len(resultList)
            keyMap={}
            columns = resultList[0]['key'].split(',')
            
            # make a list of columns, so can make ID
            for pair in columns[1:]:
                key,value = pair.split("=")
                keyMap[key]=value

            ID = measurement
            ListID = ID
            instanceIsPartOfID = False
            
            for id_part in self._id_keys:
                idTag = id_part.evaluate(keyMap)
                if None == idTag:
                    continue

                if id_part.key() == self._instance:
                    idTag="{}" # instance is part of tag, so fill it in later
                    instanceIsPartOfID = True
                else:
                    ListID += self._separator + idTag

                ID += self._separator + idTag

            if None != self._namespace and len(keyMap) > 0:
                NS = self._namespace.evaluate(keyMap)
            else:
                NS = None

            ListID += self._separator + "list"

            if self._makeList:
                if not ListID in retMap:
                    retMap[ListID] = (True,[]) # return Tuple, True means is 'list'

            for inst in range(0,instanceCount):
                #query = "select {} from {} {} and {}='{}' {}".format(self._select,measurement, self._where,self._instance,inst,self._other)
                query = "select {} from {} {}".format(self._select,measurement, self._where)

                logger.info("Querying with: " + query)
                resultList = []

                resultSet = dbClient.query(query)    
                resultList = list(resultSet.get_points())

                if True==instanceIsPartOfID:
                    entryID = ID.format(inst)
                else:
                    entryID = ID

                dataList=[]
                for counter,dp in enumerate(resultList):
                    dataPoint = str(self._value.evaluate(dp))
                    if None == dataPoint:
                        logger.error("Specified value of {} for historical collector not found".format(self._value.key()))
                        return

                    if self._makeList:
                        if inst == 0:
                            retMap[ListID][1].append(dataPoint)
                        else:
                            retMap[ListID][1][counter] += "," + dataPoint

                    if False == self._makeListOnly:
                        dataList.append(dataPoint)


                if not self._makeListOnly:
                    if not entryID in retMap:
                        retMap[entryID] = (False,dataList)

                    else:
                        print("#@##@@#")

        except Exception as Ex:
            logger.error(str(Ex))
            return (None,NS)

        for key in retMap: # in case they specified a Namespace override
            retMap[key] = (retMap[key],NS)

        return retMap

    def queryMeasurement(self,measurement,dbClient):
        query = "select {} from {} {}".format(self._select,measurement, self._where)

        logger.info("Querying with: " + query)
        resultList = []

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
            mapsUpdatedThisLoop=[]
        
        for entry in resultList:
            instanceName = measurement
            
            ID = measurement
            isInstance = False
            isInstanceKey = None

            # if measurement == 'ipmi_value' and entry['type_instance'] == 'Agg Therm Mgn 1 system_board (7.1)':
            #     pprint("-------start-----------")
            #     pprint(entry['time'])
            #     pprint("-------end-------------")
            #     pass

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
                        #csvListLenKey += 
                    
                ID += self._separator + idTag

            if self._makeList and isInstance: # generate a list if a bunch of instances
                instanceName += self._separator + "list"
                isInstanceKey = int(isInstanceKey)

                if not instanceName in self._listMap: # 1st time this list ahs been created
                    if isInstanceKey != 0:
                        self._listMap[instanceName] = [0] * (isInstanceKey +1)
                        continue  # got data from middle of a set (say started getting at CPU #20 out of 88)
                                  # during 1st loop, not likely to occur, but it can

                    else:
                        self._listMap[instanceName] = []


                # if grabbed multiple instances of same data (over a timespan), need to append the list
                if int(isInstanceKey) >= len(self._listMap[instanceName]):
                    self._listMap[instanceName].append(self._value.evaluate(entry))

                else: # otherwise just get latest value
                    self._listMap[instanceName][int(isInstanceKey)] = self._value.evaluate(entry)

                mapsUpdatedThisLoop.append(instanceName) #keep track of what was updated in this query, and only send those
            
            if (isInstance and not self._makeListOnly) or not isInstance:
                retMap[ID] = self._value.evaluate(entry)

        if self._makeList: # Made lists, now let's add to the returning data map
            for listName in mapsUpdatedThisLoop:  # But only the ones changed in this loop
                retMap[listName + self._separator + "size"] = str(len(self._listMap[listName]))
                retMap[listName] = ",".join(self._listMap[listName])

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


# def generateSortedFetchList(keyList):
#     entryMap={}

#     for entry in keyList:
#         keys = entry['key'].split(',')
#         hashKey = keys[0] + "." + entry['key']
#         keyMap={}
#         sortByKey=None
#         for keyVal in keys[1:]:
#             key,value = keyVal.split('=')

#             keyMap[key] = value
#             try:
#                 fvalue = float(value)
#                 sortByKey = key
#             except:
#                 hashKey += value

            
#         if None != sortByKey:
#             hashKey = hashKey.replace(sortByKey + '=' + keyMap[sortByKey],'')

#         if not hashKey in entryMap:
#             entryMap[hashKey] = []

#         else:
#             x = hashKey
#             pass

#         entryMap[hashKey].append((sortByKey,keyMap))

#     for key in entryMap:
#         if not isinstance(entryMap[key],(list,)):
#             continue

#         sortBy, keyMap = entryMap[key][0]

#         #sortedL = sorted(entryMap[key],key=itemgetter(sortBy))
#         pass


#     return entryMap


# def GetDBInfoCollectFunction(frameworkInterface,target,username,password,database):
#     global logger
#     logger = frameworkInterface.Logger
#     logger.info("Starting InfluxDB GetDB Info Collector {0}".format(VersionStr))

#     hostname,port = target.split(":")

#     client = InfluxDBClient(hostname, port, username, password, database)
#     for category in client.get_list_measurements():
#         query = "show series from {} ".format(category['name'])
#         resultSet = client.query(query)    
#         seriesList = list(resultSet.get_points())

#         ID = category['name'] +".columns"        
#         keys = seriesList[0]['key'].split(",")
#         columns = None
#         for colInfo in keys[1:]:
#             colName,junk = colInfo.split("=")
#             if None == columns:
#                 columns = colName
#             else:
#                 columns += ","+colName

#         if not frameworkInterface.DoesCollectorExist(ID): # Do we already have this ID?
#                     frameworkInterface.AddCollector(ID)    # Nope, so go add it, and maybe Custom NS!
#         frameworkInterface.SetCollectorValue(ID,columns)



def HistoryCollectFunction(frameworkInterface,target,username,password,database,interval,**filterList):
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

    try:
        interval = int(interval)
    except:
        raise ValueError("invalid interval specified for InfluxDB HistoryCollectFunction: {}".format(interval))

    hostname,port = target.split(":")

    client = InfluxDBClient(hostname, port, username, password, database)
    dataMap={}

    try:
        for filter in FilterSet:
            filter.determineMeasurementList(client)

            for measurement in filter.getMeasurementList():
                updateData = filter.queryHistoricalMeasurement(filter,measurement,client)
                dataMap.update(updateData)
                if frameworkInterface.KillThreadSignalled():
                    return 

        ## Have not collected all the dataoints
        client.close()
        Done = False

        listIndex = 0 # which LIST in the list of lists for each entry
        while not frameworkInterface.KillThreadSignalled() and not Done:
            updatedCount = 0

            for ID in dataMap:
                isListAndData, Namespace = dataMap[ID]
                isList,Data = isListAndData

                if listIndex >= len(Data):
                    continue

                if not frameworkInterface.DoesCollectorExist(ID,Namespace): # Do we already have this ID?
                    frameworkInterface.AddCollector(ID,Namespace)    # Nope, so go add it, and maybe Custom NS!

                if not isList: # so is a list of list data points, 
                    dVal = Data[listIndex]
                    frameworkInterface.SetCollectorValue(ID,dVal,None,Namespace)
                    updatedCount += 1

                else:
                    frameworkInterface.SetCollectorValue(ID,Data[listIndex],None,Namespace) # is a 'csv list' I created
                    updatedCount += 1

            if 0 == updatedCount:  # went through the entire list
                #Done = True
                pass

            else:
                SleepMs(interval)

            listIndex+=1

        logger.info("Exiting HistoryCollectFunction after {} sets of data sent".format(listIndex))

    except Exception as Ex:
        logger.error("Unrecoverable error in InfluxDB Collector plugin: " + str(Ex))

